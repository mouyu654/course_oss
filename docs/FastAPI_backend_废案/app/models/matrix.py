"""宏观支撑矩阵 与 内部权重矩阵模型"""
import uuid
from sqlalchemy import Column, String, DateTime, ForeignKey, UniqueConstraint, DECIMAL, func
from sqlalchemy.orm import relationship
from app.database import Base

class MacroSupportMatrix(Base):
    __tablename__ = "macro_support_matrix"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    course_id = Column(String(36), ForeignKey("courses.id", ondelete="CASCADE"), nullable=False)
    indicator_id = Column(String(36), ForeignKey("graduation_indicators.id", ondelete="CASCADE"), nullable=False)
    weight_W = Column(DECIMAL(5, 4), nullable=False)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    __table_args__ = (UniqueConstraint("course_id", "indicator_id", name="uq_course_indicator"),)
    course = relationship("Course")
    indicator = relationship("GraduationIndicator")

class InternalWeightMatrix(Base):
    __tablename__ = "internal_weight_matrix"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    objective_id = Column(String(36), ForeignKey("course_objectives.id", ondelete="CASCADE"), nullable=False)
    indicator_id = Column(String(36), ForeignKey("graduation_indicators.id", ondelete="CASCADE"), nullable=False)
    weight_w = Column(DECIMAL(5, 4), nullable=False)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    __table_args__ = (UniqueConstraint("objective_id", "indicator_id", name="uq_objective_indicator"),)
    objective = relationship("CourseObjective")
    indicator = relationship("GraduationIndicator")
