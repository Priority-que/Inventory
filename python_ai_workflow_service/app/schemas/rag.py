from pydantic import Field

from app.schemas.common import ApiModel


class RagKnowledgeImportRequest(ApiModel):
    doc_code: str | None = Field(default=None, alias="docCode")
    title: str | None = None
    doc_type: str | None = Field(default=None, alias="docType")
    biz_intent: str | None = Field(default=None, alias="bizIntent")
    source_path: str | None = Field(default=None, alias="sourcePath")
    content: str | None = None


class RagImportResultVO(ApiModel):
    doc_code: str | None = Field(default=None, alias="docCode")
    title: str | None = None
    biz_intent: str | None = Field(default=None, alias="bizIntent")
    chunk_count: int | None = Field(default=None, alias="chunkCount")
    message: str | None = None


class RagSearchRequest(ApiModel):
    query: str | None = None
    biz_intent: str | None = Field(default=None, alias="bizIntent")
    top_k: int | None = Field(default=None, alias="topK")


class RagSearchResultVO(ApiModel):
    id: str | None = None
    doc_code: str | None = Field(default=None, alias="docCode")
    title: str | None = None
    doc_type: str | None = Field(default=None, alias="docType")
    biz_intent: str | None = Field(default=None, alias="bizIntent")
    source_path: str | None = Field(default=None, alias="sourcePath")
    chunk_no: int | None = Field(default=None, alias="chunkNo")
    content: str | None = None
    score: float | None = None
