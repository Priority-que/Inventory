import json
from typing import Any

from app.agent_v2.schemas import AgentEvidence, AgentMemory, AgentMessageMemory, AgentPlan
from app.repositories.session_store import SessionStore


class MemoryLoader:
    RECENT_MESSAGE_LIMIT = 100

    def __init__(self, session_store: SessionStore):
        self.session_store = session_store

    def load(self, session: dict, user_id: int) -> AgentMemory:
        state = self.session_store.load_state_by_thread_id(session["thread_id"])
        rows = self.session_store.get_messages(session["thread_id"], user_id)

        recent_messages = self._build_recent_messages(rows)
        business_memory = state.get("businessMemory") or state.get("conversationMemory") or {}

        return AgentMemory(
            threadId=session["thread_id"],
            sessionId=session["id"],
            conversationSummary=state.get("conversationSummary"),
            recentMessages=recent_messages,
            businessMemory=business_memory if isinstance(business_memory, dict) else {},
        )

    def _build_recent_messages(self, rows: list[dict]) -> list[AgentMessageMemory]:
        messages: list[AgentMessageMemory] = []
        for row in rows:
            role = str(row.get("message_role") or "").strip()
            content = row.get("content")
            if role not in {"USER", "ASSISTANT"}:
                continue
            if not content or not str(content).strip():
                continue
            messages.append(AgentMessageMemory(role=role, content=str(content)))

        return messages[-self.RECENT_MESSAGE_LIMIT :]


class MemoryManager:
    MAX_SUMMARY_CHARS = 600

    def __init__(self, llm_client: Any):
        self.llm_client = llm_client

    async def update_conversation_summary(
        self,
        memory: AgentMemory,
        user_message: str,
        assistant_answer: str,
        plan: AgentPlan,
        evidence: AgentEvidence,
    ) -> tuple[str | None, dict[str, Any]]:
        previous_summary = (memory.conversation_summary or "").strip()

        is_configured = getattr(self.llm_client, "is_configured", None)
        if callable(is_configured) and not is_configured():
            return previous_summary or None, {
                "updated": False,
                "reason": "LLM 未配置，保留旧会话摘要",
                "summaryLength": len(previous_summary),
            }

        try:
            raw_summary = await self.llm_client.chat_text(
                self._system_prompt(),
                self._user_prompt(memory, user_message, assistant_answer, plan, evidence),
                temperature=0.15,
            )
        except Exception as exc:
            return previous_summary or None, {
                "updated": False,
                "reason": f"摘要更新失败：{exc.__class__.__name__}: {exc}",
                "summaryLength": len(previous_summary),
            }

        summary = self._clean_summary(raw_summary)
        if not summary:
            return previous_summary or None, {
                "updated": False,
                "reason": "模型返回摘要为空，保留旧会话摘要",
                "summaryLength": len(previous_summary),
            }
        if "LLM 未配置" in summary:
            return previous_summary or None, {
                "updated": False,
                "reason": "LLM 未配置，保留旧会话摘要",
                "summaryLength": len(previous_summary),
            }

        return summary, {
            "updated": summary != previous_summary,
            "reason": None,
            "summaryLength": len(summary),
        }

    def _system_prompt(self) -> str:
        return (
            "你是库存/采购协同系统 Agent 的会话记忆整理器。"
            "你的任务是更新当前会话摘要，不是回答用户。"
            "摘要要保留用户目标、项目背景、已讨论结论、业务对象、关键证据和待办。"
            "不要编造工具证据之外的订单号、供应商、数量、状态、评分或风险等级。"
            "控制在 150 到 300 个中文字，直接输出摘要文本，不要输出 JSON、标题或 Markdown。"
        )

    def _user_prompt(
        self,
        memory: AgentMemory,
        user_message: str,
        assistant_answer: str,
        plan: AgentPlan,
        evidence: AgentEvidence,
    ) -> str:
        payload = {
            "旧会话摘要": memory.conversation_summary,
            "最近消息": [item.model_dump(by_alias=True) for item in memory.recent_messages[-8:]],
            "本轮用户输入": user_message,
            "本轮助手回答": assistant_answer,
            "本轮任务计划": plan.model_dump(by_alias=True),
            "本轮证据摘要": {
                "task": evidence.task,
                "summary": evidence.summary,
                "facts": self._compact_value(evidence.facts),
                "itemCount": len(evidence.items),
                "sourceTools": evidence.source_tools,
                "errors": evidence.errors,
            },
            "输出要求": [
                "生成更新后的完整会话摘要，而不是只总结本轮。",
                "保留用户正在做什么、想解决什么、前面已经确定了什么。",
                "如果只是闲聊或情绪表达，也可以简短记录用户状态和当前讨论方向。",
                "不要添加没有证据的新业务结论。",
            ],
        }
        return json.dumps(payload, ensure_ascii=False, default=str)

    def _clean_summary(self, raw: str | None) -> str:
        value = (raw or "").strip()
        if value.startswith("```"):
            value = value.strip("`").strip()
            for prefix in ("json", "text", "markdown"):
                if value.lower().startswith(prefix):
                    value = value[len(prefix) :].strip()
        if value.startswith("{"):
            try:
                body = json.loads(value)
            except json.JSONDecodeError:
                body = {}
            for key in ("summary", "conversationSummary", "text", "content"):
                if body.get(key):
                    value = str(body[key]).strip()
                    break
        value = " ".join(value.split())
        return value[: self.MAX_SUMMARY_CHARS]

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
