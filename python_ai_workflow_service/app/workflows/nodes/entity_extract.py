import re

from app.workflows.state import WorkflowStateKeys


ORDER_NO_PATTERN = re.compile(r"\bPO\d+\b", re.IGNORECASE)
DAYS_PATTERN = re.compile(r"(?:最近|近)?(\d+)\s*天")
SUPPLIER_ID_PATTERN = re.compile(r"供应商\s*(\d+)")


class EntityExtractNode:
    async def __call__(self, state: dict) -> dict:
        message = str(state.get(WorkflowStateKeys.NORMALIZED_MESSAGE, "") or "")
        explicit_entity = {}

        order_match = ORDER_NO_PATTERN.search(message)
        if order_match:
            explicit_entity["orderNo"] = order_match.group(0).upper()

        days_match = DAYS_PATTERN.search(message)
        if days_match:
            explicit_entity["days"] = int(days_match.group(1))

        supplier_match = SUPPLIER_ID_PATTERN.search(message)
        if supplier_match:
            explicit_entity["supplierId"] = int(supplier_match.group(1))

        return {
            WorkflowStateKeys.EXPLICIT_ENTITY: explicit_entity,
            WorkflowStateKeys.ENTITY: dict(explicit_entity),
        }
