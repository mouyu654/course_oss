"""学生模型"""
import uuid
from sqlalchemy import Column, String, DateTime, Integer, Index, func
from app.database import Base

class Student(Base):
    __tablename__ = "students"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    student_id = Column(String(50), nullable=False, unique=True, name="idx_student_id")
    name = Column(String(50), nullable=False)
    college = Column(String(100), nullable=True)
    major = Column(String(100), nullable=True)
    grade = Column(String(20), nullable=True)
    is_active = Column(Integer, nullable=False, default=1)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    __table_args__ = (Index("idx_student_college", "college"), Index("idx_student_major", "major"),)
