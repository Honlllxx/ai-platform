"""SiliconFlow LLM API client (OpenAI-compatible)."""

import httpx
from app.config import get_settings

settings = get_settings()


class LLMService:
    def __init__(self):
        self.api_key = settings.siliconflow_api_key
        self.base_url = settings.siliconflow_base_url.rstrip("/")
        self.model = settings.siliconflow_model
        self.timeout = settings.pipeline_llm_timeout

    async def chat(
        self,
        messages: list[dict],
        temperature: float = 0.3,
        max_tokens: int = 4096,
        response_format: dict | None = None,
    ) -> dict:
        """Send a chat completion request to the LLM."""
        headers = {
            "Authorization": f"Bearer {self.api_key}",
            "Content-Type": "application/json",
        }
        payload = {
            "model": self.model,
            "messages": messages,
            "temperature": temperature,
            "max_tokens": max_tokens,
        }
        if response_format:
            payload["response_format"] = response_format

        async with httpx.AsyncClient(timeout=self.timeout) as client:
            response = await client.post(
                f"{self.base_url}/chat/completions",
                headers=headers,
                json=payload,
            )
            response.raise_for_status()
            return response.json()

    async def extract_json(
        self, system_prompt: str, user_content: str, temperature: float = 0.1
    ) -> dict:
        """Extract structured JSON from text content."""
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_content},
        ]
        result = await self.chat(
            messages=messages,
            temperature=temperature,
            response_format={"type": "json_object"},
        )
        content = result["choices"][0]["message"]["content"]
        import json

        return json.loads(content)

    def get_content(self, response: dict) -> str:
        return response["choices"][0]["message"]["content"]

    def get_usage(self, response: dict) -> dict:
        return response.get("usage", {})


# Singleton
llm_service = LLMService()
