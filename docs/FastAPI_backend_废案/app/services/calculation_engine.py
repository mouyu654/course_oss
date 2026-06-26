"""三级达成度计算引擎"""
import pandas as pd
import numpy as np
from sqlalchemy.orm import Session
from sqlalchemy import func as sa_func
from fastapi import HTTPException, status
from app.models.assessment import AssessmentItem
from app.models.objective import CourseObjective
from app.models.score import StudentScore
from app.models.class_student import ClassStudent
from app.models.student import Student
from app.models.matrix import InternalWeightMatrix
from app.models.result import CalculationResult
from app.models.macro_support_matrix import MacroSupportMatrix
from app.models.graduation import GraduationIndicator
from app.models.course import Course
from app.models.class_ import Class

def calculate_tier1_tier2(db: Session, course_id: str, class_id: str) -> dict:
    """
    Tier 1: 课程目标级达成度 C_bar
    Tier 2: 课程级指标点达成度 E_k = Sum(C_bar_j * w_jk)
    """
    assessment_items = db.query(AssessmentItem).filter(AssessmentItem.course_id == course_id).all()
    if not assessment_items:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="该课程尚未配置考核点")
    item_map = {item.id: item for item in assessment_items}

    class_students = (
        db.query(ClassStudent).join(Student, Student.id == ClassStudent.student_id)
        .filter(ClassStudent.class_id == class_id).all()
    )
    if not class_students:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="该班级尚未导入学生名单")
    cs_map = {cs.id: cs for cs in class_students}

    scores = db.query(StudentScore).filter(
        StudentScore.class_student_id.in_(cs_map.keys()),
        StudentScore.assessment_item_id.in_(item_map.keys()),
    ).all()
    if not scores:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="该班级尚未导入成绩数据")

    # Build DataFrame for Tier 1
    data = []
    for s in scores:
        item = item_map[s.assessment_item_id]
        data.append({
            "student_id": s.class_student_id,
            "objective_id": item.objective_id,
            "score": float(s.score),
            "max_score": float(item.max_score),
        })
    df = pd.DataFrame(data)
    grouped = df.groupby(["student_id", "objective_id"]).agg(score_sum=("score", "sum"), max_sum=("max_score", "sum")).reset_index()
    grouped["C_ij"] = grouped["score_sum"] / grouped["max_sum"].replace(0, np.nan)

    # Per-objective average
    objective_results = grouped.groupby("objective_id")["C_ij"].mean().reset_index()
    objective_results.rename(columns={"C_ij": "C_bar"}, inplace=True)

    # Tier 2: Internal weights
    objective_ids = [obj.id for obj in db.query(CourseObjective).filter(CourseObjective.course_id == course_id).all()]
    internal_weights = db.query(InternalWeightMatrix).filter(InternalWeightMatrix.objective_id.in_(objective_ids)).all()
    if not internal_weights:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="该课程尚未配置内部权重矩阵")

    weight_map = {}
    for w in internal_weights:
        weight_map[(w.objective_id, w.indicator_id)] = float(w.weight_w)

    indicator_ids = set(ind_id for (_, ind_id) in weight_map.keys())
    course_results = {}
    for ind_id in indicator_ids:
        e_k = 0.0
        for _, row in objective_results.iterrows():
            c_bar = float(row["C_bar"])
            w_val = weight_map.get((row["objective_id"], ind_id), 0.0)
            e_k += c_bar * w_val
        course_results[ind_id] = round(e_k, 4)

    # Persist results (delete old, insert new)
    try:
        db.query(CalculationResult).filter(
            CalculationResult.course_id == course_id, CalculationResult.class_id == class_id,
        ).delete(synchronize_session=False)

        objectives = {obj.id: obj for obj in db.query(CourseObjective).filter(CourseObjective.course_id == course_id).all()}
        for _, row in objective_results.iterrows():
            result = CalculationResult(
                course_id=course_id, class_id=class_id, level="objective",
                objective_id=row["objective_id"], achievement_value=round(float(row["C_bar"]), 4), locked="1",
            )
            db.add(result)

        for ind_id, e_k in course_results.items():
            result = CalculationResult(
                course_id=course_id, class_id=class_id, level="course",
                indicator_id=ind_id, achievement_value=e_k, locked="1",
            )
            db.add(result)

        db.commit()
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"计算结果持久化失败: {str(e)}")

    # Build response
    objective_results_list = []
    for _, row in objective_results.iterrows():
        obj = objectives.get(row["objective_id"])
        objective_results_list.append({
            "objective_id": row["objective_id"],
            "objective_code": obj.code if obj else "",
            "C_bar": round(float(row["C_bar"]), 4),
        })

    indicators = {ind.id: ind for ind in db.query(GraduationIndicator).all()}
    course_results_list = []
    for ind_id, e_k in course_results.items():
        ind = indicators.get(ind_id)
        course_results_list.append({
            "indicator_id": ind_id, "indicator_code": ind.code if ind else "", "E_k": e_k,
        })

    return {"objective_results": objective_results_list, "course_results": course_results_list}

def calculate_program(db: Session, academic_year: str, semester: str, major: str = None) -> dict:
    """
    Tier 3: 专业级指标点达成度 G_k = Sum(E_k * W)
    """
    course_query = db.query(Course.id).filter(
        Course.academic_year == academic_year, Course.semester == semester, Course.is_active == 1,
    )
    if major:
        course_query = course_query.filter(Course.major == major)
    course_ids = [r[0] for r in course_query.all()]
    if not course_ids:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="未找到匹配的课程")

    class_ids = [r[0] for r in db.query(Class.id).filter(Class.course_id.in_(course_ids)).all()]

    # Pre-check: all classes must be locked
    for cid in class_ids:
        locked = db.query(sa_func.count(CalculationResult.id)).filter(
            CalculationResult.class_id == cid, CalculationResult.level == "course", CalculationResult.locked == "1",
        ).scalar()
        if locked == 0:
            cls = db.query(Class).filter(Class.id == cid).first()
            course = db.query(Course).filter(Course.id == cls.course_id).first() if cls else None
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail=f"班级 {cls.name if cls else cid}（课程 {course.name if course else ''}）尚未完成计算"
            )

    course_results = db.query(CalculationResult).filter(
        CalculationResult.class_id.in_(class_ids), CalculationResult.level == "course", CalculationResult.locked == "1",
    ).all()
    if not course_results:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="未找到已锁定的课程级计算结果")

    macro_weights = db.query(MacroSupportMatrix).filter(MacroSupportMatrix.course_id.in_(course_ids)).all()
    weight_map = {}
    for w in macro_weights:
        weight_map[(w.course_id, w.indicator_id)] = float(w.weight_W)

    # G_k = Sum(E_k * W) per indicator
    indicator_sums = {}
    for result in course_results:
        ind_id = result.indicator_id
        e_k = float(result.achievement_value)
        w_val = weight_map.get((result.course_id, ind_id), 0.0)
        contribution = e_k * w_val
        indicator_sums[ind_id] = indicator_sums.get(ind_id, 0.0) + contribution

    # Persist
    try:
        db.query(CalculationResult).filter(CalculationResult.level == "program").delete(synchronize_session=False)
        for ind_id, g_k in indicator_sums.items():
            result = CalculationResult(
                course_id=None, class_id=None, level="program",
                indicator_id=ind_id, achievement_value=round(g_k, 4), locked="1",
            )
            db.add(result)
        db.commit()
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=f"专业级计算结果持久化失败: {str(e)}")

    indicators = {ind.id: ind for ind in db.query(GraduationIndicator).all()}
    program_results = {}
    for ind_id, g_k in indicator_sums.items():
        ind = indicators.get(ind_id)
        program_results[ind_id] = {
            "indicator_code": ind.code if ind else "",
            "indicator_description": ind.description if ind else "",
            "G_k": round(g_k, 4),
        }
    return {"program_results": program_results}
