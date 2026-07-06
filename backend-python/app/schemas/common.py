"""Unified response schema — aligned with Java backend format."""

from typing import Any, Generic, TypeVar, Optional
from pydantic import BaseModel
from datetime import datetime


T = TypeVar("T")


class ApiResponse(BaseModel, Generic[T]):
    code: int = 200
    data: Optional[T] = None
    message: str = "success"
    timestamp: Optional[datetime] = None

    @staticmethod
    def success(data: Any = None, message: str = "success") -> dict:
        return {
            "code": 200,
            "data": data,
            "message": message,
        }

    @staticmethod
    def error(code: int = 500, message: str = "error", data: Any = None) -> dict:
        return {
            "code": code,
            "data": data,
            "message": message,
        }


class PageResult(BaseModel):
    records: list = []
    total: int = 0
    page: int = 1
    page_size: int = 10
