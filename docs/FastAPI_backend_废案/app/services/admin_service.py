"""全局配置管理服务"""
from typing import List, Optional
from sqlalchemy.orm import Session
from fastapi import HTTPException, status
from app.models.global_config import GlobalConfig
from app.schemas.admin import GlobalConfigCreate, GlobalConfigUpdate

def get_configs(db: Session, category: Optional[str] = None, active_only: bool = True) -> List[GlobalConfig]:
    query = db.query(GlobalConfig)
    if category:
        query = query.filter(GlobalConfig.category == category)
    if active_only:
        query = query.filter(GlobalConfig.is_active == 1)
    return query.order_by(GlobalConfig.category, GlobalConfig.key).all()

def create_config(db: Session, data: GlobalConfigCreate) -> GlobalConfig:
    existing = db.query(GlobalConfig).filter(
        GlobalConfig.category == data.category, GlobalConfig.key == data.key
    ).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="配置项已存在（相同 category + key）")
    config = GlobalConfig(category=data.category, key=data.key, value=data.value, description=data.description)
    db.add(config)
    db.commit()
    db.refresh(config)
    return config

def update_config(db: Session, config_id: str, data: GlobalConfigUpdate) -> GlobalConfig:
    config = db.query(GlobalConfig).filter(GlobalConfig.id == config_id).first()
    if not config:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="配置项不存在")
    if data.value is not None:
        config.value = data.value
    if data.description is not None:
        config.description = data.description
    db.commit()
    db.refresh(config)
    return config

def delete_config(db: Session, config_id: str) -> bool:
    config = db.query(GlobalConfig).filter(GlobalConfig.id == config_id).first()
    if not config:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="配置项不存在")
    config.is_active = 0
    db.commit()
    return True
