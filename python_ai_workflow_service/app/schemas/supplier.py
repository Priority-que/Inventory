from pydantic import Field

from app.schemas.common import ApiModel


class SupplierPerformanceMetricsVO(ApiModel):
    supplier_id: int | None = Field(default=None, alias="supplierId")
    supplier_name: str | None = Field(default=None, alias="supplierName")
    total_order_count: int = Field(default=0, alias="totalOrderCount")
    completed_order_count: int = Field(default=0, alias="completedOrderCount")
    cancelled_order_count: int = Field(default=0, alias="cancelledOrderCount")
    abnormal_arrival_count: int = Field(default=0, alias="abnormalArrivalCount")
    total_arrival_count: int = Field(default=0, alias="totalArrivalCount")
    confirm_rate: float = Field(default=0.0, alias="confirmRate")
    arrival_completion_rate: float = Field(default=0.0, alias="arrivalCompletionRate")
    inbound_completion_rate: float = Field(default=0.0, alias="inboundCompletionRate")
    abnormal_arrival_rate: float = Field(default=0.0, alias="abnormalArrivalRate")


class SupplierScoreVO(ApiModel):
    supplier_id: int | None = Field(default=None, alias="supplierId")
    supplier_name: str | None = Field(default=None, alias="supplierName")
    score: int = 0
    level: str = "数据不足"
    confirm_rate: str = Field(default="0.00%", alias="confirmRate")
    arrival_completion_rate: str = Field(default="0.00%", alias="arrivalCompletionRate")
    inbound_completion_rate: str = Field(default="0.00%", alias="inboundCompletionRate")
    abnormal_arrival_rate: str = Field(default="0.00%", alias="abnormalArrivalRate")
    analysis: str | None = None
    suggestion: str | None = None
