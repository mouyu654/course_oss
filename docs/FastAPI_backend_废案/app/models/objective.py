"""课程目标模型"""
import uuid
from sqlalchemy import Column, String, Text, DateTime, ForeignKey, UniqueConstraint, func
from sqlalchemy.orm import relationship
from app.database import Base

class CourseObjective(Base):
    __tablename__ = "course_objectives"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    course_id = Column(String(36), ForeignKey("courses.id", ondelete="CASCADE"), nullable=False)
    code = Column(String(10), nullable=False)
    description = Column(Text, nullable=False)
    dimension = Column(String(20), nullable=False)  # 知识维度 / 能力维度 / 价值维度
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    __table_args__ = (UniqueConstraint("course_id", "code", name="uq_course_code"),)
    course = relationship("Course", backref="objectives")
