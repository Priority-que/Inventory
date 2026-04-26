from app.workflows.state import InteractionType, WorkflowIntent, WorkflowStateKeys


class ResponsePolicyNode:
    async def __call__(self, state: dict) -> dict:
        message = str(state.get(WorkflowStateKeys.MESSAGE, "") or "")
        interaction_type = str(state.get(WorkflowStateKeys.INTERACTION_TYPE, InteractionType.BUSINESS.value))
        selected_context = dict(state.get(WorkflowStateKeys.SELECTED_CONTEXT, {}) or {})
        plan = dict(state.get(WorkflowStateKeys.ANSWER_PLAN, {}) or {})
        previous_memory = dict(state.get(WorkflowStateKeys.CONVERSATION_MEMORY, {}) or {})

        turn = dict(state.get(WorkflowStateKeys.TURN_UNDERSTANDING, {}) or {})
        emotion = turn.get("emotion") or self._detect_emotion(message)
        speech_act = turn.get("speechAct") or "ASK_OR_CHAT"

        policy = self._build_policy(
            message,
            interaction_type,
            selected_context,
            plan,
            previous_memory,
            emotion,
            speech_act,
        )
        memory = self._update_memory(selected_context, plan, previous_memory, emotion)

        return {
            WorkflowStateKeys.RESPONSE_POLICY: policy,
            WorkflowStateKeys.CONVERSATION_MEMORY: memory,
        }

    def _build_policy(
            self,
            message: str,
            interaction_type: str,
            selected_context: dict,
            plan: dict,
            memory: dict,
            emotion: str,
            speech_act: str,
    ) -> dict:
        intent = self._effective_intent(selected_context.get("intent"), plan.get("intent"), memory.get("lastIntent"))
        focus = selected_context.get("questionFocus") or plan.get("questionFocus")
        biz_key = selected_context.get("bizKey") or plan.get("bizKey")
        is_same_line = bool(biz_key and biz_key == memory.get("lastBizKey"))

        if interaction_type != InteractionType.BUSINESS.value:
            opening = self._social_opening(message, emotion)
            return {
                "tone": "companion_warm",
                "emotion": emotion,
                "empathyLevel": "medium" if emotion in {"frustrated", "anxious"} else "low",
                "opening": opening,
                "closingOffer": self._social_closing_offer(intent, memory),
                "detailLevel": "brief",
                "includeProactiveOffer": True,
                "speechAct": speech_act,
                "shouldEmpathizeFirst": emotion in {"frustrated", "anxious"},
                "shouldOfferNextStep": speech_act not in {"THANKS", "GREETING"},
            }

        opening = ""
        if emotion in {"frustrated", "anxious"}:
            opening = "先别急，我按现在的数据帮你把关键点拆开。"
        elif emotion in {"appreciative", "relieved"}:
            opening = "好，这条线我继续帮你接着看。"
        elif is_same_line:
            opening = "我接着这条线往下说。"

        return {
            "tone": "business_companion",
            "emotion": emotion,
            "speechAct": speech_act,
            "empathyLevel": "medium" if emotion in {"frustrated", "anxious"} else "low",
            "opening": opening,
            "closingOffer": self._business_closing_offer(intent, focus),
            "detailLevel": self._detail_level(message, focus),
            "shouldEmpathizeFirst": emotion in {"frustrated", "anxious"},
            "shouldOfferNextStep": speech_act not in {"THANKS", "GREETING"},
            "sameBusinessLine": is_same_line,
        }

    def _update_memory(self, selected_context: dict, plan: dict, memory: dict, emotion: str) -> dict:
        updated = dict(memory)
        interaction_type = selected_context.get("interactionType")
        intent = self._effective_intent(selected_context.get("intent"), plan.get("intent"))
        biz_type = selected_context.get("bizType") or plan.get("bizType")
        biz_key = selected_context.get("bizKey") or plan.get("bizKey")

        if interaction_type == InteractionType.BUSINESS.value and intent != WorkflowIntent.UNKNOWN.value:
            updated["lastIntent"] = intent
            updated["lastQuestionFocus"] = selected_context.get("questionFocus") or plan.get("questionFocus")
            updated["lastBizType"] = biz_type
            updated["lastBizKey"] = biz_key

            if intent == WorkflowIntent.SUPPLIER_SCORE.value:
                entity_supplier_id = plan.get("targetSupplierId")
                if entity_supplier_id is not None:
                    updated["lastSupplierId"] = entity_supplier_id

                if biz_key and "days=" in str(biz_key):
                    updated["lastDays"] = self._extract_days_from_biz_key(str(biz_key))

        updated["lastEmotion"] = emotion
        return updated

    def _detect_emotion(self, text: str) -> str:
        if self._contains_any(text, ["一直这样", "还是不行", "又不对", "烦", "头疼", "崩溃", "离谱", "搞不定"]):
            return "frustrated"
        if self._contains_any(text, ["急", "着急", "赶紧", "来不及", "怎么办", "风险这么高", "卡住"]):
            return "anxious"
        if self._contains_any(text, ["好一些", "好多了", "明白了", "清楚了"]):
            return "relieved"
        if self._contains_any(text, ["谢谢", "辛苦", "感谢"]):
            return "appreciative"
        return "neutral"

    def _effective_intent(self, *values: str | None) -> str | None:
        for value in values:
            if value and value != WorkflowIntent.UNKNOWN.value:
                return value
        return None

    def _social_opening(self, text: str, emotion: str) -> str:
        if emotion == "appreciative":
            return "不客气，我会继续跟着这条线。"
        if emotion == "relieved":
            return "好，有进展就行，我们继续稳住往下看。"
        if emotion in {"frustrated", "anxious"}:
            return "我在，先把事情拆小一点，一个点一个点看。"
        if self._contains_any(text, ["你好", "在吗"]):
            return "我在，可以直接把业务问题发我。"
        return "好，我接着陪你看。"

    def _social_closing_offer(self, intent: str | None, memory: dict) -> str:
        last_intent = intent or memory.get("lastIntent")
        if last_intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            return "你下一句如果想继续问这张订单的卡点、责任人或下一步，我可以直接接上。"
        if last_intent == WorkflowIntent.WARNING_SCAN.value:
            return "你下一句如果想继续问风险优先级、责任方或处理顺序，我可以直接接上。"
        if last_intent == WorkflowIntent.SUPPLIER_SCORE.value:
            return "你下一句如果想继续问这个供应商差在哪、怎么提升或还能不能合作，我可以直接接上。"
        return "你随时把问题丢给我，我会按当前数据帮你拆。"

    def _business_closing_offer(self, intent: str | None, focus: str | None) -> str:
        if intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            if focus in {"OWNER", "OWNER_REASON"}:
                return "如果你需要，我可以继续把催办话术也替你整理出来。"
            return "如果你愿意，我可以继续帮你拆责任人、催办顺序和下一步沟通话术。"
        if intent == WorkflowIntent.WARNING_SCAN.value:
            return "如果你愿意，我可以继续把这些风险按优先级和责任方排成处理顺序。"
        if intent == WorkflowIntent.SUPPLIER_SCORE.value:
            if focus == "SUPPLIER_ACTION":
                return "如果你愿意，我可以继续把提升动作拆成短期、中期两组。"
            return "如果你愿意，我可以继续把这个供应商的提升重点和跟踪指标列出来。"
        return "如果你愿意，我可以继续顺着这条业务往下拆。"

    def _extract_days_from_biz_key(self, biz_key: str) -> int | None:
        parts = biz_key.split(",")
        for part in parts:
            if part.startswith("days="):
                try:
                    return int(part.replace("days=", "", 1))
                except ValueError:
                    return None
        return None


    def _detail_level(self, text: str, focus: str | None) -> str:
        if self._contains_any(text, ["简单", "一句话", "简短"]):
            return "brief"
        if self._contains_any(text, ["详细", "完整", "展开"]):
            return "detailed"
        if focus in {"FULL_DIAGNOSIS", "WARNING_SUMMARY", "SUPPLIER_FULL_ANALYSIS"}:
            return "normal"
        return "focused"

    def _contains_any(self, text: str, words: list[str]) -> bool:
        return any(word in (text or "") for word in words)
