import asyncio
import json
from typing import Any, AsyncIterator

from app.agent_v2.schemas import AgentEvidence, AgentMemory, AgentPlan, AnswerDraft, ConversationUnderstanding


class AnswerGenerator:
    def __init__(self, llm_client: Any):
        self.llm_client = llm_client

    async def generate(
        self,
        message: str,
        memory: AgentMemory,
        understanding: ConversationUnderstanding,
        plan: AgentPlan,
        evidence: AgentEvidence,
    ) -> tuple[str, AnswerDraft]:
        draft = self._build_draft(message, understanding, plan, evidence)
        fallback = self.render_draft(draft)

        if not self._should_use_llm(plan, evidence):
            return fallback, draft

        llm_answer = await self._generate_with_llm(message, memory, understanding, plan, evidence, draft)
        return (llm_answer or fallback), draft

    def build_draft(
        self,
        message: str,
        understanding: ConversationUnderstanding,
        plan: AgentPlan,
        evidence: AgentEvidence,
    ) -> AnswerDraft:
        return self._build_draft(message, understanding, plan, evidence)

    def clean_answer(self, text: str | None) -> str:
        return self._strip_model_noise(text)

    async def generate_stream(
        self,
        message: str,
        memory: AgentMemory,
        understanding: ConversationUnderstanding,
        plan: AgentPlan,
        evidence: AgentEvidence,
        draft: AnswerDraft | None = None,
    ) -> AsyncIterator[str]:
        async for event in self.generate_stream_events(message, memory, understanding, plan, evidence, draft):
            if event["type"] == "content":
                yield event["text"]

    async def generate_stream_events(
        self,
        message: str,
        memory: AgentMemory,
        understanding: ConversationUnderstanding,
        plan: AgentPlan,
        evidence: AgentEvidence,
        draft: AnswerDraft | None = None,
    ) -> AsyncIterator[dict[str, str]]:
        answer_draft = draft or self._build_draft(message, understanding, plan, evidence)
        fallback = self.render_draft(answer_draft)

        if not self._should_use_llm(plan, evidence):
            async for chunk in self._fallback_chunks(fallback):
                yield {"type": "content", "text": chunk}
            return

        stream_event_method = getattr(self.llm_client, "chat_text_stream_events", None)
        stream_method = getattr(self.llm_client, "chat_text_stream", None)
        if callable(stream_event_method):
            system_prompt, user_prompt = self._build_llm_request(
                message,
                memory,
                understanding,
                plan,
                evidence,
                answer_draft,
            )
            has_content = False

            try:
                async for event in stream_event_method(system_prompt, user_prompt, temperature=0.25):
                    text = event.get("text") or ""
                    event_type = event.get("type")
                    if not text:
                        continue
                    if "LLM 未配置" in text:
                        break
                    if event_type == "reasoning":
                        yield {"type": "thinking", "text": text}
                        continue
                    has_content = True
                    yield {"type": "content", "text": text}
            except Exception:
                if has_content:
                    raise

            if not has_content:
                async for chunk in self._fallback_chunks(fallback):
                    yield {"type": "content", "text": chunk}
            return

        if not callable(stream_method):
            async for chunk in self._fallback_chunks(fallback):
                yield {"type": "content", "text": chunk}
            return

        system_prompt, user_prompt = self._build_llm_request(message, memory, understanding, plan, evidence, answer_draft)
        has_chunk = False

        try:
            async for chunk in stream_method(system_prompt, user_prompt, temperature=0.25):
                if not chunk:
                    continue
                if "LLM 未配置" in chunk:
                    break
                has_chunk = True
                yield {"type": "content", "text": chunk}
        except Exception:
            if has_chunk:
                raise

        if not has_chunk:
            async for chunk in self._fallback_chunks(fallback):
                yield {"type": "content", "text": chunk}

    def _should_use_llm(self, plan: AgentPlan, evidence: AgentEvidence) -> bool:
        if plan.missing_fields or evidence.errors:
            return False
        is_configured = getattr(self.llm_client, "is_configured", None)
        if callable(is_configured):
            return bool(is_configured())
        return True

    async def _generate_with_llm(
        self,
        message: str,
        memory: AgentMemory,
        understanding: ConversationUnderstanding,
        plan: AgentPlan,
        evidence: AgentEvidence,
        draft: AnswerDraft,
    ) -> str | None:
        system_prompt, user_prompt = self._build_llm_request(message, memory, understanding, plan, evidence, draft)

        try:
            answer = await self.llm_client.chat_text(
                system_prompt,
                user_prompt,
                temperature=0.25,
            )
        except Exception:
            return None

        cleaned = self._strip_model_noise(answer)
        if not cleaned or "LLM 未配置" in cleaned:
            return None
        return cleaned

    def _build_llm_request(
        self,
        message: str,
        memory: AgentMemory,
        understanding: ConversationUnderstanding,
        plan: AgentPlan,
        evidence: AgentEvidence,
        draft: AnswerDraft,
    ) -> tuple[str, str]:
        if self._is_business_task(plan):
            system_prompt = (
                "你是库存采购协同系统里的业务分析助手。"
                "只有当用户明确询问库存、采购、订单、供应商、风险、规则等业务问题时，才使用业务分析口吻。"
                "业务问题必须严格基于输入证据回答，禁止编造订单号、供应商、负责人、数量、状态、评分、风险等级。"
                "如果证据里有 knowledgeItems，它们只代表静态规则、流程说明或论文表达参考，不能当作实时业务数据。"
                "不要输出 JSON、字段名、内部码或 Markdown 标题。"
                "回答要自然，不要机械套模板；能直接回答就直接回答，需要说明依据时再说明依据。"
            )
            payload = {
                "用户问题": message,
                "对话理解": understanding.model_dump(by_alias=True),
                "任务计划": plan.model_dump(by_alias=True),
                "证据": evidence.model_dump(by_alias=True),
                "稳定草稿": draft.model_dump(by_alias=True),
                "最近消息": [item.model_dump(by_alias=True) for item in memory.recent_messages[-6:]],
            }
        else:
            system_prompt = (
                "你是一个自然、温和、可靠的 AI 助手。"
                "用户闲聊、表达情绪、问你是谁、问你是什么模型或问日常问题时，像正常聊天一样回应。"
                "除非用户主动提到库存、采购、订单、供应商、风险、规则等业务问题，否则不要主动把话题引到业务系统。"
                "如果用户问你是什么模型，只说明你是当前系统中的 AI 智能助手，底层模型由系统配置决定；不要编造或猜测具体模型名称。"
                "如果用户表达焦虑、烦躁或压力，先接住情绪，再用简短、具体的方式陪他拆问题。"
                "不要输出 JSON、字段名、内部码或 Markdown 标题。"
            )
            payload = {
                "用户问题": message,
                "对话理解": {
                    "interactionType": understanding.interaction_type,
                    "emotion": understanding.emotion,
                    "speechAct": understanding.speech_act,
                    "isFollowUp": understanding.is_follow_up,
                },
                "最近消息": [item.model_dump(by_alias=True) for item in memory.recent_messages[-6:]],
            }
        return system_prompt, json.dumps(payload, ensure_ascii=False, default=str)

    def _is_business_task(self, plan: AgentPlan) -> bool:
        return plan.task in {"ORDER_DIAGNOSIS", "WARNING_SCAN", "SUPPLIER_SCORE", "KNOWLEDGE_QA"}

    async def _fallback_chunks(self, text: str) -> AsyncIterator[str]:
        chunk_size = 4
        value = text or ""
        for index in range(0, len(value), chunk_size):
            yield value[index : index + chunk_size]
            await asyncio.sleep(0.04)

    def _build_draft(
        self,
        message: str,
        understanding: ConversationUnderstanding,
        plan: AgentPlan,
        evidence: AgentEvidence,
    ) -> AnswerDraft:
        if plan.missing_fields:
            return AnswerDraft(
                conclusion="我还缺少必要信息，暂时不能安全判断。",
                limits=["缺少：" + "、".join(plan.missing_fields)],
                nextActions=[self._missing_field_action(plan.missing_fields)],
            )

        if evidence.errors:
            return AnswerDraft(
                conclusion="我已经理解你的问题，但业务工具查询失败了。",
                limits=evidence.errors[:2],
                nextActions=["请先确认 Java 后端、登录状态、Python 配置或 RAG 服务是否正常。"],
            )

        if plan.task == "CHAT":
            return self._chat_draft(message, understanding)
        if plan.task == "EMOTION_SUPPORT":
            return self._emotion_draft(understanding)
        if plan.task == "CLARIFY":
            return AnswerDraft(conclusion="我在。你可以直接把想聊的内容或业务问题发我。")
        if plan.task == "ORDER_DIAGNOSIS":
            return self._order_draft(plan, evidence)
        if plan.task == "WARNING_SCAN":
            return self._warning_draft(plan, evidence)
        if plan.task == "SUPPLIER_SCORE":
            return self._supplier_draft(plan, evidence)
        if plan.task == "KNOWLEDGE_QA":
            return self._knowledge_draft(evidence)

        return AnswerDraft(conclusion=evidence.summary or "我已经完成分析，但暂时没有形成明确结论。")

    def _chat_draft(self, message: str, understanding: ConversationUnderstanding) -> AnswerDraft:
        if understanding.speech_act == "GREETING":
            return AnswerDraft(conclusion="我在。你可以随便聊，也可以直接把订单、风险、供应商或规则问题发我。")
        if understanding.speech_act == "THANKS":
            return AnswerDraft(conclusion="不客气，我会接着当前会话上下文往下跟。")
        return AnswerDraft(conclusion="我在，可以继续聊。遇到业务问题时，我会先查证据再回答。")

    def _emotion_draft(self, understanding: ConversationUnderstanding) -> AnswerDraft:
        if understanding.emotion == "frustrated":
            return AnswerDraft(
                conclusion="我知道你现在有点烦，先别急。这个系统现在看起来乱，通常不是你写不下去，而是业务、前端、后端和 AI 链路都堆在一起了。",
                nextActions=["我们可以先只抓一个问题：你现在最想先理清页面、接口、数据库，还是 Agent 链路？"],
            )
        if understanding.emotion == "anxious":
            return AnswerDraft(
                conclusion="我知道你比较着急，我会先帮你抓最关键的信息，不把问题继续扩大。",
                nextActions=["你可以直接把最卡的一处发我，我先帮你判断它属于业务问题、代码问题还是链路问题。"],
            )
        return AnswerDraft(
            conclusion="我在。你这个感觉可以理解，项目写到后面很容易从“功能很多”变成“线索很多”。",
            nextActions=["我们可以继续往下拆，你先说现在最乱的是哪一块。"],
        )

    def _order_draft(self, plan: AgentPlan, evidence: AgentEvidence) -> AnswerDraft:
        facts = evidence.facts
        if facts.get("exists") is False:
            return AnswerDraft(
                conclusion=f"我没有查到采购订单 {facts.get('orderNo') or plan.slots.order_no}。",
                limits=["Java 后端订单上下文返回 exists=false。"],
                nextActions=["请确认订单号是否正确，或换一个采购订单号再查。"],
            )

        order_no = facts.get("orderNo") or plan.slots.order_no or "这张订单"
        stage = facts.get("currentStage") or "当前阶段未知"
        block_reason = facts.get("blockReason") or evidence.summary or "当前阻塞原因不明确"
        owner = facts.get("ownerUserName") or facts.get("ownerRoleName") or "对应责任方"
        next_action = facts.get("nextAction") or "继续核对订单、到货和入库记录。"

        if plan.focus == "NEXT_ACTION":
            conclusion = f"{order_no} 下一步建议先由 {owner} 跟进。"
            reasons = [block_reason]
        elif plan.focus == "OWNER":
            conclusion = f"{order_no} 当前建议找 {owner} 处理。"
            reasons = [facts.get("ownerReason") or block_reason]
        elif plan.focus == "CAUSE":
            conclusion = f"{order_no} 当前卡在“{stage}”，核心原因是：{block_reason}"
            reasons = self._evidence_lines(facts.get("evidence") or [])[:3]
        else:
            conclusion = f"{order_no} 当前处在“{stage}”。"
            reasons = [block_reason] + self._evidence_lines(facts.get("evidence") or [])[:2]

        return AnswerDraft(
            conclusion=conclusion,
            reasons=self._dedupe(reasons),
            nextActions=[next_action],
        )

    def _warning_draft(self, plan: AgentPlan, evidence: AgentEvidence) -> AnswerDraft:
        facts = evidence.facts
        summary = evidence.summary or "已完成风险扫描。"
        items = evidence.items or []
        top_biz = "、".join(str(item.get("bizNo")) for item in items[:5] if item.get("bizNo"))

        conclusion = summary
        if plan.focus == "TOP_RISK" and top_biz:
            conclusion = f"当前建议先看这几单：{top_biz}。"
        elif plan.focus == "NEXT_ACTION" and top_biz:
            conclusion = f"下一步先处理高优先级风险单据：{top_biz}。"

        owner_stats = facts.get("ownerStats") or []
        risk_type_stats = facts.get("riskTypeStats") or []
        reasons = []
        if risk_type_stats:
            reasons.append("风险集中类型：" + "、".join(str(item.get("problem")) for item in risk_type_stats[:3] if item.get("problem")))
        if owner_stats:
            reasons.append("主要处理角色：" + "、".join(str(item.get("ownerRoleName")) for item in owner_stats[:3] if item.get("ownerRoleName")))

        return AnswerDraft(
            conclusion=conclusion,
            reasons=self._dedupe(reasons),
            nextActions=["建议先处理高风险，再处理中风险；先处理超时天数更长、影响入库闭环的单据。"],
        )

    def _supplier_draft(self, plan: AgentPlan, evidence: AgentEvidence) -> AnswerDraft:
        facts = evidence.facts
        if facts.get("exists") is False:
            return AnswerDraft(
                conclusion=f"我没有查到供应商 {facts.get('supplierId') or plan.slots.supplier_id}。",
                limits=["Java 后端供应商上下文返回 exists=false。"],
                nextActions=["请确认供应商 ID 是否正确，或换一个供应商再查。"],
            )

        supplier_name = facts.get("supplierName") or "该供应商"
        score = facts.get("score")
        level = facts.get("level") or "等级未知"
        weak_metrics = facts.get("weakMetrics") or []
        weak_names = "、".join(str(item.get("metricName")) for item in weak_metrics[:3] if item.get("metricName"))
        suggestion = facts.get("suggestion") or "建议继续跟踪确认、到货和入库几个关键节点。"

        if plan.focus == "WEAK_METRIC" and weak_names:
            conclusion = f"{supplier_name} 主要短板是 {weak_names}。"
        elif plan.focus == "NEXT_ACTION":
            conclusion = f"下一步建议重点盯 {weak_names or '确认、到货、入库'}。"
        else:
            conclusion = f"{supplier_name} 当前得分 {score}，等级是“{level}”。"

        reasons = []
        if facts.get("levelExplain"):
            reasons.append(str(facts.get("levelExplain")))
        for item in weak_metrics[:3]:
            if item.get("reason"):
                reasons.append(str(item.get("reason")))

        return AnswerDraft(
            conclusion=conclusion,
            reasons=self._dedupe(reasons),
            nextActions=[suggestion],
        )

    def _knowledge_draft(self, evidence: AgentEvidence) -> AnswerDraft:
        if not evidence.items:
            return AnswerDraft(
                conclusion="我没有在知识库里检索到足够明确的规则资料。",
                limits=["当前只能确认没有命中可用资料，不能编造规则。"],
                nextActions=["可以换一种问法，或先导入对应规则文档到知识库。"],
            )

        titles = "、".join(str(item.get("title")) for item in evidence.items[:3] if item.get("title"))
        return AnswerDraft(
            conclusion=evidence.summary or "我查到了相关规则资料。",
            reasons=[f"命中的资料包括：{titles}。" if titles else "知识库返回了相关片段。"],
            nextActions=["下一步我会基于这些资料解释规则，不直接修改任何业务数据。"],
        )

    def render_draft(self, draft: AnswerDraft) -> str:
        parts = [draft.conclusion]
        if draft.reasons:
            parts.append("依据是：" + "；".join(self._trim_period(item) for item in draft.reasons if item))
        if draft.limits:
            parts.append("目前的边界是：" + "；".join(self._trim_period(item) for item in draft.limits if item))
        if draft.next_actions:
            parts.append("下一步建议：" + "；".join(self._trim_period(item) for item in draft.next_actions if item))
        return "\n\n".join(part for part in parts if part)

    def _missing_field_action(self, fields: list[str]) -> str:
        mapping = {
            "orderNo": "你把采购订单号发我，我就能继续查订单上下文。",
            "supplierId": "你把供应商 ID 发我，我就能继续查供应商履约上下文。",
        }
        return "；".join(mapping.get(field, f"请补充 {field}") for field in fields)

    def _evidence_lines(self, values: list[Any]) -> list[str]:
        lines = []
        for item in values:
            if isinstance(item, dict):
                label = item.get("label")
                value = item.get("value")
                explain = item.get("explain")
                if label and value:
                    lines.append(f"{label}为 {value}，{explain or '这是当前判断依据'}")
            elif item:
                lines.append(str(item))
        return lines

    def _strip_model_noise(self, text: str | None) -> str:
        value = (text or "").strip()
        if value.startswith("```"):
            value = value.strip("`").strip()
            for prefix in ("json", "text", "markdown"):
                if value.lower().startswith(prefix):
                    value = value[len(prefix):].strip()
        if value.startswith("{"):
            try:
                body = json.loads(value)
            except json.JSONDecodeError:
                return value
            for key in ("answer", "content", "text"):
                if body.get(key):
                    return str(body[key]).strip()
        return value

    def _trim_period(self, text: str) -> str:
        return str(text or "").strip().rstrip("。；;")

    def _dedupe(self, values: list[str]) -> list[str]:
        result = []
        seen = set()
        for value in values:
            text = str(value or "").strip()
            if not text or text in seen:
                continue
            seen.add(text)
            result.append(text)
        return result
