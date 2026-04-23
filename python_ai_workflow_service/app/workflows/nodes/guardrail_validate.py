from app.workflows.state import WorkflowStateKeys


class GuardrailValidateNode:
    async def __call__(self, state: dict) -> dict:
        answer = str(state.get(WorkflowStateKeys.LLM_ANSWER, ""))
        supplier_score = state.get(WorkflowStateKeys.SUPPLIER_SCORE)
        if "供应商不存在" in answer and supplier_score:
            return {
                WorkflowStateKeys.GUARDRAIL_RESULT: "REJECT",
                WorkflowStateKeys.LLM_ANSWER: "系统检测到 AI 回答可能与结构化数据冲突，请以系统结构化结果为准。",
            }
        return {WorkflowStateKeys.GUARDRAIL_RESULT: "PASS"}
