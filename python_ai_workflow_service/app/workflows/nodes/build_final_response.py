from app.schemas.workflow import WorkflowAgentResponse
from app.workflows.state import InteractionType, WorkflowStateKeys


class BuildFinalResponseNode:
    async def __call__(self, state: dict) -> dict:
        interaction_type = str(state.get(WorkflowStateKeys.INTERACTION_TYPE, InteractionType.BUSINESS.value))
        intent = str(state.get(WorkflowStateKeys.INTENT, "UNKNOWN"))
        answer = str(state.get(WorkflowStateKeys.LLM_ANSWER, ""))
        thread_id = str(state.get(WorkflowStateKeys.THREAD_ID, ""))

        response_intent = intent if interaction_type == InteractionType.BUSINESS.value else "UNKNOWN"

        response = WorkflowAgentResponse(
            threadId=thread_id,
            intent=response_intent,
            answer=answer,
            data=None,
        )

        if interaction_type != InteractionType.BUSINESS.value:
            return {WorkflowStateKeys.FINAL_RESPONSE: response.model_dump(by_alias=True)}

        if intent == "ORDER_DIAGNOSIS":
            response.data = state.get(WorkflowStateKeys.ORDER_DIAGNOSIS)
        elif intent == "WARNING_SCAN":
            response.data = state.get(WorkflowStateKeys.WARNING_ANALYSIS)
        elif intent == "SUPPLIER_SCORE":
            response.data = state.get(WorkflowStateKeys.SUPPLIER_SCORE)

        return {WorkflowStateKeys.FINAL_RESPONSE: response.model_dump(by_alias=True)}
