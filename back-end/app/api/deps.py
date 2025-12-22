# app/api/deps.py
from typing import Generator, Annotated
from fastapi import Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer
from jose import jwt, JWTError
from sqlmodel import Session
from pydantic import ValidationError

from app.core.config import settings
from app.database import get_session
from app.models import User
from app.core.config import settings

# 指明 Token 获取地址
reusable_oauth2 = OAuth2PasswordBearer(tokenUrl=f"/api/v1/auth/login")


def get_current_user(
        session: Session = Depends(get_session),
        token: str = Depends(reusable_oauth2)
) -> User:
    """
    依赖注入：验证 Token 并返回当前 User 对象
    """
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        token_data = payload.get("sub")
        if token_data is None:
            raise HTTPException(status_code=403, detail="Token 凭证无效")
    except (JWTError, ValidationError):
        raise HTTPException(status_code=403, detail="无法验证凭证")

    user = session.get(User, int(token_data))
    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")
    return user