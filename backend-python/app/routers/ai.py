"""AI processing endpoints — preprocess, OCR, layout, table, pipeline."""

import json
import asyncio
import logging
import time
from io import BytesIO

import cv2
import numpy as np
from fastapi import APIRouter, UploadFile, File, Form, HTTPException, Depends
from PIL import Image
from sqlalchemy.ext.asyncio import AsyncSession

from app.schemas.common import ApiResponse
from app.database import get_db
from app.services.preprocess_service import preprocess_service
from app.services.ocr_service import ocr_service
from app.services.layout_service import layout_service
from app.services.table_service import table_service
from app.services.pipeline_service import pipeline_service
from app.services.graph_service import graph_service
from app.services.document_converter import document_converter

logger = logging.getLogger(__name__)
router = APIRouter()

# Map Java fileType values to converter types
JAVA_TYPE_MAP = {
    "image": "image",
    "pdf": "pdf",
    "word": "word",
    "doc": "word",
    "docx": "word",
}


async def read_pages_from_upload(
    file: UploadFile,
    file_type: str | None = None,
) -> tuple[list[np.ndarray], dict]:
    """Read an uploaded file into a list of BGR page images.

    Uses DocumentConverter to handle PDF, Word, and image formats.
    """
    contents = await file.read()
    if not contents:
        raise HTTPException(status_code=400, detail="文件为空")

    # Map Java file_type to converter type
    ft = JAVA_TYPE_MAP.get((file_type or "").lower(), "image")

    pages, meta = document_converter.convert(contents, ft, file.filename or "")
    if not pages:
        raise HTTPException(status_code=400, detail="无法解析文件：未能提取任何页面")

    return pages, meta


# ── Single-step endpoints (image-only for now, per-page) ──

@router.post("/preprocess")
async def ai_preprocess(
    file: UploadFile = File(...),
    file_type: str = Form(default="image", description="File type: image, pdf, word"),
):
    """Run image preprocessing (deskew + stamp removal). Supports all file types."""
    t0 = time.perf_counter()
    logger.info("POST /ai/preprocess  file=%s  size=%d  file_type=%s",
                file.filename, file.size or 0, file_type)

    try:
        pages, conv_meta = await read_pages_from_upload(file, file_type)

        # Process first page (or all pages and return aggregate)
        processed, meta = preprocess_service.process(pages[0])
        elapsed = (time.perf_counter() - t0) * 1000
        logger.info("preprocess done  shape=%s  deskew=%s  stamp=%s  pages=%d  elapsed=%.0fms",
                    processed.shape[:2],
                    meta.get("deskew", {}).get("applied"),
                    meta.get("stamp_removal", {}).get("applied"),
                    len(pages), elapsed)

        return ApiResponse.success({
            "shape": processed.shape[:2],
            "page_count": len(pages),
            "converter_used": conv_meta.get("converter_used", "unknown"),
            "metadata": meta,
        })
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("preprocess failed — unhandled error: %s", e)
        return ApiResponse.error(
            code=400,
            message=f"无法解析文件: {file.filename}，请确认文件格式正确",
            data={"shape": [0, 0], "metadata": {"error": str(e)}},
        )


@router.post("/ocr")
async def ai_ocr(
    file: UploadFile = File(...),
    file_type: str = Query(default="image", description="File type: image, pdf, word"),
):
    """Run OCR on an uploaded document. Supports all file types."""
    pages, _ = await read_pages_from_upload(file, file_type)

    # OCR the first page (single-page endpoint)
    entries = ocr_service.recognize(pages[0])

    total_chars = sum(len(e["text"]) for e in entries)
    avg_conf = (
        sum(e["confidence"] for e in entries) / len(entries)
        if entries else 0
    )

    return ApiResponse.success({
        "entries": entries,
        "total_chars": total_chars,
        "avg_confidence": round(avg_conf, 4),
        "page_count": len(pages),
    })


@router.post("/layout")
async def ai_layout(
    file: UploadFile = File(...),
    file_type: str = Query(default="image", description="File type: image, pdf, word"),
):
    """Run layout analysis on an uploaded document. Supports all file types."""
    pages, _ = await read_pages_from_upload(file, file_type)

    # Layout analysis on the first page
    regions = layout_service.analyze(pages[0])

    return ApiResponse.success({
        "regions": regions,
        "region_count": len(regions),
        "page_count": len(pages),
    })


@router.post("/table")
async def ai_table(
    file: UploadFile = File(...),
    file_type: str = Query(default="image", description="File type: image, pdf, word"),
):
    """Extract tables from an uploaded document. Supports all file types."""
    pages, _ = await read_pages_from_upload(file, file_type)

    # Table extraction on the first page
    tables = table_service.extract_tables(pages[0])

    return ApiResponse.success({
        "tables": tables,
        "table_count": len(tables),
        "page_count": len(pages),
    })


# ── Full pipeline endpoints (multi-page) ──

@router.post("/pipeline/run")
async def ai_pipeline_run(
    file: UploadFile = File(...),
    metadata_fields: str = Form(default="[]", description="JSON encoded list of field definitions"),
    document_id: int = Form(default=None, description="Document ID for DB persistence"),
    file_type: str = Form(default="image", description="File type: image, pdf, word"),
    db: AsyncSession = Depends(get_db),
):
    """Run the full AI processing pipeline on an uploaded document.

    Supports all file types via DocumentConverter — PDF pages and Word docs
    are converted to images before processing.
    """
    try:
        pages, conv_meta = await read_pages_from_upload(file, file_type)
    except HTTPException:
        raise
    except Exception as e:
        logger.exception("pipeline failed — cannot read uploaded file")
        return ApiResponse.error(
            code=400,
            message=f"无法解析文件: {file.filename}，请确认文件格式正确",
            data={"error": str(e)},
        )

    try:
        fields = json.loads(metadata_fields)
    except json.JSONDecodeError:
        fields = []

    try:
        result = await pipeline_service.run_full_pipeline(
            pages=pages,
            metadata_fields=fields,
            document_id=document_id,
            db=db,
            raw_text=conv_meta.get("raw_text", ""),
        )
    except Exception as e:
        logger.exception("pipeline failed — unhandled error in run_full_pipeline")
        return ApiResponse.error(
            code=500,
            message=f"处理失败: {e}",
            data={"error": str(e)},
        )

    # Attach conversion metadata
    result["converter_used"] = conv_meta.get("converter_used", "unknown")
    result.setdefault("page_count", len(pages))

    if result["status"] == "failed":
        return ApiResponse.error(code=500, message=result.get("error", "Pipeline failed"), data=result)

    return ApiResponse.success(result)


@router.post("/pipeline/graph")
async def ai_pipeline_graph(
    file: UploadFile = File(...),
    metadata_fields: str = Form(default="[]", description="JSON encoded list of field definitions"),
    document_id: int = Form(default=None, description="Document ID for DB persistence"),
    file_type: str = Form(default="image", description="File type: image, pdf, word"),
    db: AsyncSession = Depends(get_db),
):
    """Run the AI pipeline using LangGraph state graph (with validate↔reflect loop).

    Supports all file types via DocumentConverter.
    """
    pages, conv_meta = await read_pages_from_upload(file, file_type)

    try:
        fields = json.loads(metadata_fields)
    except json.JSONDecodeError:
        fields = []

    result = await graph_service.run(
        pages=pages,
        metadata_fields=fields,
        document_id=document_id,
        db=db,
        raw_text=conv_meta.get("raw_text", ""),
    )

    # Attach conversion metadata
    result["converter_used"] = conv_meta.get("converter_used", "unknown")
    result.setdefault("page_count", len(pages))

    if result["status"] == "failed":
        return ApiResponse.error(code=500, message=result.get("error", "Pipeline failed"), data=result)

    return ApiResponse.success(result)
