from sqlmodel import SQLModel, create_engine, Session

from .core.config import settings
from . import models

# =================================================================
# SQLite 专用配置
# =================================================================
connect_args = {"check_same_thread": False}

# 创建数据库引擎
engine = create_engine(
    settings.DATABASE_URL,
    echo=False,
    connect_args=connect_args
)


def get_session():
    """
    FastAPI 依赖注入使用的 Session 生成器
    """
    with Session(engine) as session:
        yield session


def init_db():
    """
    初始化数据库表。
    """
    from app import models

    # 根据 metadata 创建所有表
    SQLModel.metadata.create_all(engine)