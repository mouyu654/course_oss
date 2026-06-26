"""全局配置管理路由"""
from typing import Optional, List
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.database import get_db
from app.dependencies.auth import get_current_user, require_admin
from app.services import admin_service
from app.models.user import User
from app.schemas.admin import GlobalConfigCreate, GlobalConfigUpdate, GlobalConfigResponse
from app.schemas.common import MessageResponse

router = APIRouter()

@router.get("/config", response_model=List[GlobalConfigResponse])
async def list_configs(
    category: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    return admin_service.get_configs(db, category=category)

@router.post("/config", response_model=GlobalConfigResponse)
async def create_config(
    data: GlobalConfigCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    return admin_service.create_config(db, data)

@router.put("/config/{config_id}", response_model=GlobalConfigResponse)
async def update_config(
    config_id: str,
    data: GlobalConfigUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    return admin_service.update_config(db, config_id, data)

@router.delete("/config/{config_id}", response_model=MessageResponse)
async def delete_config(
    config_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    admin_service.delete_config(db, config_id)
    return {"message": "删除成功"}
