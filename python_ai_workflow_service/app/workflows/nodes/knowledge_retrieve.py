from app.services.rag_service import RagService
from app.workflows.state import InteractionType, WorkflowStateKeys


class KnowledgeRetrieveNode:
    def __init__(self, rag_service: RagService):
        self.rag_service = rag_service

    async def __call__(self, state: dict) -> dict:
        interaction_type = str(state.get(WorkflowStateKeys.INTERACTION_TYPE, InteractionType.BUSINESS.value))
        intent = str(state.get(WorkflowStateKeys.INTENT, "UNKNOWN"))
        message = str(state.get(WorkflowStateKeys.NORMALIZED_MESSAGE, ""))
        authorization = str(state.get(WorkflowStateKeys.AUTHORIZATION, ""))

        if interaction_type != InteractionType.BUSINESS.value or intent == "UNKNOWN":
            return {WorkflowStateKeys.RAG_DOCS: ""}

        try:
            hits = await self.rag_service.search_internal(message, intent, 4, authorization)
            docs = self._build_rag_docs(hits, intent)
        except Exception:
            docs = self._fallback_docs(intent)

        return {WorkflowStateKeys.RAG_DOCS: docs}

    def _build_rag_docs(self, hits, intent: str) -> str:
        if not hits:
            return self._fallback_docs(intent)

        parts = ["以下内容来自知识库检索结果，只能作为业务规则参考，不能替代数据库实时业务数据：\n"]
        for index, hit in enumerate(hits, start=1):
            parts.append(f"【资料{index}】{hit.title}，相似度：{hit.score}\n{hit.content}\n")
        return "\n".join(parts)

    def _fallback_docs(self, intent: str) -> str:
        if intent == "ORDER_DIAGNOSIS":
            return "采购订单状态规则：WAIT_CONFIRM 待确认，IN_PROGRESS 执行中，PARTIAL_ARRIVAL 部分到货，COMPLETED 已完成。"
        if intent == "WARNING_SCAN":
            return "采购执行预警规则：待确认超时、到货停滞、待入库超时均应进入预警列表。"
        if intent == "SUPPLIER_SCORE":
            return "供应商评分规则：确认及时率、到货完成率、入库完成率、异常到货率共同影响评分。"
        return ""
