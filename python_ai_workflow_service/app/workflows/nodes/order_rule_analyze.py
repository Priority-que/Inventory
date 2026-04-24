from app.workflows.state import WorkflowStateKeys


class OrderRuleAnalyzeNode:
    async def __call__(self, state: dict) -> dict:
        existing_error = str(state.get(WorkflowStateKeys.ERROR_MESSAGE, "") or "")
        if existing_error:
            return {WorkflowStateKeys.ERROR_MESSAGE: existing_error}

        context = dict(state.get(WorkflowStateKeys.ORDER_CONTEXT, {}) or {})
        if not context:
            return {WorkflowStateKeys.ERROR_MESSAGE: "订单上下文为空，请检查 Java 订单上下文接口是否返回 exists=true。"}


        order = context.get("order") or {}
        stage = context.get("stage") or {}
        responsibility = context.get("responsibility") or {}
        next_action = context.get("nextAction") or {}
        evidence_items = context.get("evidence") or []

        evidence = [
            f"{item.get('label')}为 {item.get('value')}，{item.get('explain')}"
            for item in evidence_items
            if item.get("label") and item.get("value")
        ]

        return {
            WorkflowStateKeys.ORDER_DIAGNOSIS: {
                "orderNo": order.get("orderNo"),
                "currentStage": stage.get("currentStage"),
                "blockReason": stage.get("blockReason"),
                "evidence": evidence,
                "suggestOwner": responsibility.get("ownerRole"),
                "suggestAction": next_action.get("actionText"),
                "responsibility": responsibility,
                "nextAction": next_action,
            }
        }
