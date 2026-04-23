from app.workflows.state import InteractionType, WorkflowIntent, WorkflowStateKeys


class RouteDecisionNode:
    async def __call__(self, state: dict) -> dict:
        interaction_type = str(state.get(WorkflowStateKeys.INTERACTION_TYPE, InteractionType.BUSINESS.value))
        intent = str(state.get(WorkflowStateKeys.INTENT, WorkflowIntent.UNKNOWN.value))
        plan = dict(state.get(WorkflowStateKeys.ANSWER_PLAN, {}) or {})

        if interaction_type != InteractionType.BUSINESS.value:
            return {WorkflowStateKeys.ROUTE: "FAST_LANE"}

        if plan and not plan.get("needsRefresh", True) and self._has_reusable_result(state, intent):
            return {WorkflowStateKeys.ROUTE: "REUSE_CONTEXT"}

        return {WorkflowStateKeys.ROUTE: intent}

    def _has_reusable_result(self, state: dict, intent: str) -> bool:
        if intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            return bool(state.get(WorkflowStateKeys.ORDER_DIAGNOSIS))
        if intent == WorkflowIntent.WARNING_SCAN.value:
            return bool(state.get(WorkflowStateKeys.WARNING_ANALYSIS))
        if intent == WorkflowIntent.SUPPLIER_SCORE.value:
            return bool(state.get(WorkflowStateKeys.SUPPLIER_SCORE))
        return intent == WorkflowIntent.KNOWLEDGE_QA.value
