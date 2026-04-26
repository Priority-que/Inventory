from app.schemas.answer_card import AnswerCard
from app.workflows.state import WorkflowStateKeys


class BuildAnswerCardNode:
    async def __call__(self, state: dict) -> dict:
        selected = dict(state.get(WorkflowStateKeys.SELECTED_CONTEXT, {}) or {})
        intent = selected.get("intent")

        if intent == "ORDER_DIAGNOSIS":
            card = self._build_order_card(selected)
        elif intent == "WARNING_SCAN":
            card = self._build_warning_card(selected)
        elif intent == "SUPPLIER_SCORE":
            card = self._build_supplier_card(selected)
        else:
            card = AnswerCard(
                intent=intent or "UNKNOWN",
                questionFocus=selected.get("questionFocus", "CLARIFY"),
                bizType=selected.get("bizType"),
                bizKey=selected.get("bizKey"),
                conclusion=selected.get("summary") or "当前信息不足。",
                companionHint="先把具体业务对象补上，我再继续帮你往下拆。",
            )

        return {WorkflowStateKeys.ANSWER_CARD: card.model_dump(by_alias=True)}

    def _build_order_card(self, selected: dict) -> AnswerCard:
        focus = selected.get("questionFocus", "FULL_DIAGNOSIS")
        facts = selected.get("facts") or {}
        responsibility = facts.get("responsibility") or {}
        next_action = facts.get("nextAction") or {}

        order_no = facts.get("orderNo") or "这张订单"
        current_stage = facts.get("currentStage") or "当前阶段未知"
        block_reason = facts.get("blockReason") or "当前原因未知"
        owner_role_name = responsibility.get("ownerRoleName") or "对应责任方"
        owner_user_name = responsibility.get("ownerUserName")
        owner_reason = responsibility.get("ownerReason") or block_reason
        owner_text = f"{owner_user_name}（{owner_role_name}）" if owner_user_name else owner_role_name
        action_text = next_action.get("actionText") or "继续跟进当前流程。"

        reasons = [f"当前阶段是“{current_stage}”。", block_reason]
        if owner_reason and owner_reason not in reasons:
            reasons.append(owner_reason)

        evidence = list(facts.get("evidence") or [])[:3]
        unknowns = []
        next_actions = []

        if focus == "OWNER":
            conclusion = f"{order_no} 建议先让 {owner_text} 跟进。"
            next_actions = [action_text]
        elif focus == "OWNER_REASON":
            conclusion = f"{order_no} 建议让 {owner_text} 跟进。"
            next_actions = [action_text]
        elif focus == "NEXT_ACTION":
            conclusion = f"{order_no} 下一步先由 {owner_text} 处理。"
            next_actions = [action_text]
        elif focus == "CAUSE":
            conclusion = f"{order_no} 没完成的核心原因是：{block_reason}"
            next_actions = [action_text]
        elif focus == "EVIDENCE":
            conclusion = f"{order_no} 的判断依据主要来自订单状态、到货数量和入库数量。"
        else:
            conclusion = f"{order_no} 当前卡在“{current_stage}”。"
            next_actions = [action_text]

        if focus in {"OWNER", "OWNER_REASON"} and not owner_user_name:
            unknowns.append(f"当前还不能定位到具体负责人，只能判断到责任角色是{owner_role_name}。")

        return AnswerCard(
            intent="ORDER_DIAGNOSIS",
            questionFocus=focus,
            bizType=selected.get("bizType"),
            bizKey=selected.get("bizKey"),
            conclusion=conclusion,
            reasons=self._dedupe_texts(reasons),
            evidence=self._dedupe_texts(evidence),
            unknowns=self._dedupe_texts(unknowns),
            nextActions=self._dedupe_texts(next_actions),
            companionHint="如果用户继续追问，可以顺着责任人、催办顺序或沟通话术往下拆。",
        )

    def _build_warning_card(self, selected: dict) -> AnswerCard:
        focus = selected.get("questionFocus", "WARNING_SUMMARY")
        facts = selected.get("facts") or {}
        items = list(selected.get("items") or [])
        summary_stats = facts.get("summaryStats") or {}
        owner_stats = facts.get("ownerStats") or []
        risk_type_stats = facts.get("riskTypeStats") or []

        if focus == "SPECIFIC_WARNING_REASON":
            if not items:
                return AnswerCard(
                    intent="WARNING_SCAN",
                    questionFocus=focus,
                    bizType=selected.get("bizType"),
                    bizKey=selected.get("bizKey"),
                    conclusion="我没有在当前扫描结果里找到这张单。",
                    reasons=["它可能不在当前扫描范围内，或者这轮扫描没有命中它的风险。"],
                    unknowns=["当前缺少这张单的风险明细。"],
                    nextActions=["如果要继续查，可以扩大扫描时间范围，或者切回订单诊断单独看。"],
                    companionHint="如果用户继续追问，优先解释扫描范围和切换调查路径。",
                )

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

            return AnswerCard(
                intent="WARNING_SCAN",
                questionFocus=focus,
                bizType=selected.get("bizType"),
                bizKey=selected.get("bizKey"),
                conclusion=f"{biz_no} 被判为{risk_level_name}，主要问题是“{problem}”。",
                reasons=self._dedupe_texts(reasons),
                nextActions=[f"建议由{owner}处理：{action}"],
                companionHint="如果用户继续追问，优先解释优先级依据和处理顺序。",
            )

        total_count = summary_stats.get("totalCount", 0)
        high_count = summary_stats.get("highCount", 0)
        medium_count = summary_stats.get("mediumCount", 0)
        top_items = [item for item in items[:5] if item.get("bizNo")]
        top_biz = "、".join(item.get("bizNo") for item in top_items) or "暂无明确单据"
        top_owner = owner_stats[0].get("ownerRoleName") if owner_stats else "对应责任方"
        top_risk_types = "、".join(item.get("problem") for item in risk_type_stats[:3] if item.get("problem")) or "暂无集中类型"

        if focus == "TOP_RISK":
            conclusion = f"如果现在先处理一批，建议先看：{top_biz}。"
        elif focus == "WARNING_OWNER":
            owner_parts = [f"{item.get('ownerRoleName')} {item.get('count')} 个" for item in owner_stats]
            return AnswerCard(
                intent="WARNING_SCAN",
                questionFocus=focus,
                bizType=selected.get("bizType"),
                bizKey=selected.get("bizKey"),
                conclusion=f"这批风险主要由{top_owner}先牵头处理。",
                reasons=self._dedupe_texts(owner_parts or ["当前还没有足够信息拆分责任角色。"]),
                nextActions=[f"先让{top_owner}处理高优先级风险，再推进剩余风险。"],
                companionHint="如果用户继续追问，优先把责任方拆成批次和顺序。",
            )
        elif focus == "WARNING_ACTION":
            conclusion = f"下一步先处理高优先级风险单据：{top_biz}。"
        elif focus == "WARNING_PRIORITY_REASON":
            conclusion = f"当前优先级最高的几单是：{top_biz}。"
        else:
            conclusion = f"本次扫描共发现 {total_count} 个执行风险，其中高风险 {high_count} 个，中风险 {medium_count} 个。"

        reasons = [f"风险最集中的类型是：{top_risk_types}。"]
        reasons.extend(item.get("priorityReason") for item in top_items if item.get("priorityReason"))

        return AnswerCard(
            intent="WARNING_SCAN",
            questionFocus=focus,
            bizType=selected.get("bizType"),
            bizKey=selected.get("bizKey"),
            conclusion=conclusion,
            reasons=self._dedupe_texts(reasons),
            nextActions=[f"建议先由{top_owner}处理高风险，再处理中风险。"],
            companionHint="如果用户继续追问，优先往优先级、责任方和处理顺序展开。",
        )

    def _build_supplier_card(self, selected: dict) -> AnswerCard:
        focus = selected.get("questionFocus", "SUPPLIER_FULL_ANALYSIS")
        facts = selected.get("facts") or {}

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
        evidence = self._supplier_rate_lines(facts)

        if focus == "SCORE_MEANING":
            conclusion = f"{supplier_name} 当前得分 {score}，等级是“{level}”。"
            reasons = [level_explain or "这个等级说明供应商还能合作，但履约稳定性需要继续观察。"]
            next_actions = [suggestion] if suggestion else []
        elif focus == "SCORE_REASON":
            conclusion = f"{supplier_name} 得分 {score}，主要是被 {weak_names} 拉低。"
            reasons = metric_lines + ([analysis] if analysis else [])
            next_actions = [suggestion] if suggestion else []
        elif focus == "WEAK_METRIC":
            conclusion = f"{supplier_name} 主要差在 {weak_names}。"
            reasons = weak_lines or metric_lines or evidence
            next_actions = self._weak_metric_suggestions(weak_metrics) or ([suggestion] if suggestion else [])
        elif focus == "COOP_ADVICE":
            conclusion = suggestion or f"{supplier_name} 可以继续合作，但需要带着短板做过程管控。"
            reasons = weak_lines or metric_lines
            next_actions = self._weak_metric_suggestions(weak_metrics)
        elif focus == "SUPPLIER_ACTION":
            conclusion = f"下一步建议重点盯 {weak_names}。"
            reasons = weak_lines or metric_lines
            next_actions = self._weak_metric_suggestions(weak_metrics) or ["继续关注确认、到货、入库三个节点。"]
        else:
            conclusion = f"{supplier_name} 当前得分 {score}，等级“{level}”，主要短板是 {weak_names}。"
            reasons = metric_lines + weak_lines + ([analysis] if analysis else [])
            next_actions = [suggestion] if suggestion else self._weak_metric_suggestions(weak_metrics)

        return AnswerCard(
            intent="SUPPLIER_SCORE",
            questionFocus=focus,
            bizType=selected.get("bizType"),
            bizKey=selected.get("bizKey"),
            conclusion=conclusion,
            reasons=self._dedupe_texts(reasons),
            evidence=self._dedupe_texts(evidence),
            nextActions=self._dedupe_texts(next_actions),
            companionHint="如果用户继续追问，优先往短板指标、提升动作和后续跟踪项展开。",
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
        return [item.get("suggestion") for item in weak_metrics if item.get("suggestion")]

    def _supplier_rate_lines(self, facts: dict) -> list[str]:
        mapping = [
            ("confirmRate", "确认及时率"),
            ("arrivalCompletionRate", "到货完成率"),
            ("inboundCompletionRate", "入库完成率"),
            ("abnormalArrivalRate", "异常到货率"),
        ]
        return [f"{name}为 {facts.get(key)}。" for key, name in mapping if facts.get(key) not in (None, "")]

    def _dedupe_texts(self, values: list[str]) -> list[str]:
        result = []
        seen = set()
        for value in values:
            text = str(value or "").strip()
            if not text or text in seen:
                continue
            seen.add(text)
            result.append(text)
        return result
