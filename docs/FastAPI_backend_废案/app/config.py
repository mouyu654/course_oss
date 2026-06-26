"""FastAPI 应用配置"""
from pydantic_settings import BaseSettings
from functools import lru_cache

class Settings(BaseSettings):
    DATABASE_URL: str = "mysql+pymysql://root:password@localhost:3306/achievement_calc?charset=utf8mb4"
    SECRET_KEY: str = "dev-secret-key-change-in-production"
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_HOURS: int = 8
    ALLOWED_ORIGINS: str = "http://localhost:5173"
    WEIGHT_TOLERANCE: float = 0.001

    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

@lru_cache
def get_settings() -> Settings:
    return Settings()

settings = get_settings()
