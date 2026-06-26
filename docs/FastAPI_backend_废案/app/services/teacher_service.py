"""课程主讲教师服务"""
from typing import List, Optional
from sqlalchemy.orm import Session
from sqlalchemy import func as sa_func
from fastapi import HTTPException, status
from app.models.course import Course
from app.models.class_ import Class
from app.models.objective import CourseObjective
from app.models.assessment import AssessmentItem
from app.models.score import StudentScore
from app.models.student import Student
from app.models.class_student import ClassStudent
from app.models.result import CalculationResult
from app.models.matrix import InternalWeightMatrix, MacroSupportMatrix
from app.schemas.teacher import ObjectiveCreate, AssessmentItemCreate, InternalWeightEntry

def list_objectives(db: Session, course_id: Optional[str] = None) -> List[CourseObjective]:
    query = db.query(CourseObjective)
    if course_id:
        query = query.filter(CourseObjective.course_id == course_id)
    return query.order_by(CourseObjective.code).all()

def create_objective(db: Session, data: ObjectiveCreate) -> CourseObjective:
    existing = db.query(CourseObjective).filter(
        CourseObjective.course_id == data.course_id, CourseObjective.code == data.code,
    ).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="该课程下目标编号已存在")
    objective = CourseObjective(
        course_id=data.course_id, code=data.code, description=data.description, dimension=data.dimension,
    )
    db.add(objective)
    db.commit()
    db.refresh(objective)
    return objective

def delete_objective(db: Session, objective_id: str) -> bool:
    objective = db.query(CourseObjective).filter(CourseObjective.id == objective_id).first()
    if not objective:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="课程目标不存在")
    item_count = db.query(sa_func.count(AssessmentItem.id)).filter(AssessmentItem.objective_id == objective_id).scalar()
    if item_count > 0:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="该目标下存在考核点，请先删除考核点")
    weight_count = db.query(sa_func.count(InternalWeightMatrix.id)).filter(InternalWeightMatrix.objective_id == objective_id).scalar()
    if weight_count > 0:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="该目标存在内部权重配置，请先清除")
    db.delete(objective)
    db.commit()
    return True

def get_internal_weights(db: Session, course_id: str) -> List[InternalWeightMatrix]:
    return (
        db.query(InternalWeightMatrix)
        .join(CourseObjective, CourseObjective.id == InternalWeightMatrix.objective_id)
        .filter(CourseObjective.course_id == course_id)
        .all()
    )

def submit_internal_weights(db: Session, course_id: str, entries: List[InternalWeightEntry]) -> List[InternalWeightMatrix]:
    from app.config import settings
    if not entries:
        db.query(InternalWeightMatrix).join(
            CourseObjective, CourseObjective.id == InternalWeightMatrix.objective_id
        ).filter(CourseObjective.course_id == course_id).delete(synchronize_session=False)
        db.commit()
        return []

    indicator_sums = {}
    objective_ids = set()
    for entry in entries:
        ind_id = entry.indicator_id
        weight = float(entry.weight_w)
        indicator_sums[ind_id] = indicator_sums.get(ind_id, 0.0) + weight
        objective_ids.add(entry.objective_id)

    tolerance = settings.WEIGHT_TOLERANCE
    for ind_id, total in indicator_sums.items():
        if abs(total - 1.0) > tolerance:
            from app.models.graduation import GraduationIndicator
            indicator = db.query(GraduationIndicator).filter(GraduationIndicator.id == ind_id).first()
            ind_code = indicator.code if indicator else ind_id
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"指标点 {ind_code} 的内部贡献权重之和为 {total:.4f}，不等于 1.0（容差 ±{tolerance}）"
            )

    course_objective_ids = set(r[0] for r in db.query(CourseObjective.id).filter(CourseObjective.course_id == course_id).all())
    for entry in entries:
        if entry.objective_id not in course_objective_ids:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail=f"课程目标 {entry.objective_id} 不属于课程 {course_id}")

    db.query(InternalWeightMatrix).filter(InternalWeightMatrix.objective_id.in_(objective_ids)).delete(synchronize_session=False)
    for entry in entries:
        weight = float(entry.weight_w)
        if weight > 0:
            wm = InternalWeightMatrix(objective_id=entry.objective_id, indicator_id=entry.indicator_id, weight_w=weight)
            db.add(wm)
    db.commit()
    return db.query(InternalWeightMatrix).filter(InternalWeightMatrix.objective_id.in_(objective_ids)).all()

def list_assessment_items(db: Session, course_id: Optional[str] = None) -> List[AssessmentItem]:
    query = db.query(AssessmentItem)
    if course_id:
        query = query.filter(AssessmentItem.course_id == course_id)
    return query.order_by(AssessmentItem.name).all()

def create_assessment_item(db: Session, data: AssessmentItemCreate) -> AssessmentItem:
    item = AssessmentItem(
        course_id=data.course_id, objective_id=data.objective_id, name=data.name, max_score=data.max_score,
    )
    db.add(item)
    db.commit()
    db.refresh(item)
    return item

def delete_assessment_item(db: Session, item_id: str) -> bool:
    item = db.query(AssessmentItem).filter(AssessmentItem.id == item_id).first()
    if not item:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="考核点不存在")
    score_count = db.query(sa_func.count(StudentScore.id)).filter(StudentScore.assessment_item_id == item_id).scalar()
    if score_count > 0:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="该考核点下已存在学生成绩记录，无法删除")
    db.delete(item)
    db.commit()
    return True

def get_score_matrix(db: Session, course_id: str, class_id: str) -> dict:
    """成绩矩阵聚合查询（学生 × 考核点）"""
    items = db.query(AssessmentItem).filter(AssessmentItem.course_id == course_id).all()
    item_map = {item.id: item for item in items}
    class_students = (
        db.query(ClassStudent, Student)
        .join(Student, Student.id == ClassStudent.student_id)
        .filter(ClassStudent.class_id == class_id)
        .order_by(Student.student_id)
        .all()
    )
    cs_ids = [cs[0].id for cs in class_students]
    scores = db.query(StudentScore).filter(
        StudentScore.assessment_item_id.in_(item_map.keys()),
        StudentScore.class_student_id.in_(cs_ids),
    ).all()
    score_map = {}
    for s in scores:
        score_map[(s.class_student_id, s.assessment_item_id)] = float(s.score)
    students_data = []
    for cs, student in class_students:
        row = {"student_id": student.student_id, "student_name": student.name, "scores": {}}
        for item_id in item_map:
            key = (cs.id, item_id)
            if key in score_map:
                row["scores"][item_id] = score_map[key]
        students_data.append(row)
    items_data = [{"id": item.id, "name": item.name, "max_score": float(item.max_score), "objective_id": item.objective_id} for item in items]
    return {"students": students_data, "assessment_items": items_data}

def is_course_locked(db: Session, course_id: str, class_id: str) -> bool:
    locked = db.query(sa_func.count(CalculationResult.id)).filter(
        CalculationResult.course_id == course_id,
        CalculationResult.class_id == class_id,
        CalculationResult.level == "course",
        CalculationResult.locked == "1",
    ).scalar() > 0
    return locked

def verify_teacher_ownership(db: Session, user_id: str, class_id: str) -> bool:
    cls = db.query(Class).filter(Class.id == class_id).first()
    if not cls:
        return False
    return cls.teacher_id == user_id

def verify_teacher_course(db: Session, user_id: str, course_id: str) -> bool:
    """验证教师是否任教该课程"""
    count = db.query(sa_func.count(Class.id)).filter(
        Class.course_id == course_id, Class.teacher_id == user_id,
    ).scalar()
    return count > 0
