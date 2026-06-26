"""教师端路由"""
from typing import Optional, List
from fastapi import APIRouter, Depends, Query, UploadFile, File
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
import io
from app.database import get_db
from app.dependencies.auth import get_current_user, require_teacher, require_admin, require_any
from app.services import teacher_service
from app.models.user import User
from app.schemas.teacher import (
    ObjectiveCreate, ObjectiveResponse, InternalWeightEntry, InternalWeightResponse,
    AssessmentItemCreate, AssessmentItemResponse, ScoreMatrixResponse,
    ScoreMatrixItem, ScoreImportResult, CalculateResponse,
    ObjectiveCalcResult, CourseCalcResult,
)
from app.schemas.common import MessageResponse

router = APIRouter()

def check_teacher_access(db: Session, user: User, course_id: str, class_id: str = None):
    """检查教师是否有权操作该课程/班级"""
    if user.role == "admin":
        return
    if class_id:
        if not teacher_service.verify_teacher_ownership(db, user.id, class_id):
            from fastapi import HTTPException, status
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="您无权操作该教学班")
    else:
        if not teacher_service.verify_teacher_course(db, user.id, course_id):
            from fastapi import HTTPException, status
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="您无权操作该课程")

@router.get("/objectives", response_model=List[ObjectiveResponse])
async def list_objectives(
    course_id: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_any),
):
    return teacher_service.list_objectives(db, course_id=course_id)

@router.post("/objectives", response_model=ObjectiveResponse)
async def create_objective(
    data: ObjectiveCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_teacher),
):
    check_teacher_access(db, current_user, data.course_id)
    return teacher_service.create_objective(db, data)

@router.delete("/objectives/{objective_id}", response_model=MessageResponse)
async def delete_objective(
    objective_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_teacher),
):
    from app.models.objective import CourseObjective
    objective = db.query(CourseObjective).filter(CourseObjective.id == objective_id).first()
    if objective:
        check_teacher_access(db, current_user, objective.course_id)
    teacher_service.delete_objective(db, objective_id)
    return {"message": "删除成功"}

@router.get("/internal-weights", response_model=List[InternalWeightResponse])
async def get_internal_weights(
    course_id: str = Query(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_any),
):
    return teacher_service.get_internal_weights(db, course_id)

@router.post("/internal-weights", response_model=List[InternalWeightResponse])
async def submit_internal_weights(
    course_id: str = Query(...),
    entries: List[InternalWeightEntry] = ...,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_teacher),
):
    check_teacher_access(db, current_user, course_id)
    return teacher_service.submit_internal_weights(db, course_id, entries)

@router.get("/assessment-items", response_model=List[AssessmentItemResponse])
async def list_assessment_items(
    course_id: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_any),
):
    return teacher_service.list_assessment_items(db, course_id=course_id)

@router.post("/assessment-items", response_model=AssessmentItemResponse)
async def create_assessment_item(
    data: AssessmentItemCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_teacher),
):
    check_teacher_access(db, current_user, data.course_id)
    return teacher_service.create_assessment_item(db, data)

@router.delete("/assessment-items/{item_id}", response_model=MessageResponse)
async def delete_assessment_item(
    item_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_teacher),
):
    from app.models.assessment import AssessmentItem
    item = db.query(AssessmentItem).filter(AssessmentItem.id == item_id).first()
    if item:
        check_teacher_access(db, current_user, item.course_id)
    teacher_service.delete_assessment_item(db, item_id)
    return {"message": "删除成功"}

@router.get("/assessment-items/{course_id}/export-template")
async def export_score_template(
    course_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_teacher),
):
    check_teacher_access(db, current_user, course_id)
    from app.utils.excel_utils import generate_score_template
    from app.models.student import Student
    from app.models.class_student import ClassStudent
    from app.models.class_ import Class
    from app.models.objective import CourseObjective

    items = teacher_service.list_assessment_items(db, course_id=course_id)
    if not items:
        from fastapi import HTTPException, status
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="请先配置考核点")

    objectives = {obj.id: obj for obj in teacher_service.list_objectives(db, course_id=course_id)}
    items_data = [
        {"name": item.name, "max_score": float(item.max_score), "objective_code": objectives.get(item.objective_id).code if objectives.get(item.objective_id) else ""}
        for item in items
    ]

    class_students = (
        db.query(ClassStudent, Student)
        .join(Student, Student.id == ClassStudent.student_id)
        .join(Class, Class.id == ClassStudent.class_id)
        .filter(Class.course_id == course_id)
        .order_by(Student.student_id)
        .all()
    )
    students_data = [{"student_id": s.student_id, "name": s.name} for _, s in class_students]

    template_bytes = generate_score_template(items_data, students_data)
    return StreamingResponse(
        io.BytesIO(template_bytes),
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": f"attachment; filename=成绩模板_{course_id}.xlsx"},
    )

@router.post("/scores/import", response_model=ScoreImportResult)
async def import_scores(
    course_id: str = Query(...),
    class_id: str = Query(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_teacher),
):
    check_teacher_access(db, current_user, course_id, class_id)
    if teacher_service.is_course_locked(db, course_id, class_id):
        from fastapi import HTTPException, status
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="该课程成绩已锁定，请联系教务管理员解锁")

    from app.utils.excel_utils import parse_score_excel
    items = teacher_service.list_assessment_items(db, course_id=course_id)
    items_data = [{"name": item.name, "id": item.id, "max_score": float(item.max_score)} for item in items]

    content = await file.read()
    scores, errors = parse_score_excel(content, items_data)

    if errors:
        return ScoreImportResult(count=0, errors=errors)

    from app.models.class_student import ClassStudent
    from app.models.student import Student

    cs_map = {}
    for cs, student in db.query(ClassStudent, Student).join(Student, Student.id == ClassStudent.student_id).filter(ClassStudent.class_id == class_id).all():
        cs_map[student.student_id] = cs.id

    count = 0
    for score_entry in scores:
        student_id = score_entry["student_id"]
        item_id = score_entry["item_id"]
        score_val = score_entry["score"]
        cs_id = cs_map.get(student_id)
        if not cs_id:
            errors.append(f"学生 {student_id} 不在班级中")
            continue
        existing = db.query(StudentScore).filter(
            StudentScore.class_student_id == cs_id, StudentScore.assessment_item_id == item_id
        ).first()
        if existing:
            existing.score = score_val
        else:
            new_score = StudentScore(class_student_id=cs_id, assessment_item_id=item_id, score=score_val)
            db.add(new_score)
        count += 1

    db.commit()
    return ScoreImportResult(count=count, errors=errors)

@router.get("/scores/matrix", response_model=ScoreMatrixResponse)
async def get_score_matrix(
    course_id: str = Query(...),
    class_id: str = Query(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_teacher),
):
    check_teacher_access(db, current_user, course_id, class_id)
    matrix_data = teacher_service.get_score_matrix(db, course_id, class_id)
    students = [
        ScoreMatrixItem(student_id=s["student_id"], student_name=s["student_name"], scores=s["scores"])
        for s in matrix_data["students"]
    ]
    items = [
        AssessmentItemResponse(id=item["id"], course_id=course_id, objective_id=item["objective_id"], name=item["name"], max_score=item["max_score"])
        for item in matrix_data["assessment_items"]
    ]
    return ScoreMatrixResponse(students=students, assessment_items=items)

@router.post("/calculate", response_model=CalculateResponse)
async def calculate_course(
    course_id: str = Query(...),
    class_id: str = Query(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_teacher),
):
    check_teacher_access(db, current_user, course_id, class_id)
    if teacher_service.is_course_locked(db, course_id, class_id):
        from fastapi import HTTPException, status
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="该课程已计算并锁定，如需修改请联系教务管理员解锁")
    from app.services import calculation_engine
    result = calculation_engine.calculate_tier1_tier2(db, course_id, class_id)
    return CalculateResponse(
        objective_results=[ObjectiveCalcResult(**r) for r in result["objective_results"]],
        course_results=[CourseCalcResult(**r) for r in result["course_results"]],
    )

@router.get("/results")
async def export_course_report(
    course_id: str = Query(...),
    class_id: str = Query(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_teacher),
):
    check_teacher_access(db, current_user, course_id, class_id)
    from app.utils.excel_utils import generate_course_report_excel
    from app.models.result import CalculationResult
    from app.models.objective import CourseObjective
    from app.models.class_student import ClassStudent
    from app.models.student import Student

    objective_results = db.query(CalculationResult, CourseObjective).join(
        CourseObjective, CourseObjective.id == CalculationResult.objective_id, isouter=True
    ).filter(
        CalculationResult.course_id == course_id,
        CalculationResult.class_id == class_id,
        CalculationResult.level == "objective",
    ).all()

    course_results = db.query(CalculationResult).filter(
        CalculationResult.course_id == course_id,
        CalculationResult.class_id == class_id,
        CalculationResult.level == "course",
    ).all()

    students = (
        db.query(ClassStudent, Student)
        .join(Student, Student.id == ClassStudent.student_id)
        .filter(ClassStudent.class_id == class_id)
        .order_by(Student.student_id)
        .all()
    )

    obj_results_list = [{"objective_code": r[1].code if r[1] else "", "C_bar": float(r[0].achievement_value)} for r in objective_results]
    course_results_list = [{"indicator_code": r.indicator_id or "", "E_k": float(r.achievement_value)} for r in course_results]

    student_scores = []
    for cs, student in students:
        student_scores.append({"student_id": student.student_id, "name": student.name})

    excel_bytes = generate_course_report_excel(obj_results_list, course_results_list, student_scores)
    return StreamingResponse(
        io.BytesIO(excel_bytes),
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": f"attachment; filename=课程达成度报告_{course_id}_{class_id}.xlsx"},
    )
