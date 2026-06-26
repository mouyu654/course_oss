"""教务管理相关的 Pydantic 模型"""
from typing import Optional, List
from pydantic import BaseModel, Field

class CourseCreate(BaseModel):
    code: str
    name: str
    credit: float = Field(ge=0, le=999.9)
    hours_theory: int = Field(ge=0, default=0)
    hours_lab: int = Field(ge=0, default=0)
    college: str
    major: str
    academic_year: str
    semester: str

class CourseResponse(BaseModel):
    id: str
    code: str
    name: str
    credit: float
    hours_theory: int
    hours_lab: int
    college: str
    major: str
    academic_year: str
    semester: str
    is_active: int
    class Config:
        from_attributes = True

class ClassCreate(BaseModel):
    course_id: str
    teacher_id: str
    name: str
    academic_year: str
    semester: str

class ClassResponse(BaseModel):
    id: str
    course_id: str
    teacher_id: str
    name: str
    academic_year: str
    semester: str
    class Config:
        from_attributes = True

class ClassWithCourseResponse(ClassResponse):
    course_code: Optional[str] = None
    course_name: Optional[str] = None

class StudentImportResult(BaseModel):
    inserted: int
    updated: int
    errors: List[str] = Field(default_factory=list)

class StudentResponse(BaseModel):
    id: str
    student_id: str
    name: str
    college: Optional[str] = None
    major: Optional[str] = None
    grade: Optional[str] = None
    is_active: int
    class Config:
        from_attributes = True

class CalculationProgressItem(BaseModel):
    class_id: str
    class_name: str
    course_code: str
    course_name: str
    teacher_name: str
    status: str  # "locked" or "pending"

class CalculationProgressResponse(BaseModel):
    items: List[CalculationProgressItem]
    all_locked: bool

class UnlockRequest(BaseModel):
    course_id: str
    class_id: str
    reason: str = Field(min_length=1, max_length=500)
