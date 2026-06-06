from app.agent_v2.schemas import AgentMemory, AgentMessageMemory
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
