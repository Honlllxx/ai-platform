"""Pipeline-related ORM models."""

from sqlalchemy import Column, BigInteger, String, Integer, Float, Text, DateTime, ForeignKey
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from app.database import Base


class Pipeline(Base):
    __tablename__ = "pipelines"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    document_id = Column(BigInteger, ForeignKey("documents.id", ondelete="CASCADE"), nullable=False)
    task_id = Column(String(36), nullable=False, unique=True)
    status = Column(String(20), nullable=False, default="pending")
    current_step = Column(String(50))
    total_steps = Column(Integer, nullable=False, default=6)
    completed_steps = Column(Integer, nullable=False, default=0)
    duration = Column(Float)
    state_graph = Column(JSONB)
    error_message = Column(Text)
    started_at = Column(DateTime(timezone=True))
    finished_at = Column(DateTime(timezone=True))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now())


class ExtractionResult(Base):
    __tablename__ = "extraction_results"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    pipeline_id = Column(BigInteger, ForeignKey("pipelines.id", ondelete="CASCADE"), nullable=False)
    field_name = Column(String(100), nullable=False)
    field_code = Column(String(100), nullable=False)
    extracted_value = Column(Text)
    confidence = Column(Float, nullable=False, default=0)
    status = Column(String(20), nullable=False, default="uncertain")
    page_ref = Column(Integer)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class ValidationResult(Base):
    __tablename__ = "validation_results"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    pipeline_id = Column(BigInteger, ForeignKey("pipelines.id", ondelete="CASCADE"), nullable=False)
    rule = Column(String(200), nullable=False)
    field_name = Column(String(100))
    severity = Column(String(20), nullable=False, default="info")
    message = Column(Text, nullable=False)
    suggestion = Column(Text)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class ReflectionResult(Base):
    __tablename__ = "reflection_results"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    pipeline_id = Column(BigInteger, ForeignKey("pipelines.id", ondelete="CASCADE"), nullable=False)
    field_name = Column(String(100), nullable=False)
    before_value = Column(Text)
    after_value = Column(Text)
    reason = Column(Text)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
