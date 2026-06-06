from typing import Any

from app.agent_v2.schemas import AgentPlan, ToolCallResult
from app.clients.inventory_backend import InventoryBackendClient
from app.services.rag_service import RagService


class ToolRunner:
    def __init__(self, backend: InventoryBackendClient, rag_service: RagService):
        self.backend = backend
        self.rag_service = rag_service

    async def run(self, plan: AgentPlan, authorization: str) -> list[ToolCallResult]:
        if not plan.can_execute or not plan.tool_names:
            return []

        results: list[ToolCallResult] = []
        for tool_name in plan.tool_names:
            results.append(await self._run_one(tool_name, plan, authorization))
        return results

    async def _run_one(self, tool_name: str, plan: AgentPlan, authorization: str) -> ToolCallResult:
        request = self._request_for_tool(tool_name, plan)
        try:
            data = await self._call_tool(tool_name, plan, authorization)
            return ToolCallResult(toolName=tool_name, success=True, request=request, data=self._serialize(data))
        except Exception as exc:
            return ToolCallResult(
                toolName=tool_name,
                success=False,
                request=request,
                data=None,
                error=f"{exc.__class__.__name__}: {exc}",
            )

    async def _call_tool(self, tool_name: str, plan: AgentPlan, authorization: str) -> Any:
        slots = plan.slots
        if tool_name == "get_order_context":
            return await self.backend.get_agent_order_context(str(slots.order_no), authorization)
        if tool_name == "scan_warning_context":
            return await self.backend.get_agent_warning_context(int(slots.days or 7), authorization)
        if tool_name == "get_supplier_context":
            return await self.backend.get_agent_supplier_context(int(slots.supplier_id), int(slots.days or 30), authorization)
        if tool_name == "search_knowledge":
            return await self.rag_service.search_internal(slots.query or "", plan.task, 4, authorization)
        raise ValueError(f"未知工具：{tool_name}")

    def _request_for_tool(self, tool_name: str, plan: AgentPlan) -> dict[str, Any]:
        slots = plan.slots
        if tool_name == "get_order_context":
            return {"orderNo": slots.order_no}
        if tool_name == "scan_warning_context":
            return {"days": slots.days or 7}
        if tool_name == "get_supplier_context":
            return {"supplierId": slots.supplier_id, "days": slots.days or 30}
        if tool_name == "search_knowledge":
            return {"query": slots.query, "bizIntent": plan.task, "topK": 4}
        return {}

    def _serialize(self, value: Any) -> Any:
        if isinstance(value, list):
            return [self._serialize(item) for item in value]
        if isinstance(value, dict):
            return {key: self._serialize(item) for key, item in value.items()}
        model_dump = getattr(value, "model_dump", None)
        if callable(model_dump):
            return model_dump(by_alias=True)
        return value
