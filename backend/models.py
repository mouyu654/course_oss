"""SQLAlchemy ORM models —— achievement_calc 全部表"""

import uuid
from datetime import datetime

from sqlalchemy import (
    Column, String, Text, Integer, SmallInteger, Float,
    DECIMAL, Enum, Boolean, DateTime, ForeignKey, UniqueConstraint, Index,
)
from sqlalchemy.orm import relationship

from db.base import Base


def gen_uuid() -> str:
    return uuid.uuid4().hex


# ── 1. users ────────────────────────────────────────────

class User(Base):
    __tablename__ = "users"

    id            = Column(String(36), primary_key=True, default=gen_uuid)
    username      = Column(String(50), nullable=False, unique=True)
    password_hash = Column(String(255), nullable=False)
    role          = Column(Enum("admin","academic","director","teacher", name="role_enum"), nullable=False)
    display_name  = Column(String(50), nullable=False)
    college       = Column(String(100), nullable=True)
    major         = Column(String(100), nullable=True)
    is_active     = Column(Boolean, nullable=False, default=True)
    created_at    = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at    = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    classes_teaching = relationship("Class", back_populates="teacher")
    unlock_logs      = relationship("UnlockLog", back_populates="operator")


# ── 2. global_config ────────────────────────────────────

class GlobalConfig(Base):
    __tablename__ = "global_config"

    id          = Column(String(36), primary_key=True, default=gen_uuid)
    category    = Column(String(50), nullable=False)
    key         = Column(String(50), nullable=False)
    value       = Column(String(255), nullable=False)
    description = Column(String(255), nullable=True)
    is_active   = Column(Boolean, nullable=False, default=True)
    created_at  = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at  = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    __table_args__ = (
        UniqueConstraint("category", "key", name="idx_category_key"),
        Index("idx_category", "category"),
    )


# ── 3. courses ──────────────────────────────────────────

class Course(Base):
    __tablename__ = "courses"

    id            = Column(String(36), primary_key=True, default=gen_uuid)
    code          = Column(String(50), nullable=False, unique=True)
    name          = Column(String(100), nullable=False)
    credit        = Column(DECIMAL(4, 1), nullable=False)
    hours_theory  = Column(SmallInteger, nullable=False, default=0)
    hours_lab     = Column(SmallInteger, nullable=False, default=0)
    college       = Column(String(100), nullable=False)
    major         = Column(String(100), nullable=False)
    academic_year = Column(String(20), nullable=False)
    semester      = Column(String(1), nullable=False)
    is_active     = Column(Boolean, nullable=False, default=True)
    created_at    = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at    = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    classes           = relationship("Class", back_populates="course")
    objectives        = relationship("CourseObjective", back_populates="course")
    assessment_items  = relationship("AssessmentItem", back_populates="course")
    macro_entries      = relationship("MacroSupportMatrix", back_populates="course")
    calculation_results = relationship("CalculationResult", back_populates="course")
    unlock_logs        = relationship("UnlockLog", back_populates="course")


# ── 4. graduation_requirements ──────────────────────────

class GraduationRequirement(Base):
    __tablename__ = "graduation_requirements"

    id          = Column(String(36), primary_key=True, default=gen_uuid)
    code        = Column(String(10), nullable=False, unique=True)
    title       = Column(String(100), nullable=False)
    description = Column(Text, nullable=True)
    created_at  = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at  = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    indicators = relationship("GraduationIndicator", back_populates="requirement")


# ── 5. graduation_indicators ────────────────────────────

class GraduationIndicator(Base):
    __tablename__ = "graduation_indicators"

    id             = Column(String(36), primary_key=True, default=gen_uuid)
    requirement_id = Column(String(36), ForeignKey("graduation_requirements.id", ondelete="CASCADE"), nullable=False)
    code           = Column(String(10), nullable=False, unique=True)
    description    = Column(Text, nullable=False)
    created_at     = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at     = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    requirement           = relationship("GraduationRequirement", back_populates="indicators")
    macro_entries         = relationship("MacroSupportMatrix", back_populates="indicator")
    internal_weight_entries = relationship("InternalWeightMatrix", back_populates="indicator")
    calculation_results   = relationship("CalculationResult", back_populates="indicator")


# ── 6. classes ──────────────────────────────────────────

class Class(Base):
    __tablename__ = "classes"

    id            = Column(String(36), primary_key=True, default=gen_uuid)
    course_id     = Column(String(36), ForeignKey("courses.id", ondelete="CASCADE"), nullable=False)
    teacher_id    = Column(String(36), ForeignKey("users.id", ondelete="RESTRICT"), nullable=False)
    name          = Column(String(100), nullable=False)
    academic_year = Column(String(20), nullable=False)
    semester      = Column(String(1), nullable=False)
    created_at    = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at    = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    course              = relationship("Course", back_populates="classes")
    teacher             = relationship("User", back_populates="classes_teaching")
    class_students      = relationship("ClassStudent", back_populates="class_")
    calculation_results = relationship("CalculationResult", back_populates="class_")
    unlock_logs          = relationship("UnlockLog", back_populates="class_")


# ── 7. students ─────────────────────────────────────────

class Student(Base):
    __tablename__ = "students"

    id         = Column(String(36), primary_key=True, default=gen_uuid)
    student_id = Column(String(50), nullable=False, unique=True)
    name       = Column(String(50), nullable=False)
    college    = Column(String(100), nullable=True)
    major      = Column(String(100), nullable=True)
    grade      = Column(String(20), nullable=True)
    is_active  = Column(Boolean, nullable=False, default=True)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    class_students = relationship("ClassStudent", back_populates="student")


# ── 8. class_students ───────────────────────────────────

class ClassStudent(Base):
    __tablename__ = "class_students"

    id         = Column(String(36), primary_key=True, default=gen_uuid)
    class_id   = Column(String(36), ForeignKey("classes.id", ondelete="CASCADE"), nullable=False)
    student_id = Column(String(36), ForeignKey("students.id", ondelete="CASCADE"), nullable=False)
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    class_  = relationship("Class", back_populates="class_students")
    student = relationship("Student", back_populates="class_students")
    scores  = relationship("StudentScore", back_populates="class_student")

    __table_args__ = (
        UniqueConstraint("class_id", "student_id", name="idx_class_student"),
        Index("idx_student", "student_id"),
    )


# ── 9. macro_support_matrix ─────────────────────────────

class MacroSupportMatrix(Base):
    __tablename__ = "macro_support_matrix"

    id           = Column(String(36), primary_key=True, default=gen_uuid)
    course_id    = Column(String(36), ForeignKey("courses.id", ondelete="CASCADE"), nullable=False)
    indicator_id = Column(String(36), ForeignKey("graduation_indicators.id", ondelete="CASCADE"), nullable=False)
    weight_W     = Column(DECIMAL(5, 4), nullable=False)
    created_at   = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at   = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    course    = relationship("Course", back_populates="macro_entries")
    indicator = relationship("GraduationIndicator", back_populates="macro_entries")

    __table_args__ = (
        UniqueConstraint("course_id", "indicator_id", name="idx_course_indicator"),
        Index("idx_indicator", "indicator_id"),
    )


# ── 10. course_objectives ───────────────────────────────

class CourseObjective(Base):
    __tablename__ = "course_objectives"

    id          = Column(String(36), primary_key=True, default=gen_uuid)
    course_id   = Column(String(36), ForeignKey("courses.id", ondelete="CASCADE"), nullable=False)
    code        = Column(String(10), nullable=False)
    description = Column(Text, nullable=False)
    dimension   = Column(Enum("知识维度", "能力维度", "价值维度", name="dimension_enum"), nullable=False)
    created_at  = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at  = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    course              = relationship("Course", back_populates="objectives")
    internal_weights    = relationship("InternalWeightMatrix", back_populates="objective")
    assessment_items    = relationship("AssessmentItem", back_populates="objective")
    calculation_results = relationship("CalculationResult", back_populates="objective")

    __table_args__ = (
        UniqueConstraint("course_id", "code", name="idx_course_code"),
        Index("idx_course", "course_id"),
    )


# ── 11. internal_weight_matrix ──────────────────────────

class InternalWeightMatrix(Base):
    __tablename__ = "internal_weight_matrix"

    id           = Column(String(36), primary_key=True, default=gen_uuid)
    objective_id  = Column(String(36), ForeignKey("course_objectives.id", ondelete="CASCADE"), nullable=False)
    indicator_id = Column(String(36), ForeignKey("graduation_indicators.id", ondelete="CASCADE"), nullable=False)
    weight_w     = Column(DECIMAL(5, 4), nullable=False)
    created_at   = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at   = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    objective = relationship("CourseObjective", back_populates="internal_weights")
    indicator = relationship("GraduationIndicator", back_populates="internal_weight_entries")

    __table_args__ = (
        UniqueConstraint("objective_id", "indicator_id", name="idx_objective_indicator"),
        Index("idx_internal_indicator", "indicator_id"),
    )


# ── 12. assessment_items ────────────────────────────────

class AssessmentItem(Base):
    __tablename__ = "assessment_items"

    id           = Column(String(36), primary_key=True, default=gen_uuid)
    course_id    = Column(String(36), ForeignKey("courses.id", ondelete="CASCADE"), nullable=False)
    objective_id = Column(String(36), ForeignKey("course_objectives.id", ondelete="CASCADE"), nullable=False)
    name         = Column(String(200), nullable=False)
    max_score    = Column(DECIMAL(6, 2), nullable=False)
    created_at   = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at   = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    course    = relationship("Course", back_populates="assessment_items")
    objective = relationship("CourseObjective", back_populates="assessment_items")
    scores    = relationship("StudentScore", back_populates="assessment_item")

    __table_args__ = (
        Index("idx_item_course", "course_id"),
        Index("idx_item_objective", "objective_id"),
    )


# ── 13. student_scores ──────────────────────────────────

class StudentScore(Base):
    __tablename__ = "student_scores"

    id                  = Column(String(36), primary_key=True, default=gen_uuid)
    class_student_id    = Column(String(36), ForeignKey("class_students.id", ondelete="CASCADE"), nullable=False)
    assessment_item_id  = Column(String(36), ForeignKey("assessment_items.id", ondelete="CASCADE"), nullable=False)
    score               = Column(DECIMAL(6, 2), nullable=False)
    created_at          = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at          = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    class_student   = relationship("ClassStudent", back_populates="scores")
    assessment_item = relationship("AssessmentItem", back_populates="scores")

    __table_args__ = (
        UniqueConstraint("class_student_id", "assessment_item_id", name="idx_class_student_item"),
        Index("idx_score_item", "assessment_item_id"),
    )


# ── 14. calculation_results ─────────────────────────────

class CalculationResult(Base):
    __tablename__ = "calculation_results"

    id                = Column(String(36), primary_key=True, default=gen_uuid)
    course_id         = Column(String(36), ForeignKey("courses.id", ondelete="CASCADE"), nullable=True)
    class_id          = Column(String(36), ForeignKey("classes.id", ondelete="CASCADE"), nullable=True)
    level             = Column(Enum("objective", "course", "program", name="level_enum"), nullable=False)
    objective_id      = Column(String(36), ForeignKey("course_objectives.id", ondelete="SET NULL"), nullable=True)
    indicator_id      = Column(String(36), ForeignKey("graduation_indicators.id", ondelete="SET NULL"), nullable=True)
    achievement_value = Column(DECIMAL(6, 4), nullable=False)
    locked            = Column(Boolean, nullable=False, default=False)
    created_at        = Column(DateTime, nullable=False, default=datetime.utcnow)
    updated_at        = Column(DateTime, nullable=False, default=datetime.utcnow, onupdate=datetime.utcnow)

    # relationships
    course    = relationship("Course", back_populates="calculation_results")
    class_    = relationship("Class", back_populates="calculation_results")
    objective = relationship("CourseObjective", back_populates="calculation_results")
    indicator = relationship("GraduationIndicator", back_populates="calculation_results")

    __table_args__ = (
        Index("idx_course_class_level", "course_id", "class_id", "level"),
        Index("idx_level_locked", "level", "locked"),
        Index("idx_result_indicator", "indicator_id"),
    )


# ── 15. unlock_log ──────────────────────────────────────

class UnlockLog(Base):
    __tablename__ = "unlock_log"

    id          = Column(String(36), primary_key=True, default=gen_uuid)
    course_id   = Column(String(36), ForeignKey("courses.id", ondelete="CASCADE"), nullable=False)
    class_id    = Column(String(36), ForeignKey("classes.id", ondelete="CASCADE"), nullable=False)
    operator_id = Column(String(36), ForeignKey("users.id", ondelete="RESTRICT"), nullable=False)
    reason      = Column(String(500), nullable=False)
    created_at  = Column(DateTime, nullable=False, default=datetime.utcnow)

    # relationships
    course   = relationship("Course", back_populates="unlock_logs")
    class_   = relationship("Class", back_populates="unlock_logs")
    operator = relationship("User", back_populates="unlock_logs")
