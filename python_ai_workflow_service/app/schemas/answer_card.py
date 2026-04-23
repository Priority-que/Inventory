from typing import Any

from pydantic import Field

from app.schemas.common import ApiModel


class AnswerCard(ApiModel):
    intent: str
    question_focus: str = Field(alias="questionFocus")
    biz_type: str | None = Field(default=None, alias="bizType")
    biz_key: str | None = Field(default=None, alias="bizKey")
    conclusion: str
    reasons: list[str] = Field(default_factory=list)
    evidence: list[str] = Field(default_factory=list)
    unknowns: list[str] = Field(default_factory=list)
    next_actions: list[str] = Field(default_factory=list, alias="nextActions")
    companion_hint: str | None = Field(default=None, alias="companionHint")
