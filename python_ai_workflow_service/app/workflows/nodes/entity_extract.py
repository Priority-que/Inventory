import re

from app.workflows.state import WorkflowStateKeys


ORDER_NO_PATTERN = re.compile(r"PO\d+")
DAYS_PATTERN = re.compile(r"(?:最近|近)?(\d+)\s*天")
SUPPLIER_ID_PATTERN = re.compile(r"供应商\s*(\d+)")


class EntityExtractNode:
    async def __call__(self, state: dict) -> dict:
        message = str(state.get(WorkflowStateKeys.NORMALIZED_MESSAGE, ""))
        entity = dict(state.get(WorkflowStateKeys.ENTITY, {}) or {})

        order_match = ORDER_NO_PATTERN.search(message)
        if order_match:
            entity["orderNo"] = order_match.group(0)

        days_match = DAYS_PATTERN.search(message)
        if days_match:
            entity["days"] = int(days_match.group(1))
        elif entity.get("days") is None:
            entity["days"] = 30

        supplier_match = SUPPLIER_ID_PATTERN.search(message)
        if supplier_match:
            entity["supplierId"] = int(supplier_match.group(1))

        return {WorkflowStateKeys.ENTITY: entity}
