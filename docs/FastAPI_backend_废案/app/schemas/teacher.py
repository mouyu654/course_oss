"""教师端相关的 Pydantic 模型"""
from typing import Optional, List
from pydantic import BaseModel, Field

class ObjectiveCreate(BaseModel):
    course_id: str
    code: str = Field(min_length=1, max_length=10)
    description: str
    dimension: str  # 知识维度 / 能力维度 / 价值维度

class ObjectiveResponse(BaseModel):
    id: str
    course_id: str
    code: str
    description: str
    dimension: str
    class Config:
        from_attributes = True

class InternalWeightEntry(BaseModel):
    objective_id: str
    indicator_id: str
    weight_w: float = Field(ge=0.0, le=1.0)

class InternalWeightResponse(BaseModel):
    objective_id: str
    indicator_id: str
    weight_w: float
    class Config:
        from_attributes = True

class AssessmentItemCreate(BaseModel):
    course_id: str
    objective_id: str
    name: str = Field(min_length=1, max_length=200)
    max_score: float = Field(gt=0)

class AssessmentItemResponse(BaseModel):
    id: str
    course_id: str
    objective_id: str
    name: str
    max_score: float
    class Config:
        from_attributes = True

class ScoreMatrixItem(BaseModel):
    student_id: str
    student_name: str
    scores: dict = Field(default_factory=dict)

class ScoreMatrixResponse(BaseModel):
    students: List[ScoreMatrixItem]
    assessment_items: List[AssessmentItemResponse]

class ScoreImportResult(BaseModel):
    count: int
    errors: List[str] = Field(default_factory=list)

class ObjectiveCalcResult(BaseModel):
    objective_id: str
    objective_code: str
    C_bar: float

class CourseCalcResult(BaseModel):
    indicator_id: str
    indicator_code: str
    E_k: float

class CalculateResponse(BaseModel):
    objective_results: List[ObjectiveCalcResult]
    course_results: List[CourseCalcResult]
