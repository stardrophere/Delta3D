from sqlmodel import Session, select
from app.models import User
from app.schemas import UserCreate
from app.core.security import get_password_hash


def get_user_by_username(session: Session, username: str) -> User | None:
    """通过用户名查找用户"""
    statement = select(User).where(User.username == username)
    return session.exec(statement).first()


def create_user(session: Session, user_in: UserCreate) -> User:
    """创建新用户"""
    # 1. 将明文密码加密
    hashed_password = get_password_hash(user_in.password)

    # 2. 创建数据库对象
    db_user = User(
        username=user_in.username,
        password_hash=hashed_password,  # 存哈希值
        avatar_url=None
    )

    # 3. 存入数据库
    session.add(db_user)
    session.commit()
    session.refresh(db_user)
    return db_user