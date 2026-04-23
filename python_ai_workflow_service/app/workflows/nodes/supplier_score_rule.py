from app.workflows.state import WorkflowStateKeys


class SupplierScoreRuleNode:
    async def __call__(self, state: dict) -> dict:
        context = dict(state.get(WorkflowStateKeys.SUPPLIER_CONTEXT, {}) or {})
        if not context:
            return {WorkflowStateKeys.ERROR_MESSAGE: "供应商上下文为空"}

        supplier = context.get("supplier") or {}
        score = context.get("score") or {}
        metrics = context.get("metrics") or {}

        result = {
            "supplierId": supplier.get("supplierId"),
            "supplierName": supplier.get("supplierName"),
            "score": score.get("totalScore", 0),
            "level": score.get("level", "数据不足"),
            "levelExplain": score.get("levelExplain"),
            "confirmRate": metrics.get("confirmRate", "0.00%"),
            "arrivalCompletionRate": metrics.get("arrivalCompletionRate", "0.00%"),
            "inboundCompletionRate": metrics.get("inboundCompletionRate", "0.00%"),
            "abnormalArrivalRate": metrics.get("abnormalArrivalRate", "0.00%"),
            "scoreBreakdown": context.get("scoreBreakdown") or [],
            "weakMetrics": context.get("weakMetrics") or [],
            "analysis": context.get("analysisSummary"),
            "suggestion": context.get("suggestion"),
        }
        return {WorkflowStateKeys.SUPPLIER_SCORE: result}
