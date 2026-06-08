import json
from typing import Any, AsyncIterator

import httpx

from app.core.config import get_settings
from app.core.exceptions import ApiException


class LLMClient:
    def __init__(self):
        self.settings = get_settings()

    def is_configured(self) -> bool:
        api_key = self._api_key()
        return bool(api_key) and api_key not in (
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
        self._apply_thinking_options(payload)
        headers = {
            "Authorization": f"Bearer {self._api_key()}",
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

    async def chat_text_stream(
        self,
        system_prompt: str,
        user_prompt: str,
        temperature: float = 0.2,
    ) -> AsyncIterator[str]:
        if not self.is_configured():
            yield "LLM 未配置 MODEL_API_KEY / AI_DASHSCOPE_API_KEY，当前返回本地占位文本。"
            return

        url = self.settings.model_base_url.rstrip("/") + "/chat/completions"
        payload = {
            "model": self.settings.model_name,
            "temperature": temperature,
            "stream": True,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
        }
        self._apply_thinking_options(payload)
        headers = {
            "Authorization": f"Bearer {self._api_key()}",
            "Content-Type": "application/json",
        }

        try:
            async with httpx.AsyncClient(timeout=self.settings.model_timeout, trust_env=False) as client:
                async with client.stream("POST", url, headers=headers, json=payload) as response:
                    if response.status_code >= 400:
                        body = await response.aread()
                        raise ApiException(
                            code=500,
                            msg=f"模型服务 HTTP 异常：{response.status_code} {body.decode('utf-8', errors='ignore')}",
                            http_status_code=500,
                        )

                    async for line in response.aiter_lines():
                        events = self._parse_stream_line_events(line)
                        if events is None:
                            continue
                        if events == "[DONE]":
                            break
                        for event in events:
                            if event["type"] == "content":
                                yield event["text"]
        except httpx.RequestError as exc:
            raise ApiException(
                code=500,
                msg=f"请求模型服务失败：{exc.__class__.__name__}: {exc}",
                http_status_code=500,
            ) from exc

    async def chat_text_stream_events(
        self,
        system_prompt: str,
        user_prompt: str,
        temperature: float = 0.2,
    ) -> AsyncIterator[dict[str, str]]:
        if not self.is_configured():
            yield {"type": "content", "text": "LLM 未配置 MODEL_API_KEY / AI_DASHSCOPE_API_KEY，当前返回本地占位文本。"}
            return

        url = self.settings.model_base_url.rstrip("/") + "/chat/completions"
        payload = {
            "model": self.settings.model_name,
            "temperature": temperature,
            "stream": True,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
        }
        self._apply_thinking_options(payload)
        headers = {
            "Authorization": f"Bearer {self._api_key()}",
            "Content-Type": "application/json",
        }

        try:
            async with httpx.AsyncClient(timeout=self.settings.model_timeout, trust_env=False) as client:
                async with client.stream("POST", url, headers=headers, json=payload) as response:
                    if response.status_code >= 400:
                        body = await response.aread()
                        raise ApiException(
                            code=500,
                            msg=f"模型服务 HTTP 异常：{response.status_code} {body.decode('utf-8', errors='ignore')}",
                            http_status_code=500,
                        )

                    async for line in response.aiter_lines():
                        events = self._parse_stream_line_events(line)
                        if events is None:
                            continue
                        if events == "[DONE]":
                            break
                        for event in events:
                            yield event
        except httpx.RequestError as exc:
            raise ApiException(
                code=500,
                msg=f"请求模型服务失败：{exc.__class__.__name__}: {exc}",
                http_status_code=500,
            ) from exc

    def _parse_stream_line_events(self, line: str) -> list[dict[str, str]] | str | None:
        value = (line or "").strip()
        if not value or value.startswith(":"):
            return None
        if value.startswith("data:"):
            value = value[5:].strip()
        if value == "[DONE]":
            return "[DONE]"

        try:
            body = json.loads(value)
        except json.JSONDecodeError:
            return None

        choices = body.get("choices") or []
        if not choices:
            return None

        choice = choices[0] or {}
        delta = choice.get("delta") or {}
        events = self._message_stream_events(delta)

        if not events:
            message = choice.get("message") or {}
            events = self._message_stream_events(message)

        return events or None

    def _message_stream_events(self, message: dict[str, Any]) -> list[dict[str, str]]:
        events: list[dict[str, str]] = []
        reasoning_text = self._content_to_text(message.get("reasoning_content"))
        content_text = self._content_to_text(message.get("content"))

        if reasoning_text:
            events.append({"type": "reasoning", "text": reasoning_text})
        if content_text:
            events.append({"type": "content", "text": content_text})

        return events

    def _content_to_text(self, content: Any) -> str | None:
        if content is None:
            return None
        if isinstance(content, str):
            return content
        if isinstance(content, list):
            parts = []
            for item in content:
                if isinstance(item, str):
                    parts.append(item)
                elif isinstance(item, dict):
                    text = item.get("text") or item.get("content")
                    if text:
                        parts.append(str(text))
            return "".join(parts) if parts else None
        return str(content)

    def _apply_thinking_options(self, payload: dict[str, Any]) -> None:
        if not self.settings.model_enable_thinking:
            return

        payload["enable_thinking"] = True
        reasoning_effort = (self.settings.model_reasoning_effort or "").strip()
        if reasoning_effort:
            payload["reasoning_effort"] = reasoning_effort

    def _api_key(self) -> str:
        return self.settings.model_api_key or self.settings.ai_dashscope_api_key
