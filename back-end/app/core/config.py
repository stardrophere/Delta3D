from typing import List
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # =========================================================
    # 1. 基础配置 (匹配 .env 中的 PROJECT_NAME, ENVIRONMENT)
    # =========================================================
    PROJECT_NAME: str = "Delta3D"
    ENVIRONMENT: str = "development"


    BACKEND_CORS_ORIGINS: List[str] = []

    # =========================================================
    # 2. 数据库 (DATABASE_URL)
    # =========================================================
    DATABASE_URL: str

    # =========================================================
    # 3. 安全认证 (SECRET_KEY, ALGORITHM, ACCESS_TOKEN_EXPIRE_MINUTES)
    # =========================================================
    SECRET_KEY: str
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 7  # 默认 7 天

    # =========================================================
    # 4. 文件存储 (UPLOAD_DIR, DOMAIN)
    # =========================================================
    # 给个默认值，防止 .env 没填导致报错
    UPLOAD_DIR: str = "./static/uploads"
    DOMAIN: str = "http://127.0.0.1:8000"

    # =========================================================
    # 配置项
    # =========================================================
    class Config:
        env_file = ".env"
        case_sensitive = True
        env_file_encoding = "utf-8"


# 实例化配置对象 (单例模式)
# 以后在其他文件里直接 import settings 就能用了
settings = Settings()