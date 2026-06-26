"""学生-教学班选课关系模型"""
import uuid
from sqlalchemy import Column, String, DateTime, ForeignKey, UniqueConstraint, func
from sqlalchemy.orm import relationship
from app.database import Base

class ClassStudent(Base):
    __tablename__ = "class_students"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    class_id = Column(String(36), ForeignKey("classes.id", ondelete="CASCADE"), nullable=False)
    student_id = Column(String(36), ForeignKey("students.id", ondelete="CASCADE"), nullable=False)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    __table_args__ = (UniqueConstraint("class_id", "student_id", name="uq_class_student"),)
    class_ = relationship("Class", backref="class_students")
    student = relationship("Student", backref="class_students")
