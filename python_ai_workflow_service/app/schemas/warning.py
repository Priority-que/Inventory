from pydantic import Field

from app.schemas.common import ApiModel


class WarningSnapshotVO(ApiModel):
    biz_id: int | None = Field(default=None, alias="bizId")
    biz_no: str | None = Field(default=None, alias="bizNo")
    status: str | None = None
    supplier_id: int | None = Field(default=None, alias="supplierId")
    supplier_name: str | None = Field(default=None, alias="supplierName")
    warehouse_id: int | None = Field(default=None, alias="warehouseId")
    warehouse_name: str | None = Field(default=None, alias="warehouseName")
    last_operate_time: str | None = Field(default=None, alias="lastOperateTime")
    overdue_days: int = Field(default=0, alias="overdueDays")


class WarningItemVO(ApiModel):
    risk_level: str = Field(alias="riskLevel")
    biz_type: str = Field(alias="bizType")
    biz_id: int | None = Field(default=None, alias="bizId")
    biz_no: str | None = Field(default=None, alias="bizNo")
    problem: str
    reason: str
    suggest_owner: str = Field(alias="suggestOwner")
    suggest_action: str = Field(alias="suggestAction")


class WarningScanVO(ApiModel):
    summary: str
    items: list[WarningItemVO] = Field(default_factory=list)
    ai_summary: str | None = Field(default=None, alias="aiSummary")
