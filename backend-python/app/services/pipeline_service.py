"""Pipeline orchestrator — chains preprocessing, OCR, layout, table, LLM extraction,
validation, and reflection into a complete document AI pipeline.
"""

import json
import time
import uuid
import asyncio
from datetime import datetime, timezone

import numpy as np
import cv2
from PIL import Image
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.services.preprocess_service import preprocess_service
from app.services.ocr_service import ocr_service
from app.services.layout_service import layout_service
from app.services.table_service import table_service
from app.services.llm_service import llm_service
from app.services.prompt_service import build_extraction_prompt
from app.services.validation_service import validation_service
from app.services.reflection_service import reflection_service
from app.models.pipeline import Pipeline, ExtractionResult
from app.models.document import DocumentLayout, DocumentOcr, DocumentTable

# Total pipeline steps: preprocess → layout → ocr → table → extract → validate → reflect
TOTAL_STEPS = 7


class PipelineService:
    """Orchestrates the full AI document processing pipeline."""

    async def run_full_pipeline(
        self,
        image: np.ndarray,
        metadata_fields: list[dict] | None = None,
        progress_callback: callable = None,
        document_id: int | None = None,
        db: AsyncSession | None = None,
    ) -> dict:
        """Run the complete pipeline.

        Stages (7): preprocess → layout → OCR → table → LLM extract → validate → reflect

        Args:
            image: BGR image as numpy array.
            metadata_fields: list of field definitions for extraction.
            progress_callback: async callable(step, progress_pct, message).
            document_id: optional document ID for DB persistence.
            db: optional async DB session for persisting results.
        """
        task_id = str(uuid.uuid4())
        started_at = time.time()
        timeline = []

        # ── Create pipeline record if DB available ──
        pipeline_id = None
        if db is not None and document_id is not None:
            pipeline_id = await self._create_pipeline(db, document_id, task_id)

        try:
            # ── Stage 1: Preprocess ──
            await self._report(progress_callback, "preprocess", 5, "开始预处理...")
            if pipeline_id:
                await self._update_pipeline(db, pipeline_id, "preprocess", 1)
            processed, preprocess_meta = await asyncio.to_thread(
                preprocess_service.process, image
            )
            timeline.append({"stage": "preprocess", "status": "done", "meta": preprocess_meta})
            await self._report(progress_callback, "preprocess", 15, "预处理完成")

            # ── Stage 2: Layout analysis ──
            await self._report(progress_callback, "layout", 20, "版面分析中...")
            if pipeline_id:
                await self._update_pipeline(db, pipeline_id, "layout", 2)
            try:
                regions = await asyncio.to_thread(layout_service.analyze, processed)
            except Exception as layout_err:
                regions = []
                timeline.append({"stage": "layout", "status": "skipped", "error": str(layout_err)})
                await self._report(progress_callback, "layout", 30, f"版面分析跳过: {layout_err}")
            else:
                timeline.append({"stage": "layout", "status": "done", "regions": len(regions)})
                await self._report(progress_callback, "layout", 30, f"版面分析完成，检测到 {len(regions)} 个区域")

            # Persist layout
            if pipeline_id and document_id and regions:
                await self._persist_layout(db, document_id, regions)

            # ── Stage 3: OCR ──
            await self._report(progress_callback, "ocr", 35, "OCR 识别中...")
            if pipeline_id:
                await self._update_pipeline(db, pipeline_id, "ocr", 3)
            ocr_entries = await asyncio.to_thread(ocr_service.recognize, processed)
            total_chars = sum(len(e["text"]) for e in ocr_entries)
            avg_conf = (
                sum(e["confidence"] for e in ocr_entries) / len(ocr_entries)
                if ocr_entries else 0
            )
            ocr_text = "\n".join(e["text"] for e in ocr_entries)
            timeline.append({
                "stage": "ocr",
                "status": "done",
                "entries": len(ocr_entries),
                "total_chars": total_chars,
                "avg_confidence": round(avg_conf, 4),
            })
            await self._report(progress_callback, "ocr", 50, f"OCR 完成，识别到 {len(ocr_entries)} 条文本")

            # Persist OCR
            if pipeline_id and document_id:
                await self._persist_ocr(db, document_id, ocr_entries, total_chars, round(avg_conf, 4))

            # ── Stage 4: Table extraction ──
            await self._report(progress_callback, "table", 55, "表格提取中...")
            if pipeline_id:
                await self._update_pipeline(db, pipeline_id, "table", 4)
            try:
                tables = await asyncio.to_thread(table_service.extract_tables, processed)
            except Exception as table_err:
                tables = []
                timeline.append({"stage": "table", "status": "skipped", "error": str(table_err)})
                await self._report(progress_callback, "table", 60, f"表格提取跳过: {table_err}")
            else:
                timeline.append({"stage": "table", "status": "done", "tables": len(tables)})
                await self._report(progress_callback, "table", 60, f"表格提取完成，检测到 {len(tables)} 个表格")

            # Persist tables
            if pipeline_id and document_id and tables:
                await self._persist_tables(db, document_id, tables)

            # ── Stage 5: LLM Field Extraction ──
            await self._report(progress_callback, "extract", 65, "LLM 字段提取中...")
            if pipeline_id:
                await self._update_pipeline(db, pipeline_id, "extract", 5)
            extraction = await self._extract_fields(ocr_entries, metadata_fields)
            timeline.append({"stage": "extract", "status": "done", "fields": len(extraction)})
            await self._report(progress_callback, "extract", 75, f"LLM 提取完成，共 {len(extraction)} 个字段")

            # Persist extraction results
            if pipeline_id:
                await self._persist_extractions(db, pipeline_id, extraction)

            # ── Stage 6: Validation ──
            await self._report(progress_callback, "validate", 80, "字段校验中...")
            if pipeline_id:
                await self._update_pipeline(db, pipeline_id, "validate", 6)
            validations = await validation_service.validate(
                extraction_results=extraction,
                metadata_fields=metadata_fields or [],
                ocr_text=ocr_text,
                pipeline_id=pipeline_id,
                db=db,
            )
            passed = sum(1 for v in validations if v.get("passed", True))
            failed = len(validations) - passed
            timeline.append({
                "stage": "validate",
                "status": "done",
                "total": len(validations),
                "passed": passed,
                "failed": failed,
            })
            await self._report(
                progress_callback, "validate", 85,
                f"校验完成: {passed} 通过 / {failed} 未通过"
            )

            # ── Stage 7: Reflection ──
            await self._report(progress_callback, "reflect", 90, "低置信度字段反思中...")
            if pipeline_id:
                await self._update_pipeline(db, pipeline_id, "reflect", 7)
            reflections = await reflection_service.reflect(
                extraction_results=extraction,
                ocr_text=ocr_text,
                pipeline_id=pipeline_id,
                db=db,
            )
            # Apply corrections back to extraction results
            if reflections:
                extraction = reflection_service.apply_corrections(extraction, reflections)
                corrected = sum(1 for r in reflections if r.get("after_value") != r.get("before_value"))
                timeline.append({
                    "stage": "reflect",
                    "status": "done",
                    "examined": len(reflections),
                    "corrected": corrected,
                })
                await self._report(
                    progress_callback, "reflect", 95,
                    f"反思完成: 检查 {len(reflections)} 个字段，修正 {corrected} 个"
                )
            else:
                timeline.append({"stage": "reflect", "status": "done", "examined": 0, "corrected": 0})
                await self._report(progress_callback, "reflect", 95, "无需反思（所有字段置信度达标）")

            # ── Done ──
            await self._report(progress_callback, "done", 100, "全流程完成")
            duration = round(time.time() - started_at, 2)

            # Mark pipeline completed
            if pipeline_id:
                await self._complete_pipeline(db, pipeline_id, duration)

            return {
                "task_id": task_id,
                "status": "completed",
                "duration": duration,
                "preprocess": preprocess_meta,
                "layout": {"regions": regions},
                "ocr": {
                    "entries": ocr_entries,
                    "total_chars": total_chars,
                    "avg_confidence": round(avg_conf, 4),
                },
                "tables": tables,
                "extraction": extraction,
                "validations": validations,
                "reflections": reflections,
                "timeline": timeline,
            }

        except Exception as e:
            duration = round(time.time() - started_at, 2)
            timeline.append({"stage": "error", "status": "failed", "error": str(e)})

            # Mark pipeline failed
            if pipeline_id:
                await self._fail_pipeline(db, pipeline_id, str(e))

            return {
                "task_id": task_id,
                "status": "failed",
                "duration": duration,
                "error": str(e),
                "timeline": timeline,
            }

    # ==================== LLM extraction ====================

    async def _extract_fields(
        self, ocr_entries: list[dict], metadata_fields: list[dict] | None
    ) -> list[dict]:
        """Use LLM to extract business fields from OCR text.

        Now uses ``prompt_service.build_extraction_prompt`` for the system prompt
        instead of a hard-coded inline string.
        """
        if not metadata_fields:
            return []

        ocr_text = "\n".join(e["text"] for e in ocr_entries)

        # Build field schema block for the prompt template
        field_lines = []
        for f in metadata_fields[:30]:  # limit for context window
            code = f.get("code", f.get("field_code", ""))
            name = f.get("name", f.get("field_name", code))
            ftype = f.get("type", "string")
            field_lines.append(f"- {code}: {name} (类型: {ftype})")
        fields_schema = "\n".join(field_lines)

        system_prompt = build_extraction_prompt(
            fields_schema=fields_schema,
            ocr_text="{ocr_text}",  # placeholder — we pass OCR text as user_content
        )

        try:
            result = await llm_service.extract_json(
                system_prompt=system_prompt,
                user_content=f"OCR文本内容：\n{ocr_text[:8000]}",
                temperature=0.1,
            )

            # Normalize result to list format
            if isinstance(result, dict):
                if "fields" in result:
                    extraction_list = result["fields"]
                elif "extracted_fields" in result:
                    extraction_list = result["extracted_fields"]
                else:
                    # Key-value pairs directly
                    extraction_list = [
                        {"field_code": k, "value": str(v), "confidence": 0.8}
                        for k, v in result.items()
                        if k not in ("_comment", "_note", "summary", "document_type", "status")
                    ]
            elif isinstance(result, list):
                extraction_list = result
            else:
                extraction_list = []

            # Ensure every field has a confidence
            for fld in extraction_list:
                if "confidence" not in fld:
                    fld["confidence"] = 0.5

            return extraction_list

        except Exception as e:
            return [{"error": str(e)}]

    # ==================== Progress reporting ====================

    async def _report(
        self, callback: callable, step: str, progress: int, message: str
    ):
        if callback:
            await callback(step, progress, message)

    # ==================== DB Persistence Helpers ====================

    async def _create_pipeline(self, db: AsyncSession, document_id: int, task_id: str) -> int:
        """Create a pipeline record and return its ID."""
        pipeline = Pipeline(
            document_id=document_id,
            task_id=task_id,
            status="running",
            current_step="preprocess",
            total_steps=TOTAL_STEPS,
            completed_steps=0,
            started_at=datetime.now(timezone.utc),
        )
        db.add(pipeline)
        await db.commit()
        await db.refresh(pipeline)
        return pipeline.id

    async def _update_pipeline(self, db: AsyncSession, pipeline_id: int,
                                current_step: str, completed_steps: int):
        """Update pipeline step progress."""
        result = await db.execute(select(Pipeline).where(Pipeline.id == pipeline_id))
        pipeline = result.scalar_one_or_none()
        if pipeline:
            pipeline.current_step = current_step
            pipeline.completed_steps = completed_steps
            await db.commit()

    async def _complete_pipeline(self, db: AsyncSession, pipeline_id: int, duration: float):
        """Mark pipeline as completed."""
        result = await db.execute(select(Pipeline).where(Pipeline.id == pipeline_id))
        pipeline = result.scalar_one_or_none()
        if pipeline:
            pipeline.status = "completed"
            pipeline.completed_steps = TOTAL_STEPS
            pipeline.duration = duration
            pipeline.finished_at = datetime.now(timezone.utc)
            await db.commit()

    async def _fail_pipeline(self, db: AsyncSession, pipeline_id: int, error: str):
        """Mark pipeline as failed."""
        result = await db.execute(select(Pipeline).where(Pipeline.id == pipeline_id))
        pipeline = result.scalar_one_or_none()
        if pipeline:
            pipeline.status = "failed"
            pipeline.error_message = error
            pipeline.finished_at = datetime.now(timezone.utc)
            await db.commit()

    async def _persist_layout(self, db: AsyncSession, document_id: int, regions: list):
        """Save layout analysis results to DB."""
        layout = DocumentLayout(
            document_id=document_id,
            regions=json.dumps(regions, ensure_ascii=False),
            confidence=0.9,
        )
        db.add(layout)
        await db.commit()

    async def _persist_ocr(self, db: AsyncSession, document_id: int,
                            entries: list, total_chars: int, avg_confidence: float):
        """Save OCR results to DB."""
        ocr = DocumentOcr(
            document_id=document_id,
            total_chars=total_chars,
            avg_confidence=avg_confidence,
            language="ch",
            entries=json.dumps(entries, ensure_ascii=False),
        )
        db.add(ocr)
        await db.commit()

    async def _persist_tables(self, db: AsyncSession, document_id: int, tables: list):
        """Save table extraction results to DB."""
        for table in tables:
            dt = DocumentTable(
                document_id=document_id,
                confidence=table.get("bbox", {}).get("confidence"),
                original_table=json.dumps(table, ensure_ascii=False),
                extracted_table=json.dumps({
                    "headers": table.get("headers", []),
                    "rows": table.get("rows", []),
                    "html": table.get("html", ""),
                }, ensure_ascii=False),
            )
            db.add(dt)
        await db.commit()

    async def _persist_extractions(self, db: AsyncSession, pipeline_id: int,
                                     extractions: list):
        """Save field extraction results to DB."""
        for ext in extractions:
            result = ExtractionResult(
                pipeline_id=pipeline_id,
                field_code=str(ext.get("field_code", ext.get("code", ""))),
                field_name=str(ext.get("field_name", ext.get("field_code", ext.get("code", "")))),
                extracted_value=str(ext.get("value", "")),
                confidence=float(ext.get("confidence", 0)),
                status="extracted",
                page_ref=int(ext["page_ref"]) if ext.get("page_ref") and str(ext["page_ref"]).isdigit() else None,
            )
            db.add(result)
        await db.commit()


# Singleton
pipeline_service = PipelineService()
