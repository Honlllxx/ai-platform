"""LangGraph orchestration for the AI pipeline.

Replaces the linear PipelineService.run_full_pipeline() with a stateful
LangGraph StateGraph that can conditionally route between nodes:

    route ￫ extract ￫ validate ￫ reflect ─┐
                        │                   │
                        ▼                   ▼
                       END           (re-validate loop, max 2)
"""

import json
import time
import uuid
import asyncio
from datetime import datetime, timezone
from typing import Any, Optional

import numpy as np

from app.services.preprocess_service import preprocess_service
from app.services.ocr_service import ocr_service
from app.services.layout_service import layout_service
from app.services.table_service import table_service
from app.services.llm_service import llm_service
from app.services.prompt_service import build_extraction_prompt
from app.services.validation_service import validation_service
from app.services.reflection_service import reflection_service

# ============================================================
# State
# ============================================================

class PipelineGraphState(dict):
    """Mutable dict-based state shared across all graph nodes.

    Keys:
        pages, metadata_fields, document_id, task_id,
        processed_pages, preprocess_meta,
        layout_regions, layout_error,
        ocr_entries, ocr_text, ocr_total_chars, ocr_avg_confidence,
        tables, table_error,
        extraction_results, extraction_error,
        validation_results, validation_passed, validation_failed,
        reflection_results, reflection_corrected, reflection_examined,
        status, error_message, current_step, completed_steps,
        progress_pct, progress_message, timeline,
        db, pipeline_id, reflect_count,
    """

    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.setdefault("status", "running")
        self.setdefault("timeline", [])
        self.setdefault("current_step", "route")
        self.setdefault("completed_steps", 0)
        self.setdefault("progress_pct", 0)
        self.setdefault("reflect_count", 0)
        self.setdefault("extraction_results", [])
        self.setdefault("validation_results", [])
        self.setdefault("reflection_results", [])

# ============================================================
# Graph Builder
# ============================================================

class PipelineGraph:
    """Builds and runs the LangGraph pipeline.

    Usage::

        graph = PipelineGraph()
        result = await graph.run(image=img, metadata_fields=fields, db=db, document_id=1)
    """

    MAX_REFLECT_LOOPS = 2  # prevent infinite validate↔reflect cycles

    # ── Node implementations ──

    async def node_route(self, state: PipelineGraphState) -> PipelineGraphState:
        """Entry-point node: preprocess + layout + OCR + table extraction.

        All four stages run per-page before the LLM chain begins.
        """
        state["current_step"] = "preprocess"
        state["progress_pct"] = 5

        pages = state["pages"]
        page_count = len(pages)
        state["progress_message"] = f"开始预处理（共 {page_count} 页）..."
        await self._update_pipeline_db(state, "preprocess", 1)

        # Preprocess (per page)
        processed_pages: list[np.ndarray] = []
        preprocess_meta_list = []
        for page_idx, page_img in enumerate(pages):
            proc, meta = await asyncio.to_thread(
                preprocess_service.process, page_img
            )
            processed_pages.append(proc)
            meta["page"] = page_idx + 1
            preprocess_meta_list.append(meta)
        state["processed_pages"] = processed_pages
        state["preprocess_meta"] = preprocess_meta_list
        state["progress_pct"] = 15
        state["progress_message"] = f"预处理完成（{page_count} 页）"
        state["timeline"].append({"stage": "preprocess", "status": "done", "pages": page_count})
        await self._update_pipeline_db(state, "layout", 2)

        # Layout (per page)
        all_regions = []
        for page_idx, proc in enumerate(processed_pages):
            try:
                regions = await asyncio.to_thread(layout_service.analyze, proc)
            except Exception as e:
                state["timeline"].append({"stage": "layout", "status": "skipped",
                                          "page": page_idx + 1, "error": str(e)})
            else:
                for r in regions:
                    r["page"] = page_idx + 1
                all_regions.extend(regions)
        state["layout_regions"] = all_regions
        state["progress_pct"] = 30
        state["progress_message"] = f"版面分析完成，检测到 {len(all_regions)} 个区域"
        state["timeline"].append({"stage": "layout", "status": "done",
                                  "pages": page_count, "regions": len(all_regions)})
        await self._update_pipeline_db(state, "ocr", 3)

        # Persist layout
        await self._persist_layout(state)

        # OCR (per page, with page annotations)
        ocr_entries = []
        ocr_errors = 0
        for page_idx, proc in enumerate(processed_pages):
            try:
                entries = await asyncio.to_thread(ocr_service.recognize, proc)
            except Exception as ocr_err:
                ocr_errors += 1
                state["timeline"].append({"stage": "ocr", "status": "skipped",
                                          "page": page_idx + 1, "error": str(ocr_err)})
                entries = []
            for e in entries:
                e["page"] = page_idx + 1
            ocr_entries.extend(entries)

        # Fallback: use raw_text if PaddleOCR failed on all pages
        raw_text = state.get("raw_text", "")
        if ocr_errors == page_count and raw_text:
            import logging
            logging.getLogger(__name__).warning(
                "PaddleOCR failed on all %d pages; using raw_text fallback (%d chars)",
                page_count, len(raw_text),
            )
            ocr_entries = self._raw_text_to_ocr_entries(raw_text)
            state["timeline"].append({"stage": "ocr", "status": "fallback",
                                      "fallback_reason": "PaddleOCR unavailable (ONEDNN/PIR)",
                                      "raw_text_chars": len(raw_text)})

        state["ocr_entries"] = ocr_entries
        state["ocr_total_chars"] = sum(len(e["text"]) for e in ocr_entries)
        state["ocr_avg_confidence"] = (
            sum(e["confidence"] for e in ocr_entries) / len(ocr_entries)
            if ocr_entries else 0
        )
        state["ocr_text"] = self._build_page_ocr_text(ocr_entries)
        state["progress_pct"] = 50
        state["progress_message"] = f"OCR 完成，识别到 {len(ocr_entries)} 条文本（{page_count} 页）"
        state["timeline"].append({"stage": "ocr", "status": "done", "entries": len(ocr_entries), "pages": page_count})
        await self._update_pipeline_db(state, "table", 4)

        # Persist OCR
        await self._persist_ocr(state)

        # Table (per page)
        all_tables = []
        for page_idx, proc in enumerate(processed_pages):
            try:
                tables = await asyncio.to_thread(table_service.extract_tables, proc)
            except Exception as e:
                state["timeline"].append({"stage": "table", "status": "skipped",
                                          "page": page_idx + 1, "error": str(e)})
            else:
                for t in tables:
                    t["page"] = page_idx + 1
                all_tables.extend(tables)
        state["tables"] = all_tables
        state["progress_pct"] = 60
        state["progress_message"] = f"表格提取完成，检测到 {len(all_tables)} 个表格"
        state["timeline"].append({"stage": "table", "status": "done", "tables": len(all_tables)})
        await self._update_pipeline_db(state, "extract", 5)

        # Persist tables
        await self._persist_tables(state)

        state["current_step"] = "extract"
        return state

    async def node_extract(self, state: PipelineGraphState) -> PipelineGraphState:
        """LLM field extraction node."""
        state["current_step"] = "extract"
        state["progress_pct"] = 65
        state["progress_message"] = "LLM 字段提取中..."
        await self._update_pipeline_db(state, "extract", 5)

        ocr_entries = state.get("ocr_entries", [])
        metadata_fields = state.get("metadata_fields") or []

        extraction = await self._extract_fields(ocr_entries, metadata_fields)
        state["extraction_results"] = extraction
        state["progress_pct"] = 75
        state["progress_message"] = f"LLM 提取完成，共 {len(extraction)} 个字段"
        state["timeline"].append({"stage": "extract", "status": "done", "fields": len(extraction)})

        # Persist extraction results
        await self._persist_extractions(state)
        state["current_step"] = "validate"
        return state

    async def node_validate(self, state: PipelineGraphState) -> PipelineGraphState:
        """Validation node — checks extracted fields against rules."""
        state["current_step"] = "validate"
        state["progress_pct"] = 80
        state["progress_message"] = "字段校验中..."
        await self._update_pipeline_db(state, "validate", 6)

        extraction = state.get("extraction_results", [])
        metadata_fields = state.get("metadata_fields") or []
        ocr_text = state.get("ocr_text", "")
        pipeline_id = state.get("pipeline_id")
        db = state.get("db")

        validations = await validation_service.validate(
            extraction_results=extraction,
            metadata_fields=metadata_fields,
            ocr_text=ocr_text,
            pipeline_id=pipeline_id,
            db=db,
        )

        passed = sum(1 for v in validations if v.get("passed", True))
        failed = len(validations) - passed

        state["validation_results"] = validations
        state["validation_passed"] = passed
        state["validation_failed"] = failed
        state["progress_pct"] = 85
        state["progress_message"] = f"校验完成: {passed} 通过 / {failed} 未通过"
        state["timeline"].append({
            "stage": "validate", "status": "done",
            "total": len(validations), "passed": passed, "failed": failed,
        })
        state["current_step"] = "reflect"
        return state

    async def node_reflect(self, state: PipelineGraphState) -> PipelineGraphState:
        """Reflection node — re-examines low-confidence or failed fields."""
        state["current_step"] = "reflect"
        state["progress_pct"] = 90
        state["progress_message"] = "低置信度字段反思中..."
        await self._update_pipeline_db(state, "reflect", 7)

        extraction = state.get("extraction_results", [])
        ocr_text = state.get("ocr_text", "")
        pipeline_id = state.get("pipeline_id")
        db = state.get("db")

        reflections = await reflection_service.reflect(
            extraction_results=extraction,
            ocr_text=ocr_text,
            pipeline_id=pipeline_id,
            db=db,
        )

        state["reflection_results"] = reflections or []

        if reflections:
            # Apply corrections
            corrected = reflection_service.apply_corrections(extraction, reflections)
            state["extraction_results"] = corrected
            count = sum(1 for r in reflections if r.get("after_value") != r.get("before_value"))
            state["reflection_corrected"] = count
            state["reflection_examined"] = len(reflections)
            state["progress_pct"] = 95
            state["progress_message"] = f"反思完成: 检查 {len(reflections)} 个字段，修正 {count} 个"
            state["timeline"].append({
                "stage": "reflect", "status": "done",
                "examined": len(reflections), "corrected": count,
            })
        else:
            state["reflection_corrected"] = 0
            state["reflection_examined"] = 0
            state["progress_pct"] = 95
            state["progress_message"] = "无需反思（所有字段置信度达标）"
            state["timeline"].append({"stage": "reflect", "status": "done", "examined": 0, "corrected": 0})

        state["current_step"] = "done"
        state["reflect_count"] = state.get("reflect_count", 0) + 1
        return state

    async def node_done(self, state: PipelineGraphState) -> PipelineGraphState:
        """Terminal node — mark completion."""
        state["status"] = "completed"
        state["current_step"] = "done"
        state["completed_steps"] = 7
        state["progress_pct"] = 100
        state["progress_message"] = "全流程完成"
        await self._complete_pipeline_db(state)
        return state

    async def node_error(self, state: PipelineGraphState) -> PipelineGraphState:
        """Error handler node."""
        state["status"] = "failed"
        state["timeline"].append({"stage": "error", "status": "failed",
                                   "error": state.get("error_message", "unknown")})
        await self._fail_pipeline_db(state)
        return state

    # ── Routing conditions ──

    def should_reflect(self, state: PipelineGraphState) -> str:
        """Decide whether to enter the reflect node.

        Skips reflection if:
        - No fields to examine (no low-confidence ones)
        - Already reflected MAX_REFLECT_LOOPS times
        - Validation passed all fields
        """
        if state.get("reflect_count", 0) >= self.MAX_REFLECT_LOOPS:
            return "done"

        extraction = state.get("extraction_results", [])
        if not extraction:
            return "done"

        # Check if any field needs reflection
        low_conf = [f for f in extraction
                     if f.get("confidence", 1.0) < 0.7
                     or f.get("status") == "failed"]
        if not low_conf and state.get("validation_failed", 0) == 0:
            return "done"

        return "reflect"

    # ── Run ──

    async def run(
        self,
        pages: list[np.ndarray],
        metadata_fields: Optional[list[dict]] = None,
        document_id: Optional[int] = None,
        db=None,  # AsyncSession
        task_id: Optional[str] = None,
        raw_text: str = "",
    ) -> dict:
        """Execute the graph pipeline.

        Because LangGraph's real power is with conditional edges and state
        management, we model the flow as a deterministic run with transitions.
        """

        started_at = time.time()
        if not task_id:
            task_id = str(uuid.uuid4())

        # Create initial state
        state = PipelineGraphState(
            pages=pages,
            metadata_fields=metadata_fields or [],
            document_id=document_id,
            task_id=task_id,
            db=db,
            pipeline_id=None,
            raw_text=raw_text,
        )

        # Create pipeline record
        if db is not None and document_id is not None:
            state["pipeline_id"] = await self._create_pipeline(state)

        try:
            # ── Route (preprocess + layout + OCR + table) ──
            state = await self.node_route(state)

            # ── Extract ──
            state = await self.node_extract(state)

            # ── Validate ──
            state = await self.node_validate(state)

            # ── Reflect loop ──
            while self.should_reflect(state) == "reflect":
                state = await self.node_reflect(state)
                # Re-validate after reflection
                state = await self.node_validate(state)

            # ── Done ──
            state = await self.node_done(state)

        except Exception as e:
            state["error_message"] = str(e)
            state = await self.node_error(state)

        duration = round(time.time() - started_at, 2)

        return {
            "task_id": task_id,
            "status": state.get("status", "failed"),
            "duration": duration,
            "page_count": len(pages),
            "preprocess": state.get("preprocess_meta", {}),
            "layout": {"regions": state.get("layout_regions", [])},
            "ocr": {
                "entries": state.get("ocr_entries", []),
                "total_chars": state.get("ocr_total_chars", 0),
                "avg_confidence": state.get("ocr_avg_confidence", 0),
            },
            "tables": state.get("tables", []),
            "extraction": state.get("extraction_results", []),
            "validations": state.get("validation_results", []),
            "reflections": state.get("reflection_results", []),
            "timeline": state.get("timeline", []),
            "graph_flow": ["route", "extract", "validate"] +
                           (["reflect", "validate"] * state.get("reflect_count", 0)) +
                           ["done"],
        }

    # ═══════════════════════════════════════════════════════════
    # Private: LLM extraction
    # ═══════════════════════════════════════════════════════════

    async def _extract_fields(
        self, ocr_entries: list[dict], metadata_fields: list[dict] | None
    ) -> list[dict]:
        if not metadata_fields:
            return []

        ocr_text = self._build_page_ocr_text(ocr_entries)

        field_lines = []
        for f in metadata_fields[:30]:
            code = f.get("code", f.get("field_code", ""))
            name = f.get("name", f.get("field_name", code))
            ftype = f.get("type", "string")
            field_lines.append(f"- {code}: {name} (类型: {ftype})")
        fields_schema = "\n".join(field_lines)

        system_prompt = build_extraction_prompt(
            fields_schema=fields_schema,
            ocr_text="{ocr_text}",
        )

        try:
            result = await llm_service.extract_json(
                system_prompt=system_prompt,
                user_content=f"OCR文本内容：\n{ocr_text[:8000]}",
                temperature=0.1,
            )

            if isinstance(result, dict):
                if "fields" in result:
                    extraction_list = result["fields"]
                elif "extracted_fields" in result:
                    extraction_list = result["extracted_fields"]
                else:
                    extraction_list = [
                        {"field_code": k, "value": str(v), "confidence": 0.8}
                        for k, v in result.items()
                        if k not in ("_comment", "_note", "summary", "document_type", "status")
                    ]
            elif isinstance(result, list):
                extraction_list = result
            else:
                extraction_list = []

            for fld in extraction_list:
                if "confidence" not in fld:
                    fld["confidence"] = 0.5

            return extraction_list
        except Exception:
            return []

    @staticmethod
    def _build_page_ocr_text(ocr_entries: list[dict]) -> str:
        """Build OCR text with [Page N] markers for multi-page documents."""
        page_groups: dict[int, list[str]] = {}
        for e in ocr_entries:
            pg = e.get("page", 1)
            page_groups.setdefault(pg, []).append(e["text"])

        parts = []
        for pg in sorted(page_groups):
            parts.append(f"\n[Page {pg}]\n")
            parts.append("\n".join(page_groups[pg]))
        return "\n".join(parts)

    @staticmethod
    def _raw_text_to_ocr_entries(raw_text: str) -> list[dict]:
        """Convert raw text (e.g., from python-docx) into OCR entry dicts.

        Used as fallback when PaddleOCR is unavailable.
        """
        lines = raw_text.split("\n")
        entries = []
        for i, line in enumerate(lines):
            stripped = line.strip()
            if not stripped:
                continue
            entries.append({
                "id": len(entries),
                "text": stripped,
                "confidence": 0.7,
                "bbox": {"x": 0, "y": i * 28, "w": 800, "h": 28},
                "page": 1,
            })
        return entries

    # ═══════════════════════════════════════════════════════════
    # Private: DB persistence (delegates to SQLAlchemy)
    # ═══════════════════════════════════════════════════════════

    async def _create_pipeline(self, state: PipelineGraphState) -> int:
        from app.models.pipeline import Pipeline
        db = state.get("db")
        if db is None:
            return None
        pipeline = Pipeline(
            document_id=state.get("document_id"),
            task_id=state.get("task_id"),
            status="running",
            current_step="preprocess",
            total_steps=7,
            completed_steps=0,
            started_at=datetime.now(timezone.utc),
        )
        db.add(pipeline)
        await db.commit()
        await db.refresh(pipeline)
        return pipeline.id

    async def _update_pipeline_db(self, state: PipelineGraphState,
                                   current_step: str, completed_steps: int):
        from app.models.pipeline import Pipeline
        from sqlalchemy import select
        db = state.get("db")
        pipeline_id = state.get("pipeline_id")
        if db is None or pipeline_id is None:
            return
        result = await db.execute(select(Pipeline).where(Pipeline.id == pipeline_id))
        pipeline = result.scalar_one_or_none()
        if pipeline:
            pipeline.current_step = current_step
            pipeline.completed_steps = completed_steps
            await db.commit()

    async def _complete_pipeline_db(self, state: PipelineGraphState):
        from app.models.pipeline import Pipeline
        from sqlalchemy import select
        db = state.get("db")
        pipeline_id = state.get("pipeline_id")
        if db is None or pipeline_id is None:
            return
        result = await db.execute(select(Pipeline).where(Pipeline.id == pipeline_id))
        pipeline = result.scalar_one_or_none()
        if pipeline:
            pipeline.status = "completed"
            pipeline.completed_steps = 7
            pipeline.finished_at = datetime.now(timezone.utc)
            await db.commit()

    async def _fail_pipeline_db(self, state: PipelineGraphState):
        from app.models.pipeline import Pipeline
        from sqlalchemy import select
        db = state.get("db")
        pipeline_id = state.get("pipeline_id")
        if db is None or pipeline_id is None:
            return
        result = await db.execute(select(Pipeline).where(Pipeline.id == pipeline_id))
        pipeline = result.scalar_one_or_none()
        if pipeline:
            pipeline.status = "failed"
            pipeline.error_message = state.get("error_message")
            pipeline.finished_at = datetime.now(timezone.utc)
            await db.commit()

    async def _persist_layout(self, state: PipelineGraphState):
        from app.models.document import DocumentLayout
        db = state.get("db")
        document_id = state.get("document_id")
        regions = state.get("layout_regions")
        if db is None or document_id is None or not regions:
            return
        layout = DocumentLayout(
            document_id=document_id,
            regions=json.dumps(regions, ensure_ascii=False),
            confidence=0.9,
        )
        db.add(layout)
        await db.commit()

    async def _persist_ocr(self, state: PipelineGraphState):
        from app.models.document import DocumentOcr
        db = state.get("db")
        document_id = state.get("document_id")
        entries = state.get("ocr_entries")
        if db is None or document_id is None or not entries:
            return
        ocr = DocumentOcr(
            document_id=document_id,
            total_chars=state.get("ocr_total_chars", 0),
            avg_confidence=state.get("ocr_avg_confidence", 0),
            language="ch",
            entries=json.dumps(entries, ensure_ascii=False),
        )
        db.add(ocr)
        await db.commit()

    async def _persist_tables(self, state: PipelineGraphState):
        from app.models.document import DocumentTable
        db = state.get("db")
        document_id = state.get("document_id")
        tables = state.get("tables") or []
        if db is None or document_id is None or not tables:
            return
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

    async def _persist_extractions(self, state: PipelineGraphState):
        from app.models.pipeline import ExtractionResult
        db = state.get("db")
        pipeline_id = state.get("pipeline_id")
        extractions = state.get("extraction_results") or []
        if db is None or pipeline_id is None or not extractions:
            return
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


# ── Singleton ──
graph_service = PipelineGraph()
