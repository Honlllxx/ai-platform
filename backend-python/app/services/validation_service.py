"""Validation service — validates LLM-extracted fields against business rules.

Calls the LLM with the validation prompt template to check each field's value
for format correctness, logical consistency, and completeness.
"""

import json
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.services.llm_service import llm_service
from app.services.prompt_service import build_validation_prompt
from app.models.pipeline import ValidationResult, Pipeline


class ValidationService:
    """Validates extraction results using LLM reasoning and persists findings."""

    async def validate(
        self,
        extraction_results: list[dict],
        metadata_fields: list[dict],
        ocr_text: str = "",
        pipeline_id: int | None = None,
        db: AsyncSession | None = None,
    ) -> list[dict]:
        """Run validation on extracted fields.

        Args:
            extraction_results: List of extracted field dicts from the LLM.
            metadata_fields: Original field definitions (code, name, type, …).
            ocr_text: Raw OCR text for context (optional).
            pipeline_id: Pipeline ID for DB persistence (optional).
            db: Async DB session (required if pipeline_id is given).

        Returns:
            List of validation dicts with severity, rule, message, suggestion.
        """
        if not extraction_results:
            return []

        # Build fields schema string
        fields_schema = self._format_fields_schema(metadata_fields or extraction_results)

        # Serialise extraction results for the prompt
        extraction_json = json.dumps(extraction_results, ensure_ascii=False, indent=2)

        prompt = build_validation_prompt(
            fields_schema=fields_schema,
            extraction_results=extraction_json,
        )

        try:
            result = await llm_service.extract_json(
                system_prompt=prompt,
                user_content=f"OCR文本（供参考）：\n{ocr_text[:4000]}" if ocr_text else "请直接校验上述提取结果。",
                temperature=0.1,
            )

            # Normalise response
            validations = self._parse_response(result)

        except Exception as e:
            # Return a single error entry so the caller can handle gracefully
            validations = [{
                "field_code": "__all__",
                "field_name": "全部字段",
                "severity": "error",
                "rule": "llm_call_failed",
                "passed": False,
                "message": f"校验调用失败: {e}",
                "suggestion": "请重试或人工校验",
            }]

        # Persist to DB
        if pipeline_id is not None and db is not None and validations:
            await self._persist(db, pipeline_id, validations)

        return validations

    # ==================== helpers ====================

    def _format_fields_schema(self, metadata_fields: list[dict]) -> str:
        """Build a human-readable field listing for the prompt."""
        lines = []
        for f in metadata_fields:
            code = f.get("code", f.get("field_code", "?"))
            name = f.get("name", f.get("field_name", code))
            ftype = f.get("type", "string")
            required = "必填" if f.get("required") else "可选"
            lines.append(f"- {code} ({name}): {ftype}, {required}")
        return "\n".join(lines)

    def _parse_response(self, result: dict) -> list[dict]:
        """Normalise the LLM validation response into a uniform list."""
        if isinstance(result, list):
            return result

        # Common wrapper keys
        for key in ("validations", "results", "checks", "items"):
            if key in result and isinstance(result[key], list):
                return result[key]

        # If the result is a flat dict of field_code → validation info
        if all(isinstance(v, dict) for v in result.values()):
            return [
                {"field_code": k, **v}
                for k, v in result.items()
                if k not in ("overall_score", "summary", "_comment")
            ]

        return [result] if result else []

    async def _persist(
        self, db: AsyncSession, pipeline_id: int, validations: list[dict]
    ):
        """Write validation results to the ``validation_results`` table."""
        for v in validations:
            record = ValidationResult(
                pipeline_id=pipeline_id,
                rule=str(v.get("rule", v.get("rule_name", "unknown"))),
                field_name=str(v.get("field_name", v.get("field_code", ""))),
                severity=str(v.get("severity", "info")),
                message=str(v.get("message", "")),
                suggestion=str(v.get("suggestion", "")) if v.get("suggestion") else None,
            )
            db.add(record)
        await db.commit()


# Singleton
validation_service = ValidationService()
