from typing import Any

from app.agent_v2.schemas import AgentEvidence, AgentPlan, ToolCallResult


class EvidenceBuilder:
    def build(self, plan: AgentPlan, tool_results: list[ToolCallResult]) -> AgentEvidence:
        errors = [result.error for result in tool_results if result.error]
        source_tools = [result.tool_name for result in tool_results if result.success]

        if plan.task == "ORDER_DIAGNOSIS":
            return self._build_order_evidence(plan, tool_results, source_tools, errors)
        if plan.task == "WARNING_SCAN":
            return self._build_warning_evidence(plan, tool_results, source_tools, errors)
        if plan.task == "SUPPLIER_SCORE":
            return self._build_supplier_evidence(plan, tool_results, source_tools, errors)
        if plan.task == "KNOWLEDGE_QA":
            return self._build_knowledge_evidence(plan, tool_results, source_tools, errors)

        return AgentEvidence(
            task=plan.task,
            summary=plan.reason,
            facts={},
            items=[],
            sourceTools=source_tools,
            errors=[error for error in errors if error],
        )

    def _build_order_evidence(
        self,
        plan: AgentPlan,
        tool_results: list[ToolCallResult],
        source_tools: list[str],
        errors: list[str | None],
    ) -> AgentEvidence:
        context = self._first_success_data(tool_results)
        if not isinstance(context, dict):
            return AgentEvidence(task=plan.task, summary="没有拿到订单上下文。", sourceTools=source_tools, errors=self._clean_errors(errors))

        if context.get("exists") is False:
            return AgentEvidence(
                task=plan.task,
                summary="Java 后端没有查到该采购订单。",
                facts={"orderNo": plan.slots.order_no, "exists": False},
                sourceTools=source_tools,
                errors=self._clean_errors(errors),
            )

        order = context.get("order") or {}
        stage = context.get("stage") or {}
        responsibility = context.get("responsibility") or {}
        next_action = context.get("nextAction") or {}
        evidence_items = context.get("evidence") or []

        return AgentEvidence(
            task=plan.task,
            summary=stage.get("blockReason") or "已获取采购订单上下文。",
            facts={
                "exists": True,
                "orderNo": order.get("orderNo") or plan.slots.order_no,
                "status": order.get("status"),
                "statusName": order.get("statusName"),
                "supplierName": order.get("supplierName"),
                "currentStage": stage.get("currentStage"),
                "blockReason": stage.get("blockReason"),
                "ownerRoleName": responsibility.get("ownerRoleName"),
                "ownerUserName": responsibility.get("ownerUserName"),
                "ownerReason": responsibility.get("ownerReason"),
                "nextAction": next_action.get("actionText"),
                "evidence": evidence_items,
            },
            items=[],
            sourceTools=source_tools,
            errors=self._clean_errors(errors),
        )

    def _build_warning_evidence(
        self,
        plan: AgentPlan,
        tool_results: list[ToolCallResult],
        source_tools: list[str],
        errors: list[str | None],
    ) -> AgentEvidence:
        context = self._first_success_data(tool_results)
        if not isinstance(context, dict):
            return AgentEvidence(task=plan.task, summary="没有拿到预警上下文。", sourceTools=source_tools, errors=self._clean_errors(errors))

        summary = context.get("summary") or {}
        items = context.get("topItems") or context.get("items") or []
        return AgentEvidence(
            task=plan.task,
            summary=summary.get("summaryText") or f"已获取最近 {context.get('days') or plan.slots.days or 7} 天预警上下文。",
            facts={
                "days": context.get("days") or plan.slots.days or 7,
                "summary": summary,
                "ownerStats": context.get("ownerStats") or [],
                "riskTypeStats": context.get("riskTypeStats") or [],
            },
            items=self._trim_items(items, 10),
            sourceTools=source_tools,
            errors=self._clean_errors(errors),
        )

    def _build_supplier_evidence(
        self,
        plan: AgentPlan,
        tool_results: list[ToolCallResult],
        source_tools: list[str],
        errors: list[str | None],
    ) -> AgentEvidence:
        context = self._first_success_data(tool_results)
        if not isinstance(context, dict):
            return AgentEvidence(task=plan.task, summary="没有拿到供应商上下文。", sourceTools=source_tools, errors=self._clean_errors(errors))

        if context.get("exists") is False:
            return AgentEvidence(
                task=plan.task,
                summary=context.get("analysisSummary") or "Java 后端没有查到该供应商。",
                facts={"supplierId": plan.slots.supplier_id, "exists": False},
                sourceTools=source_tools,
                errors=self._clean_errors(errors),
            )

        supplier = context.get("supplier") or {}
        metrics = context.get("metrics") or {}
        score = context.get("score") or {}
        return AgentEvidence(
            task=plan.task,
            summary=context.get("analysisSummary") or "已获取供应商履约上下文。",
            facts={
                "exists": True,
                "supplierId": supplier.get("supplierId") or plan.slots.supplier_id,
                "supplierName": supplier.get("supplierName"),
                "days": context.get("days") or plan.slots.days or 30,
                "score": score.get("totalScore"),
                "level": score.get("level"),
                "levelExplain": score.get("levelExplain"),
                "metrics": metrics,
                "scoreBreakdown": context.get("scoreBreakdown") or [],
                "weakMetrics": context.get("weakMetrics") or [],
                "suggestion": context.get("suggestion"),
            },
            items=[],
            sourceTools=source_tools,
            errors=self._clean_errors(errors),
        )

    def _build_knowledge_evidence(
        self,
        plan: AgentPlan,
        tool_results: list[ToolCallResult],
        source_tools: list[str],
        errors: list[str | None],
    ) -> AgentEvidence:
        docs = self._first_success_data(tool_results)
        items = docs if isinstance(docs, list) else []
        return AgentEvidence(
            task=plan.task,
            summary=f"知识库检索到 {len(items)} 条资料。" if items else "知识库暂未检索到可用资料。",
            facts={"query": plan.slots.query},
            items=self._trim_items(items, 4),
            sourceTools=source_tools,
            errors=self._clean_errors(errors),
        )

    def _first_success_data(self, tool_results: list[ToolCallResult]) -> Any:
        for result in tool_results:
            if result.success:
                return result.data
        return None

    def _trim_items(self, items: list[Any], limit: int) -> list[dict[str, Any]]:
        result: list[dict[str, Any]] = []
        for item in items[:limit]:
            if isinstance(item, dict):
                result.append(item)
            else:
                result.append({"value": item})
        return result

    def _clean_errors(self, errors: list[str | None]) -> list[str]:
        return [str(error) for error in errors if error]
