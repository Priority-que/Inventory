import re

from app.agent_v2.schemas import AgentMemory, ConversationUnderstanding


class ConversationUnderstandStep:
    ORDER_NO_PATTERN = re.compile(r"\bPO\d+\b", re.IGNORECASE)
    SUPPLIER_ID_PATTERN = re.compile(r"供应商\s*\d+")

    def understand(self, message: str, memory: AgentMemory) -> ConversationUnderstanding:
        text = (message or "").strip()

        if not text:
            return ConversationUnderstanding(
                interactionType="CLARIFY",
                normalizedMessage="",
                reason="用户输入为空，需要先提示用户补充问题。",
            )

        emotion = self._detect_emotion(text)
        speech_act = self._detect_speech_act(text)
        is_follow_up = self._is_follow_up(text, memory)
        is_business_hint = self._has_business_hint(text)
        is_rule_question = self._is_rule_question(text)
        raw_reference = self._detect_reference(text)
        interaction_type = self._interaction_type(
            text,
            emotion,
            is_follow_up,
            is_business_hint,
            is_rule_question,
        )

        return ConversationUnderstanding(
            interactionType=interaction_type,
            emotion=emotion,
            speechAct=speech_act,
            isFollowUp=is_follow_up,
            isBusinessHint=is_business_hint,
            isRuleQuestion=is_rule_question,
            needsBusinessPlanner=interaction_type in {"BUSINESS", "FOLLOW_UP", "RULE_QA"},
            normalizedMessage=text,
            rawReference=raw_reference,
            reason=self._reason(interaction_type, emotion, speech_act, is_follow_up, is_business_hint, is_rule_question),
        )

    def _interaction_type(
        self,
        text: str,
        emotion: str,
        is_follow_up: bool,
        is_business_hint: bool,
        is_rule_question: bool,
    ) -> str:
        if self._is_social(text) and not is_business_hint and not is_follow_up:
            return "CHAT"
        if is_rule_question:
            return "RULE_QA"
        if is_business_hint:
            return "BUSINESS"
        if is_follow_up:
            return "FOLLOW_UP"
        if emotion in {"frustrated", "anxious", "appreciative", "relieved"}:
            return "EMOTION"
        return "CHAT"

    def _detect_emotion(self, text: str) -> str:
        if self._contains_any(text, ["谢谢", "感谢", "辛苦", "麻烦你了", "太好了"]):
            return "appreciative"
        if self._contains_any(text, ["还是不对", "不对", "一直这样", "烦", "崩了", "怎么又", "老是", "离谱"]):
            return "frustrated"
        if self._contains_any(text, ["急", "赶紧", "马上", "尽快", "来不及", "很急", "卡住"]):
            return "anxious"
        if self._contains_any(text, ["好一些", "好多了", "可以了", "明白了", "懂了", "清楚了"]):
            return "relieved"
        return "neutral"

    def _detect_speech_act(self, text: str) -> str:
        stripped = text.strip()
        lower = stripped.lower()

        if lower in {"你好", "您好", "hi", "hello", "在吗"}:
            return "GREETING"
        if self._contains_any(stripped, ["谢谢", "感谢", "辛苦"]):
            return "THANKS"
        if self._contains_any(stripped, ["还是不对", "不对", "不行", "一直这样", "没解决"]):
            return "COMPLAINT"
        if self._contains_any(stripped, ["怎么办", "怎么处理", "下一步", "怎么提升", "怎么优化", "催办", "话术", "沟通"]):
            return "ASK_ACTION"
        if self._is_rule_question(stripped):
            return "ASK_RULE"
        if self._contains_any(stripped, ["为什么", "原因", "依据", "凭什么", "怎么来的"]):
            return "ASK_REASON"
        if self._contains_any(stripped, ["谁处理", "谁负责", "责任人", "找谁"]):
            return "ASK_OWNER"
        if self._contains_any(stripped, ["总结", "概况", "整体", "完整分析", "帮我分析"]):
            return "ASK_SUMMARY"
        return "ASK_OR_CHAT"

    def _is_follow_up(self, text: str, memory: AgentMemory) -> bool:
        if not memory.recent_messages and not memory.business_memory:
            return False

        stripped = text.strip()
        if len(stripped) <= 12 and self._contains_any(
            stripped,
            ["为什么", "怎么办", "然后呢", "那呢", "这个呢", "怎么处理", "谁处理", "依据呢", "继续", "展开"],
        ):
            return True

        return self._contains_any(
            stripped,
            ["那", "这个", "这张", "它", "这些", "刚才", "上面", "继续", "再说", "展开讲", "刚刚"],
        )

    def _has_business_hint(self, text: str) -> bool:
        if self.ORDER_NO_PATTERN.search(text) or self.SUPPLIER_ID_PATTERN.search(text):
            return True
        return self._contains_any(
            text,
            [
                "订单",
                "采购单",
                "采购订单",
                "风险",
                "预警",
                "供应商",
                "履约",
                "评分",
                "到货",
                "入库",
                "库存",
                "仓库",
                "物料",
            ],
        )

    def _is_rule_question(self, text: str) -> bool:
        if self._contains_any(text, ["规则", "状态流转", "怎么操作", "权限", "流程", "是什么意思"]):
            return True
        if "为什么" in text and self._contains_any(text, ["不能", "无法", "不允许"]):
            return True
        if "怎么" in text and self._contains_any(text, ["操作", "流转", "审批", "确认"]):
            return True
        return False

    def _is_social(self, text: str) -> bool:
        lower = text.strip().lower()
        if lower in {"你好", "您好", "hi", "hello", "在吗", "收到", "好的", "好", "谢谢", "谢谢你", "辛苦了", "ok"}:
            return True
        return self._contains_any(text, ["你好", "您好", "在吗", "收到", "好的", "谢谢", "辛苦了"])

    def _detect_reference(self, text: str) -> str | None:
        references = ["这张单", "这个订单", "这个供应商", "这个风险", "刚才那个", "上面那个", "它", "这个"]
        for reference in references:
            if reference in text:
                return reference
        return None

    def _reason(
        self,
        interaction_type: str,
        emotion: str,
        speech_act: str,
        is_follow_up: bool,
        is_business_hint: bool,
        is_rule_question: bool,
    ) -> str:
        facts = [f"interactionType={interaction_type}", f"speechAct={speech_act}", f"emotion={emotion}"]
        if is_business_hint:
            facts.append("命中业务关键词或业务编号")
        if is_rule_question:
            facts.append("命中规则/流程类问法")
        if is_follow_up:
            facts.append("结合历史消息判断为追问")
        return "；".join(facts)

    def _contains_any(self, text: str, words: list[str]) -> bool:
        return any(word in (text or "") for word in words)
