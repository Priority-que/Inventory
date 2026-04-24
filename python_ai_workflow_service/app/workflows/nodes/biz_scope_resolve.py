from app.workflows.state import WorkflowIntent, WorkflowStateKeys


class BizScopeResolverNode:
    async def __call__(self, state: dict) -> dict:
        intent = str(state.get(WorkflowStateKeys.INTENT, WorkflowIntent.UNKNOWN.value))
        explicit_entity = dict(state.get(WorkflowStateKeys.EXPLICIT_ENTITY, {}) or {})
        memory = dict(state.get(WorkflowStateKeys.CONVERSATION_MEMORY, {}) or {})
        turn = dict(state.get(WorkflowStateKeys.TURN_UNDERSTANDING, {}) or {})

        if intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            return self._resolve_order_scope(explicit_entity, memory, turn, state)

        if intent == WorkflowIntent.WARNING_SCAN.value:
            return self._resolve_warning_scope(explicit_entity, memory, turn, state)

        if intent == WorkflowIntent.SUPPLIER_SCORE.value:
            return self._resolve_supplier_scope(explicit_entity, memory, turn, state)

        return {
            WorkflowStateKeys.BIZ_TYPE: None,
            WorkflowStateKeys.BIZ_KEY: None,
            WorkflowStateKeys.SCOPE_STATUS: "MISSING",
            WorkflowStateKeys.SCOPE_REASON: "当前意图不需要业务对象或无法识别业务对象。",
            WorkflowStateKeys.ENTITY: explicit_entity,
        }

    def _resolve_order_scope(self, explicit_entity: dict, memory: dict, turn: dict, state: dict) -> dict:
        order_no = explicit_entity.get("orderNo")
        if order_no:
            return {
                WorkflowStateKeys.BIZ_TYPE: "PURCHASE_ORDER",
                WorkflowStateKeys.BIZ_KEY: order_no,
                WorkflowStateKeys.SCOPE_STATUS: "EXPLICIT_NEW",
                WorkflowStateKeys.SCOPE_REASON: "用户当前轮明确提供了采购订单号。",
                WorkflowStateKeys.ENTITY: {"orderNo": order_no},
            }

        if self._can_inherit(memory, turn, "ORDER_DIAGNOSIS", "PURCHASE_ORDER"):
            inherited_order_no = memory.get("lastBizKey")
            return {
                WorkflowStateKeys.BIZ_TYPE: "PURCHASE_ORDER",
                WorkflowStateKeys.BIZ_KEY: inherited_order_no,
                WorkflowStateKeys.SCOPE_STATUS: "INHERITED",
                WorkflowStateKeys.SCOPE_REASON: "用户当前轮是追问，沿用上一轮采购订单。",
                WorkflowStateKeys.ENTITY: {"orderNo": inherited_order_no},
            }

        fallback_order_no = self._fallback_order_no_from_state(state)
        if fallback_order_no and self._looks_like_order_follow_up(turn):
            return {
                WorkflowStateKeys.BIZ_TYPE: "PURCHASE_ORDER",
                WorkflowStateKeys.BIZ_KEY: fallback_order_no,
                WorkflowStateKeys.SCOPE_STATUS: "INHERITED",
                WorkflowStateKeys.SCOPE_REASON: "用户当前轮是订单追问，沿用上一轮订单诊断结果。",
                WorkflowStateKeys.ENTITY: {"orderNo": fallback_order_no},
            }

        return {
            WorkflowStateKeys.BIZ_TYPE: "PURCHASE_ORDER",
            WorkflowStateKeys.BIZ_KEY: None,
            WorkflowStateKeys.SCOPE_STATUS: "MISSING",
            WorkflowStateKeys.SCOPE_REASON: "用户没有提供采购订单号，也没有可安全继承的订单上下文。",
            WorkflowStateKeys.ENTITY: {},
        }

    def _resolve_warning_scope(self, explicit_entity: dict, memory: dict, turn: dict, state: dict) -> dict:
        days = explicit_entity.get("days")

        if days is None and self._can_inherit(memory, turn, "WARNING_SCAN", "WARNING_SCAN_RANGE"):
            last_biz_key = str(memory.get("lastBizKey") or "")
            days = self._parse_days_from_biz_key(last_biz_key)

        if days is None:
            days = self._fallback_warning_days_from_state(state)

        if days is None:
            days = 7

        biz_key = f"days={days}"
        return {
            WorkflowStateKeys.BIZ_TYPE: "WARNING_SCAN_RANGE",
            WorkflowStateKeys.BIZ_KEY: biz_key,
            WorkflowStateKeys.SCOPE_STATUS: "RANGE",
            WorkflowStateKeys.SCOPE_REASON: "风险扫描按时间范围作为业务对象。",
            WorkflowStateKeys.ENTITY: {"days": days},
        }

    def _resolve_supplier_scope(self, explicit_entity: dict, memory: dict, turn: dict, state: dict) -> dict:
        supplier_id = explicit_entity.get("supplierId")
        days = explicit_entity.get("days")

        if supplier_id is not None:
            if days is None:
                days = 30
            return {
                WorkflowStateKeys.BIZ_TYPE: "SUPPLIER",
                WorkflowStateKeys.BIZ_KEY: f"supplierId={supplier_id},days={days}",
                WorkflowStateKeys.SCOPE_STATUS: "EXPLICIT_NEW",
                WorkflowStateKeys.SCOPE_REASON: "用户当前轮明确提供了供应商。",
                WorkflowStateKeys.ENTITY: {"supplierId": supplier_id, "days": days},
            }

        if self._can_inherit(memory, turn, "SUPPLIER_SCORE", "SUPPLIER"):
            last_supplier_id = memory.get("lastSupplierId")
            if last_supplier_id is not None:
                if days is None:
                    days = memory.get("lastDays") or 30
                return {
                    WorkflowStateKeys.BIZ_TYPE: "SUPPLIER",
                    WorkflowStateKeys.BIZ_KEY: f"supplierId={last_supplier_id},days={days}",
                    WorkflowStateKeys.SCOPE_STATUS: "INHERITED",
                    WorkflowStateKeys.SCOPE_REASON: "用户当前轮是追问，沿用上一轮供应商。",
                    WorkflowStateKeys.ENTITY: {"supplierId": last_supplier_id, "days": days},
                }

        fallback_supplier = self._fallback_supplier_from_state(state)
        if fallback_supplier and self._looks_like_business_follow_up(turn):
            if days is None:
                days = fallback_supplier.get("days") or 30
            supplier_id = fallback_supplier.get("supplierId")
            return {
                WorkflowStateKeys.BIZ_TYPE: "SUPPLIER",
                WorkflowStateKeys.BIZ_KEY: f"supplierId={supplier_id},days={days}",
                WorkflowStateKeys.SCOPE_STATUS: "INHERITED",
                WorkflowStateKeys.SCOPE_REASON: "用户当前轮是供应商追问，沿用上一轮供应商评分结果。",
                WorkflowStateKeys.ENTITY: {"supplierId": supplier_id, "days": days},
            }

        return {
            WorkflowStateKeys.BIZ_TYPE: "SUPPLIER",
            WorkflowStateKeys.BIZ_KEY: None,
            WorkflowStateKeys.SCOPE_STATUS: "MISSING",
            WorkflowStateKeys.SCOPE_REASON: "用户没有提供供应商，也没有可安全继承的供应商上下文。",
            WorkflowStateKeys.ENTITY: {},
        }

    def _can_inherit(self, memory: dict, turn: dict, expected_intent: str, expected_biz_type: str) -> bool:
        return (
            bool(turn.get("isFollowUp"))
            and memory.get("lastIntent") == expected_intent
            and memory.get("lastBizType") == expected_biz_type
            and bool(memory.get("lastBizKey"))
        )

    def _looks_like_order_follow_up(self, turn: dict) -> bool:
        return self._looks_like_business_follow_up(turn)

    def _looks_like_business_follow_up(self, turn: dict) -> bool:
        speech_act = str(turn.get("speechAct") or "")
        return bool(turn.get("isFollowUp")) or speech_act in {
            "ASK_ACTION",
            "ASK_REASON",
            "ASK_OWNER",
            "ASK_SUMMARY",
            "ASK_OR_CHAT",
            "COMPLAINT",
        }

    def _fallback_order_no_from_state(self, state: dict) -> str | None:
        diagnosis = dict(state.get(WorkflowStateKeys.ORDER_DIAGNOSIS, {}) or {})
        if diagnosis.get("orderNo"):
            return str(diagnosis.get("orderNo"))

        snapshot = dict(state.get(WorkflowStateKeys.ORDER_SNAPSHOT, {}) or {})
        if snapshot.get("orderNo"):
            return str(snapshot.get("orderNo"))

        plan = dict(state.get(WorkflowStateKeys.ANSWER_PLAN, {}) or {})
        if plan.get("intent") == WorkflowIntent.ORDER_DIAGNOSIS.value and plan.get("bizKey"):
            return str(plan.get("bizKey"))

        return None

    def _fallback_warning_days_from_state(self, state: dict) -> int | None:
        context = dict(state.get(WorkflowStateKeys.WARNING_CONTEXT, {}) or {})
        if context.get("days") is not None:
            return int(context.get("days"))

        plan = dict(state.get(WorkflowStateKeys.ANSWER_PLAN, {}) or {})
        if plan.get("intent") == WorkflowIntent.WARNING_SCAN.value:
            return self._parse_days_from_biz_key(str(plan.get("bizKey") or ""))

        return None

    def _fallback_supplier_from_state(self, state: dict) -> dict | None:
        score = dict(state.get(WorkflowStateKeys.SUPPLIER_SCORE, {}) or {})
        supplier_id = score.get("supplierId")
        if supplier_id is not None:
            return {"supplierId": supplier_id, "days": 30}

        context = dict(state.get(WorkflowStateKeys.SUPPLIER_CONTEXT, {}) or {})
        supplier = dict(context.get("supplier") or {})
        supplier_id = supplier.get("supplierId") or supplier.get("id")
        if supplier_id is not None:
            return {"supplierId": supplier_id, "days": context.get("days") or 30}

        plan = dict(state.get(WorkflowStateKeys.ANSWER_PLAN, {}) or {})
        if plan.get("intent") == WorkflowIntent.SUPPLIER_SCORE.value:
            return self._parse_supplier_biz_key(str(plan.get("bizKey") or ""))

        return None

    def _parse_supplier_biz_key(self, biz_key: str) -> dict | None:
        values = {}
        for part in biz_key.split(","):
            if "=" not in part:
                continue
            key, value = part.split("=", 1)
            values[key.strip()] = value.strip()
        if not values.get("supplierId"):
            return None
        return {
            "supplierId": int(values["supplierId"]),
            "days": int(values.get("days") or 30),
        }

    def _parse_days_from_biz_key(self, biz_key: str) -> int | None:
        if not biz_key.startswith("days="):
            return None
        try:
            return int(biz_key.replace("days=", "", 1))
        except ValueError:
            return None
