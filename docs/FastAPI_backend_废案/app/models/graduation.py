"""毕业要求与指标点模型"""
import uuid
from sqlalchemy import Column, String, Text, DateTime, ForeignKey, func
from sqlalchemy.orm import relationship
from app.database import Base

class GraduationRequirement(Base):
    __tablename__ = "graduation_requirements"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    code = Column(String(10), nullable=False, unique=True)
    title = Column(String(100), nullable=False)
    description = Column(Text, nullable=True)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    indicators = relationship("GraduationIndicator", back_populates="requirement", cascade="all, delete-orphan")

class GraduationIndicator(Base):
    __tablename__ = "graduation_indicators"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    requirement_id = Column(String(36), ForeignKey("graduation_requirements.id", ondelete="CASCADE"), nullable=False)
    code = Column(String(10), nullable=False, unique=True)
    description = Column(Text, nullable=False)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    requirement = relationship("GraduationRequirement", back_populates="indicators")
