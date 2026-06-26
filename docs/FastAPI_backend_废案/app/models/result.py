"""计算结果模型"""
import uuid
from sqlalchemy import Column, String, DateTime, ForeignKey, DECIMAL, func, Index
from sqlalchemy.orm import relationship
from app.database import Base

class CalculationResult(Base):
    __tablename__ = "calculation_results"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    course_id = Column(String(36), ForeignKey("courses.id", ondelete="CASCADE"), nullable=True)
    class_id = Column(String(36), ForeignKey("classes.id", ondelete="CASCADE"), nullable=True)
    level = Column(String(20), nullable=False)  # objective / course / program
    objective_id = Column(String(36), ForeignKey("course_objectives.id", ondelete="SET NULL"), nullable=True)
    indicator_id = Column(String(36), ForeignKey("graduation_indicators.id", ondelete="SET NULL"), nullable=True)
    achievement_value = Column(DECIMAL(6, 4), nullable=False)
    locked = Column(String(1), nullable=False, default="0")  # 1=locked, 0=unlocked
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    __table_args__ = (
        Index("idx_result_course_class_level", "course_id", "class_id", "level"),
        Index("idx_result_level_locked", "level", "locked"),
        Index("idx_result_indicator", "indicator_id"),
    )
    course = relationship("Course")
    class_ = relationship("Class")
    objective = relationship("CourseObjective")
    indicator = relationship("GraduationIndicator")
