"""Reflection service — re-examines low-confidence extractions and suggests corrections.

After the extraction and validation stages, fields with confidence below a threshold
are sent back to the LLM with surrounding OCR context for a second pass.  The before/after
values and reasoning are persisted to ``reflection_results``.
"""

import json
from sqlalchemy.ext.asyncio import AsyncSession

from app.services.llm_service import llm_service
from app.services.prompt_service import build_reflection_prompt
from app.models.pipeline import ReflectionResult


# Confidence threshold below which a field is sent to reflection
DEFAULT_REFLECTION_THRESHOLD = 0.7


class ReflectionService:
    """Re-examines low-confidence extraction fields via LLM."""

    async def reflect(
        self,
        extraction_results: list[dict],
        ocr_text: str = "",
        threshold: float = DEFAULT_REFLECTION_THRESHOLD,
        pipeline_id: int | None = None,
        db: AsyncSession | None = None,
    ) -> list[dict]:
        """Reflect on low-confidence fields and attempt correction.

        Args:
            extraction_results: Extracted fields with ``confidence`` scores.
            ocr_text: Full OCR text for context.
            threshold: Confidence below which a field is re-examined.
            pipeline_id: Pipeline ID for DB persistence (optional).
            db: Async DB session (required if pipeline_id is given).

        Returns:
            List of reflection dicts: field_code, before_value, after_value, reason.
        """
        # Identify candidates
        candidates = [
            e for e in extraction_results
            if float(e.get("confidence", 0)) < threshold
        ]

        if not candidates:
            return []

        reflections: list[dict] = []

        for field in candidates:
            code = field.get("field_code", field.get("code", ""))
            name = field.get("field_name", field.get("name", code))
            value = field.get("value", "")
            conf = float(field.get("confidence", 0))

            # Build context window around this field in the OCR text
            context = self._build_context(ocr_text, value)

            prompt = build_reflection_prompt(
                field_code=code,
                field_name=name,
                current_value=str(value),
                confidence=conf,
                ocr_context=context,
            )

            try:
                result = await llm_service.extract_json(
                    system_prompt=prompt,
                    user_content=f"请重新审视字段 '{name}' 的提取结果。",
                    temperature=0.2,
                )

                reflections.append({
                    "field_code": code,
                    "field_name": name,
                    "before_value": str(result.get("before_value", value)),
                    "after_value": str(result.get("after_value", value)),
                    "reason": str(result.get("reason", "")),
                    "confidence_after": float(result.get("confidence_after", conf)),
                })

            except Exception as e:
                # On failure, keep original value
                reflections.append({
                    "field_code": code,
                    "field_name": name,
                    "before_value": str(value),
                    "after_value": str(value),
                    "reason": f"Reflection call failed: {e}",
                    "confidence_after": conf,
                })

        # Persist to DB
        if pipeline_id is not None and db is not None and reflections:
            await self._persist(db, pipeline_id, reflections)

        return reflections

    # ==================== helpers ====================

    def _build_context(self, ocr_text: str, target_value: str, window: int = 500) -> str:
        """Extract a window of OCR text around the target value for LLM context.

        Args:
            ocr_text: Full OCR text.
            target_value: The field value to find.
            window: Number of characters on each side.

        Returns:
            A context window string.
        """
        if not ocr_text or not target_value:
            return ocr_text[:window * 2] if ocr_text else "（无上下文）"

        pos = ocr_text.find(str(target_value))
        if pos == -1:
            # Value not literally in OCR — return beginning of text
            return ocr_text[:window * 2]

        start = max(0, pos - window)
        end = min(len(ocr_text), pos + len(str(target_value)) + window)
        return ocr_text[start:end]

    async def _persist(
        self, db: AsyncSession, pipeline_id: int, reflections: list[dict]
    ):
        """Write reflection results to the ``reflection_results`` table."""
        for r in reflections:
            record = ReflectionResult(
                pipeline_id=pipeline_id,
                field_name=str(r.get("field_name", r.get("field_code", ""))),
                before_value=str(r.get("before_value", "")),
                after_value=str(r.get("after_value", "")),
                reason=str(r.get("reason", "")),
            )
            db.add(record)
        await db.commit()

    def apply_corrections(
        self, extraction_results: list[dict], reflections: list[dict]
    ) -> list[dict]:
        """Apply reflection corrections back to the extraction results.

        Returns a *new* list with corrected values and confidences.
        """
        # Build lookup
        corrections: dict[str, dict] = {}
        for r in reflections:
            code = r.get("field_code", "")
            if r.get("after_value") != r.get("before_value"):
                corrections[code] = r

        updated = []
        for field in extraction_results:
            code = field.get("field_code", field.get("code", ""))
            if code in corrections:
                corr = corrections[code]
                updated.append({
                    **field,
                    "value": corr["after_value"],
                    "confidence": corr.get("confidence_after", field.get("confidence", 0)),
                    "reflected": True,
                })
            else:
                updated.append({**field, "reflected": False})

        return updated


# Singleton
reflection_service = ReflectionService()
