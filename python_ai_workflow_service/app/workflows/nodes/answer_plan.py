import re

from app.schemas.answer_plan import AnswerPlan
from app.workflows.state import InteractionType, WorkflowIntent, WorkflowStateKeys


BIZ_NO_PATTERN = re.compile(r"(PO|AR|IN)\d+", re.IGNORECASE)


class AnswerPlanNode:
    async def __call__(self, state: dict) -> dict:
        interaction_type = str(state.get(WorkflowStateKeys.INTERACTION_TYPE, InteractionType.BUSINESS.value))
        intent = str(state.get(WorkflowStateKeys.INTENT, WorkflowIntent.UNKNOWN.value))
        message = str(state.get(WorkflowStateKeys.MESSAGE, ""))
        biz_type, biz_key = self._resolve_biz_scope(state, intent, message)

        if interaction_type != InteractionType.BUSINESS.value:
            plan = AnswerPlan(
                interactionType=interaction_type,
                intent=WorkflowIntent.UNKNOWN.value,
                questionFocus=interaction_type,
                turnType="FIRST_TURN",
                answerMode=interaction_type,
                bizType=biz_type,
                bizKey=biz_key,
                needsRefresh=False,
                useLlm=False,
                maxContextItems=0,
            )
            return {WorkflowStateKeys.ANSWER_PLAN: plan.model_dump(by_alias=True)}

        plan = AnswerPlan(
            interactionType=InteractionType.BUSINESS.value,
            intent=intent,
            questionFocus=self._question_focus(intent, message),
            turnType="FOLLOW_UP" if self._has_reusable_result(state, intent, biz_key) else "FIRST_TURN",
            answerMode="AGENT_ANSWER",
            bizType=biz_type,
            bizKey=biz_key,
            targetBizNo=self._extract_biz_no(message),
            targetOrderNo=self._extract_order_no(message),
            targetSupplierId=self._extract_supplier_id(message),
            needsRefresh=self._needs_refresh(state, intent, message, biz_key),
            useLlm=True,
            maxContextItems=10,
        )
        return {WorkflowStateKeys.ANSWER_PLAN: plan.model_dump(by_alias=True)}

    def _question_focus(self, intent: str, message: str) -> str:
        text = message or ""

        if intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            asks_owner = self._contains_any(text, ["谁处理", "谁跟进", "哪个采购员", "哪位采购员", "负责人"])
            asks_reason = self._contains_any(text, ["为什么", "原因", "为什么选", "依据", "凭什么"])
            if asks_owner and asks_reason:
                return "OWNER_REASON"
            if asks_owner:
                return "OWNER"
            if self._contains_any(text, ["下一步", "怎么办", "怎么处理", "怎么解决"]):
                return "NEXT_ACTION"
            if asks_reason or self._contains_any(text, ["卡在哪", "没完成"]):
                return "CAUSE"
            if self._contains_any(text, ["证据", "根据什么"]):
                return "EVIDENCE"
            return "FULL_DIAGNOSIS"

        if intent == WorkflowIntent.WARNING_SCAN.value:
            if self._extract_biz_no(text):
                return "SPECIFIC_WARNING_REASON"
            if self._contains_any(text, ["最严重", "优先", "先处理", "先看哪几个"]):
                return "TOP_RISK"
            if self._contains_any(text, ["谁处理", "谁负责", "哪个角色处理"]):
                return "WARNING_OWNER"
            if self._contains_any(text, ["为什么优先", "为什么先", "为什么最严重"]):
                return "WARNING_PRIORITY_REASON"
            if self._contains_any(text, ["怎么处理", "怎么办", "下一步"]):
                return "WARNING_ACTION"
            return "WARNING_SUMMARY"

        if intent == WorkflowIntent.SUPPLIER_SCORE.value:
            if self._contains_any(text, ["差在哪", "差在哪里", "哪里差", "哪儿差", "短板", "拖后腿", "哪个指标", "问题在哪"]):
                return "WEAK_METRIC"
            if self._contains_any(text, ["怎么提升", "如何提升", "怎么提高", "如何提高", "提升", "提高", "优化", "改进"]):
                return "SUPPLIER_ACTION"
            if self._contains_any(text, ["为什么", "怎么算", "怎么来的", "为什么只有这个分"]):
                return "SCORE_REASON"
            if self._contains_any(text, ["分数", "意味着", "等级"]):
                return "SCORE_MEANING"
            if self._contains_any(text, ["合作", "还能不能", "继续合作", "建议"]):
                return "COOP_ADVICE"
            if self._contains_any(text, ["怎么改善", "怎么管控", "下一步"]):
                return "SUPPLIER_ACTION"
            return "SUPPLIER_FULL_ANALYSIS"

        return "CLARIFY"

    def _needs_refresh(self, state: dict, intent: str, message: str, biz_key: str | None) -> bool:
        if not self._has_reusable_result(state, intent, biz_key):
            return True
        return self._contains_any(message, ["重新", "刷新", "重新扫描", "再算", "最新"])

    def _has_reusable_result(self, state: dict, intent: str, biz_key: str | None) -> bool:
        if not self._scope_matches_cached_result(state, intent, biz_key):
            return False
        if intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            return bool(state.get(WorkflowStateKeys.ORDER_DIAGNOSIS))
        if intent == WorkflowIntent.WARNING_SCAN.value:
            return bool(state.get(WorkflowStateKeys.WARNING_ANALYSIS))
        if intent == WorkflowIntent.SUPPLIER_SCORE.value:
            return bool(state.get(WorkflowStateKeys.SUPPLIER_SCORE))
        return False

    def _resolve_biz_scope(self, state: dict, intent: str, message: str) -> tuple[str | None, str | None]:
        entity = dict(state.get(WorkflowStateKeys.ENTITY, {}) or {})

        if intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            order_no = entity.get("orderNo")
            if not order_no:
                diagnosis = dict(state.get(WorkflowStateKeys.ORDER_DIAGNOSIS, {}) or {})
                order_no = diagnosis.get("orderNo")
            return "PURCHASE_ORDER", str(order_no) if order_no else None

        if intent == WorkflowIntent.WARNING_SCAN.value:
            explicit_biz_no = self._extract_biz_no(message)
            if explicit_biz_no and self._contains_any(message, ["风险", "预警", "优先", "高", "中", "低"]):
                return "WARNING_ITEM", explicit_biz_no
            days = entity.get("days") or 7
            return "WARNING_SCAN_RANGE", f"days={days}"

        if intent == WorkflowIntent.SUPPLIER_SCORE.value:
            supplier_id = entity.get("supplierId")
            if supplier_id is None:
                score = dict(state.get(WorkflowStateKeys.SUPPLIER_SCORE, {}) or {})
                supplier_id = score.get("supplierId")
            days = entity.get("days") or 30
            if supplier_id is None:
                return "SUPPLIER", None
            return "SUPPLIER", f"supplierId={supplier_id},days={days}"

        return None, None

    def _scope_matches_cached_result(self, state: dict, intent: str, biz_key: str | None) -> bool:
        if not biz_key:
            return True
        cached_plan = dict(state.get(WorkflowStateKeys.ANSWER_PLAN, {}) or {})
        return cached_plan.get("intent") == intent and cached_plan.get("bizKey") == biz_key

    def _extract_biz_no(self, text: str) -> str | None:
        match = BIZ_NO_PATTERN.search(text or "")
        return match.group(0).upper() if match else None

    def _extract_order_no(self, text: str) -> str | None:
        biz_no = self._extract_biz_no(text)
        return biz_no if biz_no and biz_no.startswith("PO") else None

    def _extract_supplier_id(self, text: str) -> int | None:
        match = re.search(r"供应商\s*(\d+)", text or "")
        return int(match.group(1)) if match else None

    def _contains_any(self, text: str, words: list[str]) -> bool:
        return any(word in (text or "") for word in words)
