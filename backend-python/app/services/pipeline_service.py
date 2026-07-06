"""Pipeline orchestrator — chains preprocessing, OCR, layout, table, and LLM extraction."""

import json
import time
import uuid
import asyncio

import numpy as np
import cv2
from PIL import Image

from app.services.preprocess_service import preprocess_service
from app.services.ocr_service import ocr_service
from app.services.layout_service import layout_service
from app.services.table_service import table_service
from app.services.llm_service import llm_service


class PipelineService:
    """Orchestrates the full AI document processing pipeline."""

    async def run_full_pipeline(
        self,
        image: np.ndarray,
        metadata_fields: list[dict] | None = None,
        progress_callback: callable = None,
    ) -> dict:
        """Run the complete pipeline: preprocess → layout → OCR → table → LLM extract.

        Args:
            image: BGR image as numpy array
            metadata_fields: list of field definitions for extraction
            progress_callback: async callable(step, progress_pct, message)
        """
        task_id = str(uuid.uuid4())
        started_at = time.time()
        timeline = []

        try:
            # Stage 1: Preprocess
            await self._report(progress_callback, "preprocess", 5, "开始预处理...")
            processed, preprocess_meta = await asyncio.to_thread(
                preprocess_service.process, image
            )
            timeline.append({"stage": "preprocess", "status": "done", "meta": preprocess_meta})
            await self._report(progress_callback, "preprocess", 20, "预处理完成")

            # Stage 2: Layout analysis
            await self._report(progress_callback, "layout", 25, "版面分析中...")
            regions = await asyncio.to_thread(layout_service.analyze, processed)
            timeline.append({"stage": "layout", "status": "done", "regions": len(regions)})
            await self._report(progress_callback, "layout", 45, f"版面分析完成，检测到 {len(regions)} 个区域")

            # Stage 3: OCR
            await self._report(progress_callback, "ocr", 50, "OCR 识别中...")
            ocr_entries = await asyncio.to_thread(ocr_service.recognize, processed)
            total_chars = sum(len(e["text"]) for e in ocr_entries)
            avg_conf = (
                sum(e["confidence"] for e in ocr_entries) / len(ocr_entries)
                if ocr_entries else 0
            )
            timeline.append({
                "stage": "ocr",
                "status": "done",
                "entries": len(ocr_entries),
                "total_chars": total_chars,
                "avg_confidence": round(avg_conf, 4),
            })
            await self._report(progress_callback, "ocr", 70, f"OCR 完成，识别到 {len(ocr_entries)} 条文本")

            # Stage 4: Table extraction
            await self._report(progress_callback, "table", 75, "表格提取中...")
            tables = await asyncio.to_thread(table_service.extract_tables, processed)
            timeline.append({"stage": "table", "status": "done", "tables": len(tables)})
            await self._report(progress_callback, "table", 85, f"表格提取完成，检测到 {len(tables)} 个表格")

            # Stage 5: LLM Field Extraction
            await self._report(progress_callback, "extract", 90, "LLM 字段提取中...")
            extraction = await self._extract_fields(ocr_entries, metadata_fields)
            timeline.append({"stage": "extract", "status": "done", "fields": len(extraction)})
            await self._report(progress_callback, "extract", 100, "全流程完成")

            duration = round(time.time() - started_at, 2)

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
                "timeline": timeline,
            }

        except Exception as e:
            duration = round(time.time() - started_at, 2)
            timeline.append({"stage": "error", "status": "failed", "error": str(e)})
            return {
                "task_id": task_id,
                "status": "failed",
                "duration": duration,
                "error": str(e),
                "timeline": timeline,
            }

    async def _extract_fields(
        self, ocr_entries: list[dict], metadata_fields: list[dict] | None
    ) -> list[dict]:
        """Use LLM to extract business fields from OCR text."""
        if not metadata_fields:
            return []

        ocr_text = "\n".join(e["text"] for e in ocr_entries)

        field_definitions = "\n".join(
            f"- {f.get('code')}: {f.get('name')} (类型: {f.get('type', 'string')})"
            for f in metadata_fields[:20]  # limit for context
        )

        system_prompt = (
            "你是一个商业单据信息提取助手。从OCR识别的文本中提取指定的业务字段。\n\n"
            "要提取的字段定义：\n"
            f"{field_definitions}\n\n"
            "请以JSON格式返回提取结果，每个字段包含：field_code, value, confidence(0-1)。"
            "对于无法确定的字段，confidence设为0，value留空。"
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
                        if k not in ("_comment", "_note", "status")
                    ]
            else:
                extraction_list = result

            return extraction_list

        except Exception as e:
            return [{"error": str(e)}]

    async def _report(
        self, callback: callable, step: str, progress: int, message: str
    ):
        if callback:
            await callback(step, progress, message)


# Singleton
pipeline_service = PipelineService()
