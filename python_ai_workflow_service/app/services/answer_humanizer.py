from typing import Any


class AnswerHumanizer:
    CODE_LABELS = {
        "PURCHASER": "采购侧",
        "WAREHOUSE": "仓库侧",
        "SUPPLIER": "供应商",
        "NONE": "无需处理",
        "WAIT_CONFIRM": "待供应商确认",
        "IN_PROGRESS": "执行中",
        "PARTIAL_ARRIVAL": "部分到货",
        "COMPLETED": "已完成",
        "CLOSED": "已关闭",
        "CANCELLED": "已取消",
        "PENDING": "待入库",
        "HIGH": "高风险",
        "MEDIUM": "中风险",
        "LOW": "低风险",
        "PURCHASE_ORDER": "采购订单",
        "ARRIVAL": "到货单",
        "INBOUND": "入库单",
    }

    def normalize_text(self, text: Any) -> str:
        result = "" if text is None else str(text)
        for code, label in self.CODE_LABELS.items():
            result = result.replace(code, label)
        return result

    def join_items(self, items: list[dict], key: str = "bizNo", limit: int = 5) -> str:
        values = [str(item.get(key)) for item in items[:limit] if item.get(key)]
        return "、".join(values) if values else "暂无"
