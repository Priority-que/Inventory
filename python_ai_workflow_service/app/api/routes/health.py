from fastapi import APIRouter

from app.core.config import get_settings
from app.core.response import success


router = APIRouter(tags=["health"])


@router.get("/health")
async def health():
    settings = get_settings()
    return success({
        "status": "UP",
        "service": settings.app_name,
        "env": settings.app_env,
    })
