from typing import Any

from pydantic import Field

from app.schemas.common import ApiModel


class AgentMessageMemory(ApiModel):
    role: str
    content: str | None = None


class AgentMemory(ApiModel):
    thread_id: str = Field(alias="threadId")
    session_id: int = Field(alias="sessionId")
    conversation_summary: str | None = Field(default=None, alias="conversationSummary")
    recent_messages: list[AgentMessageMemory] = Field(default_factory=list, alias="recentMessages")
    business_memory: dict[str, Any] = Field(default_factory=dict, alias="businessMemory")


class ConversationUnderstanding(ApiModel):
    interaction_type: str = Field(alias="interactionType")
    emotion: str = "neutral"
    speech_act: str = Field(default="ASK_OR_CHAT", alias="speechAct")
    is_follow_up: bool = Field(default=False, alias="isFollowUp")
    is_business_hint: bool = Field(default=False, alias="isBusinessHint")
    is_rule_question: bool = Field(default=False, alias="isRuleQuestion")
    needs_business_planner: bool = Field(default=False, alias="needsBusinessPlanner")
    normalized_message: str = Field(alias="normalizedMessage")
    raw_reference: str | None = Field(default=None, alias="rawReference")
    reason: str | None = None


class ResolvedSlots(ApiModel):
    order_no: str | None = Field(default=None, alias="orderNo")
    supplier_id: int | None = Field(default=None, alias="supplierId")
    days: int | None = None
    query: str | None = None
    inherited: bool = False
    missing_fields: list[str] = Field(default_factory=list, alias="missingFields")
    reason: str | None = None


class AgentPlan(ApiModel):
    interaction_type: str = Field(alias="interactionType")
    task: str
    focus: str
    tool_names: list[str] = Field(default_factory=list, alias="toolNames")
    slots: ResolvedSlots
    missing_fields: list[str] = Field(default_factory=list, alias="missingFields")
    can_execute: bool = Field(default=True, alias="canExecute")
    reason: str | None = None


class ToolCallResult(ApiModel):
    tool_name: str = Field(alias="toolName")
    success: bool
    request: dict[str, Any] = Field(default_factory=dict)
    data: Any = None
    error: str | None = None


class AgentEvidence(ApiModel):
    task: str
    summary: str | None = None
    facts: dict[str, Any] = Field(default_factory=dict)
    items: list[dict[str, Any]] = Field(default_factory=list)
    source_tools: list[str] = Field(default_factory=list, alias="sourceTools")
    errors: list[str] = Field(default_factory=list)


class AnswerDraft(ApiModel):
    conclusion: str
    reasons: list[str] = Field(default_factory=list)
    next_actions: list[str] = Field(default_factory=list, alias="nextActions")
    limits: list[str] = Field(default_factory=list)


class FactCheckResult(ApiModel):
    passed: bool
    answer: str
    issues: list[str] = Field(default_factory=list)
