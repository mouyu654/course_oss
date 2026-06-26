"""教务管理服务"""
from typing import List, Optional
from sqlalchemy.orm import Session
from sqlalchemy import func as sa_func
from fastapi import HTTPException, status
from app.models.course import Course
from app.models.class_ import Class
from app.models.student import Student
from app.models.class_student import ClassStudent
from app.models.result import CalculationResult
from app.models.matrix import MacroSupportMatrix
from app.models.unlock_log import UnlockLog
from app.models.user import User
from app.schemas.academic import CourseCreate, ClassCreate, CalculationProgressItem, CalculationProgressResponse, UnlockRequest

def create_course(db: Session, data: CourseCreate) -> Course:
    existing = db.query(Course).filter(Course.code == data.code).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="课程代码已存在")
    course = Course(
        code=data.code, name=data.name, credit=data.credit,
        hours_theory=data.hours_theory, hours_lab=data.hours_lab,
        college=data.college, major=data.major,
        academic_year=data.academic_year, semester=data.semester,
    )
    db.add(course)
    db.commit()
    db.refresh(course)
    return course

def list_courses(db: Session, major: Optional[str] = None, academic_year: Optional[str] = None, semester: Optional[str] = None) -> List[Course]:
    query = db.query(Course).filter(Course.is_active == 1)
    if major:
        query = query.filter(Course.major == major)
    if academic_year:
        query = query.filter(Course.academic_year == academic_year)
    if semester:
        query = query.filter(Course.semester == semester)
    return query.order_by(Course.code).all()

def delete_course(db: Session, course_id: str) -> bool:
    course = db.query(Course).filter(Course.id == course_id).first()
    if not course:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="课程不存在")
    class_count = db.query(sa_func.count(Class.id)).filter(Class.course_id == course_id).scalar()
    if class_count > 0:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="该课程下存在教学班，请先删除教学班")
    matrix_count = db.query(sa_func.count(MacroSupportMatrix.id)).filter(MacroSupportMatrix.course_id == course_id).scalar()
    if matrix_count > 0:
        raise HTTPException(status_code=status.HTTP_409_CONFLICT, detail="该课程存在宏观支撑矩阵配置，请先清除")
    course.is_active = 0
    db.commit()
    return True

def create_class(db: Session, data: ClassCreate) -> Class:
    cls = Class(
        course_id=data.course_id, teacher_id=data.teacher_id,
        name=data.name, academic_year=data.academic_year, semester=data.semester,
    )
    db.add(cls)
    db.commit()
    db.refresh(cls)
    return cls

def list_classes(db: Session, course_id: Optional[str] = None, teacher_id: Optional[str] = None) -> List[Class]:
    query = db.query(Class)
    if course_id:
        query = query.filter(Class.course_id == course_id)
    if teacher_id:
        query = query.filter(Class.teacher_id == teacher_id)
    return query.order_by(Class.created_at.desc()).all()

def get_class_with_course(db: Session, class_id: Optional[str] = None) -> List[tuple]:
    """查询教学班及其关联课程信息"""
    query = db.query(Class, Course, User).join(Course, Course.id == Class.course_id).join(User, User.id == Class.teacher_id)
    if class_id:
        query = query.filter(Class.id == class_id)
    return query.order_by(Class.created_at.desc()).all()

def import_students(db: Session, class_id: str, students_data: List[dict]) -> dict:
    """Excel 批量导入学生名单（Upsert 模式）"""
    inserted = 0
    updated = 0
    errors = []
    class_students_to_add = []

    for idx, row in enumerate(students_data, start=2):
        try:
            stu_id_val = str(row.get("student_id", "")).strip()
            stu_name = str(row.get("name", "")).strip()
            if not stu_id_val or not stu_name:
                errors.append(f"第{idx}行: 学号或姓名为空")
                continue
            student = db.query(Student).filter(Student.student_id == stu_id_val).first()
            if student:
                if student.name != stu_name:
                    student.name = stu_name
                    updated += 1
            else:
                student = Student(
                    student_id=stu_id_val, name=stu_name,
                    college=row.get("college"), major=row.get("major"), grade=row.get("grade"),
                )
                db.add(student)
                db.flush()
                inserted += 1
            # 检查选课关系是否已存在
            existing_cs = db.query(ClassStudent).filter(
                ClassStudent.class_id == class_id, ClassStudent.student_id == student.id
            ).first()
            if not existing_cs:
                class_students_to_add.append(ClassStudent(class_id=class_id, student_id=student.id))
        except Exception as e:
            errors.append(f"第{idx}行: {str(e)}")

    if errors:
        db.rollback()
        return {"inserted": 0, "updated": 0, "errors": errors}

    for cs in class_students_to_add:
        db.add(cs)
    db.commit()
    return {"inserted": inserted, "updated": updated, "errors": []}

def get_class_students(db: Session, class_id: str) -> List[Student]:
    return (
        db.query(Student)
        .join(ClassStudent, ClassStudent.student_id == Student.id)
        .filter(ClassStudent.class_id == class_id)
        .order_by(Student.student_id)
        .all()
    )

def get_calculation_progress(db: Session, academic_year: Optional[str] = None, semester: Optional[str] = None, major: Optional[str] = None) -> CalculationProgressResponse:
    query = db.query(Class, Course, User).join(Course, Course.id == Class.course_id).join(User, User.id == Class.teacher_id)
    if academic_year:
        query = query.filter(Class.academic_year == academic_year)
    if semester:
        query = query.filter(Class.semester == semester)
    if major:
        query = query.filter(Course.major == major)
    classes = query.order_by(Class.academic_year.desc(), Class.semester.desc()).all()
    items = []
    for cls, course, teacher in classes:
        locked_count = db.query(sa_func.count(CalculationResult.id)).filter(
            CalculationResult.class_id == cls.id,
            CalculationResult.level == "course",
            CalculationResult.locked == "1",
        ).scalar()
        items.append(CalculationProgressItem(
            class_id=cls.id, class_name=cls.name,
            course_code=course.code, course_name=course.name,
            teacher_name=teacher.display_name,
            status="locked" if locked_count > 0 else "pending",
        ))
    all_locked = all(item.status == "locked" for item in items) if items else False
    return CalculationProgressResponse(items=items, all_locked=all_locked)

def unlock_course(db: Session, current_user_id: str, data: UnlockRequest) -> dict:
    db.query(CalculationResult).filter(
        CalculationResult.course_id == data.course_id,
        CalculationResult.class_id == data.class_id,
    ).update({"locked": "0"}, synchronize_session=False)
    log = UnlockLog(course_id=data.course_id, class_id=data.class_id, operator_id=current_user_id, reason=data.reason)
    db.add(log)
    db.commit()
    return {"message": "解锁成功", "reason": data.reason}

def get_program_results(db: Session) -> dict:
    """获取专业级计算结果用于雷达图"""
    results = db.query(CalculationResult).filter(
        CalculationResult.level == "program", CalculationResult.locked == "1"
    ).all()
    from app.models.graduation import GraduationIndicator
    indicators = {ind.id: ind for ind in db.query(GraduationIndicator).all()}
    data = {}
    for r in results:
        ind = indicators.get(r.indicator_id)
        data[r.indicator_id] = {
            "indicator_code": ind.code if ind else "",
            "indicator_description": ind.description if ind else "",
            "achievement_value": float(r.achievement_value),
        }
    return data
