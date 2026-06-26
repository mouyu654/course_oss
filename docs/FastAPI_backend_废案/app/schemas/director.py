"""专业设计相关的 Pydantic 模型"""
from typing import Optional, List
from pydantic import BaseModel, Field

class RequirementCreate(BaseModel):
    code: str = Field(min_length=1, max_length=10)
    title: str = Field(min_length=1, max_length=100)
    description: Optional[str] = None

class RequirementResponse(BaseModel):
    id: str
    code: str
    title: str
    description: Optional[str] = None
    class Config:
        from_attributes = True

class IndicatorCreate(BaseModel):
    requirement_id: str
    code: str = Field(min_length=1, max_length=10)
    description: str = Field(min_length=1)

class IndicatorResponse(BaseModel):
    id: str
    requirement_id: str
    code: str
    description: str
    class Config:
        from_attributes = True

class RequirementWithIndicatorsResponse(RequirementResponse):
    indicators: List[IndicatorResponse] = Field(default_factory=list)

class MacroMatrixEntry(BaseModel):
    course_id: str
    indicator_id: str
    weight_W: float = Field(ge=0.0, le=1.0)

class MacroMatrixResponse(BaseModel):
    id: str
    course_id: str
    indicator_id: str
    weight_W: float
    class Config:
        from_attributes = True
