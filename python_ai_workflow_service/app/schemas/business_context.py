from typing import Any

from pydantic import Field

from app.schemas.common import ApiModel


class AgentOrderContextVO(ApiModel):
    exists: bool = False
    order: dict[str, Any] | None = None
    stage: dict[str, Any] | None = None
    responsibility: dict[str, Any] | None = None
    evidence: list[dict[str, Any]] = Field(default_factory=list)
    next_action: dict[str, Any] | None = Field(default=None, alias="nextAction")


class AgentWarningContextVO(ApiModel):
    days: int = 7
    summary: dict[str, Any] | None = None
    items: list[dict[str, Any]] = Field(default_factory=list)
    top_items: list[dict[str, Any]] = Field(default_factory=list, alias="topItems")
    owner_stats: list[dict[str, Any]] = Field(default_factory=list, alias="ownerStats")
    risk_type_stats: list[dict[str, Any]] = Field(default_factory=list, alias="riskTypeStats")


class AgentSupplierContextVO(ApiModel):
    exists: bool = False
    days: int = 30
    supplier: dict[str, Any] | None = None
    metrics: dict[str, Any] | None = None
    score: dict[str, Any] | None = None
    score_breakdown: list[dict[str, Any]] = Field(default_factory=list, alias="scoreBreakdown")
    weak_metrics: list[dict[str, Any]] = Field(default_factory=list, alias="weakMetrics")
    analysis_summary: str | None = Field(default=None, alias="analysisSummary")
    suggestion: str | None = None
