from app.agent_v2.schemas import AgentMemory, AgentPlan, ConversationUnderstanding, ResolvedSlots


class Planner:
    def plan(
        self,
        message: str,
        understanding: ConversationUnderstanding,
        slots: ResolvedSlots,
        memory: AgentMemory,
    ) -> AgentPlan:
        interaction_type = understanding.interaction_type

        if interaction_type == "CLARIFY":
            return self._simple_plan(interaction_type, "CLARIFY", "ASK_INPUT", slots, "用户输入为空")

        if interaction_type == "CHAT":
            return self._simple_plan(interaction_type, "CHAT", "CASUAL_REPLY", slots, "普通对话不需要业务工具")

        if interaction_type == "EMOTION":
            return self._simple_plan(interaction_type, "EMOTION_SUPPORT", "EMPATHY", slots, "情绪表达先做承接")

        if interaction_type == "RULE_QA":
            return AgentPlan(
                interactionType=interaction_type,
                task="KNOWLEDGE_QA",
                focus="RULE_EXPLAIN",
                toolNames=["search_knowledge"],
                slots=slots,
                missingFields=[],
                canExecute=True,
                reason="规则/流程类问题，先检索知识库或规则资料",
            )

        task = self._infer_business_task(message, slots, memory)
        focus = self._infer_focus(message, task, understanding)
        tool_names = self._tools_for_task(task)
        missing_fields = self._missing_fields(task, slots)

        return AgentPlan(
            interactionType=interaction_type,
            task=task,
            focus=focus,
            toolNames=tool_names,
            slots=slots,
            missingFields=missing_fields,
            canExecute=len(missing_fields) == 0 and task != "CLARIFY",
            reason=self._reason(task, focus, slots, missing_fields),
        )

    def _simple_plan(
        self,
        interaction_type: str,
        task: str,
        focus: str,
        slots: ResolvedSlots,
        reason: str,
    ) -> AgentPlan:
        return AgentPlan(
            interactionType=interaction_type,
            task=task,
            focus=focus,
            toolNames=[],
            slots=slots,
            missingFields=[],
            canExecute=False,
            reason=reason,
        )

    def _infer_business_task(self, message: str, slots: ResolvedSlots, memory: AgentMemory) -> str:
        text = message or ""
        if slots.supplier_id is not None or self._contains_any(text, ["供应商", "履约", "评分", "合作"]):
            return "SUPPLIER_SCORE"
        if self._contains_any(text, ["风险", "预警", "高风险", "中风险", "先处理", "优先级"]):
            return "WARNING_SCAN"
        if slots.order_no or self._contains_any(text, ["订单", "采购单", "采购订单", "到货", "入库"]):
            return "ORDER_DIAGNOSIS"

        last_task = str((memory.business_memory or {}).get("lastTask") or "")
        if last_task in {"ORDER_DIAGNOSIS", "WARNING_SCAN", "SUPPLIER_SCORE"}:
            return last_task
        return "CLARIFY"

    def _infer_focus(self, message: str, task: str, understanding: ConversationUnderstanding) -> str:
        text = message or ""
        if task == "ORDER_DIAGNOSIS":
            if understanding.speech_act == "ASK_OWNER" or self._contains_any(text, ["谁处理", "谁负责", "责任人"]):
                return "OWNER"
            if understanding.speech_act == "ASK_ACTION" or self._contains_any(text, ["下一步", "怎么办", "怎么处理", "催办", "话术"]):
                return "NEXT_ACTION"
            if understanding.speech_act == "ASK_REASON" or self._contains_any(text, ["为什么", "原因", "卡在哪", "没完成"]):
                return "CAUSE"
            return "SUMMARY"

        if task == "WARNING_SCAN":
            if self._contains_any(text, ["最严重", "优先", "先处理", "先看哪几个"]):
                return "TOP_RISK"
            if understanding.speech_act == "ASK_OWNER":
                return "OWNER"
            if understanding.speech_act == "ASK_ACTION":
                return "NEXT_ACTION"
            return "SUMMARY"

        if task == "SUPPLIER_SCORE":
            if self._contains_any(text, ["差在哪", "短板", "拖后腿", "哪里差", "问题在哪"]):
                return "WEAK_METRIC"
            if self._contains_any(text, ["怎么提升", "如何提升", "提升", "优化", "改进"]):
                return "NEXT_ACTION"
            if understanding.speech_act == "ASK_REASON":
                return "SCORE_REASON"
            if self._contains_any(text, ["合作", "还能不能", "继续合作"]):
                return "COOP_ADVICE"
            return "SUMMARY"

        return "CLARIFY"

    def _tools_for_task(self, task: str) -> list[str]:
        mapping = {
            "ORDER_DIAGNOSIS": ["get_order_context"],
            "WARNING_SCAN": ["scan_warning_context"],
            "SUPPLIER_SCORE": ["get_supplier_context"],
            "KNOWLEDGE_QA": ["search_knowledge"],
        }
        return mapping.get(task, [])

    def _missing_fields(self, task: str, slots: ResolvedSlots) -> list[str]:
        if task == "ORDER_DIAGNOSIS" and not slots.order_no:
            return ["orderNo"]
        if task == "SUPPLIER_SCORE" and slots.supplier_id is None:
            return ["supplierId"]
        return []

    def _reason(self, task: str, focus: str, slots: ResolvedSlots, missing_fields: list[str]) -> str:
        parts = [f"task={task}", f"focus={focus}"]
        if slots.inherited:
            parts.append("存在从历史记忆继承的业务对象")
        if missing_fields:
            parts.append("缺少字段：" + ",".join(missing_fields))
        return "；".join(parts)

    def _contains_any(self, text: str, words: list[str]) -> bool:
        return any(word in (text or "") for word in words)
