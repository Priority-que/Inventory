import re

from app.clients.llm_client import LLMClient
from app.workflows.prompts import INTENT_CLASSIFY_PROMPT
from app.workflows.state import InteractionType, WorkflowIntent, WorkflowStateKeys


class IntentClassifyNode:
    ORDER_NO_PATTERN = re.compile(r"\bPO\d+\b", re.IGNORECASE)
    SUPPLIER_ID_PATTERN = re.compile(r"供应商\s*(\d+)")

    def __init__(self, llm_client: LLMClient):
        self.llm_client = llm_client

    async def __call__(self, state: dict) -> dict:
        message = str(state.get(WorkflowStateKeys.NORMALIZED_MESSAGE, "")).strip()
        active_intent = self._resolve_active_intent(state)

        interaction_type = self._classify_interaction_type(message, active_intent)
        if interaction_type == InteractionType.SOCIAL.value:
            return {
                WorkflowStateKeys.INTERACTION_TYPE: interaction_type,
                WorkflowStateKeys.INTENT: WorkflowIntent.UNKNOWN.value,
            }

        if interaction_type == InteractionType.META.value:
            return {
                WorkflowStateKeys.INTERACTION_TYPE: interaction_type,
                WorkflowStateKeys.INTENT: WorkflowIntent.UNKNOWN.value,
            }

        if interaction_type == InteractionType.CLARIFY.value:
            return {
                WorkflowStateKeys.INTERACTION_TYPE: interaction_type,
                WorkflowStateKeys.INTENT: WorkflowIntent.UNKNOWN.value,
            }

        rule_intent = self._rule_business_intent(message, active_intent)
        if rule_intent is not None:
            return {
                WorkflowStateKeys.INTERACTION_TYPE: InteractionType.BUSINESS.value,
                WorkflowStateKeys.INTENT: rule_intent,
                WorkflowStateKeys.ACTIVE_INTENT: rule_intent,
            }

        if active_intent != WorkflowIntent.UNKNOWN.value and interaction_type == InteractionType.BUSINESS.value:
            return {
                WorkflowStateKeys.INTERACTION_TYPE: InteractionType.BUSINESS.value,
                WorkflowStateKeys.INTENT: active_intent,
                WorkflowStateKeys.ACTIVE_INTENT: active_intent,
            }

        prompt = (
            INTENT_CLASSIFY_PROMPT
            .replace("{previousIntent}", active_intent)
            .replace("{message}", message)
        )
        intent_text = await self.llm_client.chat_text(
            "你是意图分类器，只输出意图编码。",
            prompt,
            temperature=0.0,
        )
        intent = self._parse_intent(intent_text)

        if intent == WorkflowIntent.UNKNOWN.value:
            if active_intent != WorkflowIntent.UNKNOWN.value and self._looks_like_business_follow_up(message):
                intent = active_intent
            else:
                return {
                    WorkflowStateKeys.INTERACTION_TYPE: InteractionType.CLARIFY.value,
                    WorkflowStateKeys.INTENT: WorkflowIntent.UNKNOWN.value,
                }

        return {
            WorkflowStateKeys.INTERACTION_TYPE: InteractionType.BUSINESS.value,
            WorkflowStateKeys.INTENT: intent,
            WorkflowStateKeys.ACTIVE_INTENT: intent,
        }

    def _resolve_active_intent(self, state: dict) -> str:
        active_intent = str(state.get(WorkflowStateKeys.ACTIVE_INTENT, "")).strip()
        if active_intent in {
            WorkflowIntent.ORDER_DIAGNOSIS.value,
            WorkflowIntent.WARNING_SCAN.value,
            WorkflowIntent.SUPPLIER_SCORE.value,
            WorkflowIntent.KNOWLEDGE_QA.value,
        }:
            return active_intent

        current_intent = str(state.get(WorkflowStateKeys.INTENT, WorkflowIntent.UNKNOWN.value))
        if current_intent in {
            WorkflowIntent.ORDER_DIAGNOSIS.value,
            WorkflowIntent.WARNING_SCAN.value,
            WorkflowIntent.SUPPLIER_SCORE.value,
            WorkflowIntent.KNOWLEDGE_QA.value,
        }:
            return current_intent

        memory = dict(state.get(WorkflowStateKeys.CONVERSATION_MEMORY, {}) or {})
        memory_intent = str(memory.get("lastIntent", "")).strip()
        if memory_intent in {
            WorkflowIntent.ORDER_DIAGNOSIS.value,
            WorkflowIntent.WARNING_SCAN.value,
            WorkflowIntent.SUPPLIER_SCORE.value,
            WorkflowIntent.KNOWLEDGE_QA.value,
        }:
            return memory_intent

        if state.get(WorkflowStateKeys.ORDER_DIAGNOSIS):
            return WorkflowIntent.ORDER_DIAGNOSIS.value
        if state.get(WorkflowStateKeys.WARNING_ANALYSIS):
            return WorkflowIntent.WARNING_SCAN.value
        if state.get(WorkflowStateKeys.SUPPLIER_SCORE):
            return WorkflowIntent.SUPPLIER_SCORE.value

        return WorkflowIntent.UNKNOWN.value

    def _parse_intent(self, text: str | None) -> str:
        if not text:
            return WorkflowIntent.UNKNOWN.value
        value = text.strip()
        for intent in WorkflowIntent:
            if intent.value in value:
                return intent.value
        return WorkflowIntent.UNKNOWN.value

    def _classify_interaction_type(self, message: str, active_intent: str) -> str:
        if not message:
            return InteractionType.CLARIFY.value

        if self._is_social_message(message):
            return InteractionType.SOCIAL.value

        if self._is_meta_message(message):
            return InteractionType.META.value

        if active_intent != WorkflowIntent.UNKNOWN.value and self._looks_like_business_follow_up(message):
            return InteractionType.BUSINESS.value

        if self.ORDER_NO_PATTERN.search(message):
            return InteractionType.BUSINESS.value

        if self.SUPPLIER_ID_PATTERN.search(message):
            return InteractionType.BUSINESS.value

        if self._contains_any(
            message,
            [
                "订单",
                "采购单",
                "采购订单",
                "风险",
                "预警",
                "扫描",
                "供应商",
                "履约",
                "到货",
                "入库",
                "规则",
                "状态流转",
            ],
        ):
            return InteractionType.BUSINESS.value

        if len(message) <= 6:
            return InteractionType.CLARIFY.value

        return InteractionType.BUSINESS.value

    def _rule_business_intent(self, message: str, active_intent: str) -> str | None:
        text = (message or "").strip()

        explicit_intent = self._detect_explicit_business_intent(text)
        if explicit_intent is not None:
            return explicit_intent

        if active_intent != WorkflowIntent.UNKNOWN.value and self._looks_like_business_follow_up(text):
            explicit_intent = self._detect_explicit_business_intent(text)
            if explicit_intent is not None:
                return explicit_intent
            return active_intent

        return None

    def _detect_explicit_business_intent(self, text: str) -> str | None:
        if self.SUPPLIER_ID_PATTERN.search(text) or (
            "供应商" in text and self._contains_any(text, ["履约", "评分", "分数", "等级", "合作", "表现", "分析"])
        ):
            return WorkflowIntent.SUPPLIER_SCORE.value

        if self._contains_any(text, ["规则", "状态流转", "为什么不能", "为什么无法", "怎么操作", "是什么意思"]):
            return WorkflowIntent.KNOWLEDGE_QA.value

        if self._contains_any(text, ["风险", "预警", "扫描", "优先处理", "高风险", "中风险", "低风险"]):
            return WorkflowIntent.WARNING_SCAN.value

        if self.ORDER_NO_PATTERN.search(text):
            return WorkflowIntent.ORDER_DIAGNOSIS.value

        if self._contains_any(text, ["订单", "采购单", "采购订单"]) and self._contains_any(
            text,
            ["为什么", "没完成", "卡在哪", "下一步", "谁处理", "怎么处理", "怎么办", "到货", "入库"],
        ):
            return WorkflowIntent.ORDER_DIAGNOSIS.value

        return None

    def _is_social_message(self, text: str) -> bool:
        if text in {"你好", "您好", "hi", "hello", "在吗", "收到", "好的", "好", "谢谢", "谢谢你", "辛苦了", "ok", "OK"}:
            return True
        return self._contains_any(text, ["你好", "您好", "在吗", "收到", "好的", "谢谢", "辛苦了"])

    def _is_meta_message(self, text: str) -> bool:
        return self._contains_any(
            text,
            [
                "你是谁",
                "你是什么",
                "你是什么模型",
                "你用的什么模型",
                "底层模型",
                "你会做什么",
                "你能做什么",
                "你可以做什么",
                "你有什么能力",
                "怎么用你",
                "怎么和你说",
                "你怎么工作",
                "你和普通助手有什么区别",
                "你有什么限制",
            ],
        )

    def _looks_like_business_follow_up(self, text: str) -> bool:
        return self._contains_any(
            text,
            [
                "为什么",
                "原因",
                "下一步",
                "怎么处理",
                "怎么办",
                "怎么提升",
                "如何提升",
                "提升",
                "优化",
                "改善",
                "提高",
                "谁处理",
                "什么意思",
                "依据",
                "哪个",
                "哪些",
                "先处理",
                "怎么解决",
                "整理",
                "话术",
                "催办",
                "发给",
                "沟通",
                "这个问题",
                "这个分数",
                "这个风险",
            ],
        )

    def _contains_any(self, text: str, words: list[str]) -> bool:
        return any(word in (text or "") for word in words)
