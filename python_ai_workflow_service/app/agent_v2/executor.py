from typing import Any

from app.agent_v2.answer_generator import AnswerGenerator
from app.agent_v2.evidence import EvidenceBuilder
from app.agent_v2.fact_checker import FactChecker
from app.agent_v2.memory import MemoryLoader
from app.agent_v2.planner import Planner
from app.agent_v2.schemas import AgentEvidence, AgentMemory, AgentPlan
from app.agent_v2.slot_resolver import SlotResolver
from app.agent_v2.title_generator import SessionTitleGenerator
from app.agent_v2.tools import ToolRunner
from app.agent_v2.understander import ConversationUnderstandStep
from app.clients.inventory_backend import InventoryBackendClient
from app.clients.llm_client import LLMClient
from app.repositories.session_store import SessionStore
from app.schemas.workflow import WorkflowAgentRequest, WorkflowAgentResponse
from app.services.rag_service import RagService


class AgentV2Executor:
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
        self.memory_loader = MemoryLoader(session_store)
        self.understander = ConversationUnderstandStep()
        self.slot_resolver = SlotResolver()
        self.planner = Planner()
        self.tool_runner = ToolRunner(backend, rag_service)
        self.evidence_builder = EvidenceBuilder()
        self.answer_generator = AnswerGenerator(llm_client)
        self.fact_checker = FactChecker()
        self.title_generator = SessionTitleGenerator(llm_client)

    async def execute(self, request: WorkflowAgentRequest, authorization: str) -> WorkflowAgentResponse:
        current_user = await self.backend.get_current_user(authorization)
        session = self.session_store.prepare_session(request.thread_id, current_user.id, request.message)
        memory = self.memory_loader.load(session, current_user.id)
        self.session_store.save_user_message(session, request.message)

        message = request.message or ""
        understanding = self.understander.understand(message, memory)
        slots = self.slot_resolver.resolve(message, understanding, memory)
        plan = self.planner.plan(message, understanding, slots, memory)
        tool_results = await self.tool_runner.run(plan, authorization)
        evidence = self.evidence_builder.build(plan, tool_results)
        business_memory = self._update_business_memory(memory, plan, evidence)
        current_intent = plan.task if plan.can_execute or plan.task in {"KNOWLEDGE_QA", "CLARIFY"} else None
        raw_answer, answer_draft = await self.answer_generator.generate(message, memory, understanding, plan, evidence)
        fallback_answer = self.answer_generator.render_draft(answer_draft)
        fact_check = self.fact_checker.validate(raw_answer, plan, evidence, answer_draft, fallback_answer)
        answer = fact_check.answer
        session_title, title_generated_by = await self._generate_session_title_if_needed(
            session,
            current_user.id,
            message,
            current_intent or plan.task,
        )

        response = WorkflowAgentResponse(
            sessionId=session["id"],
            threadId=session["thread_id"],
            intent=plan.task,
            answer=answer,
            data={
                "agentVersion": "V2_ANSWER_CHAIN",
                "legacyWorkflowRemoved": True,
                "understanding": understanding.model_dump(by_alias=True),
                "plan": plan.model_dump(by_alias=True),
                "toolResults": [result.model_dump(by_alias=True) for result in tool_results],
                "evidence": evidence.model_dump(by_alias=True),
                "answerDraft": answer_draft.model_dump(by_alias=True),
                "factCheck": fact_check.model_dump(by_alias=True),
                "memory": {
                    "recentMessageCount": len(memory.recent_messages),
                    "hasBusinessMemory": bool(business_memory),
                    "hasConversationSummary": bool(memory.conversation_summary),
                },
                "sessionTitle": session_title,
                "sessionTitleGeneratedBy": title_generated_by,
            },
        )

        self.session_store.update_session_intent(session["id"], current_intent)
        self.session_store.save_assistant_message(session, answer)
        self.session_store.save_state(
            session,
            "UNDERSTAND",
            current_intent,
            {
                "agentVersion": "V2_ANSWER_CHAIN",
                "lastUserMessage": message,
                "conversationMemory": {
                    "lastInteractionType": understanding.interaction_type,
                    "lastEmotion": understanding.emotion,
                    "lastSpeechAct": understanding.speech_act,
                    "lastNeedsBusinessPlanner": understanding.needs_business_planner,
                },
                "businessMemory": business_memory,
                "conversationSummary": memory.conversation_summary,
                "lastUnderstanding": understanding.model_dump(by_alias=True),
                "lastPlan": plan.model_dump(by_alias=True),
                "lastEvidence": evidence.model_dump(by_alias=True),
                "lastFactCheck": fact_check.model_dump(by_alias=True),
            },
        )
        return response

    def _update_business_memory(
        self,
        memory: AgentMemory,
        plan: AgentPlan,
        evidence: AgentEvidence,
    ) -> dict[str, Any]:
        updated = dict(memory.business_memory or {})
        updated["lastTask"] = plan.task
        updated["lastFocus"] = plan.focus

        if plan.slots.order_no:
            updated["lastOrderNo"] = plan.slots.order_no
            updated["lastBizType"] = "PURCHASE_ORDER"
            updated["lastBizKey"] = plan.slots.order_no

        if plan.slots.supplier_id is not None:
            updated["lastSupplierId"] = plan.slots.supplier_id
            updated["lastDays"] = plan.slots.days or 30
            updated["lastBizType"] = "SUPPLIER"
            updated["lastBizKey"] = f"supplierId={plan.slots.supplier_id},days={plan.slots.days or 30}"

        if plan.task == "WARNING_SCAN":
            updated["lastDays"] = plan.slots.days or 7
            updated["lastBizType"] = "WARNING_SCAN_RANGE"
            updated["lastBizKey"] = f"days={plan.slots.days or 7}"

        if evidence.summary:
            updated["lastEvidenceSummary"] = evidence.summary

        return updated

    async def _generate_session_title_if_needed(
        self,
        session: dict[str, Any],
        user_id: int,
        first_message: str,
        current_intent: str | None,
    ) -> tuple[str | None, str | None]:
        if not session.get("_is_new_session"):
            return session.get("title"), None

        title, generated_by = await self.title_generator.generate(first_message, current_intent)
        try:
            self.session_store.update_session_title(session["thread_id"], user_id, title)
        except Exception:
            return session.get("title"), None
        return title, generated_by
