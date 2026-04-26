from typing import Any

from pydantic import Field

from app.schemas.common import ApiModel


class AnswerPlan(ApiModel):
    interaction_type: str = Field(default="BUSINESS", alias="interactionType")
    intent: str
    question_focus: str = Field(default="FULL_ANALYSIS", alias="questionFocus")
    turn_type: str = Field(default="FIRST_TURN", alias="turnType")
    answer_mode: str = Field(default="FULL_ANALYSIS", alias="answerMode")
    biz_type: str | None = Field(default=None, alias="bizType")
    biz_key: str | None = Field(default=None, alias="bizKey")
    target_biz_no: str | None = Field(default=None, alias="targetBizNo")
    target_order_no: str | None = Field(default=None, alias="targetOrderNo")
    target_supplier_id: int | None = Field(default=None, alias="targetSupplierId")
    needs_refresh: bool = Field(default=True, alias="needsRefresh")
    use_llm: bool = Field(default=True, alias="useLlm")
    max_context_items: int = Field(default=10, alias="maxContextItems")


class SelectedContext(ApiModel):
    interaction_type: str = Field(default="BUSINESS", alias="interactionType")
    intent: str
    question_focus: str = Field(default="FULL_ANALYSIS", alias="questionFocus")
    answer_mode: str = Field(default="FULL_ANALYSIS", alias="answerMode")
    biz_type: str | None = Field(default=None, alias="bizType")
    biz_key: str | None = Field(default=None, alias="bizKey")
    use_llm: bool = Field(default=True, alias="useLlm")
    summary: str | None = None
    facts: dict[str, Any] = Field(default_factory=dict)
    items: list[dict[str, Any]] = Field(default_factory=list)
    instruction: str | None = None
