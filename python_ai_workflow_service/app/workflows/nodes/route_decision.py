from app.workflows.state import InteractionType, WorkflowIntent, WorkflowStateKeys


class RouteDecisionNode:
    async def __call__(self, state: dict) -> dict:
        interaction_type = str(state.get(WorkflowStateKeys.INTERACTION_TYPE, InteractionType.BUSINESS.value))
        intent = str(state.get(WorkflowStateKeys.INTENT, WorkflowIntent.UNKNOWN.value))
        plan = dict(state.get(WorkflowStateKeys.ANSWER_PLAN, {}) or {})
        scope_status = str(state.get(WorkflowStateKeys.SCOPE_STATUS, "") or "")

        if interaction_type != InteractionType.BUSINESS.value:
            return {WorkflowStateKeys.ROUTE: "FAST_LANE"}

        if intent in {
            WorkflowIntent.ORDER_DIAGNOSIS.value,
            WorkflowIntent.SUPPLIER_SCORE.value,
        } and scope_status == "MISSING":
            return {WorkflowStateKeys.ROUTE: "FAST_LANE"}

        if self._can_reuse_context(state, intent, plan, scope_status):
            return {WorkflowStateKeys.ROUTE: "REUSE_CONTEXT"}

        return {WorkflowStateKeys.ROUTE: intent}

    def _can_reuse_context(self, state: dict, intent: str, plan: dict, scope_status: str) -> bool:
        if not plan:
            return False

        if plan.get("needsRefresh", True):
            return False

        if scope_status not in {"INHERITED"}:
            return False

        if not self._same_cached_scope(state, intent, plan):
            return False

        return self._has_reusable_result(state, intent)

    def _same_cached_scope(self, state: dict, intent: str, plan: dict) -> bool:
        cached_plan = dict(state.get(WorkflowStateKeys.ANSWER_PLAN, {}) or {})
        return (
            cached_plan.get("intent") == intent
            and cached_plan.get("bizKey") == plan.get("bizKey")
            and bool(plan.get("bizKey"))
        )

    def _has_reusable_result(self, state: dict, intent: str) -> bool:
        if intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            return bool(state.get(WorkflowStateKeys.ORDER_DIAGNOSIS))
        if intent == WorkflowIntent.WARNING_SCAN.value:
            return bool(state.get(WorkflowStateKeys.WARNING_ANALYSIS))
        if intent == WorkflowIntent.SUPPLIER_SCORE.value:
            return bool(state.get(WorkflowStateKeys.SUPPLIER_SCORE))
        return intent == WorkflowIntent.KNOWLEDGE_QA.value
