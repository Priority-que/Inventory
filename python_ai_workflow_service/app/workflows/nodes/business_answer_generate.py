import json
import re
from typing import Any

from app.clients.llm_client import LLMClient
from app.workflows.state import InteractionType, WorkflowStateKeys


INTERNAL_CODE_MAP = {
    "PURCHASE_ORDER_PURCHASER": "订单采购负责人",
    "ORDER_DIAGNOSIS": "订单诊断",
    "WARNING_SCAN": "风险扫描",
    "SUPPLIER_SCORE": "供应商履约分析",
    "WAIT_CONFIRM": "待供应商确认",
    "IN_PROGRESS": "执行中",
    "PARTIAL_ARRIVAL": "部分到货",
    "COMPLETED": "已完成",
    "PURCHASE_ORDER": "采购订单",
    "CONFIRM_RATE": "确认及时率",
    "ARRIVAL_COMPLETION_RATE": "到货完成率",
    "INBOUND_COMPLETION_RATE": "入库完成率",
    "ABNORMAL_ARRIVAL_RATE": "异常到货率",
    "PURCHASER": "采购侧",
    "WAREHOUSE": "仓库侧",
    "ROLE_ONLY": "只能定位到责任角色",
    "HIGH": "高风险",
    "MEDIUM": "中风险",
    "LOW": "低风险",
}

INTERNAL_CODE_RE = re.compile(
    r"(?<![A-Z0-9_])("
    + "|".join(re.escape(code) for code in sorted(INTERNAL_CODE_MAP, key=len, reverse=True))
    + r")(?![A-Z0-9_])"
)


class BusinessAnswerGenerateNode:
    def __init__(self, llm_client: LLMClient):
        self.llm_client = llm_client

    async def __call__(self, state: dict) -> dict:
        error_message = str(state.get(WorkflowStateKeys.ERROR_MESSAGE, ""))
        if error_message:
            return {WorkflowStateKeys.LLM_ANSWER: error_message}

        selected_context = dict(state.get(WorkflowStateKeys.SELECTED_CONTEXT, {}) or {})
        answer_card = dict(state.get(WorkflowStateKeys.ANSWER_CARD, {}) or {})
        response_policy = dict(state.get(WorkflowStateKeys.RESPONSE_POLICY, {}) or {})
        conversation_memory = dict(state.get(WorkflowStateKeys.CONVERSATION_MEMORY, {}) or {})
        interaction_type = selected_context.get("interactionType", InteractionType.BUSINESS.value)

        if interaction_type != InteractionType.BUSINESS.value:
            return {
                WorkflowStateKeys.LLM_ANSWER: self._non_business_answer(
                    interaction_type,
                    response_policy,
                    conversation_memory,
                )
            }

        answer = await self._focused_answer(
            selected_context,
            answer_card,
            str(state.get(WorkflowStateKeys.MESSAGE, "")),
            response_policy,
        )
        return {WorkflowStateKeys.LLM_ANSWER: answer}

    def _non_business_answer(self, interaction_type: str, response_policy: dict, conversation_memory: dict) -> str:
        if interaction_type == InteractionType.SOCIAL.value:
            opening = response_policy.get("opening") or "好，我接着陪你看。"
            closing = response_policy.get("closingOffer")
            if closing:
                return self._sanitize_answer(f"{opening}{closing}")
            if conversation_memory.get("lastBizKey"):
                return self._sanitize_answer(f"{opening}你继续问，我会接着 {conversation_memory.get('lastBizKey')} 这条线看。")
            return opening
        if interaction_type == InteractionType.META.value:
            return (
                "我是你的采购入库协同 Agent。"
                "我不只做查询摘要，也会按当前数据帮你拆业务卡点、风险优先级和供应商履约问题；"
                "你情绪着急或不确定时，也可以直接把问题丢给我，我会陪你一步步往下看。"
            )
        return "我需要一个更具体的业务对象，比如订单号、供应商ID，或者一个明确的扫描范围。"

    async def _focused_answer(self, selected_context: dict, answer_card: dict, message: str, response_policy: dict) -> str:
        draft = self._build_answer_draft(selected_context, answer_card, message)
        fallback = self._fallback_answer(draft, response_policy)

        if not self._should_use_llm(selected_context):
            return self._sanitize_answer(fallback)

        llm_answer = await self._generate_with_llm(selected_context, draft, response_policy)
        if llm_answer:
            return llm_answer
        return self._sanitize_answer(fallback)

    def _should_use_llm(self, selected_context: dict) -> bool:
        if selected_context.get("useLlm") is False:
            return False

        is_configured = getattr(self.llm_client, "is_configured", None)
        if callable(is_configured):
            return bool(is_configured())
        return True

    async def _generate_with_llm(self, selected_context: dict, draft: dict, response_policy: dict) -> str | None:
        system_prompt = (
            "你是采购入库协同业务 Agent，只负责把结构化业务事实转成用户能直接执行的中文回答。"
            "必须遵守：1. 只使用输入事实，不编造人名、时间、数量；"
            "2. 用户问什么就答什么，不展开成长报告；"
            "3. 如果事实不足，明确说边界；"
            "4. 不输出内部码、字段名、JSON、Markdown 标题；"
            "5. 回答结构按“结论 -> 关键依据 -> 下一步”自然组织，缺哪段就省略；"
            "6. 要有专属业务伙伴的承接感，但不要油腻、不要空泛安慰。"
        )
        user_prompt = self._build_llm_prompt(selected_context, draft, response_policy)

        try:
            raw_answer = await self.llm_client.chat_text(system_prompt, user_prompt, temperature=0.35)
        except Exception:
            return None

        answer = self._strip_model_noise(raw_answer)
        answer = self._sanitize_answer(answer)
        if self._is_unusable_llm_answer(answer):
            return None
        return answer

    def _build_llm_prompt(self, selected_context: dict, draft: dict, response_policy: dict) -> str:
        payload = {
            "用户问题": draft.get("userQuestion"),
            "回答目标": draft.get("answerGoal"),
            "陪伴与表达策略": response_policy,
            "已整理好的答案草稿": draft,
            "可用业务上下文": self._compact_llm_context(selected_context),
        }
        return (
            "请基于下面 JSON 生成最终 answer。不要复述 JSON，不要暴露字段名。\n"
            f"{json.dumps(payload, ensure_ascii=False, default=str)}"
        )

    def _build_answer_draft(self, selected_context: dict, answer_card: dict, message: str) -> dict:
        if answer_card:
            return self._draft_from_answer_card(answer_card, selected_context, message)

        intent = selected_context.get("intent")
        if intent == "ORDER_DIAGNOSIS":
            return self._order_answer_draft(selected_context, message)
        if intent == "WARNING_SCAN":
            return self._warning_answer_draft(selected_context, message)
        if intent == "SUPPLIER_SCORE":
            return self._supplier_answer_draft(selected_context, message)

        return {
            "userQuestion": message,
            "answerGoal": "提示用户补充业务对象",
            "conclusion": selected_context.get("summary") or "当前问题暂时无法生成明确回答。",
            "reasons": [],
            "unknowns": [],
            "nextActions": [],
        }

    def _draft_from_answer_card(self, answer_card: dict, selected_context: dict, message: str) -> dict:
        question_focus = answer_card.get("questionFocus") or selected_context.get("questionFocus")
        instruction = selected_context.get("instruction") or ""
        answer_goal = instruction or self._answer_goal_by_focus(question_focus)

        reasons = list(answer_card.get("reasons") or [])
        evidence = list(answer_card.get("evidence") or [])
        if evidence:
            reasons = reasons + evidence

        return {
            "userQuestion": message,
            "intent": answer_card.get("intent"),
            "questionFocus": question_focus,
            "answerGoal": answer_goal,
            "conclusion": answer_card.get("conclusion") or selected_context.get("summary") or "当前问题暂时无法生成明确回答。",
            "reasons": self._dedupe_texts(reasons),
            "unknowns": self._dedupe_texts(answer_card.get("unknowns") or []),
            "nextActions": self._dedupe_texts(answer_card.get("nextActions") or []),
            "toneHint": answer_card.get("companionHint"),
        }

    def _answer_goal_by_focus(self, focus: str | None) -> str:
        mapping = {
            "OWNER": "只回答谁跟进，并说明是否能精确到具体人",
            "OWNER_REASON": "解释为什么是这个人或这个角色，不编造缺失负责人",
            "NEXT_ACTION": "只回答下一步谁处理、怎么处理",
            "CAUSE": "解释为什么没完成、卡在哪，并给一句可执行动作",
            "EVIDENCE": "只解释判断依据",
            "FULL_DIAGNOSIS": "给出简洁完整的订单诊断",
            "WARNING_SUMMARY": "回答总风险概况、最值得先处理的风险类型、建议先由谁处理",
            "TOP_RISK": "回答最该先处理的几单，以及为什么优先",
            "SPECIFIC_WARNING_REASON": "解释某张单为什么风险高、为什么优先、由谁处理",
            "WARNING_OWNER": "解释主要由哪些角色处理这些风险",
            "WARNING_PRIORITY_REASON": "解释为什么这些风险优先级更高",
            "WARNING_ACTION": "告诉用户接下来怎么处理这些风险",
            "SUPPLIER_FULL_ANALYSIS": "给出完整但简洁的履约分析，直接指出主要短板",
            "SCORE_MEANING": "解释这个分数和等级意味着什么",
            "SCORE_REASON": "解释分数怎么来的，哪些指标影响最大",
            "WEAK_METRIC": "直接指出差在哪、哪个指标拖后腿",
            "COOP_ADVICE": "说明还能不能继续合作以及理由",
            "SUPPLIER_ACTION": "告诉用户下一步怎么管控或改善",
        }
        return mapping.get(focus or "", "按用户问题回答。")

    def _order_answer_draft(self, selected_context: dict, message: str) -> dict:
        focus = selected_context.get("questionFocus")
        facts = selected_context.get("facts") or {}

        order_no = facts.get("orderNo") or "这张订单"
        current_stage = facts.get("currentStage") or "当前阶段未知"
        block_reason = facts.get("blockReason") or "当前原因未知"
        responsibility = facts.get("responsibility") or {}
        next_action = facts.get("nextAction") or {}
        evidence = facts.get("evidence") or []

        owner_role_name = responsibility.get("ownerRoleName") or "对应责任方"
        owner_user_name = responsibility.get("ownerUserName")
        owner_reason = responsibility.get("ownerReason") or block_reason
        owner_text = f"{owner_user_name}（{owner_role_name}）" if owner_user_name else owner_role_name
        action_text = next_action.get("actionText") or "继续跟进当前流程。"

        reasons = [f"当前阶段是“{current_stage}”。", block_reason]
        if owner_reason and owner_reason not in reasons:
            reasons.append(owner_reason)
        reasons.extend(evidence[:3])

        unknowns = []
        if focus in {"OWNER", "OWNER_REASON"} and not owner_user_name:
            unknowns.append(f"当前上下文没有具体负责人姓名，只能定位到责任角色是{owner_role_name}。")

        if focus == "OWNER":
            conclusion = f"{order_no} 建议先让 {owner_text} 跟进。"
            answer_goal = "只回答谁跟进，并说明是否能精确到具体人"
            next_actions = [action_text]
        elif focus == "OWNER_REASON":
            conclusion = f"{order_no} 建议让 {owner_text} 跟进。"
            answer_goal = "解释为什么是这个人或这个角色，不编造缺失负责人"
            next_actions = [action_text]
        elif focus == "NEXT_ACTION":
            conclusion = f"{order_no} 下一步先由 {owner_text} 处理。"
            answer_goal = "只回答下一步谁处理、怎么处理"
            next_actions = [action_text]
        elif focus == "CAUSE":
            conclusion = f"{order_no} 没完成的核心原因是：{block_reason}"
            answer_goal = "解释为什么没完成、卡在哪，并给一句可执行动作"
            next_actions = [action_text]
        elif focus == "EVIDENCE":
            conclusion = f"{order_no} 的判断依据主要来自订单状态、到货数量和入库数量。"
            answer_goal = "只解释判断依据"
            next_actions = []
        else:
            conclusion = f"{order_no} 当前卡在“{current_stage}”。"
            answer_goal = "给出简洁完整的订单诊断"
            next_actions = [action_text]

        return {
            "userQuestion": message,
            "intent": "订单诊断",
            "questionFocus": focus,
            "answerGoal": answer_goal,
            "conclusion": conclusion,
            "reasons": self._dedupe_texts(reasons),
            "unknowns": unknowns,
            "nextActions": self._dedupe_texts(next_actions),
            "toneHint": "像懂采购执行流程的人在解释，不要像模板报告。",
        }

    def _warning_answer_draft(self, selected_context: dict, message: str) -> dict:
        focus = selected_context.get("questionFocus")
        facts = selected_context.get("facts") or {}
        items = selected_context.get("items") or []
        summary_stats = facts.get("summaryStats") or {}
        owner_stats = facts.get("ownerStats") or []
        risk_type_stats = facts.get("riskTypeStats") or []

        if focus == "SPECIFIC_WARNING_REASON":
            if not items:
                return {
                    "userQuestion": message,
                    "intent": "风险扫描",
                    "questionFocus": focus,
                    "answerGoal": "说明当前扫描结果找不到目标单据",
                    "conclusion": "我没有在本次扫描结果里找到这张单。",
                    "reasons": ["它可能不在当前扫描范围内，或者这轮扫描没有命中它的风险。"],
                    "unknowns": ["当前缺少这张单的风险明细。"],
                    "nextActions": ["如果要继续查，请重新扫描更大的时间范围，或直接发起这张订单的订单诊断。"],
                }

            item = items[0]
            biz_no = item.get("bizNo") or "这张单"
            risk_level_name = item.get("riskLevelName") or "风险"
            problem = item.get("problem") or "执行异常"
            reason = item.get("reason") or "当前命中了风险规则。"
            priority_reason = item.get("priorityReason")
            owner = item.get("suggestOwnerName") or "对应责任方"
            action = item.get("suggestAction") or "请尽快处理。"
            reasons = [reason, f"命中的风险类型是“{problem}”。"]
            if priority_reason:
                reasons.append(priority_reason)
            if item.get("overdueDays") is not None:
                reasons.append(f"已超时 {item.get('overdueDays')} 天。")

            return {
                "userQuestion": message,
                "intent": "风险扫描",
                "questionFocus": focus,
                "answerGoal": "解释某张单为什么风险高、为什么优先、由谁处理",
                "conclusion": f"{biz_no} 被判为{risk_level_name}，主要问题是“{problem}”。",
                "reasons": self._dedupe_texts(reasons),
                "unknowns": [],
                "nextActions": [f"建议由{owner}处理：{action}"],
            }

        total_count = summary_stats.get("totalCount", 0)
        high_count = summary_stats.get("highCount", 0)
        medium_count = summary_stats.get("mediumCount", 0)
        top_items = [item for item in items[:5] if item.get("bizNo")]
        top_biz = "、".join(item.get("bizNo") for item in top_items) or "暂无明确单据"
        top_owner = owner_stats[0].get("ownerRoleName") if owner_stats else "对应责任方"
        top_risk_types = "、".join(
            item.get("problem") for item in risk_type_stats[:3] if item.get("problem")
        ) or "暂无集中类型"

        if focus == "TOP_RISK":
            conclusion = f"如果现在先处理一批，建议先看：{top_biz}。"
            answer_goal = "回答最该先处理的几单，以及为什么优先"
        elif focus == "WARNING_OWNER":
            owner_parts = [f"{item.get('ownerRoleName')} {item.get('count')} 个" for item in owner_stats]
            conclusion = f"这批风险主要由{top_owner}先牵头处理。"
            answer_goal = "解释主要由哪些角色处理这些风险"
            return {
                "userQuestion": message,
                "intent": "风险扫描",
                "questionFocus": focus,
                "answerGoal": answer_goal,
                "conclusion": conclusion,
                "reasons": owner_parts or ["当前还没有足够信息拆分责任角色。"],
                "unknowns": [],
                "nextActions": [f"先让{top_owner}处理高优先级风险，再推进剩余风险。"],
            }
        elif focus == "WARNING_ACTION":
            conclusion = f"下一步先处理高优先级风险单据：{top_biz}。"
            answer_goal = "告诉用户接下来怎么处理这些风险"
        elif focus == "WARNING_PRIORITY_REASON":
            conclusion = f"当前优先级最高的几单是：{top_biz}。"
            answer_goal = "解释为什么这些风险优先级更高"
        else:
            conclusion = f"本次扫描共发现 {total_count} 个执行风险，其中高风险 {high_count} 个，中风险 {medium_count} 个。"
            answer_goal = "回答总风险概况、最值得先处理的风险类型、建议先由谁处理"

        reasons = [f"风险最集中的类型是：{top_risk_types}。"]
        reasons.extend(item.get("priorityReason") for item in top_items if item.get("priorityReason"))

        return {
            "userQuestion": message,
            "intent": "风险扫描",
            "questionFocus": focus,
            "answerGoal": answer_goal,
            "conclusion": conclusion,
            "reasons": self._dedupe_texts(reasons),
            "unknowns": [],
            "nextActions": [f"建议先由{top_owner}处理高风险，再处理中风险。"],
        }

    def _supplier_answer_draft(self, selected_context: dict, message: str) -> dict:
        focus = selected_context.get("questionFocus")
        facts = selected_context.get("facts") or {}

        supplier_name = facts.get("supplierName") or "该供应商"
        score = facts.get("score")
        level = facts.get("level") or "未知等级"
        level_explain = facts.get("levelExplain")
        breakdown = facts.get("scoreBreakdown") or []
        weak_metrics = facts.get("weakMetrics") or []
        suggestion = facts.get("suggestion")
        analysis = facts.get("analysis")

        weak_names = self._weak_metric_names(weak_metrics, breakdown)
        metric_lines = self._score_breakdown_lines(breakdown)
        weak_lines = self._weak_metric_lines(weak_metrics)
        rate_lines = self._supplier_rate_lines(facts)

        if focus == "SCORE_MEANING":
            conclusion = f"{supplier_name} 当前得分 {score}，等级是“{level}”。"
            answer_goal = "解释这个分数和等级意味着什么"
            reasons = [level_explain or "这个等级说明供应商还可合作，但履约稳定性需要继续观察。"]
            next_actions = [suggestion] if suggestion else []
        elif focus == "SCORE_REASON":
            conclusion = f"{supplier_name} 得分 {score}，主要是被 {weak_names} 拉低。"
            answer_goal = "解释分数怎么来的，哪些指标影响最大"
            reasons = metric_lines + rate_lines + ([analysis] if analysis else [])
            next_actions = [suggestion] if suggestion else []
        elif focus == "WEAK_METRIC":
            conclusion = f"{supplier_name} 主要差在 {weak_names}。"
            answer_goal = "直接指出差在哪、哪个指标拖后腿"
            reasons = weak_lines or metric_lines or rate_lines
            next_actions = self._weak_metric_suggestions(weak_metrics) or ([suggestion] if suggestion else [])
        elif focus == "COOP_ADVICE":
            conclusion = suggestion or f"{supplier_name} 可以继续合作，但需要带着短板做过程管控。"
            answer_goal = "说明还能不能继续合作以及理由"
            reasons = weak_lines or metric_lines
            next_actions = self._weak_metric_suggestions(weak_metrics)
        elif focus == "SUPPLIER_ACTION":
            conclusion = f"下一步建议重点盯 {weak_names}。"
            answer_goal = "告诉用户下一步怎么管控或改善"
            reasons = weak_lines or metric_lines
            next_actions = self._weak_metric_suggestions(weak_metrics) or ["继续关注确认、到货、入库三个节点。"]
        else:
            conclusion = f"{supplier_name} 当前得分 {score}，等级“{level}”，主要短板是 {weak_names}。"
            answer_goal = "给出完整但简洁的履约分析，直接指出主要短板"
            reasons = metric_lines + weak_lines + ([analysis] if analysis else [])
            next_actions = [suggestion] if suggestion else self._weak_metric_suggestions(weak_metrics)

        return {
            "userQuestion": message,
            "intent": "供应商履约分析",
            "questionFocus": focus,
            "answerGoal": answer_goal,
            "conclusion": conclusion,
            "reasons": self._dedupe_texts(reasons),
            "unknowns": [],
            "nextActions": self._dedupe_texts(next_actions),
            "toneHint": "把评分拆成业务原因，不要只报分数。",
        }

    def _fallback_answer(self, draft: dict, response_policy: dict | None = None) -> str:
        response_policy = response_policy or {}
        parts = []
        opening = str(response_policy.get("opening") or "").strip()
        if opening:
            parts.append(opening)

        parts.append(str(draft.get("conclusion") or "").strip())

        reasons = self._dedupe_texts(draft.get("reasons") or [])
        unknowns = self._dedupe_texts(draft.get("unknowns") or [])
        next_actions = self._dedupe_texts(draft.get("nextActions") or [])

        if reasons:
            parts.append("我判断的依据是：" + self._join_points(reasons))
        if unknowns:
            parts.append("目前的边界是：" + self._join_points(unknowns))
        if next_actions:
            parts.append("下一步建议：" + self._join_points(next_actions))
        closing_offer = str(response_policy.get("closingOffer") or "").strip()
        if closing_offer:
            parts.append(closing_offer)

        return "\n\n".join(part for part in parts if part)

    def _strip_model_noise(self, text: str | None) -> str:
        value = (text or "").strip()
        if not value:
            return ""

        if value.startswith("```"):
            value = re.sub(r"^```(?:json|text|markdown)?", "", value, flags=re.IGNORECASE).strip()
            value = re.sub(r"```$", "", value).strip()

        if value.startswith("{"):
            try:
                body = json.loads(value)
            except json.JSONDecodeError:
                return value
            for key in ("answer", "content", "text"):
                if body.get(key):
                    return str(body[key]).strip()

        return value

    def _sanitize_answer(self, answer: str) -> str:
        return INTERNAL_CODE_RE.sub(lambda match: INTERNAL_CODE_MAP[match.group(1)], answer or "")

    def _is_unusable_llm_answer(self, answer: str) -> bool:
        if not answer or "LLM 未配置" in answer:
            return True
        if INTERNAL_CODE_RE.search(answer):
            return True
        return False

    def _humanize_value(self, value: Any) -> Any:
        if isinstance(value, dict):
            return {key: self._humanize_value(item) for key, item in value.items()}
        if isinstance(value, list):
            return [self._humanize_value(item) for item in value]
        if isinstance(value, str):
            return self._sanitize_answer(value)
        return value

    def _compact_llm_context(self, selected_context: dict) -> dict:
        intent = selected_context.get("intent")
        facts = dict(selected_context.get("facts") or {})
        items = list(selected_context.get("items") or [])

        if intent == "ORDER_DIAGNOSIS":
            return self._humanize_value(
                {
                    "intent": intent,
                    "questionFocus": selected_context.get("questionFocus"),
                    "orderNo": facts.get("orderNo"),
                    "currentStage": facts.get("currentStage"),
                    "blockReason": facts.get("blockReason"),
                    "responsibility": facts.get("responsibility"),
                    "nextAction": facts.get("nextAction"),
                    "evidence": (facts.get("evidence") or [])[:3],
                }
            )

        if intent == "WARNING_SCAN":
            trimmed_items = [
                {
                    "bizNo": item.get("bizNo"),
                    "riskLevelName": item.get("riskLevelName"),
                    "problem": item.get("problem"),
                    "reason": item.get("reason"),
                    "priorityReason": item.get("priorityReason"),
                    "suggestOwnerName": item.get("suggestOwnerName"),
                    "suggestAction": item.get("suggestAction"),
                    "overdueDays": item.get("overdueDays"),
                }
                for item in items[:3]
            ]
            return self._humanize_value(
                {
                    "intent": intent,
                    "questionFocus": selected_context.get("questionFocus"),
                    "summaryStats": facts.get("summaryStats"),
                    "ownerStats": (facts.get("ownerStats") or [])[:3],
                    "riskTypeStats": (facts.get("riskTypeStats") or [])[:3],
                    "items": trimmed_items,
                }
            )

        if intent == "SUPPLIER_SCORE":
            return self._humanize_value(
                {
                    "intent": intent,
                    "questionFocus": selected_context.get("questionFocus"),
                    "supplierName": facts.get("supplierName"),
                    "score": facts.get("score"),
                    "level": facts.get("level"),
                    "levelExplain": facts.get("levelExplain"),
                    "confirmRate": facts.get("confirmRate"),
                    "arrivalCompletionRate": facts.get("arrivalCompletionRate"),
                    "inboundCompletionRate": facts.get("inboundCompletionRate"),
                    "abnormalArrivalRate": facts.get("abnormalArrivalRate"),
                    "scoreBreakdown": (facts.get("scoreBreakdown") or [])[:4],
                    "weakMetrics": (facts.get("weakMetrics") or [])[:4],
                    "analysis": facts.get("analysis"),
                    "suggestion": facts.get("suggestion"),
                }
            )

        return self._humanize_value(
            {
                "intent": intent,
                "questionFocus": selected_context.get("questionFocus"),
                "summary": selected_context.get("summary"),
            }
        )

    def _score_breakdown_lines(self, breakdown: list[dict]) -> list[str]:
        lines = []
        for item in breakdown:
            metric_name = item.get("metricName")
            actual_score = item.get("actualScore")
            max_score = item.get("maxScore")
            value = item.get("value")
            explain = item.get("explain")
            if not metric_name:
                continue
            score_text = f"{actual_score}/{max_score} 分" if actual_score is not None and max_score is not None else ""
            value_text = f"，指标值 {value}" if value else ""
            explain_text = f"，{explain}" if explain else ""
            lines.append(f"{metric_name}{score_text}{value_text}{explain_text}。")
        return lines

    def _weak_metric_names(self, weak_metrics: list[dict], breakdown: list[dict]) -> str:
        names = [item.get("metricName") for item in weak_metrics if item.get("metricName")]
        if not names:
            names = [
                item.get("metricName")
                for item in breakdown
                if item.get("metricName") and (item.get("actualScore") or 0) < (item.get("maxScore") or 0)
            ]
        return "、".join(names[:3]) if names else "暂无明显单项短板"

    def _weak_metric_lines(self, weak_metrics: list[dict]) -> list[str]:
        lines = []
        for item in weak_metrics:
            metric_name = item.get("metricName")
            reason = item.get("reason")
            if metric_name and reason:
                lines.append(f"{metric_name}：{reason}。")
        return lines

    def _weak_metric_suggestions(self, weak_metrics: list[dict]) -> list[str]:
        return [
            item.get("suggestion")
            for item in weak_metrics
            if item.get("suggestion")
        ]

    def _supplier_rate_lines(self, facts: dict) -> list[str]:
        mapping = [
            ("confirmRate", "确认及时率"),
            ("arrivalCompletionRate", "到货完成率"),
            ("inboundCompletionRate", "入库完成率"),
            ("abnormalArrivalRate", "异常到货率"),
        ]
        return [
            f"{name}为 {facts.get(key)}。"
            for key, name in mapping
            if facts.get(key) not in (None, "")
        ]

    def _dedupe_texts(self, values: list[Any]) -> list[str]:
        result = []
        seen = set()
        for value in values:
            text = str(value or "").strip()
            if not text or text in seen:
                continue
            seen.add(text)
            result.append(text)
        return result

    def _join_points(self, values: list[str]) -> str:
        cleaned = [str(value or "").strip().rstrip("。；;") for value in values if str(value or "").strip()]
        return "；".join(cleaned)
