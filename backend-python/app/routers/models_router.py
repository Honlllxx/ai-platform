"""Model asset management routes (Stub in Phase 2)."""

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.sql import text

from app.database import get_db
from app.schemas.common import ApiResponse

router = APIRouter()

ASSETS_TABLE = "model_assets"
BINDINGS_TABLE = "model_bindings"


# ── Model Assets ──────────────────────────────────────────

@router.get("")
async def list_assets(
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=10, ge=1, le=100),
    type: str = Query(default=None),
    status: str = Query(default=None),
    db: AsyncSession = Depends(get_db),
):
    """List model assets."""
    conditions = []
    params = {}
    if type:
        conditions.append("type = :type")
        params["type"] = type
    if status:
        conditions.append("status = :status")
        params["status"] = status

    where_clause = ""
    if conditions:
        where_clause = "WHERE " + " AND ".join(conditions) + " AND deleted_at IS NULL"
    else:
        where_clause = "WHERE deleted_at IS NULL"

    count_sql = f"SELECT COUNT(*) FROM {ASSETS_TABLE} {where_clause}"
    total = (await db.execute(text(count_sql), params)).scalar() or 0

    offset = (page - 1) * page_size
    query_sql = (
        f"SELECT * FROM {ASSETS_TABLE} {where_clause} "
        f"ORDER BY created_at DESC LIMIT :limit OFFSET :offset"
    )
    params["limit"] = page_size
    params["offset"] = offset
    result = await db.execute(text(query_sql), params)
    rows = result.fetchall()

    records = []
    for row in rows:
        records.append(_asset_to_dict(row))

    return ApiResponse.success({
        "records": records,
        "total": total,
        "page": page,
        "page_size": page_size,
    })


@router.get("/{asset_id}")
async def get_asset(asset_id: int, db: AsyncSession = Depends(get_db)):
    """Get model asset detail."""
    result = await db.execute(
        text(f"SELECT * FROM {ASSETS_TABLE} WHERE id = :id"),
        {"id": asset_id},
    )
    row = result.fetchone()
    if not row:
        return ApiResponse.error(404, "模型资产不存在")
    return ApiResponse.success(_asset_to_dict(row))


@router.post("")
async def create_asset(body: dict, db: AsyncSession = Depends(get_db)):
    """Register a new model asset."""
    result = await db.execute(
        text(
            f"INSERT INTO {ASSETS_TABLE} (name, version, type, description, size, "
            f"file_path, framework, status) "
            f"VALUES (:name, :version, :type, :desc, :size, :path, :framework, 'inactive') RETURNING id"
        ),
        {
            "name": body.get("name", ""),
            "version": body.get("version", "v1.0"),
            "type": body.get("type", "base"),
            "desc": body.get("description", ""),
            "size": body.get("size", ""),
            "path": body.get("filePath", ""),
            "framework": body.get("framework", ""),
        },
    )
    await db.commit()
    new_id = result.scalar()
    return ApiResponse.success({"id": new_id}, "创建成功")


@router.put("/{asset_id}")
async def update_asset(asset_id: int, body: dict, db: AsyncSession = Depends(get_db)):
    """Update model asset metadata."""
    updates = []
    params = {"id": asset_id}
    for key, col in [
        ("name", "name"), ("version", "version"), ("type", "type"),
        ("description", "description"), ("size", "size"),
        ("filePath", "file_path"), ("framework", "framework"), ("status", "status"),
        ("metrics", "metrics"),
    ]:
        if key in body:
            updates.append(f"{col} = :{key}")
            params[key] = body[key]

    if not updates:
        return ApiResponse.error(400, "无更新字段")

    params["now"] = func.now()
    updates.append("updated_at = :now")

    await db.execute(
        text(f"UPDATE {ASSETS_TABLE} SET {', '.join(updates)} WHERE id = :id"),
        params,
    )
    await db.commit()
    return ApiResponse.success(None, "更新成功")


@router.delete("/{asset_id}")
async def delete_asset(asset_id: int, db: AsyncSession = Depends(get_db)):
    """Soft-delete a model asset."""
    await db.execute(
        text(f"UPDATE {ASSETS_TABLE} SET deleted_at = NOW() WHERE id = :id"),
        {"id": asset_id},
    )
    await db.commit()
    return ApiResponse.success(None, "删除成功")


# ── Model Bindings ────────────────────────────────────────

@router.get("/bindings/list")
async def list_bindings(db: AsyncSession = Depends(get_db)):
    """List all model bindings."""
    result = await db.execute(
        text(f"SELECT * FROM {BINDINGS_TABLE} WHERE deleted_at IS NULL ORDER BY created_at DESC")
    )
    rows = result.fetchall()
    records = [_binding_to_dict(r) for r in rows]
    return ApiResponse.success(records)


@router.post("/bindings")
async def create_binding(body: dict, db: AsyncSession = Depends(get_db)):
    """Create a model binding."""
    result = await db.execute(
        text(
            f"INSERT INTO {BINDINGS_TABLE} (binding_name, base_model_id, "
            f"lora_adapter_id, dataset_id, status) "
            f"VALUES (:name, :base_id, :lora_id, :dataset_id, 'inactive') RETURNING id"
        ),
        {
            "name": body.get("bindingName", ""),
            "base_id": body.get("baseModelId"),
            "lora_id": body.get("loraAdapterId"),
            "dataset_id": body.get("datasetId"),
        },
    )
    await db.commit()
    return ApiResponse.success({"id": result.scalar()}, "创建成功")


def _asset_to_dict(row) -> dict:
    return {
        "id": row.id,
        "name": row.name,
        "version": row.version,
        "type": row.type,
        "description": row.description,
        "size": row.size,
        "filePath": row.file_path,
        "framework": row.framework,
        "status": row.status,
        "metrics": row.metrics,
        "createTime": row.created_at.isoformat() if row.created_at else None,
        "updateTime": row.updated_at.isoformat() if row.updated_at else None,
    }


def _binding_to_dict(row) -> dict:
    return {
        "id": row.id,
        "bindingName": row.binding_name,
        "baseModelId": row.base_model_id,
        "loraAdapterId": row.lora_adapter_id,
        "datasetId": row.dataset_id,
        "status": row.status,
        "createTime": row.created_at.isoformat() if row.created_at else None,
    }
