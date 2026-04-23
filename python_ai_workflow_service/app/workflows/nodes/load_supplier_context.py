import json
from typing import Any

from app.clients.inventory_backend import InventoryBackendClient
from app.repositories.session_store import SessionStore
from app.workflows.state import WorkflowStateKeys


class LoadSupplierContextNode:
    def __init__(self, backend: InventoryBackendClient, session_store: SessionStore):
        self.backend = backend
        self.session_store = session_store

    async def __call__(self, state: dict) -> dict:
        entity = dict(state.get(WorkflowStateKeys.ENTITY, {}) or {})
        thread_id = str(state.get(WorkflowStateKeys.THREAD_ID, ""))
        authorization = str(state.get(WorkflowStateKeys.AUTHORIZATION, ""))
        supplier_id = entity.get("supplierId")
        days = int(entity.get("days") or 30)

        if supplier_id is None:
            return {WorkflowStateKeys.ERROR_MESSAGE: "未识别到供应商ID"}

        context = await self.backend.get_agent_supplier_context(int(supplier_id), days, authorization)
        self.session_store.save_tool_message(thread_id, "loadSupplierContext", self._json(entity), self._json(context))

        if not context or not context.get("exists"):
            return {WorkflowStateKeys.ERROR_MESSAGE: "供应商不存在"}

        return {
            WorkflowStateKeys.SUPPLIER_CONTEXT: context,
            WorkflowStateKeys.SUPPLIER_METRICS: context.get("metrics") or {},
        }

    def _json(self, value: Any) -> str:
        return json.dumps(value, ensure_ascii=False, default=str)
