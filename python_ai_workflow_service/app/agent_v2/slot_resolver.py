import re

from app.agent_v2.schemas import AgentMemory, ConversationUnderstanding, ResolvedSlots


class SlotResolver:
    ORDER_NO_PATTERN = re.compile(r"\bPO\d+\b", re.IGNORECASE)
    DAYS_PATTERN = re.compile(r"(?:最近|近)?\s*(\d+)\s*天")
    SUPPLIER_ID_PATTERN = re.compile(r"供应商\s*(\d+)")

    def resolve(
        self,
        message: str,
        understanding: ConversationUnderstanding,
        memory: AgentMemory,
    ) -> ResolvedSlots:
        text = (message or "").strip()
        order_no = self._extract_order_no(text)
        supplier_id = self._extract_supplier_id(text)
        days = self._extract_days(text)
        inherited = False
        reasons: list[str] = []

        if order_no:
            reasons.append("当前输入包含采购订单号")
        if supplier_id is not None:
            reasons.append("当前输入包含供应商ID")
        if days is not None:
            reasons.append("当前输入包含时间范围")

        can_inherit = understanding.is_follow_up and (
            understanding.interaction_type != "RULE_QA" or bool(understanding.raw_reference)
        )

        if can_inherit:
            if not order_no:
                inherited_order_no = self._memory_order_no(memory)
                if inherited_order_no:
                    order_no = inherited_order_no
                    inherited = True
                    reasons.append("从历史业务记忆继承采购订单号")

            if supplier_id is None:
                inherited_supplier_id = self._memory_supplier_id(memory)
                if inherited_supplier_id is not None:
                    supplier_id = inherited_supplier_id
                    inherited = True
                    reasons.append("从历史业务记忆继承供应商ID")

            if days is None:
                inherited_days = self._memory_days(memory)
                if inherited_days is not None:
                    days = inherited_days
                    inherited = True
                    reasons.append("从历史业务记忆继承时间范围")

        return ResolvedSlots(
            orderNo=order_no,
            supplierId=supplier_id,
            days=days,
            query=text or None,
            inherited=inherited,
            missingFields=[],
            reason="；".join(reasons) or "当前输入没有解析出明确业务对象",
        )

    def _extract_order_no(self, text: str) -> str | None:
        match = self.ORDER_NO_PATTERN.search(text or "")
        return match.group(0).upper() if match else None

    def _extract_supplier_id(self, text: str) -> int | None:
        match = self.SUPPLIER_ID_PATTERN.search(text or "")
        if not match:
            return None
        return int(match.group(1))

    def _extract_days(self, text: str) -> int | None:
        match = self.DAYS_PATTERN.search(text or "")
        if match:
            return int(match.group(1))

        if self._contains_any(text, ["半个月", "两周", "2周"]):
            return 14 if self._contains_any(text, ["两周", "2周"]) else 15
        if self._contains_any(text, ["一周", "1周", "本周"]):
            return 7
        if self._contains_any(text, ["一个月", "1个月", "近一月", "最近一月"]):
            return 30
        if self._contains_any(text, ["三个月", "3个月", "近三月", "最近三月"]):
            return 90
        return None

    def _memory_order_no(self, memory: AgentMemory) -> str | None:
        business_memory = memory.business_memory or {}
        for key in ("lastOrderNo", "orderNo"):
            value = business_memory.get(key)
            if value:
                return str(value)

        last_biz_type = str(business_memory.get("lastBizType") or "")
        last_biz_key = business_memory.get("lastBizKey")
        if last_biz_type == "PURCHASE_ORDER" and last_biz_key:
            return str(last_biz_key)
        return None

    def _memory_supplier_id(self, memory: AgentMemory) -> int | None:
        business_memory = memory.business_memory or {}
        for key in ("lastSupplierId", "supplierId"):
            value = business_memory.get(key)
            if value is None:
                continue
            try:
                return int(value)
            except (TypeError, ValueError):
                return None

        last_biz_type = str(business_memory.get("lastBizType") or "")
        last_biz_key = str(business_memory.get("lastBizKey") or "")
        if last_biz_type == "SUPPLIER" and "supplierId=" in last_biz_key:
            values = self._parse_key_values(last_biz_key)
            supplier_id = values.get("supplierId")
            if supplier_id:
                return int(supplier_id)
        return None

    def _memory_days(self, memory: AgentMemory) -> int | None:
        business_memory = memory.business_memory or {}
        value = business_memory.get("lastDays") or business_memory.get("days")
        if value is not None:
            try:
                return int(value)
            except (TypeError, ValueError):
                return None

        last_biz_key = str(business_memory.get("lastBizKey") or "")
        if "days=" in last_biz_key:
            values = self._parse_key_values(last_biz_key)
            days = values.get("days")
            if days:
                return int(days)
        return None

    def _parse_key_values(self, text: str) -> dict[str, str]:
        values: dict[str, str] = {}
        for part in text.split(","):
            if "=" not in part:
                continue
            key, value = part.split("=", 1)
            values[key.strip()] = value.strip()
        return values

    def _contains_any(self, text: str, words: list[str]) -> bool:
        return any(word in (text or "") for word in words)
