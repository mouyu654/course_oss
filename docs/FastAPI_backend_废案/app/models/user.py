"""用户模型"""
import uuid
from sqlalchemy import Column, String, Enum as SAEnum, DateTime, func
from app.database import Base

class User(Base):
    __tablename__ = "users"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    username = Column(String(50), nullable=False, unique=True, index=True)
    password_hash = Column(String(255), nullable=False)
    role = Column(SAEnum("admin", "academic", "director", "teacher", name="user_role"), nullable=False)
    display_name = Column(String(50), nullable=False)
    college = Column(String(100), nullable=True)
    major = Column(String(100), nullable=True)
    is_active = Column(String(1), nullable=False, default="1")
    created_at = Column(DateTime, nullable=False, server_default=func.now())
    updated_at = Column(DateTime, nullable=False, server_default=func.now(), onupdate=func.now())
