from enum import Enum
from typing import Any, TypedDict

from pydantic import Field

from app.schemas.common import ApiModel


class WorkflowIntent(str, Enum):
    ORDER_DIAGNOSIS = "ORDER_DIAGNOSIS"
    WARNING_SCAN = "WARNING_SCAN"
    SUPPLIER_SCORE = "SUPPLIER_SCORE"
    KNOWLEDGE_QA = "KNOWLEDGE_QA"
    UNKNOWN = "UNKNOWN"


class InteractionType(str, Enum):
    BUSINESS = "BUSINESS"
    SOCIAL = "SOCIAL"
    META = "META"
    CLARIFY = "CLARIFY"


class WorkflowStateKeys:
    MESSAGE = "message"
    THREAD_ID = "threadId"
    AUTHORIZATION = "authorization"
    USER_ID = "userId"
    NORMALIZED_MESSAGE = "normalizedMessage"

    INTENT = "intent"
    ACTIVE_INTENT = "activeIntent"
    INTERACTION_TYPE = "interactionType"

    ENTITY = "entity"
    RAG_DOCS = "ragDocs"

    ORDER_SNAPSHOT = "orderSnapshot"
    ORDER_DIAGNOSIS = "orderDiagnosis"

    WARNING_CONTEXT = "warningContext"
    WARNING_ANALYSIS = "warningAnalysis"

    SUPPLIER_METRICS = "supplierMetrics"
    SUPPLIER_SCORE = "supplierScore"

    ANSWER_PLAN = "answerPlan"
    SELECTED_CONTEXT = "selectedContext"
    ANSWER_CARD = "answerCard"
    RESPONSE_POLICY = "responsePolicy"
    CONVERSATION_MEMORY = "conversationMemory"

    LLM_ANSWER = "llmAnswer"
    GUARDRAIL_RESULT = "guardrailResult"
    FINAL_RESPONSE = "finalResponse"
    ERROR_MESSAGE = "errorMessage"
    ROUTE = "_route"

    ORDER_CONTEXT = "orderContext"
    SUPPLIER_CONTEXT = "supplierContext"
    
    TURN_UNDERSTANDING = "turnUnderstanding"
    EXPLICIT_ENTITY = "explicitEntity"
    BIZ_TYPE = "bizType"
    BIZ_KEY = "bizKey"
    SCOPE_STATUS = "scopeStatus"
    SCOPE_REASON = "scopeReason"


class WorkflowEntity(ApiModel):
    order_no: str | None = Field(default=None, alias="orderNo")
    supplier_id: int | None = Field(default=None, alias="supplierId")
    days: int | None = None
    material_code: str | None = Field(default=None, alias="materialCode")
    warehouse_id: int | None = Field(default=None, alias="warehouseId")


class WorkflowGraphState(TypedDict, total=False):
    message: str
    threadId: str
    authorization: str
    userId: int
    normalizedMessage: str

    turnUnderstanding: dict[str, Any]
    explicitEntity: dict[str, Any]
    bizType: str
    bizKey: str
    scopeStatus: str
    scopeReason: str

    intent: str
    activeIntent: str
    interactionType: str

    entity: dict[str, Any]
    ragDocs: str

    orderSnapshot: dict[str, Any]
    orderDiagnosis: dict[str, Any]

    warningContext: dict[str, Any]
    warningAnalysis: dict[str, Any]

    orderContext: dict[str, Any]
    supplierContext: dict[str, Any]

    supplierMetrics: dict[str, Any]
    supplierScore: dict[str, Any]

    answerPlan: dict[str, Any]
    selectedContext: dict[str, Any]
    answerCard: dict[str, Any]
    responsePolicy: dict[str, Any]
    conversationMemory: dict[str, Any]

    llmAnswer: str
    guardrailResult: str
    finalResponse: dict[str, Any]
    errorMessage: str
    _route: str
