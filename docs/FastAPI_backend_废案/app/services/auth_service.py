"""认证与用户管理服务"""
from datetime import datetime, timedelta
from typing import Optional, List
from jose import jwt
from passlib.context import CryptContext
from sqlalchemy.orm import Session
from sqlalchemy import or_
from fastapi import HTTPException, status
from app.models.user import User
from app.config import settings
from app.schemas.auth import LoginResponse, UserCreate, UserUpdate

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def authenticate_user(db: Session, username: str, password: str) -> Optional[User]:
    user = db.query(User).filter(User.username == username, User.is_active == "1").first()
    if not user:
        return None
    if not pwd_context.verify(password, user.password_hash):
        return None
    return user

def create_access_token(user_id: str, role: str) -> str:
    expire = datetime.utcnow() + timedelta(hours=settings.ACCESS_TOKEN_EXPIRE_HOURS)
    payload = {"sub": user_id, "role": role, "exp": expire}
    return jwt.encode(payload, settings.SECRET_KEY, algorithm=settings.ALGORITHM)

def login(db: Session, username: str, password: str) -> LoginResponse:
    user = authenticate_user(db, username, password)
    if not user:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="用户名或密码错误")
    token = create_access_token(user.id, user.role)
    return LoginResponse(access_token=token, role=user.role, display_name=user.display_name, user_id=user.id)

def get_user_info(user: User) -> dict:
    return {
        "id": user.id, "username": user.username, "role": user.role,
        "display_name": user.display_name, "college": user.college,
        "major": user.major, "is_active": user.is_active,
    }

def list_users(db: Session, role: Optional[str] = None, is_active: Optional[str] = None, keyword: Optional[str] = None) -> List[User]:
    query = db.query(User)
    if role:
        query = query.filter(User.role == role)
    if is_active is not None:
        query = query.filter(User.is_active == is_active)
    if keyword:
        query = query.filter(or_(User.username.like(f"%{keyword}%"), User.display_name.like(f"%{keyword}%")))
    return query.order_by(User.created_at.desc()).all()

def create_user(db: Session, data: UserCreate) -> User:
    existing = db.query(User).filter(User.username == data.username).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="用户名已存在")
    user = User(
        username=data.username, password_hash=pwd_context.hash(data.password),
        role=data.role, display_name=data.display_name, college=data.college, major=data.major,
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user

def update_user(db: Session, user_id: str, data: UserUpdate) -> User:
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="用户不存在")
    if data.display_name is not None:
        user.display_name = data.display_name
    if data.college is not None:
        user.college = data.college
    if data.major is not None:
        user.major = data.major
    if data.role is not None:
        if data.role not in ("admin", "academic", "director", "teacher"):
            raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="无效的角色")
        user.role = data.role
    if data.is_active is not None:
        user.is_active = data.is_active
    db.commit()
    db.refresh(user)
    return user
