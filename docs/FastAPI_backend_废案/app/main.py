"""FastAPI 应用入口"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from app.config import settings

app = FastAPI(
    title="毕业要求达成度计算平台",
    description="面向专业认证的毕业要求达成度统一计算平台后端 API",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS
origins = settings.ALLOWED_ORIGINS.split(",") if settings.ALLOWED_ORIGINS else []
app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Global exception handler
@app.exception_handler(Exception)
async def general_exception_handler(request, exc):
    return JSONResponse(
        status_code=500,
        content={"code": 500, "message": f"服务器内部错误: {str(exc)}"},
    )

# Health check
@app.get("/health")
async def health_check():
    return {"status": "ok", "version": "1.0.0"}


# ========== 注册路由 ==========

# 认证与用户管理
@app.post("/api/auth/login")
async def auth_login():
    from app.routers.auth_router import router as auth_r
    return auth_r

# 使用 include_router 注册各模块路由
def register_routers():
    from app.routers.auth_router import router as auth_router
    from app.routers.admin_router import router as admin_router
    from app.routers.academic_router import router as academic_router
    from app.routers.director_router import router as director_router
    from app.routers.teacher_router import router as teacher_router

    app.include_router(auth_router, prefix="/api/auth", tags=["认证"])
    app.include_router(admin_router, prefix="/api/admin", tags=["管理"])
    app.include_router(academic_router, prefix="/api/academic", tags=["教务"])
    app.include_router(director_router, prefix="/api/director", tags=["专业设计"])
    app.include_router(teacher_router, prefix="/api/teacher", tags=["教师"])

register_routers()


# ========== 数据库初始化 ==========
from app.database import engine, Base

def init_db():
    """创建所有数据表"""
    # 导入所有模型以确保 Base.metadata 知道它们
    from app.models.user import User
    from app.models.global_config import GlobalConfig
    from app.models.course import Course
    from app.models.class_ import Class
    from app.models.student import Student
    from app.models.class_student import ClassStudent
    from app.models.graduation import GraduationRequirement, GraduationIndicator
    from app.models.matrix import MacroSupportMatrix, InternalWeightMatrix
    from app.models.objective import CourseObjective
    from app.models.assessment import AssessmentItem
    from app.models.score import StudentScore
    from app.models.result import CalculationResult
    from app.models.unlock_log import UnlockLog

    Base.metadata.create_all(bind=engine)
    print("Database tables created successfully!")

# 在启动时初始化数据库（仅开发环境）
# init_db()
