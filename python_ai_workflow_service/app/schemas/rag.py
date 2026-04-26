from pydantic import Field

from app.schemas.common import ApiModel


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
