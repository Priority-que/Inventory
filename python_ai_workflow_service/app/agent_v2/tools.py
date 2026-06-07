from dataclasses import dataclass, field
from typing import Any

from app.agent_v2.schemas import AgentPlan, ToolCallResult
from app.clients.inventory_backend import InventoryBackendClient
from app.services.rag_service import RagService


@dataclass(frozen=True)
class ToolSpec:
    name: str
    task: str
    description: str
    required: list[str] = field(default_factory=list)
    optional: list[str] = field(default_factory=list)
    defaults: dict[str, Any] = field(default_factory=dict)
    available_tasks: list[str] = field(default_factory=list)

    def to_prompt_dict(self) -> dict[str, Any]:
        return {
            "name": self.name,
            "task": self.task,
            "description": self.description,
            "required": self.required,
            "optional": self.optional,
            "defaults": self.defaults,
            "availableTasks": self.available_tasks or [self.task],
        }


class ToolRegistry:
    def __init__(self):
        self._specs = {
            "get_order_context": ToolSpec(
                name="get_order_context",
                task="ORDER_DIAGNOSIS",
                description="根据采购订单号查询订单状态、阻塞原因、责任方和下一步建议。",
                required=["orderNo"],
            ),
            "scan_warning_context": ToolSpec(
                name="scan_warning_context",
                task="WARNING_SCAN",
                description="按最近 N 天查询采购/入库协同风险、预警列表、风险类型和责任方分布。",
                optional=["days"],
                defaults={"days": 7},
            ),
            "get_supplier_context": ToolSpec(
                name="get_supplier_context",
                task="SUPPLIER_SCORE",
                description="根据供应商 ID 和天数查询供应商履约评分、短板指标和改进建议。",
                required=["supplierId"],
                optional=["days"],
                defaults={"days": 30},
            ),
            "search_knowledge": ToolSpec(
                name="search_knowledge",
                task="KNOWLEDGE_QA",
                description="查询系统规则、业务流程、操作解释、论文说明和知识库资料；业务任务中只能作为规则解释辅助，不能提供实时业务事实。",
                required=["query"],
                optional=["topK"],
                defaults={"topK": 4},
                available_tasks=["ORDER_DIAGNOSIS", "WARNING_SCAN", "SUPPLIER_SCORE", "KNOWLEDGE_QA"],
            ),
        }

    def list_specs(self) -> list[ToolSpec]:
        return list(self._specs.values())

    def specs_for_prompt(self) -> list[dict[str, Any]]:
        return [spec.to_prompt_dict() for spec in self.list_specs()]

    def get(self, tool_name: str) -> ToolSpec | None:
        return self._specs.get(tool_name)

    def has_tool(self, tool_name: str) -> bool:
        return tool_name in self._specs

    def names_for_task(self, task: str) -> list[str]:
        return [spec.name for spec in self.list_specs() if task in (spec.available_tasks or [spec.task])]

    def primary_names_for_task(self, task: str) -> list[str]:
        return [spec.name for spec in self.list_specs() if spec.task == task]

    def valid_names(self) -> set[str]:
        return set(self._specs.keys())


class ToolExecutor:
    def __init__(
        self,
        backend: InventoryBackendClient,
        rag_service: RagService,
        registry: ToolRegistry | None = None,
    ):
        self.backend = backend
        self.rag_service = rag_service
        self.registry = registry or ToolRegistry()
        self._request_builders = {
            "get_order_context": self._order_request,
            "scan_warning_context": self._warning_request,
            "get_supplier_context": self._supplier_request,
            "search_knowledge": self._knowledge_request,
        }
        self._handlers = {
            "get_order_context": self._get_order_context,
            "scan_warning_context": self._scan_warning_context,
            "get_supplier_context": self._get_supplier_context,
            "search_knowledge": self._search_knowledge,
        }

    async def run(self, plan: AgentPlan, authorization: str) -> list[ToolCallResult]:
        if not plan.can_execute or not plan.tool_names:
            return []

        results: list[ToolCallResult] = []
        for tool_name in plan.tool_names:
            results.append(await self._run_one(tool_name, plan, authorization))
        return results

    async def _run_one(self, tool_name: str, plan: AgentPlan, authorization: str) -> ToolCallResult:
        request = self._request_for_tool(tool_name, plan)
        spec = self.registry.get(tool_name)
        if spec is None:
            return ToolCallResult(
                toolName=tool_name,
                success=False,
                request=request,
                data=None,
                error=f"未知工具：{tool_name}",
            )

        missing_fields = self._missing_required_fields(spec, request)
        if missing_fields:
            return ToolCallResult(
                toolName=tool_name,
                success=False,
                request=request,
                data=None,
                error="缺少工具参数：" + "、".join(missing_fields),
            )

        try:
            handler = self._handlers.get(tool_name)
            if handler is None:
                raise ValueError(f"工具未绑定执行器：{tool_name}")
            data = await handler(request, plan, authorization)
            return ToolCallResult(toolName=tool_name, success=True, request=request, data=self._serialize(data))
        except Exception as exc:
            return ToolCallResult(
                toolName=tool_name,
                success=False,
                request=request,
                data=None,
                error=f"{exc.__class__.__name__}: {exc}",
            )

    def _request_for_tool(self, tool_name: str, plan: AgentPlan) -> dict[str, Any]:
        builder = self._request_builders.get(tool_name)
        return builder(plan) if builder is not None else {}

    def _order_request(self, plan: AgentPlan) -> dict[str, Any]:
        return {"orderNo": plan.slots.order_no}

    def _warning_request(self, plan: AgentPlan) -> dict[str, Any]:
        return {"days": plan.slots.days or 7}

    def _supplier_request(self, plan: AgentPlan) -> dict[str, Any]:
        return {"supplierId": plan.slots.supplier_id, "days": plan.slots.days or 30}

    def _knowledge_request(self, plan: AgentPlan) -> dict[str, Any]:
        return {"query": plan.slots.query, "bizIntent": plan.task, "topK": 4}

    async def _get_order_context(self, request: dict[str, Any], plan: AgentPlan, authorization: str) -> Any:
        return await self.backend.get_agent_order_context(str(request["orderNo"]), authorization)

    async def _scan_warning_context(self, request: dict[str, Any], plan: AgentPlan, authorization: str) -> Any:
        return await self.backend.get_agent_warning_context(int(request.get("days") or 7), authorization)

    async def _get_supplier_context(self, request: dict[str, Any], plan: AgentPlan, authorization: str) -> Any:
        return await self.backend.get_agent_supplier_context(
            int(request["supplierId"]),
            int(request.get("days") or 30),
            authorization,
        )

    async def _search_knowledge(self, request: dict[str, Any], plan: AgentPlan, authorization: str) -> Any:
        return await self.rag_service.search_internal(
            request.get("query") or "",
            str(request.get("bizIntent") or plan.task),
            int(request.get("topK") or 4),
            authorization,
        )

    def _missing_required_fields(self, spec: ToolSpec, request: dict[str, Any]) -> list[str]:
        missing = []
        for field_name in spec.required:
            value = request.get(field_name)
            if value is None or value == "":
                missing.append(field_name)
        return missing

    def _serialize(self, value: Any) -> Any:
        if isinstance(value, list):
            return [self._serialize(item) for item in value]
        if isinstance(value, dict):
            return {key: self._serialize(item) for key, item in value.items()}
        model_dump = getattr(value, "model_dump", None)
        if callable(model_dump):
            return model_dump(by_alias=True)
        return value


ToolRunner = ToolExecutor
