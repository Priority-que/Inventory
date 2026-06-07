import asyncio
import json
from typing import Any, AsyncIterator

from fastapi.encoders import jsonable_encoder

from app.agent_v2.answer_generator import AnswerGenerator
from app.agent_v2.brain import AgentBrain
from app.agent_v2.evidence import EvidenceBuilder
from app.agent_v2.fact_checker import FactChecker
from app.agent_v2.memory import MemoryLoader, MemoryManager
from app.agent_v2.planner import Planner
from app.agent_v2.schemas import AgentEvidence, AgentMemory, AgentPlan, ToolCallResult
from app.agent_v2.slot_resolver import SlotResolver
from app.agent_v2.title_generator import SessionTitleGenerator
from app.agent_v2.tools import ToolExecutor, ToolRegistry
from app.agent_v2.understander import ConversationUnderstandStep
from app.clients.inventory_backend import InventoryBackendClient
from app.clients.llm_client import LLMClient
from app.repositories.session_store import SessionStore
from app.schemas.workflow import WorkflowAgentRequest, WorkflowAgentResponse
from app.services.rag_service import RagService


class AgentV2Executor:
    MAX_AGENT_LOOP_STEPS = 2

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
        self.memory_manager = MemoryManager(llm_client)
        self.understander = ConversationUnderstandStep()
        self.slot_resolver = SlotResolver()
        self.planner = Planner()
        self.tool_registry = ToolRegistry()
        self.agent_brain = AgentBrain(llm_client, self.planner, self.tool_registry)
        self.tool_executor = ToolExecutor(backend, rag_service, self.tool_registry)
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
        plan, tool_results, evidence_tool_results, brain_steps = await self._run_agent_loop(
            message,
            memory,
            understanding,
            slots,
            authorization,
        )
        last_brain_step = brain_steps[-1] if brain_steps else {}
        evidence = self.evidence_builder.build(plan, evidence_tool_results)
        business_memory = self._update_business_memory(memory, plan, evidence)
        current_intent = self._current_intent(plan, evidence_tool_results)
        raw_answer, answer_draft = await self.answer_generator.generate(message, memory, understanding, plan, evidence)
        fallback_answer = self.answer_generator.render_draft(answer_draft)
        fact_check = self.fact_checker.validate(raw_answer, plan, evidence, answer_draft, fallback_answer)
        answer = fact_check.answer
        updated_conversation_summary, memory_update = await self.memory_manager.update_conversation_summary(
            memory,
            message,
            answer,
            plan,
            evidence,
        )
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
                "agentBrain": {
                    "usedLLM": last_brain_step.get("usedLLM"),
                    "fallbackReason": last_brain_step.get("fallbackReason"),
                },
                "agentBrainSteps": brain_steps,
                "agentLoop": {
                    "maxSteps": self.MAX_AGENT_LOOP_STEPS,
                    "actualSteps": len(brain_steps),
                    "observationCount": len(tool_results),
                    "evidenceObservationCount": len(evidence_tool_results),
                },
                "plan": plan.model_dump(by_alias=True),
                "toolResults": [result.model_dump(by_alias=True) for result in tool_results],
                "evidence": evidence.model_dump(by_alias=True),
                "answerDraft": answer_draft.model_dump(by_alias=True),
                "factCheck": fact_check.model_dump(by_alias=True),
                "memory": {
                    "recentMessageCount": len(memory.recent_messages),
                    "hasBusinessMemory": bool(business_memory),
                    "hasConversationSummary": bool(updated_conversation_summary),
                    "conversationSummaryUpdated": memory_update.get("updated"),
                    "conversationSummaryLength": memory_update.get("summaryLength"),
                    "conversationSummaryUpdateReason": memory_update.get("reason"),
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
                "conversationSummary": updated_conversation_summary,
                "conversationSummaryUpdate": memory_update,
                "lastUnderstanding": understanding.model_dump(by_alias=True),
                "lastAgentBrain": {
                    "usedLLM": last_brain_step.get("usedLLM"),
                    "fallbackReason": last_brain_step.get("fallbackReason"),
                },
                "lastAgentBrainSteps": brain_steps,
                "lastPlan": plan.model_dump(by_alias=True),
                "lastEvidence": evidence.model_dump(by_alias=True),
                "lastFactCheck": fact_check.model_dump(by_alias=True),
            },
        )
        return response

    async def stream_execute(
        self,
        request: WorkflowAgentRequest,
        authorization: str,
    ) -> AsyncIterator[str]:
        try:
            yield self._stream_event(
                "start",
                {
                    "message": "已收到问题，开始处理",
                    "threadId": request.thread_id,
                },
            )

            yield self._stream_event("step", {"stage": "AUTH", "message": "正在确认当前用户"})
            current_user = await self.backend.get_current_user(authorization)
            session = self.session_store.prepare_session(request.thread_id, current_user.id, request.message)
            memory = self.memory_loader.load(session, current_user.id)
            self.session_store.save_user_message(session, request.message)

            message = request.message or ""
            yield self._stream_event("step", {"stage": "UNDERSTAND", "message": "正在理解你的问题"})
            understanding = self.understander.understand(message, memory)
            slots = self.slot_resolver.resolve(message, understanding, memory)

            yield self._stream_event("step", {"stage": "PLAN", "message": "正在判断是否需要查询业务数据"})
            plan, tool_results, evidence_tool_results, brain_steps = await self._run_agent_loop(
                message,
                memory,
                understanding,
                slots,
                authorization,
            )
            last_brain_step = brain_steps[-1] if brain_steps else {}

            yield self._stream_event("step", {"stage": "EVIDENCE", "message": "正在整理业务证据"})
            evidence = self.evidence_builder.build(plan, evidence_tool_results)
            business_memory = self._update_business_memory(memory, plan, evidence)
            current_intent = self._current_intent(plan, evidence_tool_results)
            answer_draft = self.answer_generator.build_draft(message, understanding, plan, evidence)
            fallback_answer = self.answer_generator.render_draft(answer_draft)

            yield self._stream_event("step", {"stage": "ANSWER", "message": "正在生成回答"})
            raw_answer_parts: list[str] = []
            async for event in self.answer_generator.generate_stream_events(
                message,
                memory,
                understanding,
                plan,
                evidence,
                answer_draft,
            ):
                text = event.get("text") or ""
                if not text:
                    continue
                if event.get("type") == "thinking":
                    yield self._stream_event("thinking", {"text": text})
                    continue
                raw_answer_parts.append(text)
                yield self._stream_event("token", {"text": text})

            raw_answer = self.answer_generator.clean_answer("".join(raw_answer_parts)) or fallback_answer
            fact_check = self.fact_checker.validate(raw_answer, plan, evidence, answer_draft, fallback_answer)
            answer = fact_check.answer
            updated_conversation_summary = memory.conversation_summary
            memory_update = {
                "updated": False,
                "reason": "流式模式下后台更新会话摘要",
                "summaryLength": len(updated_conversation_summary or ""),
                "backgroundPending": True,
            }
            session_title = session.get("title")
            title_generated_by = "background_pending" if session.get("_is_new_session") else None

            response = WorkflowAgentResponse(
                sessionId=session["id"],
                threadId=session["thread_id"],
                intent=plan.task,
                answer=answer,
                data={
                    "agentVersion": "V2_ANSWER_CHAIN",
                    "streamMode": "MODEL_REASONING_AND_TOKEN_STREAM",
                    "legacyWorkflowRemoved": True,
                    "understanding": understanding.model_dump(by_alias=True),
                    "agentBrain": {
                        "usedLLM": last_brain_step.get("usedLLM"),
                        "fallbackReason": last_brain_step.get("fallbackReason"),
                    },
                    "agentBrainSteps": brain_steps,
                    "agentLoop": {
                        "maxSteps": self.MAX_AGENT_LOOP_STEPS,
                        "actualSteps": len(brain_steps),
                        "observationCount": len(tool_results),
                        "evidenceObservationCount": len(evidence_tool_results),
                    },
                    "plan": plan.model_dump(by_alias=True),
                    "toolResults": [result.model_dump(by_alias=True) for result in tool_results],
                    "evidence": evidence.model_dump(by_alias=True),
                    "answerDraft": answer_draft.model_dump(by_alias=True),
                    "factCheck": fact_check.model_dump(by_alias=True),
                    "memory": {
                        "recentMessageCount": len(memory.recent_messages),
                        "hasBusinessMemory": bool(business_memory),
                        "hasConversationSummary": bool(updated_conversation_summary),
                        "conversationSummaryUpdated": memory_update.get("updated"),
                        "conversationSummaryLength": memory_update.get("summaryLength"),
                        "conversationSummaryUpdateReason": memory_update.get("reason"),
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
                    "conversationSummary": updated_conversation_summary,
                    "conversationSummaryUpdate": memory_update,
                    "postAnswerFinalization": {
                        "status": "PENDING",
                        "titleGeneration": title_generated_by,
                    },
                    "lastUnderstanding": understanding.model_dump(by_alias=True),
                    "lastAgentBrain": {
                        "usedLLM": last_brain_step.get("usedLLM"),
                        "fallbackReason": last_brain_step.get("fallbackReason"),
                    },
                    "lastAgentBrainSteps": brain_steps,
                    "lastPlan": plan.model_dump(by_alias=True),
                    "lastEvidence": evidence.model_dump(by_alias=True),
                    "lastFactCheck": fact_check.model_dump(by_alias=True),
                },
            )

            self._schedule_stream_finalization(
                session=session,
                user_id=current_user.id,
                message=message,
                answer=answer,
                memory=memory,
                understanding=understanding,
                plan=plan,
                evidence=evidence,
                business_memory=business_memory,
                current_intent=current_intent,
                last_brain_step=last_brain_step,
                brain_steps=brain_steps,
                fact_check=fact_check,
            )
            yield self._stream_event("done", response.model_dump(by_alias=True))
        except Exception as exc:
            yield self._stream_event(
                "error",
                {
                    "message": self._exception_message(exc),
                },
            )

    async def _run_agent_loop(
        self,
        message: str,
        memory: AgentMemory,
        understanding,
        slots,
        authorization: str,
    ) -> tuple[AgentPlan, list[ToolCallResult], list[ToolCallResult], list[dict[str, Any]]]:
        tool_results: list[ToolCallResult] = []
        brain_steps: list[dict[str, Any]] = []
        plan: AgentPlan | None = None

        for step_index in range(self.MAX_AGENT_LOOP_STEPS):
            brain_output = await self.agent_brain.decide(
                message,
                memory,
                understanding,
                slots,
                observations=tool_results,
            )
            plan = self._without_repeated_tools(brain_output.plan, tool_results)
            brain_steps.append(
                {
                    "step": step_index + 1,
                    "usedLLM": brain_output.used_llm,
                    "fallbackReason": brain_output.fallback_reason,
                    "plan": plan.model_dump(by_alias=True),
                    "observationCountBeforeStep": len(tool_results),
                }
            )

            if not plan.can_execute or not plan.tool_names:
                break

            new_results = await self.tool_executor.run(plan, authorization)
            tool_results.extend(new_results)
            brain_steps[-1]["newObservationCount"] = len(new_results)

            if not self._should_continue_agent_loop(step_index, brain_output.used_llm, new_results):
                break

        if plan is None:
            plan = self.planner.plan(message, understanding, slots, memory)

        evidence_tool_results = self._evidence_tool_results(plan, tool_results)
        return plan, tool_results, evidence_tool_results, brain_steps

    def _without_repeated_tools(self, plan: AgentPlan, tool_results: list[ToolCallResult]) -> AgentPlan:
        if not plan.tool_names:
            return plan

        called_tools = {result.tool_name for result in tool_results}
        tool_names = [tool_name for tool_name in plan.tool_names if tool_name not in called_tools]
        if len(tool_names) == len(plan.tool_names):
            return plan

        reason = (plan.reason or "").rstrip("。")
        if reason:
            reason += "；"
        reason += "跳过已调用过的工具，避免重复查询"

        return plan.model_copy(
            update={
                "tool_names": tool_names,
                "can_execute": bool(tool_names) and plan.can_execute,
                "reason": reason,
            }
        )

    def _should_continue_agent_loop(
        self,
        step_index: int,
        used_llm: bool,
        new_results: list[ToolCallResult],
    ) -> bool:
        if step_index >= self.MAX_AGENT_LOOP_STEPS - 1:
            return False
        if not used_llm:
            return False
        if not new_results:
            return False
        return True

    def _evidence_tool_results(
        self,
        plan: AgentPlan,
        tool_results: list[ToolCallResult],
    ) -> list[ToolCallResult]:
        allowed_tools = set(self.tool_registry.names_for_task(plan.task))
        if not allowed_tools:
            return tool_results

        matched = [result for result in tool_results if result.tool_name in allowed_tools]
        return matched or tool_results

    def _current_intent(self, plan: AgentPlan, evidence_tool_results: list[ToolCallResult]) -> str | None:
        if plan.can_execute or plan.task in {"KNOWLEDGE_QA", "CLARIFY"}:
            return plan.task
        if evidence_tool_results and plan.task not in {"CHAT", "EMOTION_SUPPORT"}:
            return plan.task
        return None

    def _stream_event(self, event: str, data: Any) -> str:
        payload = json.dumps(jsonable_encoder(data), ensure_ascii=False)
        return f"event: {event}\ndata: {payload}\n\n"

    def _exception_message(self, exc: Exception) -> str:
        message = getattr(exc, "msg", None) or str(exc)
        return message or "AI 助手处理失败，请稍后重试"

    def _schedule_stream_finalization(
        self,
        *,
        session: dict[str, Any],
        user_id: int,
        message: str,
        answer: str,
        memory: AgentMemory,
        understanding,
        plan: AgentPlan,
        evidence: AgentEvidence,
        business_memory: dict[str, Any],
        current_intent: str | None,
        last_brain_step: dict[str, Any],
        brain_steps: list[dict[str, Any]],
        fact_check,
    ) -> None:
        try:
            task = asyncio.create_task(
                self._finalize_stream_session(
                    session=session,
                    user_id=user_id,
                    message=message,
                    answer=answer,
                    memory=memory,
                    understanding=understanding,
                    plan=plan,
                    evidence=evidence,
                    business_memory=business_memory,
                    current_intent=current_intent,
                    last_brain_step=last_brain_step,
                    brain_steps=brain_steps,
                    fact_check=fact_check,
                )
            )
            task.add_done_callback(self._consume_background_exception)
        except RuntimeError:
            return

    async def _finalize_stream_session(
        self,
        *,
        session: dict[str, Any],
        user_id: int,
        message: str,
        answer: str,
        memory: AgentMemory,
        understanding,
        plan: AgentPlan,
        evidence: AgentEvidence,
        business_memory: dict[str, Any],
        current_intent: str | None,
        last_brain_step: dict[str, Any],
        brain_steps: list[dict[str, Any]],
        fact_check,
    ) -> None:
        updated_conversation_summary, memory_update = await self.memory_manager.update_conversation_summary(
            memory,
            message,
            answer,
            plan,
            evidence,
        )
        session_title, title_generated_by = await self._generate_session_title_if_needed(
            session,
            user_id,
            message,
            current_intent or plan.task,
        )
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
                "conversationSummary": updated_conversation_summary,
                "conversationSummaryUpdate": memory_update,
                "postAnswerFinalization": {
                    "status": "COMPLETED",
                    "sessionTitle": session_title,
                    "sessionTitleGeneratedBy": title_generated_by,
                },
                "lastUnderstanding": understanding.model_dump(by_alias=True),
                "lastAgentBrain": {
                    "usedLLM": last_brain_step.get("usedLLM"),
                    "fallbackReason": last_brain_step.get("fallbackReason"),
                },
                "lastAgentBrainSteps": brain_steps,
                "lastPlan": plan.model_dump(by_alias=True),
                "lastEvidence": evidence.model_dump(by_alias=True),
                "lastFactCheck": fact_check.model_dump(by_alias=True),
            },
        )

    def _consume_background_exception(self, task: asyncio.Task) -> None:
        try:
            task.result()
        except Exception:
            return

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
