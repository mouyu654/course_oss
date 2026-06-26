"""数据库连接配置"""
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from app.config import settings

engine = create_engine(
    settings.DATABASE_URL,
    pool_pre_ping=True,
    pool_recycle=3600,
    pool_size=10,
    max_overflow=20,
    connect_args={"init_command": "SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ"}
)

SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def get_db():
    """获取数据库 Session 的 FastAPI 依赖"""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
