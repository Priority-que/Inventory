from app.workflows.state import WorkflowStateKeys


class WarningRuleAnalyzeNode:
    async def __call__(self, state: dict) -> dict:
        context = dict(state.get(WorkflowStateKeys.WARNING_CONTEXT, {}) or {})
        if not context:
            return {WorkflowStateKeys.ERROR_MESSAGE: "预警上下文为空"}

        summary = context.get("summary") or {}
        items = context.get("items") or []
        result = {
            "summary": summary.get("summaryText") or f"本次扫描共发现 {len(items)} 个执行风险。",
            "summaryStats": summary,
            "items": items,
            "topItems": context.get("topItems") or [],
            "ownerStats": context.get("ownerStats") or [],
            "riskTypeStats": context.get("riskTypeStats") or [],
        }
        return {WorkflowStateKeys.WARNING_ANALYSIS: result}
