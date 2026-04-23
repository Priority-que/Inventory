import json
from typing import Any

from app.clients.inventory_backend import InventoryBackendClient
from app.repositories.session_store import SessionStore
from app.workflows.state import WorkflowStateKeys


class LoadOrderContextNode:
    def __init__(self, backend: InventoryBackendClient, session_store: SessionStore):
        self.backend = backend
        self.session_store = session_store

    async def __call__(self, state: dict) -> dict:
        entity = dict(state.get(WorkflowStateKeys.ENTITY, {}) or {})
        thread_id = str(state.get(WorkflowStateKeys.THREAD_ID, ""))
        authorization = str(state.get(WorkflowStateKeys.AUTHORIZATION, ""))
        order_no = entity.get("orderNo")

        if not order_no:
            return {WorkflowStateKeys.ERROR_MESSAGE: "未识别采购订单号"}

        context = await self.backend.get_agent_order_context(str(order_no), authorization)
        self.session_store.save_tool_message(thread_id, "loadOrderContext", self._json(entity), self._json(context))

        if not context or not context.get("exists"):
            return {WorkflowStateKeys.ERROR_MESSAGE: "采购订单号不存在"}

        order = context.get("order") or {}
        return {
            WorkflowStateKeys.ORDER_CONTEXT: context,
            WorkflowStateKeys.ORDER_SNAPSHOT: {
                "orderId": order.get("orderId"),
                "orderNo": order.get("orderNo"),
                "status": order.get("status"),
                "statusName": order.get("statusName"),
                "supplierId": order.get("supplierId"),
                "supplierName": order.get("supplierName"),
                "totalOrderNumber": order.get("totalOrderNumber", 0),
                "totalArriveNumber": order.get("totalArriveNumber", 0),
                "totalInboundNumber": order.get("totalInboundNumber", 0),
                "arrivalCount": order.get("arrivalCount", 0),
                "inboundCount": order.get("inboundCount", 0),
            },
        }

    def _json(self, value: Any) -> str:
        return json.dumps(value, ensure_ascii=False, default=str)
