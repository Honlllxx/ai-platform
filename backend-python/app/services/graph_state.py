"""LangGraph state definition for the AI pipeline.

Graph state is passed between nodes and updated at each step.
"""

from typing import TypedDict, Annotated, Any
import operator


class PipelineState(TypedDict, total=False):
    """Typed state shared across all LangGraph nodes.

    Keys not set in every node should have default-factory semantics;
    nodes use ``state.get(key, default)`` when reading.
    """

    # ── Inputs ──
    image: Any  # numpy ndarray (not serialisable, passed in-memory)
    metadata_fields: list[dict]
    document_id: int | None
    task_id: str

    # ── Preprocess ──
    processed_image: Any
    preprocess_meta: dict

    # ── Layout ──
    layout_regions: list[dict]
    layout_error: str | None

    # ── OCR ──
    ocr_entries: list[dict]
    ocr_text: str
    ocr_total_chars: int
    ocr_avg_confidence: float

    # ── Table ──
    tables: list[dict]
    table_error: str | None

    # ── Extraction ──
    extraction_results: list[dict]
    extraction_error: str | None

    # ── Validation ──
    validation_results: list[dict]
    validation_passed: int
    validation_failed: int

    # ── Reflection ──
    reflection_results: list[dict]
    reflection_corrected: int
    reflection_examined: int

    # ── Meta ──
    status: str  # "running" | "completed" | "failed"
    error_message: str | None
    current_step: str
    completed_steps: int
    progress_pct: int
    progress_message: str

    # ── Accumulators (list fields use operator.add reducer) ──
    timeline: Annotated[list[dict], operator.add]
