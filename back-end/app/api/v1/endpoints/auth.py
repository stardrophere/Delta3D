from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordRequestForm
from sqlmodel import Session

from app.database import get_session
from app.schemas import UserCreate, UserOut, Token
from app.crud import crud_user
from app.core.security import verify_password, create_access_token

router = APIRouter()


# 1. 注册接口
@router.post("/register", response_model=UserOut)
def register(user_in: UserCreate, session: Session = Depends(get_session)):
    # 检查用户名是否已存在
    user = crud_user.get_user_by_username(session, username=user_in.username)
    if user:
        raise HTTPException(
            status_code=400,
            detail="该用户名已被注册",
        )
    # 创建用户
    new_user = crud_user.create_user(session, user_in)
    return new_user


# 2. 登录接口
@router.post("/login", response_model=Token)
def login(
        form_data: OAuth2PasswordRequestForm = Depends(),
        session: Session = Depends(get_session)
):
    # form_data.username 和 form_data.password 是前端传来的
    user = crud_user.get_user_by_username(session, username=form_data.username)

    # 验证账号和密码
    if not user or not verify_password(form_data.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="用户名或密码错误",
            headers={"WWW-Authenticate": "Bearer"},
        )

    # 生成 Token
    access_token = create_access_token(subject=user.id)
    return {"access_token": access_token, "token_type": "bearer"}