"""Business configuration ORM models."""

from sqlalchemy import Column, BigInteger, String, Integer, Boolean, Text, DateTime
from sqlalchemy.sql import func
from app.database import Base


class MetadataField(Base):
    __tablename__ = "metadata_fields"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    name = Column(String(100), nullable=False)
    code = Column(String(100), nullable=False, unique=True)
    type = Column(String(20), nullable=False, default="string")
    length = Column(Integer)
    enum_values = Column(String(500))
    validation_rule = Column(String(500))
    group_code = Column(String(50), nullable=False)
    group_name = Column(String(100), nullable=False)
    required = Column(Boolean, nullable=False, default=False)
    description = Column(String(500))
    sort = Column(Integer, nullable=False, default=0)
    status = Column(Integer, nullable=False, default=1)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now())
    deleted_at = Column(DateTime(timezone=True))


class Template(Base):
    __tablename__ = "templates"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    name = Column(String(200), nullable=False)
    document_type = Column(String(50), nullable=False)
    description = Column(String(500))
    regions = Column(Text, nullable=False, default="[]")
    status = Column(Integer, nullable=False, default=1)
    version = Column(Integer, nullable=False, default=1)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now())
    deleted_at = Column(DateTime(timezone=True))


class PromptVersion(Base):
    __tablename__ = "prompt_versions"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    name = Column(String(200), nullable=False)
    version = Column(String(20), nullable=False)
    document_type = Column(String(50), nullable=False)
    stage = Column(String(50), nullable=False)
    content = Column(Text, nullable=False)
    variables = Column(Text)
    author = Column(String(100))
    status = Column(String(20), nullable=False, default="draft")
    commit_message = Column(String(500))
    git_commit_hash = Column(String(64))
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), server_default=func.now())
