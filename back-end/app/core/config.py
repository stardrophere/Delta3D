from typing import List
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    # =========================================================
    # 基础配置
    # =========================================================
    PROJECT_NAME: str = "Delta3D"
    ENVIRONMENT: str = "development"

    BACKEND_CORS_ORIGINS: List[str] = []

    # =========================================================
    # 数据库
    # =========================================================
    DATABASE_URL: str

    # =========================================================
    # 安全认证
    # =========================================================
    SECRET_KEY: str
    ALGORITHM: str = "HS256"
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 30

    # =========================================================
    # 文件存储
    # =========================================================
    UPLOAD_DIR: str = "./static/uploads"
    DOMAIN: str = "http://127.0.0.1:8000"

    # 扩展配置
    NGP_PYTHON_PATH: str | None = None
    COLMAP2NERF_SCRIPT_PATH: str | None = None
    NGP_RUN_SCRIPT_PATH: str | None = None
    RTSP_URL: str | None = None

    # =========================================================
    # 配置项
    # =========================================================
    class Config:
        env_file = ".env"
        case_sensitive = True
        env_file_encoding = "utf-8"


# 实例化配置对象 (单例模式)
settings = Settings()
