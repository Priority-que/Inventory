import re

from app.clients.llm_client import LLMClient


class SessionTitleGenerator:
    MAX_TITLE_LENGTH = 24

    def __init__(self, llm_client: LLMClient):
        self.llm_client = llm_client

    async def generate(self, first_message: str | None, current_intent: str | None = None) -> tuple[str, str]:
        fallback = self._fallback_title(first_message, current_intent)
        if not first_message or not first_message.strip():
            return fallback, "fallback"

        if not self.llm_client.is_configured():
            return fallback, "fallback"

        system_prompt = (
            "你是库存采购系统的会话标题生成器。"
            "请根据用户第一条问题生成简洁中文标题，只输出标题本身。"
        )
        user_prompt = "\n".join(
            [
                "要求：",
                "1. 不超过18个汉字，语义清楚。",
                "2. 不要输出解释、引号、句号、问号。",
                "3. 如果用户提到采购订单号，可以保留订单号关键部分。",
                "4. 如果是闲聊或情绪表达，生成自然的聊天标题。",
                "",
                f"业务意图：{current_intent or 'UNKNOWN'}",
                f"用户第一条问题：{first_message.strip()}",
            ]
        )

        try:
            raw_title = await self.llm_client.chat_text(system_prompt, user_prompt, temperature=0.1)
            title = self._sanitize_title(raw_title)
            if title:
                return title, "llm"
        except Exception:
            return fallback, "fallback"

        return fallback, "fallback"

    def _fallback_title(self, message: str | None, current_intent: str | None) -> str:
        text = self._normalize(message)
        if not text:
            return "新会话"

        order_no = self._extract_order_no(text)
        if order_no:
            return self._truncate(f"订单诊断：{order_no}")

        if current_intent == "WARNING_SCAN" or self._contains_any(text, ["风险", "预警", "扫描"]):
            return "采购风险扫描"
        if current_intent == "SUPPLIER_SCORE" or self._contains_any(text, ["供应商", "履约", "评分"]):
            return "供应商履约分析"
        if current_intent == "KNOWLEDGE_QA" or self._contains_any(text, ["规则", "流程", "知识", "怎么规定"]):
            return "业务规则咨询"
        if self._contains_any(text, ["烦", "难受", "焦虑", "崩溃", "卡住"]):
            return "问题求助"

        return self._truncate(text)

    def _sanitize_title(self, value: str | None) -> str:
        text = self._normalize(value)
        if not text:
            return ""

        text = re.sub(r"^(标题|会话标题)\s*[:：]\s*", "", text)
        text = text.strip(" \t\r\n\"'“”‘’`。，.!！?？")
        return self._truncate(text)

    def _normalize(self, value: str | None) -> str:
        text = (value or "").strip()
        text = re.sub(r"\s+", " ", text)
        text = text.splitlines()[0] if text else ""
        return text

    def _truncate(self, text: str) -> str:
        clean_text = text.strip()
        if len(clean_text) <= self.MAX_TITLE_LENGTH:
            return clean_text
        return clean_text[: self.MAX_TITLE_LENGTH].rstrip()

    def _extract_order_no(self, text: str) -> str | None:
        match = re.search(r"\bPO[A-Za-z0-9_-]{6,}\b", text, flags=re.IGNORECASE)
        return match.group(0).upper() if match else None

    def _contains_any(self, text: str, words: list[str]) -> bool:
        return any(word in text for word in words)
