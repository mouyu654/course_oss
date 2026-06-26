"""学生成绩模型"""
import uuid
from sqlalchemy import Column, String, DateTime, ForeignKey, UniqueConstraint, DECIMAL, func
from sqlalchemy.orm import relationship
from app.database import Base

class StudentScore(Base):
    __tablename__ = "student_scores"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    class_student_id = Column(String(36), ForeignKey("class_students.id", ondelete="CASCADE"), nullable=False)
    assessment_item_id = Column(String(36), ForeignKey("assessment_items.id", ondelete="CASCADE"), nullable=False)
    score = Column(DECIMAL(6, 2), nullable=False)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    __table_args__ = (UniqueConstraint("class_student_id", "assessment_item_id", name="uq_cs_item"),)
    class_student = relationship("ClassStudent")
    assessment_item = relationship("AssessmentItem")
