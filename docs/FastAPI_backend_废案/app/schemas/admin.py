"""全局配置相关的 Pydantic 模型"""
from typing import Optional
from pydantic import BaseModel

class GlobalConfigCreate(BaseModel):
    category: str
    key: str
    value: str
    description: Optional[str] = None

class GlobalConfigUpdate(BaseModel):
    value: Optional[str] = None
    description: Optional[str] = None

class GlobalConfigResponse(BaseModel):
    id: str
    category: str
    key: str
    value: str
    description: Optional[str] = None
    is_active: int

    class Config:
        from_attributes = True
