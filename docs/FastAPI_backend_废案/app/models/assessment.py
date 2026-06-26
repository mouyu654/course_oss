"""考核点模型"""
import uuid
from sqlalchemy import Column, String, DateTime, ForeignKey, DECIMAL, func, Index
from sqlalchemy.orm import relationship
from app.database import Base

class AssessmentItem(Base):
    __tablename__ = "assessment_items"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    course_id = Column(String(36), ForeignKey("courses.id", ondelete="CASCADE"), nullable=False)
    objective_id = Column(String(36), ForeignKey("course_objectives.id", ondelete="CASCADE"), nullable=False)
    name = Column(String(200), nullable=False)
    max_score = Column(DECIMAL(6, 2), nullable=False)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    __table_args__ = (Index("idx_item_course", "course_id"), Index("idx_item_objective", "objective_id"),)
    course = relationship("Course", backref="assessment_items")
    objective = relationship("CourseObjective", backref="assessment_items")
