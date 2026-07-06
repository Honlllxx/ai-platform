"""Prompt version management routes (Stub — DB CRUD only in Phase 2)."""

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession

from app.database import get_db
from app.models.business import PromptVersion
from app.schemas.common import ApiResponse, PageResult

router = APIRouter()


@router.get("/versions")
async def list_versions(
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=10, ge=1, le=100),
    document_type: str = Query(default=None),
    stage: str = Query(default=None),
    db: AsyncSession = Depends(get_db),
):
    """List prompt versions with optional filters."""
    conditions = []
    if document_type:
        conditions.append(PromptVersion.document_type == document_type)
    if stage:
        conditions.append(PromptVersion.stage == stage)

    # Count
    count_q = select(func.count()).select_from(PromptVersion)
    if conditions:
        count_q = count_q.where(*conditions)
    total = (await db.execute(count_q)).scalar() or 0

    # Query
    q = select(PromptVersion).order_by(PromptVersion.created_at.desc())
    if conditions:
        q = q.where(*conditions)
    q = q.offset((page - 1) * page_size).limit(page_size)
    result = await db.execute(q)
    rows = result.scalars().all()

    records = [_prompt_to_dict(r) for r in rows]
    return ApiResponse.success({
        "records": records,
        "total": total,
        "page": page,
        "page_size": page_size,
    })


@router.get("/versions/{version_id}")
async def get_version(version_id: int, db: AsyncSession = Depends(get_db)):
    """Get a single prompt version detail."""
    row = await db.get(PromptVersion, version_id)
    if not row:
        return ApiResponse.error(404, "版本不存在")
    return ApiResponse.success(_prompt_to_dict(row))


@router.post("/versions")
async def create_version(body: dict, db: AsyncSession = Depends(get_db)):
    """Create a new prompt version."""
    pv = PromptVersion(
        name=body.get("name", ""),
        version=body.get("version", "v1.0"),
        document_type=body.get("document_type", ""),
        stage=body.get("stage", "field_extraction"),
        content=body.get("content", ""),
        author=body.get("author", ""),
        status=body.get("status", "draft"),
        commit_message=body.get("commit_message", ""),
    )
    db.add(pv)
    await db.commit()
    await db.refresh(pv)
    return ApiResponse.success(_prompt_to_dict(pv))


@router.put("/versions/{version_id}")
async def update_version(version_id: int, body: dict, db: AsyncSession = Depends(get_db)):
    """Update a prompt version."""
    pv = await db.get(PromptVersion, version_id)
    if not pv:
        return ApiResponse.error(404, "版本不存在")
    for key in ("name", "version", "document_type", "stage", "content", "author", "status", "commit_message"):
        if key in body:
            setattr(pv, key, body[key])
    await db.commit()
    await db.refresh(pv)
    return ApiResponse.success(_prompt_to_dict(pv))


@router.delete("/versions/{version_id}")
async def delete_version(version_id: int, db: AsyncSession = Depends(get_db)):
    """Delete a prompt version."""
    pv = await db.get(PromptVersion, version_id)
    if not pv:
        return ApiResponse.error(404, "版本不存在")
    await db.delete(pv)
    await db.commit()
    return ApiResponse.success(None, "删除成功")


def _prompt_to_dict(pv: PromptVersion) -> dict:
    return {
        "id": pv.id,
        "name": pv.name,
        "version": pv.version,
        "documentType": pv.document_type,
        "stage": pv.stage,
        "content": pv.content,
        "variables": pv.variables,
        "author": pv.author,
        "status": pv.status,
        "commitMessage": pv.commit_message,
        "gitCommitHash": pv.git_commit_hash,
        "createTime": pv.created_at.isoformat() if pv.created_at else None,
        "updateTime": pv.updated_at.isoformat() if pv.updated_at else None,
    }
