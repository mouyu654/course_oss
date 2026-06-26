"""认证与用户管理路由"""
from typing import Optional, List
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.database import get_db
from app.dependencies.auth import get_current_user, require_admin
from app.services import auth_service
from app.models.user import User
from app.schemas.auth import LoginRequest, LoginResponse, UserCreate, UserUpdate, UserResponse
from app.schemas.common import MessageResponse

router = APIRouter()

@router.post("/login", response_model=LoginResponse)
async def login(data: LoginRequest, db: Session = Depends(get_db)):
    return auth_service.login(db, data.username, data.password)

@router.get("/me", response_model=dict)
async def get_me(current_user: User = Depends(get_current_user)):
    return auth_service.get_user_info(current_user)

@router.get("/users", response_model=List[UserResponse])
async def list_users_api(
    role: Optional[str] = Query(None),
    is_active: Optional[str] = Query(None),
    keyword: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    return auth_service.list_users(db, role=role, is_active=is_active, keyword=keyword)

@router.post("/users", response_model=UserResponse)
async def create_user_api(data: UserCreate, db: Session = Depends(get_db), current_user: User = Depends(require_admin)):
    return auth_service.create_user(db, data)

@router.patch("/users/{user_id}", response_model=UserResponse)
async def update_user_api(user_id: str, data: UserUpdate, db: Session = Depends(get_db), current_user: User = Depends(require_admin)):
    return auth_service.update_user(db, user_id, data)
