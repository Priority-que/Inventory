import re

from app.agent_v2.schemas import AgentEvidence, AgentPlan, AnswerDraft, FactCheckResult


class FactChecker:
    ORDER_NO_PATTERN = re.compile(r"\bPO\d+\b", re.IGNORECASE)
    INTERNAL_CODE_MAP = {
        "ORDER_DIAGNOSIS": "订单诊断",
        "WARNING_SCAN": "风险扫描",
        "SUPPLIER_SCORE": "供应商履约分析",
        "KNOWLEDGE_QA": "规则问答",
        "PURCHASE_ORDER": "采购订单",
        "WAIT_CONFIRM": "待供应商确认",
        "IN_PROGRESS": "执行中",
        "PARTIAL_ARRIVAL": "部分到货",
        "COMPLETED": "已完成",
        "CLOSED": "已关闭",
        "CANCELLED": "已取消",
        "PENDING": "待入库",
        "PURCHASER": "采购侧",
        "WAREHOUSE": "仓库侧",
        "SUPPLIER": "供应商",
        "HIGH": "高风险",
        "MEDIUM": "中风险",
        "LOW": "低风险",
    }

    def validate(
        self,
        answer: str,
        plan: AgentPlan,
        evidence: AgentEvidence,
        draft: AnswerDraft,
        fallback_answer: str,
    ) -> FactCheckResult:
        cleaned = self._sanitize(answer)
        issues: list[str] = []

        if self._mentions_wrong_order(cleaned, plan, evidence):
            issues.append("回答中出现了证据之外的采购订单号")

        if self._contradicts_missing_entity(cleaned, evidence):
            issues.append("回答与结构化存在性结果冲突")

        if self._contains_internal_code(cleaned):
            cleaned = self._sanitize(cleaned)
            if self._contains_internal_code(cleaned):
                issues.append("回答仍包含内部编码")

        if issues:
            return FactCheckResult(passed=False, answer=self._sanitize(fallback_answer), issues=issues)

        if not cleaned.strip():
            return FactCheckResult(passed=False, answer=self._sanitize(fallback_answer), issues=["回答为空"])

        return FactCheckResult(passed=True, answer=cleaned, issues=[])

    def _mentions_wrong_order(self, answer: str, plan: AgentPlan, evidence: AgentEvidence) -> bool:
        mentioned = {match.group(0).upper() for match in self.ORDER_NO_PATTERN.finditer(answer or "")}
        if not mentioned:
            return False

        allowed = set()
        order_no = evidence.facts.get("orderNo") or plan.slots.order_no
        if order_no:
            allowed.add(str(order_no).upper())
        for item in evidence.items:
            biz_no = item.get("bizNo")
            if biz_no:
                allowed.add(str(biz_no).upper())

        return bool(allowed) and not mentioned.issubset(allowed)

    def _contradicts_missing_entity(self, answer: str, evidence: AgentEvidence) -> bool:
        exists = evidence.facts.get("exists")
        if exists is False:
            return False
        if exists is True:
            return "不存在" in answer or "没有查到" in answer
        return False

    def _sanitize(self, answer: str) -> str:
        result = answer or ""
        for code, label in self.INTERNAL_CODE_MAP.items():
            result = re.sub(rf"(?<![A-Z0-9_]){re.escape(code)}(?![A-Z0-9_])", label, result)
        return result.strip()

    def _contains_internal_code(self, answer: str) -> bool:
        return any(
            re.search(rf"(?<![A-Z0-9_]){re.escape(code)}(?![A-Z0-9_])", answer or "")
            for code in self.INTERNAL_CODE_MAP
        )
