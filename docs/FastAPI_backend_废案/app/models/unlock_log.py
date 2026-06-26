"""成绩解锁日志模型"""
import uuid
from sqlalchemy import Column, String, DateTime, ForeignKey, func, Index
from sqlalchemy.orm import relationship
from app.database import Base

class UnlockLog(Base):
    __tablename__ = "unlock_log"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    course_id = Column(String(36), ForeignKey("courses.id", ondelete="CASCADE"), nullable=False)
    class_id = Column(String(36), ForeignKey("classes.id", ondelete="CASCADE"), nullable=False)
    operator_id = Column(String(36), ForeignKey("users.id", ondelete="RESTRICT"), nullable=False)
    reason = Column(String(500), nullable=False)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    __table_args__ = (Index("idx_unlock_course_class", "course_id", "class_id"),)
    course = relationship("Course")
    class_ = relationship("Class")
    operator = relationship("User")
