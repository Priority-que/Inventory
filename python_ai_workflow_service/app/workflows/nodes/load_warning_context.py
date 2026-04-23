import json
from typing import Any

from app.clients.inventory_backend import InventoryBackendClient
from app.repositories.session_store import SessionStore
from app.workflows.state import WorkflowStateKeys


class LoadWarningContextNode:
    def __init__(self, backend: InventoryBackendClient, session_store: SessionStore):
        self.backend = backend
        self.session_store = session_store

    async def __call__(self, state: dict) -> dict:
        entity = dict(state.get(WorkflowStateKeys.ENTITY, {}) or {})
        thread_id = str(state.get(WorkflowStateKeys.THREAD_ID, ""))
        authorization = str(state.get(WorkflowStateKeys.AUTHORIZATION, ""))
        days = int(entity.get("days") or 7)

        context = await self.backend.get_agent_warning_context(days, authorization)
        self.session_store.save_tool_message(thread_id, "loadWarningContext", self._json({"days": days}), self._json(context))

        if not context:
            return {WorkflowStateKeys.ERROR_MESSAGE: "预警上下文为空"}

        return {WorkflowStateKeys.WARNING_CONTEXT: context}

    def _json(self, value: Any) -> str:
        return json.dumps(value, ensure_ascii=False, default=str)
