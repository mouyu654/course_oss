"""Teacher module router —— M3-1 课程目标 / M3-2 内部权重 / M3-3 考核点

共 8 个 API 端点，全部挂载于 /api/teacher。
"""

from collections import defaultdict
from decimal import Decimal
from typing import List

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func
from sqlalchemy.orm import Session, joinedload

from core.config import WEIGHT_TOLERANCE
from core.deps import get_current_user, require_role
from db.session import get_db
from models import (
    Course,
    Class,
    CourseObjective,
    InternalWeightMatrix,
    AssessmentItem,
    StudentScore,
    GraduationIndicator,
    MacroSupportMatrix,
    CalculationResult,
)
from schemas.teacher import (
    ObjectiveOut,
    ObjectiveCreate,
    WeightEntry,
    WeightEntryOut,
    InternalWeightSubmit,
    AssessmentItemOut,
    AssessmentItemCreate,
)

router = APIRouter(prefix="/api/teacher", tags=["teacher"])

TOL = Decimal(str(WEIGHT_TOLERANCE))

# ── helpers ─────────────────────────────────────────────

def _get_teacher_class(db: Session, course_id: str, user_id: str) -> Class:
    """获取当前教师名下某门课程的教学班。未找到则 403。"""
    cls = db.query(Class).filter(
        Class.course_id == course_id,
        Class.teacher_id == user_id,
    ).first()
    if not cls:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="您不是该课程的主讲教师，无权操作",
        )
    return cls


def _check_course_locked(db: Session, course_id: str, class_id: str) -> None:
    """检查课程级计算结果是否已锁定。已锁定则拒绝操作。"""
    locked = db.query(CalculationResult).filter(
        CalculationResult.course_id == course_id,
        CalculationResult.class_id == class_id,
        CalculationResult.level == "course",
        CalculationResult.locked == True,
    ).first()
    if locked:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="该课程成绩已锁定，请联系教务管理员解锁",
        )


def _validate_weight_sums(weights: List[dict], label: str = "指标点") -> None:
    """按 indicator_id 校验 Σweight = 1.0。"""
    sums: dict[str, Decimal] = defaultdict(lambda: Decimal("0"))
    for w in weights:
        sums[w["indicator_id"]] += Decimal(str(w["weight_w"]))
    for ind_id, total in sums.items():
        if abs(total - Decimal("1")) > TOL:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"{label} {ind_id} 的权重合计为 {total}，不等于 1.0（容差 ±{TOL}）",
            )


# ═════════════════════════════════════════════════════════
# M3-1  课程目标  (3 APIs)
# ═════════════════════════════════════════════════════════

@router.get("/objectives", response_model=List[ObjectiveOut])
def list_objectives(
    course_id: str = Query(..., description="课程 ID"),
    db: Session = Depends(get_db),
    _current_user: dict = Depends(get_current_user),
):
    """查询某门课程的全部目标。"""
    objs = (
        db.query(CourseObjective)
        .filter(CourseObjective.course_id == course_id)
        .order_by(CourseObjective.code)
        .all()
    )
    return objs


@router.post("/objectives", response_model=ObjectiveOut, status_code=status.HTTP_201_CREATED)
def create_objective(
    body: ObjectiveCreate,
    db: Session = Depends(get_db),
    current_user: dict = Depends(require_role("teacher", "admin")),
):
    """新增课程目标。需归属校验：仅限该课程教学班的主讲教师。"""
    _get_teacher_class(db, body.course_id, current_user["sub"])

    # 同课程内编号唯一性
    existing = db.query(CourseObjective).filter(
        CourseObjective.course_id == body.course_id,
        CourseObjective.code == body.code,
    ).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"该课程下已存在编号为 {body.code} 的目标",
        )

    obj = CourseObjective(
        course_id=body.course_id,
        code=body.code,
        description=body.description,
        dimension=body.dimension,
    )
    db.add(obj)
    db.commit()
    db.refresh(obj)
    return obj


@router.delete("/objectives/{objective_id}")
def delete_objective(
    objective_id: str,
    db: Session = Depends(get_db),
    current_user: dict = Depends(require_role("teacher", "admin")),
):
    """删除课程目标。存在关联考核点或内部权重时返回 409。"""
    obj = db.query(CourseObjective).filter(CourseObjective.id == objective_id).first()
    if not obj:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="课程目标不存在")

    # 归属校验
    cls = db.query(Class).filter(
        Class.course_id == obj.course_id,
        Class.teacher_id == current_user["sub"],
    ).first()
    if not cls and current_user.get("role") != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="您不是该课程的主讲教师",
        )

    # 依赖检查：考核点
    linked_items = db.query(AssessmentItem).filter(
        AssessmentItem.objective_id == objective_id
    ).count()
    if linked_items > 0:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"该目标下存在 {linked_items} 个考核点，请先删除考核点后再删除目标",
        )

    # 依赖检查：内部权重
    linked_weights = db.query(InternalWeightMatrix).filter(
        InternalWeightMatrix.objective_id == objective_id
    ).count()
    if linked_weights > 0:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail="该目标存在关联的内部权重记录，请先清除权重配置",
        )

    db.delete(obj)
    db.commit()
    return {"ok": True}


# ═════════════════════════════════════════════════════════
# M3-2  内部权重矩阵  (2 APIs)
# ═════════════════════════════════════════════════════════

@router.get("/internal-weights", response_model=List[WeightEntryOut])
def list_internal_weights(
    course_id: str = Query(..., description="课程 ID"),
    db: Session = Depends(get_db),
    _current_user: dict = Depends(get_current_user),
):
    """查询某门课程的内部权重矩阵。"""
    # 通过课程目标关联
    objective_ids = (
        db.query(CourseObjective.id)
        .filter(CourseObjective.course_id == course_id)
        .subquery()
    )
    rows = (
        db.query(InternalWeightMatrix)
        .filter(InternalWeightMatrix.objective_id.in_(objective_ids))
        .all()
    )
    return rows


@router.post("/internal-weights")
def submit_internal_weights(
    body: InternalWeightSubmit,
    course_id: str = Query(..., description="课程 ID"),
    db: Session = Depends(get_db),
    current_user: dict = Depends(require_role("teacher", "admin")),
):
    """提交内部权重矩阵。

    后端再次按 indicator_id 分组校验 Σw = 1.0（容差 0.001），通过后事务中全量替换。
    """
    cls = _get_teacher_class(db, course_id, current_user["sub"])
    _check_course_locked(db, course_id, cls.id)

    # 校验权重
    raw = [w.model_dump() for w in body.weights]
    _validate_weight_sums(raw, label="指标点")

    # 获取该课程的全部目标 ID
    obj_ids = {
        row.id
        for row in db.query(CourseObjective.id).filter(
            CourseObjective.course_id == course_id
        ).all()
    }
    for entry in raw:
        if entry["objective_id"] not in obj_ids:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"课程目标 {entry['objective_id']} 不属于本课程",
            )

    # 事务：DELETE 旧权重 + INSERT 新权重
    obj_id_list = list(obj_ids)
    db.query(InternalWeightMatrix).filter(
        InternalWeightMatrix.objective_id.in_(obj_id_list)
    ).delete(synchronize_session=False)

    for entry in raw:
        db.add(InternalWeightMatrix(
            objective_id=entry["objective_id"],
            indicator_id=entry["indicator_id"],
            weight_w=entry["weight_w"],
        ))

    db.commit()
    return {"ok": True, "count": len(raw)}


# ═════════════════════════════════════════════════════════
# M3-3  考核点  (3 APIs)
# ═════════════════════════════════════════════════════════

@router.get("/assessment-items", response_model=List[AssessmentItemOut])
def list_assessment_items(
    course_id: str = Query(..., description="课程 ID"),
    db: Session = Depends(get_db),
    _current_user: dict = Depends(get_current_user),
):
    """查询某门课程的全部考核点。"""
    items = (
        db.query(AssessmentItem)
        .filter(AssessmentItem.course_id == course_id)
        .order_by(AssessmentItem.name)
        .all()
    )
    return items


@router.post("/assessment-items", response_model=AssessmentItemOut, status_code=status.HTTP_201_CREATED)
def create_assessment_item(
    body: AssessmentItemCreate,
    db: Session = Depends(get_db),
    current_user: dict = Depends(require_role("teacher", "admin")),
):
    """新增考核点。需归属校验。"""
    _get_teacher_class(db, body.course_id, current_user["sub"])

    # 验证 objective 属于该课程
    obj = db.query(CourseObjective).filter(
        CourseObjective.id == body.objective_id,
        CourseObjective.course_id == body.course_id,
    ).first()
    if not obj:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="目标不属于该课程"
        )

    item = AssessmentItem(
        course_id=body.course_id,
        objective_id=body.objective_id,
        name=body.name,
        max_score=body.max_score,
    )
    db.add(item)
    db.commit()
    db.refresh(item)
    return item


@router.delete("/assessment-items/{item_id}")
def delete_assessment_item(
    item_id: str,
    db: Session = Depends(get_db),
    current_user: dict = Depends(require_role("teacher", "admin")),
):
    """删除考核点。已有学生成绩时返回 409。"""
    item = db.query(AssessmentItem).filter(AssessmentItem.id == item_id).first()
    if not item:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="考核点不存在")

    # 归属校验
    cls = db.query(Class).filter(
        Class.course_id == item.course_id,
        Class.teacher_id == current_user["sub"],
    ).first()
    if not cls and current_user.get("role") != "admin":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="您不是该课程的主讲教师",
        )

    # 依赖检查：学生成绩
    score_count = db.query(StudentScore).filter(
        StudentScore.assessment_item_id == item_id
    ).count()
    if score_count > 0:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"该考核点下已有 {score_count} 条学生成绩，无法删除",
        )

    db.delete(item)
    db.commit()
    return {"ok": True}
