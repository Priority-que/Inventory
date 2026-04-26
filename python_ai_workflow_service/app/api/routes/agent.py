from fastapi import APIRouter, Header

from app.clients.inventory_backend import InventoryBackendClient
from app.clients.llm_client import LLMClient
from app.core.response import success
from app.repositories.session_store import SessionStore
from app.schemas.rag import RagKnowledgeImportRequest, RagSearchRequest
from app.schemas.workflow import AgentMessageVO, AgentSessionVO, WorkflowAgentRequest
from app.services.rag_service import RagService
from app.workflows.workflow_executor import WorkflowExecutor


router = APIRouter(prefix="/agent", tags=["agent"])

backend = InventoryBackendClient()
llm_client = LLMClient()
session_store = SessionStore()
rag_service = RagService()
workflow_executor = WorkflowExecutor(backend, llm_client, rag_service, session_store)


@router.post("/workflow/execute")
async def execute_workflow(
    request: WorkflowAgentRequest,
    authorization: str | None = Header(default=None, alias="Authorization"),
):
    result = await workflow_executor.execute(request, authorization or "")
    return success(result)


@router.post("/rag/import")
async def import_rag_knowledge(
    request: RagKnowledgeImportRequest,
    authorization: str | None = Header(default=None, alias="Authorization"),
):
    await backend.get_current_user(authorization or "")
    result = await rag_service.import_knowledge(request)
    return success(result)


@router.post("/rag/search")
async def search_rag_knowledge(
    request: RagSearchRequest,
    authorization: str | None = Header(default=None, alias="Authorization"),
):
    await backend.get_current_user(authorization or "")
    result = await rag_service.search(request)
    return success(result)


@router.get("/session/list")
async def list_sessions(
    authorization: str | None = Header(default=None, alias="Authorization"),
):
    current_user = await backend.get_current_user(authorization or "")
    rows = session_store.list_sessions(current_user.id)
    return success([AgentSessionVO(**row) for row in rows])


@router.get("/session/messages/{thread_id}")
async def get_messages(
    thread_id: str,
    authorization: str | None = Header(default=None, alias="Authorization"),
):
    current_user = await backend.get_current_user(authorization or "")
    rows = session_store.get_messages(thread_id, current_user.id)
    return success([AgentMessageVO(**row) for row in rows])
