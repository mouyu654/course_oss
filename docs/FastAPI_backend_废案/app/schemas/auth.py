"""认证与用户相关的 Pydantic 模型"""
from typing import Optional
from pydantic import BaseModel

class LoginRequest(BaseModel):
    username: str
    password: str

class LoginResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    role: str
    display_name: str
    user_id: str

class UserCreate(BaseModel):
    username: str
    password: str
    role: str
    display_name: str
    college: Optional[str] = None
    major: Optional[str] = None

class UserUpdate(BaseModel):
    display_name: Optional[str] = None
    college: Optional[str] = None
    major: Optional[str] = None
    role: Optional[str] = None
    is_active: Optional[str] = None

class UserResponse(BaseModel):
    id: str
    username: str
    role: str
    display_name: str
    college: Optional[str] = None
    major: Optional[str] = None
    is_active: str

    class Config:
        from_attributes = True
