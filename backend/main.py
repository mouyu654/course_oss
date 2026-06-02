from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

# 导入全部模型，确保 Base.metadata 能感知所有表
import models  # noqa: F401
from db.session import engine
from db.base import Base
from routers.teacher import router as teacher_router

app = FastAPI(title="毕业要求达成度统一计算平台", version="0.1.0")

# 允许来自前端本地启动端口的跨域请求
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],  # 默认 Vite 启动端口，如需修改前端端口同步调整
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.on_event("startup")
def on_startup():
    """开发环境：自动建表（生产环境请使用 Alembic 迁移）。"""
    Base.metadata.create_all(bind=engine)


# ── 注册路由 ────────────────────────────────────────────

app.include_router(teacher_router)


@app.get("/api/ping")
async def ping():
    return {"message": "pong"}
