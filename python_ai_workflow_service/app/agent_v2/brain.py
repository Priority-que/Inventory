import json
from dataclasses import dataclass
from typing import Any

from app.agent_v2.planner import Planner
from app.agent_v2.schemas import AgentMemory, AgentPlan, ConversationUnderstanding, ResolvedSlots, ToolCallResult
from app.agent_v2.tools import ToolRegistry


@dataclass(frozen=True)
class AgentBrainOutput:
    plan: AgentPlan
    used_llm: bool
    fallback_reason: str | None = None


class AgentBrain:
    VALID_TASKS = {
        "CHAT",
        "EMOTION_SUPPORT",
        "CLARIFY",
        "ORDER_DIAGNOSIS",
        "WARNING_SCAN",
        "SUPPLIER_SCORE",
        "KNOWLEDGE_QA",
    }
    VALID_INTERACTION_TYPES = {"CHAT", "EMOTION", "CLARIFY", "BUSINESS", "FOLLOW_UP", "RULE_QA"}
    TASK_ALIASES = {
        "CASUAL_REPLY": "CHAT",
        "NORMAL_CHAT": "CHAT",
        "EMOTION": "EMOTION_SUPPORT",
        "EMPATHY": "EMOTION_SUPPORT",
        "RULE_QA": "KNOWLEDGE_QA",
        "KNOWLEDGE": "KNOWLEDGE_QA",
        "SUPPLIER": "SUPPLIER_SCORE",
        "ORDER": "ORDER_DIAGNOSIS",
        "WARNING": "WARNING_SCAN",
    }

    def __init__(
        self,
        llm_client: Any,
        fallback_planner: Planner,
        tool_registry: ToolRegistry | None = None,
    ):
        self.llm_client = llm_client
        self.fallback_planner = fallback_planner
        self.tool_registry = tool_registry or ToolRegistry()

    async def decide(
        self,
        message: str,
        memory: AgentMemory,
        rule_understanding: ConversationUnderstanding,
        rule_slots: ResolvedSlots,
        observations: list[ToolCallResult] | None = None,
    ) -> AgentBrainOutput:
        fallback_plan = self.fallback_planner.plan(message, rule_understanding, rule_slots, memory)

        if self._should_use_fast_rule_plan(rule_understanding, observations or []):
            return AgentBrainOutput(
                fallback_plan,
                used_llm=False,
                fallback_reason="轻量对话使用本地规则快路径，避免无业务工具场景超时",
            )

        is_configured = getattr(self.llm_client, "is_configured", None)
        if callable(is_configured) and not is_configured():
            return AgentBrainOutput(fallback_plan, used_llm=False, fallback_reason="LLM 未配置，使用旧规则 Planner")

        try:
            raw = await self.llm_client.chat_text(
                self._system_prompt(),
                self._user_prompt(message, memory, rule_understanding, rule_slots, fallback_plan, observations or []),
                temperature=0.1,
            )
            body = self._parse_json_object(raw)
            plan = self._build_plan(body, message, rule_understanding, rule_slots, fallback_plan)
            return AgentBrainOutput(plan, used_llm=True)
        except Exception as exc:
            return AgentBrainOutput(
                fallback_plan,
                used_llm=False,
                fallback_reason=f"AgentBrain 决策失败，使用旧规则 Planner：{exc.__class__.__name__}: {exc}",
            )

    def _system_prompt(self) -> str:
        return (
            "你是库存/采购协同系统里的 Agent 决策器。"
            "你不直接回答用户，只判断下一步该怎么处理。"
            "你必须只输出一个合法 JSON 对象，不能输出 Markdown、解释文字或代码块。"
            "普通聊天不调用工具；情绪表达先归为 EMOTION_SUPPORT；业务问题必须选择合适工具或要求补充信息。"
            "规则、流程、论文说明、系统解释类问题优先选择 KNOWLEDGE_QA。"
            "业务事实只能来自工具，不能编造订单号、供应商、数量、状态、评分、风险等级。"
            "如果参数不明确，返回 CLARIFY 或在 missingFields 写明缺失字段。"
            "如果已经有工具观察结果且足够回答，就返回同一业务 task，toolNames=[]，canExecute=false。"
            "如果工具失败，不要盲目重复调用同一个工具。"
        )

    def _should_use_fast_rule_plan(
        self,
        rule_understanding: ConversationUnderstanding,
        observations: list[ToolCallResult],
    ) -> bool:
        if observations:
            return False
        if rule_understanding.interaction_type not in {"CHAT", "EMOTION", "CLARIFY"}:
            return False
        return not (
            rule_understanding.is_business_hint
            or rule_understanding.is_rule_question
            or rule_understanding.is_follow_up
            or rule_understanding.needs_business_planner
        )

    def _user_prompt(
        self,
        message: str,
        memory: AgentMemory,
        rule_understanding: ConversationUnderstanding,
        rule_slots: ResolvedSlots,
        fallback_plan: AgentPlan,
        observations: list[ToolCallResult],
    ) -> str:
        payload = {
            "输出格式": {
                "interactionType": "CHAT | EMOTION | CLARIFY | BUSINESS | FOLLOW_UP | RULE_QA",
                "task": "CHAT | EMOTION_SUPPORT | CLARIFY | ORDER_DIAGNOSIS | WARNING_SCAN | SUPPLIER_SCORE | KNOWLEDGE_QA",
                "focus": "SUMMARY | CAUSE | OWNER | NEXT_ACTION | TOP_RISK | WEAK_METRIC | SCORE_REASON | COOP_ADVICE | RULE_EXPLAIN | ASK_INPUT",
                "toolNames": ["只能从可用工具中选择；不需要工具时返回空数组"],
                "slots": {
                    "orderNo": "采购订单号，没有则 null",
                    "supplierId": "供应商ID，没有则 null",
                    "days": "查询天数，没有则 null",
                    "query": "用于知识库或规则咨询的查询文本",
                    "inherited": "是否使用了历史业务记忆",
                },
                "missingFields": ["缺少的必要字段，例如 orderNo、supplierId"],
                "canExecute": "参数足够且需要调用工具时为 true，否则 false",
                "reason": "简短说明为什么这样决策",
            },
            "可用工具": self.tool_registry.specs_for_prompt(),
            "决策原则": [
                "如果用户只是打招呼、闲聊、感谢，task=CHAT，toolNames=[]，canExecute=false。",
                "如果用户表达焦虑、烦躁、挫败，task=EMOTION_SUPPORT，toolNames=[]，canExecute=false。",
                "如果用户问规则、流程、权限、系统设计、论文写法，task=KNOWLEDGE_QA，toolNames=['search_knowledge']。",
                "如果用户问采购订单为什么卡住、谁负责、下一步怎么办，task=ORDER_DIAGNOSIS，需要 orderNo。",
                "如果用户问风险、预警、最近异常、先处理哪些，task=WARNING_SCAN，days 没有时默认 7。",
                "如果用户问供应商履约、评分、短板、是否继续合作，task=SUPPLIER_SCORE，需要 supplierId，days 没有时默认 30。",
                "如果用户追问这个、刚才那个、继续讲，可以结合历史业务记忆继承 orderNo、supplierId 或 days。",
                "业务实时事实必须先查 Java 业务工具，RAG 只能补充规则、流程、论文表达和系统解释。",
                "如果业务工具已经返回观察结果，且用户还问为什么、规则、流程、论文写法，可以第二轮选择 search_knowledge。",
                "如果工具观察结果已经足够回答，保持原业务 task，但不要再调用工具。",
                "如果必须补查，只能选择还没有调用过且和 task 匹配的工具。",
                "不要选择不存在的工具，不要发明参数。",
            ],
            "当前用户消息": message,
            "最近消息": [item.model_dump(by_alias=True) for item in memory.recent_messages[-8:]],
            "会话摘要": memory.conversation_summary,
            "业务记忆": memory.business_memory,
            "工具观察结果": self._observations_for_prompt(observations),
            "已调用工具": self._called_tool_names(observations),
            "旧规则理解": rule_understanding.model_dump(by_alias=True),
            "旧槽位解析": rule_slots.model_dump(by_alias=True),
            "旧规则计划": fallback_plan.model_dump(by_alias=True),
        }
        return json.dumps(payload, ensure_ascii=False, default=str)

    def _parse_json_object(self, raw: str | None) -> dict[str, Any]:
        text = (raw or "").strip()
        if not text:
            raise ValueError("模型返回为空")

        if text.startswith("```"):
            text = text.strip("`").strip()
            for prefix in ("json", "text"):
                if text.lower().startswith(prefix):
                    text = text[len(prefix) :].strip()

        start = text.find("{")
        end = text.rfind("}")
        if start < 0 or end < start:
            raise ValueError("模型返回不是 JSON 对象")

        body = json.loads(text[start : end + 1])
        if not isinstance(body, dict):
            raise ValueError("模型 JSON 根节点不是对象")
        return body

    def _build_plan(
        self,
        body: dict[str, Any],
        message: str,
        rule_understanding: ConversationUnderstanding,
        rule_slots: ResolvedSlots,
        fallback_plan: AgentPlan,
    ) -> AgentPlan:
        task = self._normalize_task(body.get("task"))
        if task not in self.VALID_TASKS:
            raise ValueError(f"非法 task：{body.get('task')}")

        interaction_type = self._normalize_interaction_type(body.get("interactionType"), task, rule_understanding)
        slots = self._build_slots(body.get("slots"), message, rule_slots)
        missing_fields = self._dedupe(self._string_list(body.get("missingFields")))
        missing_fields = self._dedupe(missing_fields + self._required_missing_fields(task, slots))
        can_execute = self._can_execute(task, body.get("canExecute"), missing_fields)
        tool_names = self._sanitize_tools(body.get("toolNames"), task, can_execute)
        focus = self._clean_text(body.get("focus")) or self._default_focus(task, fallback_plan)
        reason = self._clean_text(body.get("reason")) or f"AgentBrain 基于上下文决策为 {task}"

        return AgentPlan(
            interactionType=interaction_type,
            task=task,
            focus=focus,
            toolNames=tool_names,
            slots=slots,
            missingFields=missing_fields,
            canExecute=can_execute,
            reason=f"AgentBrain：{reason}",
        )

    def _normalize_task(self, value: Any) -> str:
        task = self._clean_text(value).upper()
        return self.TASK_ALIASES.get(task, task)

    def _normalize_interaction_type(
        self,
        value: Any,
        task: str,
        rule_understanding: ConversationUnderstanding,
    ) -> str:
        interaction_type = self._clean_text(value).upper()
        if interaction_type in self.VALID_INTERACTION_TYPES:
            return interaction_type

        mapping = {
            "CHAT": "CHAT",
            "EMOTION_SUPPORT": "EMOTION",
            "CLARIFY": "CLARIFY",
            "KNOWLEDGE_QA": "RULE_QA",
        }
        if task in mapping:
            return mapping[task]
        if task in {"ORDER_DIAGNOSIS", "WARNING_SCAN", "SUPPLIER_SCORE"}:
            return "BUSINESS" if not rule_understanding.is_follow_up else "FOLLOW_UP"
        return rule_understanding.interaction_type

    def _build_slots(self, value: Any, message: str, rule_slots: ResolvedSlots) -> ResolvedSlots:
        raw = value if isinstance(value, dict) else {}
        order_no = self._clean_text(raw.get("orderNo") or raw.get("order_no") or rule_slots.order_no) or None
        supplier_id = self._to_int(raw.get("supplierId") or raw.get("supplier_id"))
        days = self._to_int(raw.get("days"))
        query = self._clean_text(raw.get("query") or rule_slots.query or message) or None

        if supplier_id is None:
            supplier_id = rule_slots.supplier_id
        if days is None:
            days = rule_slots.days

        inherited = bool(raw.get("inherited")) or bool(rule_slots.inherited)

        return ResolvedSlots(
            orderNo=order_no,
            supplierId=supplier_id,
            days=days,
            query=query,
            inherited=inherited,
            missingFields=[],
            reason=self._clean_text(raw.get("reason")) or rule_slots.reason,
        )

    def _required_missing_fields(self, task: str, slots: ResolvedSlots) -> list[str]:
        if task == "ORDER_DIAGNOSIS" and not slots.order_no:
            return ["orderNo"]
        if task == "SUPPLIER_SCORE" and slots.supplier_id is None:
            return ["supplierId"]
        return []

    def _can_execute(self, task: str, value: Any, missing_fields: list[str]) -> bool:
        if task in {"CHAT", "EMOTION_SUPPORT", "CLARIFY"}:
            return False
        if missing_fields:
            return False
        if isinstance(value, bool):
            return value
        if isinstance(value, str):
            text = value.strip().lower()
            if text in {"true", "1", "yes", "y"}:
                return True
            if text in {"false", "0", "no", "n"}:
                return False
        if task == "WARNING_SCAN":
            return True
        if task == "KNOWLEDGE_QA":
            return True
        return True

    def _sanitize_tools(self, value: Any, task: str, can_execute: bool) -> list[str]:
        if not can_execute:
            return []

        requested = self._string_list(value)
        allowed_for_task = set(self.tool_registry.names_for_task(task))
        valid = [tool for tool in requested if self.tool_registry.has_tool(tool) and tool in allowed_for_task]
        if valid:
            return self._dedupe(valid)
        return self.tool_registry.primary_names_for_task(task)

    def _default_focus(self, task: str, fallback_plan: AgentPlan) -> str:
        if task == fallback_plan.task and fallback_plan.focus:
            return fallback_plan.focus
        defaults = {
            "CHAT": "CASUAL_REPLY",
            "EMOTION_SUPPORT": "EMPATHY",
            "CLARIFY": "ASK_INPUT",
            "ORDER_DIAGNOSIS": "SUMMARY",
            "WARNING_SCAN": "SUMMARY",
            "SUPPLIER_SCORE": "SUMMARY",
            "KNOWLEDGE_QA": "RULE_EXPLAIN",
        }
        return defaults.get(task, "SUMMARY")

    def _string_list(self, value: Any) -> list[str]:
        if not isinstance(value, list):
            return []
        return [self._clean_text(item) for item in value if self._clean_text(item)]

    def _clean_text(self, value: Any) -> str:
        return str(value or "").strip()

    def _to_int(self, value: Any) -> int | None:
        if value is None or value == "":
            return None
        try:
            return int(value)
        except (TypeError, ValueError):
            return None

    def _observations_for_prompt(self, observations: list[ToolCallResult]) -> list[dict[str, Any]]:
        result = []
        for item in observations[-6:]:
            result.append(
                {
                    "toolName": item.tool_name,
                    "success": item.success,
                    "request": item.request,
                    "error": item.error,
                    "dataPreview": self._compact_value(item.data),
                }
            )
        return result

    def _called_tool_names(self, observations: list[ToolCallResult]) -> list[str]:
        return self._dedupe([item.tool_name for item in observations if item.tool_name])

    def _compact_value(self, value: Any, depth: int = 0) -> Any:
        if value is None:
            return None
        if depth >= 2:
            return self._compact_scalar(value)
        if isinstance(value, dict):
            result: dict[str, Any] = {}
            for index, (key, item) in enumerate(value.items()):
                if index >= 12:
                    result["_truncated"] = True
                    break
                result[str(key)] = self._compact_value(item, depth + 1)
            return result
        if isinstance(value, list):
            return [self._compact_value(item, depth + 1) for item in value[:5]]
        return self._compact_scalar(value)

    def _compact_scalar(self, value: Any) -> Any:
        text = str(value)
        return text[:300] + "..." if len(text) > 300 else value

    def _dedupe(self, values: list[str]) -> list[str]:
        result: list[str] = []
        seen = set()
        for value in values:
            text = self._clean_text(value)
            if not text or text in seen:
                continue
            seen.add(text)
            result.append(text)
        return result
