import httpx

from app.core.config import get_settings
from app.core.exceptions import ApiException


class LLMClient:
    def __init__(self):
        self.settings = get_settings()

    def is_configured(self) -> bool:
        return bool(self.settings.model_api_key) and self.settings.model_api_key not in (
            "replace-with-your-api-key",
            "replace-with-your-dashscope-api-key",
        )

    async def chat_text(self, system_prompt: str, user_prompt: str, temperature: float = 0.2) -> str:
        if not self.is_configured():
            return "LLM 未配置 MODEL_API_KEY / AI_DASHSCOPE_API_KEY，当前返回本地占位文本。"

        url = self.settings.model_base_url.rstrip("/") + "/chat/completions"
        payload = {
            "model": self.settings.model_name,
            "temperature": temperature,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
        }
        headers = {
            "Authorization": f"Bearer {self.settings.model_api_key}",
            "Content-Type": "application/json",
        }

        try:
            async with httpx.AsyncClient(timeout=self.settings.model_timeout, trust_env=False) as client:
                response = await client.post(url, headers=headers, json=payload)
        except httpx.RequestError as exc:
            raise ApiException(
                code=500,
                msg=f"请求模型服务失败：{exc.__class__.__name__}: {exc}",
                http_status_code=500,
            ) from exc

        if response.status_code >= 400:
            raise ApiException(code=500, msg=f"模型服务 HTTP 异常：{response.status_code} {response.text}", http_status_code=500)

        body = response.json()
        choices = body.get("choices") or []
        if not choices:
            raise ApiException(code=500, msg=f"模型服务响应缺少 choices：{body}", http_status_code=500)

        message = choices[0].get("message") or {}
        content = message.get("content")
        if not content:
            raise ApiException(code=500, msg=f"模型服务响应缺少 content：{body}", http_status_code=500)
        return content
