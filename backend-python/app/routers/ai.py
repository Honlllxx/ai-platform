"""AI processing endpoints — preprocess, OCR, layout, table, pipeline."""

import json
import asyncio
from io import BytesIO

import cv2
import numpy as np
from fastapi import APIRouter, UploadFile, File, Query, HTTPException, Depends
from PIL import Image
from sqlalchemy.ext.asyncio import AsyncSession

from app.schemas.common import ApiResponse
from app.database import get_db
from app.services.preprocess_service import preprocess_service
from app.services.ocr_service import ocr_service
from app.services.layout_service import layout_service
from app.services.table_service import table_service
from app.services.pipeline_service import pipeline_service

router = APIRouter()


async def read_image_from_upload(file: UploadFile) -> np.ndarray:
    """Read an uploaded file into a BGR numpy array."""
    contents = await file.read()
    if not contents:
        raise HTTPException(status_code=400, detail="文件为空")

    pil_image = Image.open(BytesIO(contents)).convert("RGB")
    return cv2.cvtColor(np.array(pil_image), cv2.COLOR_RGB2BGR)


@router.post("/preprocess")
async def ai_preprocess(file: UploadFile = File(...)):
    """Run image preprocessing (deskew + stamp removal)."""
    image = await read_image_from_upload(file)
    processed, meta = preprocess_service.process(image)

    return ApiResponse.success({
        "shape": processed.shape[:2],
        "metadata": meta,
    })


@router.post("/ocr")
async def ai_ocr(file: UploadFile = File(...)):
    """Run OCR on an uploaded document image."""
    image = await read_image_from_upload(file)
    entries = ocr_service.recognize(image)

    total_chars = sum(len(e["text"]) for e in entries)
    avg_conf = (
        sum(e["confidence"] for e in entries) / len(entries)
        if entries else 0
    )

    return ApiResponse.success({
        "entries": entries,
        "total_chars": total_chars,
        "avg_confidence": round(avg_conf, 4),
    })


@router.post("/layout")
async def ai_layout(file: UploadFile = File(...)):
    """Run layout analysis on an uploaded document image."""
    image = await read_image_from_upload(file)
    regions = layout_service.analyze(image)

    return ApiResponse.success({
        "regions": regions,
        "region_count": len(regions),
    })


@router.post("/table")
async def ai_table(file: UploadFile = File(...)):
    """Extract tables from an uploaded document image."""
    image = await read_image_from_upload(file)
    tables = table_service.extract_tables(image)

    return ApiResponse.success({
        "tables": tables,
        "table_count": len(tables),
    })


@router.post("/pipeline/run")
async def ai_pipeline_run(
    file: UploadFile = File(...),
    metadata_fields: str = Query(default="[]", description="JSON encoded list of field definitions"),
    document_id: int = Query(default=None, description="Document ID for DB persistence"),
    db: AsyncSession = Depends(get_db),
):
    """Run the full AI processing pipeline on an uploaded document."""
    image = await read_image_from_upload(file)

    try:
        fields = json.loads(metadata_fields)
    except json.JSONDecodeError:
        fields = []

    result = await pipeline_service.run_full_pipeline(
        image=image,
        metadata_fields=fields,
        document_id=document_id,
        db=db,
    )

    if result["status"] == "failed":
        return ApiResponse.error(code=500, message=result.get("error", "Pipeline failed"), data=result)

    return ApiResponse.success(result)
