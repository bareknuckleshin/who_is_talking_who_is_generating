import asyncio

import httpx

from app.config import settings


class LLMClient:
    def __init__(self):
        self.base_url = settings.llm_base_url.rstrip("/")
        self.api_key = settings.llm_api_key
        self.timeout = settings.llm_timeout_secs

    async def chat(self, *, model: str, messages: list[dict], temperature: float, max_tokens: int) -> str:
        headers = {"Authorization": f"Bearer {self.api_key}", "Content-Type": "application/json"}
        payload = {
            "model": model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
        }
        retries = 2
        for attempt in range(retries + 1):
            try:
                async with httpx.AsyncClient(timeout=self.timeout) as client:
                    resp = await client.post(f"{self.base_url}/chat/completions", headers=headers, json=payload)
                if resp.status_code >= 500:
                    raise httpx.HTTPStatusError("server error", request=resp.request, response=resp)
                resp.raise_for_status()
                data = resp.json()
                return data["choices"][0]["message"]["content"]
            except (httpx.TimeoutException, httpx.TransportError, httpx.HTTPStatusError):
                if attempt == retries:
                    raise
                await asyncio.sleep(0.5 * (attempt + 1))
