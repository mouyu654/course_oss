"""教务管理路由"""
from typing import Optional, List
from fastapi import APIRouter, Depends, Query, UploadFile, File
from sqlalchemy.orm import Session
import pandas as pd
import io
from app.database import get_db
from app.dependencies.auth import get_current_user, require_academic, require_director_or_academic, require_any
from app.services import academic_service
from app.models.user import User
from app.schemas.academic import (
    CourseCreate, CourseResponse, ClassCreate, ClassResponse, ClassWithCourseResponse,
    StudentImportResult, StudentResponse, CalculationProgressResponse, UnlockRequest
)
from app.schemas.common import MessageResponse

router = APIRouter()

@router.post("/courses", response_model=CourseResponse)
async def create_course(data: CourseCreate, db: Session = Depends(get_db), current_user: User = Depends(require_academic)):
    return academic_service.create_course(db, data)

@router.post("/courses/import")
async def import_courses(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_academic),
):
    content = await file.read()
    df = pd.read_excel(io.BytesIO(content))
    imported = 0
    errors = []
    for idx, row in df.iterrows():
        try:
            data = CourseCreate(
                code=str(row.get("code", "")).strip(),
                name=str(row.get("name", "")).strip(),
                credit=float(row.get("credit", 0)),
                hours_theory=int(row.get("hours_theory", 0)),
                hours_lab=int(row.get("hours_lab", 0)),
                college=str(row.get("college", "")).strip(),
                major=str(row.get("major", "")).strip(),
                academic_year=str(row.get("academic_year", "")).strip(),
                semester=str(row.get("semester", "")).strip(),
            )
            academic_service.create_course(db, data)
            imported += 1
        except Exception as e:
            errors.append(f"第{idx + 2}行: {str(e)}")
    return {"imported": imported, "errors": errors}

@router.get("/courses", response_model=List[CourseResponse])
async def list_courses(
    major: Optional[str] = Query(None),
    academic_year: Optional[str] = Query(None),
    semester: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_any),
):
    return academic_service.list_courses(db, major=major, academic_year=academic_year, semester=semester)

@router.delete("/courses/{course_id}", response_model=MessageResponse)
async def delete_course(
    course_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_academic),
):
    academic_service.delete_course(db, course_id)
    return {"message": "删除成功"}

@router.post("/classes", response_model=ClassResponse)
async def create_class(data: ClassCreate, db: Session = Depends(get_db), current_user: User = Depends(require_academic)):
    return academic_service.create_class(db, data)

@router.get("/classes", response_model=List[ClassWithCourseResponse])
async def list_classes(
    course_id: Optional[str] = Query(None),
    teacher_id: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_any),
):
    results = academic_service.get_class_with_course(db, class_id=None if not course_id and not teacher_id else None)
    if course_id or teacher_id:
        if course_id:
            results = academic_service.get_class_with_course(db)
            results = [(c, co, t) for c, co, t in results if c.course_id == course_id]
        if teacher_id:
            results = academic_service.get_class_with_course(db)
            results = [(c, co, t) for c, co, t in results if c.teacher_id == teacher_id]
    return [
        ClassWithCourseResponse(
            id=c.id, course_id=c.course_id, teacher_id=c.teacher_id, name=c.name,
            academic_year=c.academic_year, semester=c.semester,
            course_code=co.code if co else None, course_name=co.name if co else None,
        )
        for c, co, t in results
    ]

@router.post("/students/import", response_model=StudentImportResult)
async def import_students(
    class_id: str = Query(...),
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_academic),
):
    content = await file.read()
    df = pd.read_excel(io.BytesIO(content))
    students_data = []
    for _, row in df.iterrows():
        students_data.append({
            "student_id": str(row.get("student_id", "")),
            "name": str(row.get("name", "")),
            "college": str(row.get("college", "")) if pd.notna(row.get("college")) else None,
            "major": str(row.get("major", "")) if pd.notna(row.get("major")) else None,
            "grade": str(row.get("grade", "")) if pd.notna(row.get("grade")) else None,
        })
    result = academic_service.import_students(db, class_id, students_data)
    return result

@router.get("/students", response_model=List[StudentResponse])
async def get_class_students(
    class_id: str = Query(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_any),
):
    return academic_service.get_class_students(db, class_id)

@router.get("/calculation/progress", response_model=CalculationProgressResponse)
async def get_progress(
    academic_year: Optional[str] = Query(None),
    semester: Optional[str] = Query(None),
    major: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_academic),
):
    return academic_service.get_calculation_progress(db, academic_year=academic_year, semester=semester, major=major)

@router.post("/calculate/program")
async def calculate_program(
    academic_year: str = Query(...),
    semester: str = Query(...),
    major: Optional[str] = Query(None),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_director_or_academic),
):
    from app.services import calculation_engine
    return calculation_engine.calculate_program(db, academic_year, semester, major)

@router.post("/unlock", response_model=MessageResponse)
async def unlock_course(
    data: UnlockRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_academic),
):
    result = academic_service.unlock_course(db, current_user.id, data)
    return {"message": result["message"]}

@router.get("/results/program")
async def get_program_results(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_director_or_academic),
):
    return academic_service.get_program_results(db)

@router.get("/reports/program-excel")
async def export_program_report(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_director_or_academic),
):
    from fastapi.responses import StreamingResponse
    from app.utils.excel_utils import generate_program_report_excel
    from app.models.result import CalculationResult
    from app.models.matrix import MacroSupportMatrix
    from app.models.course import Course
    from app.models.graduation import GraduationIndicator
    from app.services import academic_service

    program_results = academic_service.get_program_results(db)
    if not program_results:
        from fastapi import HTTPException, status
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="尚无专业级计算结果")

    # 获取每个指标点的支撑课程明细
    indicators = {ind.id: ind for ind in db.query(GraduationIndicator).all()}
    indicator_detail = {}
    for ind_id in program_results:
        results = db.query(CalculationResult, Course).join(
            Course, Course.id == CalculationResult.course_id
        ).filter(
            CalculationResult.level == "course",
            CalculationResult.locked == "1",
            CalculationResult.indicator_id == ind_id,
        ).all()
        weights = db.query(MacroSupportMatrix).filter(MacroSupportMatrix.indicator_id == ind_id).all()
        weight_map = {w.course_id: float(w.weight_W) for w in weights}
        detail = []
        for result, course in results:
            e_k = float(result.achievement_value)
            w_val = weight_map.get(result.course_id, 0.0)
            detail.append({
                "course_name": f"{course.code} - {course.name}",
                "E_k": e_k,
                "W": w_val,
                "contribution": round(e_k * w_val, 4),
            })
        indicator_detail[ind_id] = detail

    excel_bytes = generate_program_report_excel(program_results, indicator_detail)
    return StreamingResponse(
        io.BytesIO(excel_bytes),
        media_type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        headers={"Content-Disposition": "attachment; filename=专业达成度台账.xlsx"},
    )
