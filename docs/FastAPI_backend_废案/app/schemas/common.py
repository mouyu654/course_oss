"""通用响应模型"""
from typing import Optional, Any, List
from pydantic import BaseModel

class ErrorResponse(BaseModel):
    code: int
    message: str
    detail: Optional[Any] = None

class MessageResponse(BaseModel):
    message: str
