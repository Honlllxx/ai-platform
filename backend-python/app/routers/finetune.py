"""Fine-tuning task management routes (Stub in Phase 2)."""

from fastapi import APIRouter, Depends, Query
from sqlalchemy import select, func
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.sql import text

from app.database import get_db
from app.schemas.common import ApiResponse

router = APIRouter()

FINETUNE_TABLE = "finetune_tasks"


@router.get("/tasks")
async def list_tasks(
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=10, ge=1, le=100),
    status: str = Query(default=None),
    db: AsyncSession = Depends(get_db),
):
    """List fine-tuning tasks."""
    where_clause = ""
    params = {}
    if status:
        where_clause = "WHERE status = :status"
        params["status"] = status

    count_sql = f"SELECT COUNT(*) FROM {FINETUNE_TABLE} {where_clause}"
    total = (await db.execute(text(count_sql), params)).scalar() or 0

    offset = (page - 1) * page_size
    query_sql = (
        f"SELECT * FROM {FINETUNE_TABLE} {where_clause} "
        f"ORDER BY created_at DESC LIMIT :limit OFFSET :offset"
    )
    params["limit"] = page_size
    params["offset"] = offset
    result = await db.execute(text(query_sql), params)
    rows = result.fetchall()

    records = []
    for row in rows:
        records.append({
            "id": row.id,
            "taskName": row.task_name,
            "baseModel": row.base_model,
            "datasetName": row.dataset_name,
            "datasetSize": row.dataset_size,
            "status": row.status,
            "progress": row.progress,
            "epochs": row.epochs,
            "currentEpoch": row.current_epoch,
            "learningRate": row.learning_rate,
            "loss": row.loss,
            "metrics": row.metrics,
            "outputModel": row.output_model,
            "errorMessage": row.error_message,
            "startedAt": row.started_at.isoformat() if row.started_at else None,
            "finishedAt": row.finished_at.isoformat() if row.finished_at else None,
            "duration": row.duration,
            "createTime": row.created_at.isoformat() if row.created_at else None,
        })

    return ApiResponse.success({
        "records": records,
        "total": total,
        "page": page,
        "page_size": page_size,
    })


@router.get("/tasks/{task_id}")
async def get_task(task_id: int, db: AsyncSession = Depends(get_db)):
    """Get fine-tuning task detail."""
    result = await db.execute(
        text(f"SELECT * FROM {FINETUNE_TABLE} WHERE id = :id"),
        {"id": task_id},
    )
    row = result.fetchone()
    if not row:
        return ApiResponse.error(404, "任务不存在")
    return ApiResponse.success({
        "id": row.id,
        "taskName": row.task_name,
        "baseModel": row.base_model,
        "datasetName": row.dataset_name,
        "datasetSize": row.dataset_size,
        "status": row.status,
        "progress": row.progress,
        "epochs": row.epochs,
        "currentEpoch": row.current_epoch,
        "learningRate": row.learning_rate,
        "loss": row.loss,
        "metrics": row.metrics,
        "outputModel": row.output_model,
        "errorMessage": row.error_message,
        "startedAt": row.started_at.isoformat() if row.started_at else None,
        "finishedAt": row.finished_at.isoformat() if row.finished_at else None,
        "duration": row.duration,
        "createTime": row.created_at.isoformat() if row.created_at else None,
    })


@router.post("/tasks")
async def create_task(body: dict, db: AsyncSession = Depends(get_db)):
    """Create a new fine-tuning task (stub — no actual training)."""
    result = await db.execute(
        text(
            f"INSERT INTO {FINETUNE_TABLE} (task_name, base_model, dataset_name, dataset_size, "
            f"status, epochs, learning_rate) "
            f"VALUES (:name, :base_model, :dataset, :size, 'pending', :epochs, :lr) RETURNING id"
        ),
        {
            "name": body.get("taskName", ""),
            "base_model": body.get("baseModel", "Qwen2-7B"),
            "dataset": body.get("datasetName", ""),
            "size": body.get("datasetSize", 0),
            "epochs": body.get("epochs", 3),
            "lr": body.get("learningRate", 2e-5),
        },
    )
    await db.commit()
    new_id = result.scalar()
    return ApiResponse.success({"id": new_id}, "创建成功")


@router.delete("/tasks/{task_id}")
async def delete_task(task_id: int, db: AsyncSession = Depends(get_db)):
    """Cancel/delete a fine-tuning task."""
    await db.execute(
        text(f"DELETE FROM {FINETUNE_TABLE} WHERE id = :id"),
        {"id": task_id},
    )
    await db.commit()
    return ApiResponse.success(None, "删除成功")
