from typing import Any
from datetime import datetime

from pydantic import Field

from app.schemas.common import ApiModel


class WorkflowAgentRequest(ApiModel):
    message: str | None = None
    thread_id: str | None = Field(default=None, alias="threadId")


class WorkflowAgentResponse(ApiModel):
    session_id: int | None = Field(default=None, alias="sessionId")
    thread_id: str = Field(alias="threadId")
    intent: str
    answer: str
    data: Any = None

class AgentSessionVO(ApiModel):
    id: int
    session_no: str = Field(alias="sessionNo")
    thread_id: str = Field(alias="threadId")
    user_id: int = Field(alias="userId")
    title: str | None = None
    agent_type: str = Field(alias="agentType")
    current_intent: str | None = Field(default=None, alias="currentIntent")
    status: str
    last_message_time: datetime | None = Field(default=None, alias="lastMessageTime")
    create_time: datetime | None = Field(default=None, alias="createTime")


class AgentMessageVO(ApiModel):
    id: int
    session_id: int = Field(alias="sessionId")
    thread_id: str = Field(alias="threadId")
    message_role: str = Field(alias="messageRole")
    message_type: str = Field(alias="messageType")
    content: str | None = None
    create_time: datetime | None = Field(default=None, alias="createTime")
