from decimal import Decimal

from pydantic import Field

from app.schemas.common import ApiModel


class OrderSnapshotVO(ApiModel):
    order_id: int | None = Field(default=None, alias="orderId")
    order_no: str | None = Field(default=None, alias="orderNo")
    status: str | None = None
    supplier_id: int | None = Field(default=None, alias="supplierId")
    supplier_name: str | None = Field(default=None, alias="supplierName")
    total_order_number: Decimal = Field(default=Decimal("0"), alias="totalOrderNumber")
    total_arrive_number: Decimal = Field(default=Decimal("0"), alias="totalArriveNumber")
    total_inbound_number: Decimal = Field(default=Decimal("0"), alias="totalInboundNumber")
    arrival_count: int = Field(default=0, alias="arrivalCount")
    inbound_count: int = Field(default=0, alias="inboundCount")


class OrderDiagnosisVO(ApiModel):
    order_no: str | None = Field(default=None, alias="orderNo")
    current_stage: str | None = Field(default=None, alias="currentStage")
    block_reason: str | None = Field(default=None, alias="blockReason")
    evidence: list[str] = Field(default_factory=list)
    suggest_owner: str | None = Field(default=None, alias="suggestOwner")
    suggest_action: str | None = Field(default=None, alias="suggestAction")
    ai_summary: str | None = Field(default=None, alias="aiSummary")
