from langgraph.graph import END, START, StateGraph

from app.clients.inventory_backend import InventoryBackendClient
from app.clients.llm_client import LLMClient
from app.repositories.session_store import SessionStore
from app.schemas.workflow import WorkflowAgentRequest, WorkflowAgentResponse
from app.services.rag_service import RagService
from app.workflows.nodes.answer_plan import AnswerPlanNode
from app.workflows.nodes.build_answer_card import BuildAnswerCardNode
from app.workflows.nodes.build_final_response import BuildFinalResponseNode
from app.workflows.nodes.business_answer_generate import BusinessAnswerGenerateNode
from app.workflows.nodes.context_select import ContextSelectNode
from app.workflows.nodes.entity_extract import EntityExtractNode
from app.workflows.nodes.guardrail_validate import GuardrailValidateNode
from app.workflows.nodes.intent_classify import IntentClassifyNode
from app.workflows.nodes.knowledge_retrieve import KnowledgeRetrieveNode
from app.workflows.nodes.Load_order_context import LoadOrderContextNode
from app.workflows.nodes.load_supplier_context import LoadSupplierContextNode
from app.workflows.nodes.load_warning_context import LoadWarningContextNode
from app.workflows.nodes.order_rule_analyze import OrderRuleAnalyzeNode
from app.workflows.nodes.preprocess_input import PreprocessInputNode
from app.workflows.nodes.response_policy import ResponsePolicyNode
from app.workflows.nodes.route_decision import RouteDecisionNode
from app.workflows.nodes.supplier_score_rule import SupplierScoreRuleNode
from app.workflows.nodes.warning_rule_analyze import WarningRuleAnalyzeNode
from app.workflows.state import WorkflowGraphState, WorkflowIntent, WorkflowStateKeys
from app.workflows.nodes.turn_understand import TurnUnderstandNode
from app.workflows.nodes.biz_scope_resolve import BizScopeResolverNode


class WorkflowExecutor:
    def __init__(
        self,
        backend: InventoryBackendClient,
        llm_client: LLMClient,
        rag_service: RagService,
        session_store: SessionStore,
    ):
        self.backend = backend
        self.llm_client = llm_client
        self.rag_service = rag_service
        self.session_store = session_store
        self.graph = self._build_graph()

    def _build_graph(self):
        builder = StateGraph(WorkflowGraphState)

        builder.add_node("preprocessInput", PreprocessInputNode())
        builder.add_node("classifyIntent", IntentClassifyNode(self.llm_client))
        builder.add_node("extractEntities", EntityExtractNode())
        builder.add_node("retrieveKnowledge", KnowledgeRetrieveNode(self.rag_service))
        builder.add_node("answerPlan", AnswerPlanNode())
        builder.add_node("routeByIntent", RouteDecisionNode())

        builder.add_node("loadOrderContext", LoadOrderContextNode(self.backend, self.session_store))
        builder.add_node("analyzeOrderByRules", OrderRuleAnalyzeNode())
        builder.add_node("loadWarningContext", LoadWarningContextNode(self.backend, self.session_store))
        builder.add_node("analyzeWarningsByRules", WarningRuleAnalyzeNode())
        builder.add_node("loadSupplierContext", LoadSupplierContextNode(self.backend, self.session_store))
        builder.add_node("scoreSupplierByRules", SupplierScoreRuleNode())

        builder.add_node("contextSelect", ContextSelectNode())
        builder.add_node("buildAnswerCard", BuildAnswerCardNode())
        builder.add_node("buildResponsePolicy", ResponsePolicyNode())
        builder.add_node("generateBusinessAnswer", BusinessAnswerGenerateNode(self.llm_client))
        builder.add_node("guardrailValidate", GuardrailValidateNode())
        builder.add_node("buildFinalResponse", BuildFinalResponseNode())

        builder.add_node("turnUnderstand", TurnUnderstandNode())
        builder.add_node("bizScopeResolve", BizScopeResolverNode())

        builder.add_edge(START, "preprocessInput")
        builder.add_edge("preprocessInput", "turnUnderstand")
        builder.add_edge("turnUnderstand", "classifyIntent")
        builder.add_edge("classifyIntent", "extractEntities")
        builder.add_edge("extractEntities", "bizScopeResolve")
        builder.add_edge("bizScopeResolve", "retrieveKnowledge")
        builder.add_edge("retrieveKnowledge", "answerPlan")
        builder.add_edge("answerPlan", "routeByIntent")

        builder.add_conditional_edges(
            "routeByIntent",
            lambda state: state.get(WorkflowStateKeys.ROUTE, WorkflowIntent.UNKNOWN.value),
            {
                "FAST_LANE": "contextSelect",
                "REUSE_CONTEXT": "contextSelect",
                WorkflowIntent.ORDER_DIAGNOSIS.value: "loadOrderContext",
                WorkflowIntent.WARNING_SCAN.value: "loadWarningContext",
                WorkflowIntent.SUPPLIER_SCORE.value: "loadSupplierContext",
                WorkflowIntent.KNOWLEDGE_QA.value: "contextSelect",
                WorkflowIntent.UNKNOWN.value: "contextSelect",
            },
        )

        builder.add_edge("loadOrderContext", "analyzeOrderByRules")
        builder.add_edge("analyzeOrderByRules", "contextSelect")

        builder.add_edge("loadWarningContext", "analyzeWarningsByRules")
        builder.add_edge("analyzeWarningsByRules", "contextSelect")

        builder.add_edge("loadSupplierContext", "scoreSupplierByRules")
        builder.add_edge("scoreSupplierByRules", "contextSelect")

        builder.add_edge("contextSelect", "buildAnswerCard")
        builder.add_edge("buildAnswerCard", "buildResponsePolicy")
        builder.add_edge("buildResponsePolicy", "generateBusinessAnswer")
        builder.add_edge("generateBusinessAnswer", "guardrailValidate")
        builder.add_edge("guardrailValidate", "buildFinalResponse")
        builder.add_edge("buildFinalResponse", END)

        return builder.compile()

    async def execute(self, request: WorkflowAgentRequest, authorization: str) -> WorkflowAgentResponse:
        current_user = await self.backend.get_current_user(authorization)
        session = self.session_store.prepare_session(request.thread_id, current_user.id, request.message)
        self.session_store.save_user_message(session, request.message)

        restored_state = self.session_store.load_state_by_thread_id(session["thread_id"])
        input_state = self._sanitize_restored_state(dict(restored_state))
        input_state[WorkflowStateKeys.MESSAGE] = request.message or ""
        input_state[WorkflowStateKeys.THREAD_ID] = session["thread_id"]
        input_state[WorkflowStateKeys.AUTHORIZATION] = authorization
        input_state[WorkflowStateKeys.USER_ID] = current_user.id

        result_state = await self.graph.ainvoke(input_state)
        final_response = result_state.get(WorkflowStateKeys.FINAL_RESPONSE)
        if not final_response:
            response = WorkflowAgentResponse(
                sessionId=session["id"],
                threadId=session["thread_id"],
                intent="UNKNOWN",
                answer="工作流执行失败，未生成最终响应。",
                data=None,
            )
        else:
            response = WorkflowAgentResponse(**final_response)
            response.session_id = session["id"]
            response.thread_id = session["thread_id"]

        active_intent = str(result_state.get(WorkflowStateKeys.ACTIVE_INTENT, "")).strip()
        current_intent_for_session = active_intent or response.intent
        if current_intent_for_session == "UNKNOWN":
            current_intent_for_session = None

        self.session_store.update_session_intent(session["id"], current_intent_for_session)
        self.session_store.save_assistant_message(session, response.answer)
        self.session_store.save_state(session, "END", current_intent_for_session, dict(result_state))
        self.session_store.save_result(session, response)
        return response


    def _sanitize_restored_state(self, restored_state: dict) -> dict:
        keep_keys = {
            WorkflowStateKeys.CONVERSATION_MEMORY,
            WorkflowStateKeys.ORDER_DIAGNOSIS,
            WorkflowStateKeys.WARNING_ANALYSIS,
            WorkflowStateKeys.SUPPLIER_SCORE,
            WorkflowStateKeys.ANSWER_PLAN,
            WorkflowStateKeys.SELECTED_CONTEXT,
        }

        return {
            key: value
            for key, value in restored_state.items()
            if key in keep_keys
        }
