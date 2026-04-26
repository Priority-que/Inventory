import hashlib
import re
import struct
from typing import Any

import httpx
import redis.asyncio as redis
from redis.exceptions import ResponseError

from app.core.config import get_settings
from app.core.exceptions import ApiException
from app.schemas.rag import RagImportResultVO, RagKnowledgeImportRequest, RagSearchRequest, RagSearchResultVO


class RagService:
    CHUNK_SIZE = 500
    CHUNK_OVERLAP = 80
    DOC_CODE_PATTERN = re.compile(r"^[A-Z0-9_\-]{3,64}$")

    def __init__(self):
        self.settings = get_settings()
        self.redis = redis.Redis(
            host=self.settings.redis_host,
            port=self.settings.redis_port,
            db=self.settings.redis_db,
            password=self.settings.redis_password or None,
            socket_timeout=self.settings.redis_socket_timeout,
            decode_responses=False,
        )
        self._vector_dim: int | None = None

    async def import_knowledge(self, request: RagKnowledgeImportRequest) -> RagImportResultVO:
        doc_code = self._validate_doc_code(request.doc_code)
        title = self._require_text(request.title, "title")
        content = self._require_text(request.content, "content")
        doc_type = self._default_text(request.doc_type, "BUSINESS_RULE")
        biz_intent = self._default_text(request.biz_intent, "COMMON")
        source_path = self._default_text(request.source_path, "manual")

        chunks = self._split_content(content)
        if not chunks:
            raise ApiException(code=500, msg="content不能为空", http_status_code=500)

        embeddings = await self._embed_texts(chunks)
        if not embeddings:
            raise ApiException(code=500, msg="向量生成失败", http_status_code=500)

        vector_dim = len(embeddings[0])
        await self._ensure_index(vector_dim)
        await self._delete_doc_chunks(doc_code)

        pipe = self.redis.pipeline(transaction=False)
        for index, (chunk, embedding) in enumerate(zip(chunks, embeddings, strict=True), start=1):
            key = self._build_chunk_key(doc_code, index, chunk)
            pipe.hset(
                key,
                mapping={
                    "docCode": doc_code,
                    "title": title,
                    "docType": doc_type,
                    "bizIntent": biz_intent,
                    "sourcePath": source_path,
                    "chunkNo": str(index),
                    "content": chunk,
                    "embedding": self._vector_to_bytes(embedding),
                },
            )
        await pipe.execute()

        return RagImportResultVO(
            docCode=doc_code,
            title=title,
            bizIntent=biz_intent,
            chunkCount=len(chunks),
            message="知识导入成功",
        )

    async def search(self, request: RagSearchRequest) -> list[RagSearchResultVO]:
        return await self.search_internal(
            request.query or "",
            request.biz_intent,
            request.top_k or self.settings.rag_default_top_k,
            authorization="",
        )

    async def search_internal(
        self,
        query: str,
        biz_intent: str | None,
        top_k: int,
        authorization: str = "",
    ) -> list[RagSearchResultVO]:
        if not query or not query.strip():
            return []

        query_embedding = (await self._embed_texts([query.strip()]))[0]
        await self._ensure_index(len(query_embedding))

        normalized_top_k = self._normalize_top_k(top_k)
        redis_query = self._build_redis_query(biz_intent, normalized_top_k)

        try:
            raw = await self.redis.execute_command(
                "FT.SEARCH",
                self.settings.rag_index_name,
                redis_query,
                "PARAMS",
                4,
                "topK",
                normalized_top_k,
                "vector",
                self._vector_to_bytes(query_embedding),
                "RETURN",
                8,
                "docCode",
                "title",
                "docType",
                "bizIntent",
                "sourcePath",
                "chunkNo",
                "content",
                "score",
                "SORTBY",
                "score",
                "ASC",
                "DIALECT",
                2,
            )
        except ResponseError as exc:
            message = str(exc)
            if "no such index" in message.lower() or "unknown index" in message.lower():
                return []
            raise

        rows = self._parse_search_rows(raw)
        threshold = self.settings.rag_similarity_threshold
        return [row for row in rows if row.score is None or row.score <= threshold]

    async def _embed_texts(self, texts: list[str]) -> list[list[float]]:
        if not self._is_embedding_configured():
            raise ApiException(
                code=500,
                msg="Python RAG 未配置 MODEL_API_KEY / AI_DASHSCOPE_API_KEY",
                http_status_code=500,
            )

        url = self.settings.model_base_url.rstrip("/") + "/embeddings"
        payload = {
            "model": self.settings.rag_embedding_model,
            "input": texts,
        }
        headers = {
            "Authorization": f"Bearer {self._api_key()}",
            "Content-Type": "application/json",
        }

        try:
            async with httpx.AsyncClient(timeout=self.settings.rag_embedding_timeout, trust_env=False) as client:
                response = await client.post(url, headers=headers, json=payload)
        except httpx.RequestError as exc:
            raise ApiException(
                code=500,
                msg=f"Python RAG 请求 embedding 服务失败：{exc.__class__.__name__}: {exc}",
                http_status_code=500,
            ) from exc

        if response.status_code >= 400:
            raise ApiException(
                code=500,
                msg=f"Python RAG embedding 服务 HTTP 异常：{response.status_code} {response.text}",
                http_status_code=500,
            )

        body = response.json()
        data = body.get("data") or []
        if not data:
            raise ApiException(code=500, msg=f"Python RAG embedding 响应缺少 data：{body}", http_status_code=500)

        data = sorted(data, key=lambda item: int(item.get("index", 0)))
        embeddings: list[list[float]] = []
        for item in data:
            embedding = item.get("embedding")
            if not isinstance(embedding, list) or not embedding:
                raise ApiException(code=500, msg=f"Python RAG embedding 响应非法：{body}", http_status_code=500)
            embeddings.append([float(value) for value in embedding])
        return embeddings

    def _is_embedding_configured(self) -> bool:
        api_key = self._api_key()
        return bool(api_key) and api_key not in (
            "replace-with-your-api-key",
            "replace-with-your-dashscope-api-key",
        )

    def _api_key(self) -> str:
        return self.settings.model_api_key or self.settings.ai_dashscope_api_key

    async def _ensure_index(self, vector_dim: int) -> None:
        if self._vector_dim == vector_dim:
            return

        meta_key = self._meta_key("dimension")
        stored_dim = await self.redis.get(meta_key)
        if stored_dim:
            stored_dim_int = int(stored_dim)
            if stored_dim_int != vector_dim:
                raise ApiException(
                    code=500,
                    msg=f"Redis RAG 索引维度不一致：当前索引 {stored_dim_int}，本次向量 {vector_dim}",
                    http_status_code=500,
                )

        try:
            await self.redis.execute_command("FT.INFO", self.settings.rag_index_name)
            await self.redis.set(meta_key, vector_dim)
            self._vector_dim = vector_dim
            return
        except ResponseError as exc:
            message = str(exc).lower()
            if "unknown index" not in message and "no such index" not in message:
                raise

        try:
            await self.redis.execute_command(
                "FT.CREATE",
                self.settings.rag_index_name,
                "ON",
                "HASH",
                "PREFIX",
                1,
                self.settings.rag_key_prefix,
                "SCHEMA",
                "docCode",
                "TAG",
                "title",
                "TEXT",
                "docType",
                "TAG",
                "bizIntent",
                "TAG",
                "sourcePath",
                "TEXT",
                "chunkNo",
                "NUMERIC",
                "content",
                "TEXT",
                "embedding",
                "VECTOR",
                "HNSW",
                6,
                "TYPE",
                "FLOAT32",
                "DIM",
                vector_dim,
                "DISTANCE_METRIC",
                "COSINE",
            )
        except ResponseError as exc:
            message = str(exc)
            if "Index already exists" not in message:
                raise ApiException(
                    code=500,
                    msg=f"Redis Stack RAG 索引创建失败，请确认 Redis 已启用 RediSearch：{message}",
                    http_status_code=500,
                ) from exc

        await self.redis.set(meta_key, vector_dim)
        self._vector_dim = vector_dim

    async def _delete_doc_chunks(self, doc_code: str) -> None:
        pattern = f"{self.settings.rag_key_prefix}{doc_code}:*"
        keys: list[bytes] = []
        async for key in self.redis.scan_iter(match=pattern, count=200):
            keys.append(key)

        if keys:
            await self.redis.delete(*keys)

    def _build_redis_query(self, biz_intent: str | None, top_k: int) -> str:
        vector_clause = f"=>[KNN $topK @embedding $vector AS score]"
        if not biz_intent or not biz_intent.strip():
            return f"*{vector_clause}"

        intent = self._escape_tag_value(biz_intent.strip())
        return f"(@bizIntent:{{COMMON|{intent}}}){vector_clause}"

    def _parse_search_rows(self, raw: Any) -> list[RagSearchResultVO]:
        if not isinstance(raw, list) or len(raw) <= 1:
            return []

        results: list[RagSearchResultVO] = []
        for index in range(1, len(raw), 2):
            key = self._decode(raw[index])
            fields = raw[index + 1] if index + 1 < len(raw) else []
            row = self._fields_to_dict(fields)
            row["id"] = key
            score = row.get("score")
            results.append(
                RagSearchResultVO(
                    id=key,
                    docCode=row.get("docCode"),
                    title=row.get("title"),
                    docType=row.get("docType"),
                    bizIntent=row.get("bizIntent"),
                    sourcePath=row.get("sourcePath"),
                    chunkNo=self._to_int(row.get("chunkNo")),
                    content=row.get("content"),
                    score=self._to_float(score),
                )
            )
        return results

    def _fields_to_dict(self, fields: Any) -> dict[str, str]:
        result: dict[str, str] = {}
        if not isinstance(fields, list):
            return result

        for index in range(0, len(fields), 2):
            key = self._decode(fields[index])
            value = self._decode(fields[index + 1]) if index + 1 < len(fields) else ""
            result[key] = value
        return result

    def _split_content(self, content: str) -> list[str]:
        text = content.replace("\r\n", "\n").strip()
        chunks: list[str] = []
        start = 0

        while start < len(text):
            end = min(start + self.CHUNK_SIZE, len(text))
            split_end = self._find_split_position(text, start, end)
            chunk = text[start:split_end].strip()
            if chunk:
                chunks.append(chunk)
            if split_end >= len(text):
                break
            next_start = split_end - self.CHUNK_OVERLAP
            start = split_end if next_start <= start else next_start
        return chunks

    def _find_split_position(self, text: str, start: int, end: int) -> int:
        if end >= len(text):
            return len(text)

        min_split = start + self.CHUNK_SIZE // 2
        newline = text.rfind("\n", start, end)
        if newline > min_split:
            return newline

        for index in range(end - 1, min_split, -1):
            if text[index] in ("。", "；", ";", "."):
                return index + 1
        return end

    def _build_chunk_key(self, doc_code: str, chunk_no: int, content: str) -> str:
        digest = hashlib.sha256(content.encode("utf-8")).hexdigest()[:16]
        return f"{self.settings.rag_key_prefix}{doc_code}:{chunk_no}:{digest}"

    def _meta_key(self, name: str) -> str:
        return f"{self.settings.rag_key_prefix}meta:{name}"

    def _vector_to_bytes(self, vector: list[float]) -> bytes:
        return struct.pack(f"{len(vector)}f", *vector)

    def _normalize_top_k(self, top_k: int | None) -> int:
        if top_k is None or top_k <= 0:
            return self.settings.rag_default_top_k
        return min(top_k, self.settings.rag_max_top_k)

    def _validate_doc_code(self, value: str | None) -> str:
        doc_code = self._require_text(value, "docCode")
        if not self.DOC_CODE_PATTERN.match(doc_code):
            raise ApiException(
                code=500,
                msg="docCode只能包含大写字母、数字、下划线和中划线，长度3到64位",
                http_status_code=500,
            )
        return doc_code

    def _require_text(self, value: str | None, field_name: str) -> str:
        if not value or not value.strip():
            raise ApiException(code=500, msg=f"{field_name}不能为空", http_status_code=500)
        return value.strip()

    def _default_text(self, value: str | None, default_value: str) -> str:
        return value.strip() if value and value.strip() else default_value

    def _escape_tag_value(self, value: str) -> str:
        return re.sub(r"([,{}\|\s])", r"\\\1", value)

    def _decode(self, value: Any) -> str:
        if isinstance(value, bytes):
            return value.decode("utf-8", errors="ignore")
        return "" if value is None else str(value)

    def _to_int(self, value: str | None) -> int | None:
        if value is None or value == "":
            return None
        try:
            return int(value)
        except ValueError:
            return None

    def _to_float(self, value: str | None) -> float | None:
        if value is None or value == "":
            return None
        try:
            return float(value)
        except ValueError:
            return None
