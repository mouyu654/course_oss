"""教学班模型"""
import uuid
from sqlalchemy import Column, String, DateTime, ForeignKey, Index, func
from sqlalchemy.orm import relationship
from app.database import Base

class Class(Base):
    __tablename__ = "classes"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    course_id = Column(String(36), ForeignKey("courses.id", ondelete="CASCADE"), nullable=False)
    teacher_id = Column(String(36), ForeignKey("users.id", ondelete="RESTRICT"), nullable=False)
    name = Column(String(100), nullable=False)
    academic_year = Column(String(20), nullable=False)
    semester = Column(String(1), nullable=False)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    __table_args__ = (
        Index("idx_class_course", "course_id"),
        Index("idx_class_teacher", "teacher_id"),
        Index("idx_class_year_sem", "academic_year", "semester"),
    )
    course = relationship("Course")
    teacher = relationship("User", foreign_keys=[teacher_id])
