from app.clients.inventory_backend import InventoryBackendClient
from app.schemas.rag import RagSearchRequest, RagSearchResultVO


class RagService:
    def __init__(self, backend: InventoryBackendClient):
        self.backend = backend

    async def search_internal(
        self,
        query: str,
        biz_intent: str | None,
        top_k: int,
        authorization: str,
    ) -> list[RagSearchResultVO]:
        request = RagSearchRequest(query=query, bizIntent=biz_intent, topK=top_k)
        rows = await self.backend.rag_search(request.model_dump(by_alias=True), authorization)
        return [RagSearchResultVO(**row) for row in rows]
