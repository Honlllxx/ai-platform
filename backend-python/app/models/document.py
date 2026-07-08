"""Document-related ORM models."""

from sqlalchemy import Column, BigInteger, String, Integer, Float, Text, DateTime, ForeignKey
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.sql import func
from app.database import Base


class Document(Base):
    __tablename__ = "documents"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    file_name = Column(String(500), nullable=False)
    file_type = Column(String(20), nullable=False)
    file_size = Column(BigInteger, nullable=False)
    file_url = Column(String(1000), nullable=False)
    page_count = Column(Integer)
    upload_user_id = Column(BigInteger)
    status = Column(String(30), nullable=False, default="pending")
    error_message = Column(Text)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now())
    deleted_at = Column(DateTime(timezone=True))


class DocumentLayout(Base):
    __tablename__ = "document_layouts"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    document_id = Column(BigInteger, ForeignKey("documents.id", ondelete="CASCADE"), nullable=False)  # non-unique: allow re-processing
    image_url = Column(String(1000))
    regions = Column(JSONB, nullable=False, default=list)
    confidence = Column(Float)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class DocumentOcr(Base):
    __tablename__ = "document_ocr"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    document_id = Column(BigInteger, ForeignKey("documents.id", ondelete="CASCADE"), nullable=False)  # non-unique: allow re-processing
    total_chars = Column(Integer)
    avg_confidence = Column(Float)
    language = Column(String(20), default="ch")
    entries = Column(JSONB, nullable=False, default=list)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class DocumentTable(Base):
    __tablename__ = "document_tables"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    document_id = Column(BigInteger, ForeignKey("documents.id", ondelete="CASCADE"), nullable=False)  # non-unique: one doc may have many tables
    confidence = Column(Float)
    original_table = Column(JSONB, nullable=False, default=dict)
    extracted_table = Column(JSONB, nullable=False, default=dict)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
