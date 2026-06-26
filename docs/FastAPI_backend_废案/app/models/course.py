"""课程模型"""
import uuid
from sqlalchemy import Column, String, DateTime, Integer, DECIMAL, SMALLINT, func, Index
from app.database import Base

class Course(Base):
    __tablename__ = "courses"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    code = Column(String(50), nullable=False, unique=True)
    name = Column(String(100), nullable=False)
    credit = Column(DECIMAL(4, 1), nullable=False)
    hours_theory = Column(SMALLINT, nullable=False, default=0)
    hours_lab = Column(SMALLINT, nullable=False, default=0)
    college = Column(String(100), nullable=False)
    major = Column(String(100), nullable=False)
    academic_year = Column(String(20), nullable=False)
    semester = Column(String(1), nullable=False)
    is_active = Column(Integer, nullable=False, default=1)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    __table_args__ = (Index("idx_major_year", "major", "academic_year"),)
