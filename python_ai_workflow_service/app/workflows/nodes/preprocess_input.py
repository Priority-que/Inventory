from app.workflows.state import WorkflowStateKeys


class PreprocessInputNode:
    async def __call__(self, state: dict) -> dict:
        message = str(state.get(WorkflowStateKeys.MESSAGE, "")).strip()
        if not message:
            return {
                WorkflowStateKeys.ERROR_MESSAGE: "请输入要分析的问题",
                WorkflowStateKeys.NORMALIZED_MESSAGE: "",
            }
        return {WorkflowStateKeys.NORMALIZED_MESSAGE: message}
