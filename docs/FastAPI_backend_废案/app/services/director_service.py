"""专业设计管理服务"""
from typing import List, Optional
from sqlalchemy.orm import Session, joinedload
from sqlalchemy import func as sa_func
from fastapi import HTTPException, status
from app.models.graduation import GraduationRequirement, GraduationIndicator
from app.models.matrix import MacroSupportMatrix
from app.config import settings

def list_requirements(db: Session) -> List[GraduationRequirement]:
    return db.query(GraduationRequirement).options(
        joinedload(GraduationRequirement.indicators)
    ).order_by(GraduationRequirement.code).all()

def create_requirement(db: Session, code: str, title: str, description: Optional[str] = None) -> GraduationRequirement:
    existing = db.query(GraduationRequirement).filter(GraduationRequirement.code == code).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="毕业要求编号已存在")
    req = GraduationRequirement(code=code, title=title, description=description)
    db.add(req)
    db.commit()
    db.refresh(req)
    return req

def delete_requirement(db: Session, req_id: str) -> bool:
    req = db.query(GraduationRequirement).filter(GraduationRequirement.id == req_id).first()
    if not req:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="毕业要求不存在")
    indicator_count = db.query(sa_func.count(GraduationIndicator.id)).filter(
        GraduationIndicator.requirement_id == req_id
    ).scalar()
    if indicator_count > 0:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="该毕业要求下存在指标点，请先删除指标点")
    db.delete(req)
    db.commit()
    return True

def list_indicators(db: Session, requirement_id: Optional[str] = None) -> List[GraduationIndicator]:
    query = db.query(GraduationIndicator)
    if requirement_id:
        query = query.filter(GraduationIndicator.requirement_id == requirement_id)
    return query.order_by(GraduationIndicator.code).all()

def create_indicator(db: Session, requirement_id: str, code: str, description: str) -> GraduationIndicator:
    existing = db.query(GraduationIndicator).filter(GraduationIndicator.code == code).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="指标点编号已存在")
    req = db.query(GraduationRequirement).filter(GraduationRequirement.id == requirement_id).first()
    if not req:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="毕业要求不存在")
    indicator = GraduationIndicator(requirement_id=requirement_id, code=code, description=description)
    db.add(indicator)
    db.commit()
    db.refresh(indicator)
    return indicator

def delete_indicator(db: Session, indicator_id: str) -> bool:
    indicator = db.query(GraduationIndicator).filter(GraduationIndicator.id == indicator_id).first()
    if not indicator:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="指标点不存在")
    matrix_count = db.query(sa_func.count(MacroSupportMatrix.id)).filter(
        MacroSupportMatrix.indicator_id == indicator_id
    ).scalar()
    if matrix_count > 0:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="该指标点存在宏观支撑矩阵配置，请先清除")
    db.delete(indicator)
    db.commit()
    return True

def get_macro_matrix(db: Session) -> List[MacroSupportMatrix]:
    return db.query(MacroSupportMatrix).all()

def submit_macro_matrix(db: Session, entries: List[dict]) -> List[MacroSupportMatrix]:
    """提交宏观矩阵配置（全量替换，后端列合计校验）"""
    if not entries:
        db.query(MacroSupportMatrix).delete()
        db.commit()
        return []

    indicator_sums = {}
    course_ids = set()
    for entry in entries:
        ind_id = entry["indicator_id"]
        weight = float(entry["weight_W"])
        indicator_sums[ind_id] = indicator_sums.get(ind_id, 0.0) + weight
        course_ids.add(entry["course_id"])

    tolerance = settings.WEIGHT_TOLERANCE
    for ind_id, total in indicator_sums.items():
        if abs(total - 1.0) > tolerance:
            indicator = db.query(GraduationIndicator).filter(GraduationIndicator.id == ind_id).first()
            ind_code = indicator.code if indicator else ind_id
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"指标点 {ind_code} 的支撑权重之和为 {total:.4f}，不等于 1.0（容差 ±{tolerance}）"
            )

    try:
        db.query(MacroSupportMatrix).filter(MacroSupportMatrix.course_id.in_(course_ids)).delete(synchronize_session=False)
        for entry in entries:
            weight = float(entry["weight_W"])
            if weight > 0:
                matrix = MacroSupportMatrix(
                    course_id=entry["course_id"], indicator_id=entry["indicator_id"], weight_W=weight,
                )
                db.add(matrix)
        db.commit()
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"矩阵提交失败: {str(e)}")

    return db.query(MacroSupportMatrix).filter(MacroSupportMatrix.course_id.in_(course_ids)).all()
