from fastapi import APIRouter, File, Form, Header, UploadFile
from fastapi.responses import StreamingResponse

from app.agent_v2.executor import AgentV2Executor
from app.clients.inventory_backend import InventoryBackendClient
from app.clients.llm_client import LLMClient
from app.core.exceptions import ApiException
from app.core.response import success
from app.repositories.session_store import SessionStore
from app.schemas.rag import RagKnowledgeImportRequest, RagSearchRequest
from app.schemas.workflow import (
    AgentMessageVO,
    AgentSessionVO,
    SessionTitleGenerateRequest,
    SessionTitleGenerateVO,
    WorkflowAgentRequest,
)
from app.services.rag_service import RagService


router = APIRouter(prefix="/agent", tags=["agent"])

backend = InventoryBackendClient()
llm_client = LLMClient()
session_store = SessionStore()
rag_service = RagService()
workflow_executor = AgentV2Executor(backend, llm_client, rag_service, session_store)
UPLOAD_READ_CHUNK_SIZE = 64 * 1024


async def _read_upload_file_bytes(file: UploadFile, max_bytes: int) -> bytes:
    chunks: list[bytes] = []
    total_size = 0

    while True:
        chunk = await file.read(UPLOAD_READ_CHUNK_SIZE)
        if not chunk:
            break

        total_size += len(chunk)
        if total_size > max_bytes:
            raise ApiException(code=400, msg="上传文件不能超过 5MB", http_status_code=400)

        chunks.append(chunk)

    return b"".join(chunks)


@router.post("/workflow/execute")
async def execute_workflow(
    request: WorkflowAgentRequest,
    authorization: str | None = Header(default=None, alias="Authorization"),
):
    result = await workflow_executor.execute(request, authorization or "")
    return success(result)


@router.post("/workflow/stream")
async def stream_workflow(
    request: WorkflowAgentRequest,
    authorization: str | None = Header(default=None, alias="Authorization"),
):
    return StreamingResponse(
        workflow_executor.stream_execute(request, authorization or ""),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
        },
    )


@router.post("/rag/import")
async def import_rag_knowledge(
    request: RagKnowledgeImportRequest,
    authorization: str | None = Header(default=None, alias="Authorization"),
):
    await backend.get_current_user(authorization or "")
    result = await rag_service.import_knowledge(request)
    return success(result)


@router.post("/rag/upload")
async def upload_rag_knowledge(
    file: UploadFile = File(...),
    doc_code: str | None = Form(default=None, alias="docCode"),
    title: str | None = Form(default=None),
    doc_type: str | None = Form(default=None, alias="docType"),
    biz_intent: str | None = Form(default=None, alias="bizIntent"),
    source_path: str | None = Form(default=None, alias="sourcePath"),
    authorization: str | None = Header(default=None, alias="Authorization"),
):
    await backend.get_current_user(authorization or "")
    try:
        content_bytes = await _read_upload_file_bytes(file, rag_service.MAX_UPLOAD_BYTES)
        result = await rag_service.import_text_file(
            filename=file.filename,
            content_bytes=content_bytes,
            doc_code=doc_code,
            title=title,
            doc_type=doc_type,
            biz_intent=biz_intent,
            source_path=source_path,
        )
    finally:
        await file.close()
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


@router.post("/session/title/generate")
async def generate_session_title(
    request: SessionTitleGenerateRequest,
    authorization: str | None = Header(default=None, alias="Authorization"),
):
    current_user = await backend.get_current_user(authorization or "")
    first_message = session_store.get_first_user_message(request.thread_id, current_user.id)
    if not first_message:
        raise ApiException(code=400, msg="该会话没有可用于生成标题的用户消息", http_status_code=400)

    state = session_store.load_state_by_thread_id(request.thread_id)
    last_plan = state.get("lastPlan") if isinstance(state.get("lastPlan"), dict) else {}
    title, generated_by = await workflow_executor.title_generator.generate(first_message, last_plan.get("task"))
    session_store.update_session_title(request.thread_id, current_user.id, title)
    return success(SessionTitleGenerateVO(threadId=request.thread_id, title=title, generatedBy=generated_by))
