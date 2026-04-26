from app.workflows.state import InteractionType, WorkflowIntent, WorkflowStateKeys


class TurnUnderstandNode:
    async def __call__(self, state: dict) -> dict:
        message = str(state.get(WorkflowStateKeys.NORMALIZED_MESSAGE, "") or "").strip()
        memory = dict(state.get(WorkflowStateKeys.CONVERSATION_MEMORY, {}) or {})

        emotion = self._detect_emotion(message)
        speech_act = self._detect_speech_act(message)
        is_follow_up = self._looks_like_follow_up(message)
        is_object_switch_hint = self._looks_like_object_switch(message)

        understanding = {
            "emotion": emotion,
            "speechAct": speech_act,
            "isFollowUp": is_follow_up,
            "isObjectSwitchHint": is_object_switch_hint,
            "lastIntent": memory.get("lastIntent"),
            "lastBizType": memory.get("lastBizType"),
            "lastBizKey": memory.get("lastBizKey"),
        }

        return {WorkflowStateKeys.TURN_UNDERSTANDING: understanding}

    def _detect_emotion(self, text: str) -> str:
        if self._contains_any(text, ["谢谢", "感谢", "辛苦", "麻烦你了", "太好了"]):
            return "appreciative"
        if self._contains_any(text, ["还是不对", "不对", "一直这样", "烦", "崩了", "怎么又", "老是"]):
            return "frustrated"
        if self._contains_any(text, ["急", "赶紧", "马上", "尽快", "来不及", "很急"]):
            return "anxious"
        if self._contains_any(text, ["好一些", "好多了", "可以了", "明白了", "懂了"]):
            return "relieved"
        return "neutral"

    def _detect_speech_act(self, text: str) -> str:
        stripped = text.strip()

        if stripped in {"你好", "您好", "hi", "hello", "在吗"}:
            return "GREETING"

        if self._contains_any(stripped, ["谢谢", "感谢", "辛苦"]):
            return "THANKS"

        if self._contains_any(stripped, ["还是不对", "不对", "不行", "一直这样", "没解决"]):
            return "COMPLAINT"

        if self._contains_any(stripped, ["怎么办", "怎么处理", "下一步", "怎么提升", "怎么优化", "怎么管", "催办", "话术", "发给", "沟通", "提醒"]):
            return "ASK_ACTION"

        if self._contains_any(stripped, ["为什么", "原因", "怎么来的", "依据", "凭什么"]):
            return "ASK_REASON"

        if self._contains_any(stripped, ["谁处理", "谁负责", "责任人", "找谁"]):
            return "ASK_OWNER"

        if self._contains_any(stripped, ["总结", "概况", "整体", "完整分析", "帮我分析"]):
            return "ASK_SUMMARY"

        return "ASK_OR_CHAT"

    def _looks_like_follow_up(self, text: str) -> bool:
        stripped = text.strip()
        if len(stripped) <= 12 and self._contains_any(
            stripped,
            ["为什么", "怎么办", "然后呢", "那呢", "这个呢", "怎么处理", "谁处理", "依据呢", "继续", "展开"],
        ):
            return True

        return self._contains_any(
            stripped,
            ["那", "这个", "它", "这张", "这些", "刚才", "上面", "继续", "再说", "展开讲"],
        )

    def _looks_like_object_switch(self, text: str) -> bool:
        return self._contains_any(text, ["换成", "另一个", "另外", "再看", "看一下"]) or "PO" in text or "供应商" in text

    def _contains_any(self, text: str, words: list[str]) -> bool:
        return any(word in (text or "") for word in words)
