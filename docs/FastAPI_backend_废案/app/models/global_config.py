"""全局数据字典模型"""
import uuid
from sqlalchemy import Column, String, DateTime, Integer, UniqueConstraint, func
from app.database import Base

class GlobalConfig(Base):
    __tablename__ = "global_config"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    category = Column(String(50), nullable=False)
    key = Column(String(50), nullable=False)
    value = Column(String(255), nullable=False)
    description = Column(String(255), nullable=True)
    is_active = Column(Integer, nullable=False, default=1)
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
    __table_args__ = (UniqueConstraint("category", "key", name="uq_category_key"),)
