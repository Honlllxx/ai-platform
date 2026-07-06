"""Document-related ORM models."""

from sqlalchemy import Column, BigInteger, String, Integer, Float, Text, DateTime, ForeignKey
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
    document_id = Column(BigInteger, ForeignKey("documents.id", ondelete="CASCADE"), nullable=False, unique=True)
    image_url = Column(String(1000))
    regions = Column(Text, nullable=False, default="[]")
    confidence = Column(Float)
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class DocumentOcr(Base):
    __tablename__ = "document_ocr"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    document_id = Column(BigInteger, ForeignKey("documents.id", ondelete="CASCADE"), nullable=False, unique=True)
    total_chars = Column(Integer)
    avg_confidence = Column(Float)
    language = Column(String(20), default="ch")
    entries = Column(Text, nullable=False, default="[]")
    created_at = Column(DateTime(timezone=True), server_default=func.now())


class DocumentTable(Base):
    __tablename__ = "document_tables"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    document_id = Column(BigInteger, ForeignKey("documents.id", ondelete="CASCADE"), nullable=False, unique=True)
    confidence = Column(Float)
    original_table = Column(Text, nullable=False, default="{}")
    extracted_table = Column(Text, nullable=False, default="{}")
    created_at = Column(DateTime(timezone=True), server_default=func.now())
