from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.api.routes.agent import router as agent_router
from app.api.routes.health import router as health_router
from app.core.config import get_settings
from app.core.exceptions import ApiException
from app.core.response import fail


settings = get_settings()

app = FastAPI(
    title=settings.app_name,
    version="1.0.0",
    description="Python workflow agent service for inventory system.",
)


@app.exception_handler(ApiException)
async def api_exception_handler(request: Request, exc: ApiException):
    return JSONResponse(
        status_code=exc.http_status_code,
        content=fail(code=exc.code, msg=exc.msg, data=exc.data),
    )


@app.exception_handler(Exception)
async def common_exception_handler(request: Request, exc: Exception):
    return JSONResponse(
        status_code=500,
        content=fail(code=500, msg=f"Python workflow 服务异常：{exc}", data=None),
    )


app.include_router(health_router)
app.include_router(agent_router)
