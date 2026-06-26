"""专业设计路由"""
from typing import Optional, List
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.database import get_db
from app.dependencies.auth import get_current_user, require_director, require_any
from app.services import director_service
from app.models.user import User
from app.schemas.director import (
    RequirementCreate, RequirementResponse, RequirementWithIndicatorsResponse,
    IndicatorCreate, IndicatorResponse, MacroMatrixEntry, MacroMatrixResponse,
)
from app.schemas.common import MessageResponse

router = APIRouter()

@router.post("/requirements", response_model=RequirementResponse)
async def create_requirement(
    data: RequirementCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_director),
):
    return director_service.create_requirement(db, data.code, data.title, data.description)

@router.get("/requirements", response_model=List[RequirementWithIndicatorsResponse])
async def list_requirements(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_any),
):
    requirements = director_service.list_requirements(db)
    result = []
    for req in requirements:
        indicators = [
            IndicatorResponse(id=ind.id, requirement_id=ind.requirement_id, code=ind.code, description=ind.description)
            for ind in req.indicators
        ]
        result.append(RequirementWithIndicatorsResponse(
            id=req.id, code=req.code, title=req.title, description=req.description, indicators=indicators,
        ))
    return result

@router.delete("/requirements/{req_id}", response_model=MessageResponse)
async def delete_requirement(
    req_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_director),
):
    director_service.delete_requirement(db, req_id)
    return {"message": "删除成功"}

@router.post("/indicators", response_model=IndicatorResponse)
async def create_indicator(
    data: IndicatorCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_director),
):
    return director_service.create_indicator(db, data.requirement_id, data.code, data.description)

@router.get("/indicators", response_model=List[IndicatorResponse])
async def list_indicators(
    requirement_id: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_any),
):
    indicators = director_service.list_indicators(db, requirement_id=requirement_id)
    return [
        IndicatorResponse(id=ind.id, requirement_id=ind.requirement_id, code=ind.code, description=ind.description)
        for ind in indicators
    ]

@router.delete("/indicators/{indicator_id}", response_model=MessageResponse)
async def delete_indicator(
    indicator_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_director),
):
    director_service.delete_indicator(db, indicator_id)
    return {"message": "删除成功"}

@router.get("/macro-matrix", response_model=List[MacroMatrixResponse])
async def get_macro_matrix(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_any),
):
    matrices = director_service.get_macro_matrix(db)
    return [
        MacroMatrixResponse(id=m.id, course_id=m.course_id, indicator_id=m.indicator_id, weight_W=float(m.weight_W))
        for m in matrices
    ]

@router.post("/macro-matrix", response_model=List[MacroMatrixResponse])
async def submit_macro_matrix(
    entries: List[MacroMatrixEntry],
    db: Session = Depends(get_db),
    current_user: User = Depends(require_director),
):
    result = director_service.submit_macro_matrix(db, [
        {"course_id": e.course_id, "indicator_id": e.indicator_id, "weight_W": e.weight_W} for e in entries
    ])
    return [
        MacroMatrixResponse(id=m.id, course_id=m.course_id, indicator_id=m.indicator_id, weight_W=float(m.weight_W))
        for m in result
    ]
