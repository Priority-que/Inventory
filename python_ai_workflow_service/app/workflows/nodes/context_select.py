from app.schemas.answer_plan import SelectedContext
from app.workflows.state import InteractionType, WorkflowIntent, WorkflowStateKeys


class ContextSelectNode:
    async def __call__(self, state: dict) -> dict:
        interaction_type = str(state.get(WorkflowStateKeys.INTERACTION_TYPE, InteractionType.BUSINESS.value))
        intent = str(state.get(WorkflowStateKeys.INTENT, WorkflowIntent.UNKNOWN.value))
        plan = dict(state.get(WorkflowStateKeys.ANSWER_PLAN, {}) or {})
        scope_status = str(state.get(WorkflowStateKeys.SCOPE_STATUS, "") or "")
        scope_reason = str(state.get(WorkflowStateKeys.SCOPE_REASON, "") or "")

        if interaction_type != InteractionType.BUSINESS.value:
            selected = SelectedContext(
                interactionType=interaction_type,
                intent=WorkflowIntent.UNKNOWN.value,
                questionFocus=plan.get("questionFocus", interaction_type),
                answerMode=plan.get("answerMode", interaction_type),
                bizType=plan.get("bizType"),
                bizKey=plan.get("bizKey"),
                useLlm=plan.get("useLlm", False),
                summary="非业务问题",
                facts={},
            )
        elif scope_status == "MISSING":
            selected = SelectedContext(
                interactionType=InteractionType.CLARIFY.value,
                intent=intent,
                questionFocus="CLARIFY",
                answerMode="CLARIFY",
                bizType=state.get(WorkflowStateKeys.BIZ_TYPE),
                bizKey=None,
                useLlm=False,
                summary=scope_reason or "我需要你补充一下业务对象。",
                facts={
                    "scopeReason": scope_reason,
                    "missingObject": self._missing_object_name(intent),
                },
                instruction="请提示用户补充必要的业务对象，不要编造业务结论。",
            )
        elif intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            selected = self._select_order_context(state, plan)
        elif intent == WorkflowIntent.WARNING_SCAN.value:
            selected = self._select_warning_context(state, plan)
        elif intent == WorkflowIntent.SUPPLIER_SCORE.value:
            selected = self._select_supplier_context(state, plan)
        else:
            selected = SelectedContext(
                interactionType=interaction_type,
                intent=intent,
                questionFocus=plan.get("questionFocus", "CLARIFY"),
                answerMode=plan.get("answerMode", "CLARIFY"),
                bizType=plan.get("bizType"),
                bizKey=plan.get("bizKey"),
                useLlm=plan.get("useLlm", False),
                summary="信息不足",
                facts={},
            )
        return {WorkflowStateKeys.SELECTED_CONTEXT: selected.model_dump(by_alias=True)}

    def _select_order_context(self, state: dict, plan: dict) -> SelectedContext:
        diagnosis = dict(state.get(WorkflowStateKeys.ORDER_DIAGNOSIS, {}) or {})
        focus = plan.get("questionFocus", "FULL_DIAGNOSIS")

        responsibility = dict(diagnosis.get("responsibility") or {})
        next_action = dict(diagnosis.get("nextAction") or {})

        facts = {
            "orderNo": diagnosis.get("orderNo"),
            "currentStage": diagnosis.get("currentStage"),
            "blockReason": diagnosis.get("blockReason"),
            "responsibility": responsibility,
            "nextAction": next_action,
            "evidence": diagnosis.get("evidence") or [],
        }

        instruction_map = {
            "OWNER": "只回答谁处理；如果没有具体人，明确说只能定位到角色。",
            "OWNER_REASON": "回答为什么是这个人或这个角色处理；如果没有具体人，不允许编造。",
            "NEXT_ACTION": "只回答下一步谁处理、怎么处理。",
            "CAUSE": "只解释为什么没完成、卡在哪。",
            "EVIDENCE": "只解释判断依据。",
            "FULL_DIAGNOSIS": "给出完整但简洁的订单诊断。",
        }

        return SelectedContext(
            interactionType=InteractionType.BUSINESS.value,
            intent=WorkflowIntent.ORDER_DIAGNOSIS.value,
            questionFocus=focus,
            answerMode=plan.get("answerMode", "FOCUSED_ANSWER"),
            bizType=plan.get("bizType"),
            bizKey=plan.get("bizKey"),
            useLlm=plan.get("useLlm", True),
            summary=diagnosis.get("blockReason"),
            facts=facts,
            instruction=instruction_map.get(focus, "按用户问题回答。"),
        )

    def _select_warning_context(self, state: dict, plan: dict) -> SelectedContext:
        analysis = dict(state.get(WorkflowStateKeys.WARNING_ANALYSIS, {}) or {})
        focus = plan.get("questionFocus", "WARNING_SUMMARY")
        target_biz_no = plan.get("targetBizNo")
        items = analysis.get("items") or []

        if focus == "SPECIFIC_WARNING_REASON" and target_biz_no:
            selected_items = [item for item in items if item.get("bizNo") == target_biz_no]
        elif focus in {"TOP_RISK", "WARNING_PRIORITY_REASON"}:
            selected_items = analysis.get("topItems") or items[:10]
        elif focus == "WARNING_OWNER":
            selected_items = analysis.get("topItems") or items[:10]
        else:
            selected_items = analysis.get("topItems") or items[:10]

        facts = {
            "summaryStats": analysis.get("summaryStats") or {},
            "ownerStats": analysis.get("ownerStats") or [],
            "riskTypeStats": analysis.get("riskTypeStats") or [],
        }

        instruction_map = {
            "WARNING_SUMMARY": "回答总风险概况、最值得先处理的风险类型、建议先由谁处理。",
            "TOP_RISK": "回答最该先处理的几单，以及为什么优先。",
            "SPECIFIC_WARNING_REASON": "解释某张单为什么高风险、为什么优先、由谁处理。",
            "WARNING_OWNER": "解释主要由哪些角色处理这些风险。",
            "WARNING_PRIORITY_REASON": "解释为什么这些风险优先级更高。",
            "WARNING_ACTION": "告诉用户接下来怎么处理这些风险。",
        }

        return SelectedContext(
            interactionType=InteractionType.BUSINESS.value,
            intent=WorkflowIntent.WARNING_SCAN.value,
            questionFocus=focus,
            answerMode=plan.get("answerMode", "FOCUSED_ANSWER"),
            bizType=plan.get("bizType"),
            bizKey=plan.get("bizKey"),
            useLlm=plan.get("useLlm", True),
            summary=analysis.get("summary"),
            facts=facts,
            items=selected_items,
            instruction=instruction_map.get(focus, "按用户问题回答。"),
        )

    def _select_supplier_context(self, state: dict, plan: dict) -> SelectedContext:
        score = dict(state.get(WorkflowStateKeys.SUPPLIER_SCORE, {}) or {})
        focus = plan.get("questionFocus", "SUPPLIER_FULL_ANALYSIS")

        facts = {
            "supplierName": score.get("supplierName"),
            "score": score.get("score"),
            "level": score.get("level"),
            "levelExplain": score.get("levelExplain"),
            "confirmRate": score.get("confirmRate"),
            "arrivalCompletionRate": score.get("arrivalCompletionRate"),
            "inboundCompletionRate": score.get("inboundCompletionRate"),
            "abnormalArrivalRate": score.get("abnormalArrivalRate"),
            "scoreBreakdown": score.get("scoreBreakdown") or [],
            "weakMetrics": score.get("weakMetrics") or [],
            "analysis": score.get("analysis"),
            "suggestion": score.get("suggestion"),
        }

        instruction_map = {
            "SUPPLIER_FULL_ANALYSIS": "给出完整但简洁的履约分析，必须直接指出主要短板。",
            "SCORE_MEANING": "解释这个分数和等级意味着什么。",
            "SCORE_REASON": "解释这个分数是怎么来的，哪些指标影响最大。",
            "WEAK_METRIC": "直接指出差在哪、哪个指标拖后腿。",
            "COOP_ADVICE": "说明还能不能继续合作以及理由。",
            "SUPPLIER_ACTION": "告诉用户下一步怎么管控或改善。",
        }

        return SelectedContext(
            interactionType=InteractionType.BUSINESS.value,
            intent=WorkflowIntent.SUPPLIER_SCORE.value,
            questionFocus=focus,
            answerMode=plan.get("answerMode", "FOCUSED_ANSWER"),
            bizType=plan.get("bizType"),
            bizKey=plan.get("bizKey"),
            useLlm=plan.get("useLlm", True),
            summary=score.get("analysis") or score.get("suggestion"),
            facts=facts,
            instruction=instruction_map.get(focus, "按用户问题回答。"),
        )

    def _missing_object_name(self, intent: str) -> str:
        if intent == WorkflowIntent.ORDER_DIAGNOSIS.value:
            return "采购订单号"
        if intent == WorkflowIntent.SUPPLIER_SCORE.value:
            return "供应商"
        return "业务对象"
