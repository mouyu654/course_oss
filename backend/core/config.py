import os


DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "mysql+pymysql://root:root@127.0.0.1:3306/achievement_calc?charset=utf8mb4",
)

JWT_SECRET_KEY = os.getenv("JWT_SECRET_KEY", "change-me-in-production-use-a-strong-random-key")
JWT_ALGORITHM = "HS256"
JWT_EXPIRE_HOURS = 8

WEIGHT_TOLERANCE = 0.001
