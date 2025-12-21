import shutil
import uuid

from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from sqlmodel import Session
from app.database import get_session
from app.schemas import ToggleResponse, UserDetail, UserAvatar, UserOut, UserUpdate
from app.models import User
from app.api.deps import get_current_user
from app.crud import crud_user
from pathlib import Path
from typing import List

router = APIRouter()


@router.post("/{target_user_id}/follow", response_model=ToggleResponse)
def follow_user(
        target_user_id: int,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    关注/取消关注 用户
    """
    # 检查目标用户是否存在
    target_user = session.get(User, target_user_id)
    if not target_user:
        raise HTTPException(status_code=404, detail="用户不存在")

    if target_user_id == current_user.id:
        raise HTTPException(status_code=400, detail="You cannot follow yourself")

    is_following, new_follower_count = crud_user.toggle_follow(
        session=session,
        follower_id=current_user.id,
        followed_id=target_user_id
    )

    return ToggleResponse(
        is_active=is_following,
        new_count=new_follower_count
    )


@router.get("/me", response_model=UserDetail)
def read_user_me(
        current_user: User = Depends(get_current_user)
):
    """
    获取【我自己】的详细信息
    包含：个人资料、粉丝数、关注数、获赞总数
    """
    return UserDetail(
        id=current_user.id,
        username=current_user.username,
        gender=current_user.gender,
        avatar_url=current_user.avatar_url,
        cover_url=current_user.cover_url,
        bio=current_user.bio,
        created_at=str(current_user.created_at),
        follower_count=current_user.follower_count,
        following_count=current_user.following_count,
        liked_total_count=current_user.liked_total_count
    )


AVATAR_UPLOAD_ROOT = Path("static/uploads/avatars")
AVATAR_UPLOAD_ROOT.mkdir(parents=True, exist_ok=True)


@router.post("/me/avatar", response_model=UserDetail)
def update_my_avatar(
        file: UploadFile = File(...),
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    上传并更新当前用户的头像
    """
    # 简单校验文件类型
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="只能上传图片文件")

    # 生成唯一文件名 (user_id + uuid + 后缀)
    ext = file.filename.split(".")[-1] if "." in file.filename else "png"
    new_filename = f"{current_user.id}_{uuid.uuid4().hex[:8]}.{ext}"

    disk_path = AVATAR_UPLOAD_ROOT / new_filename
    web_path = f"/static/uploads/avatars/{new_filename}"

    # 保存文件
    try:
        with disk_path.open("wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"头像保存失败: {str(e)}")
    finally:
        file.file.close()

    # 更新数据库
    updated_user = crud_user.update_avatar(session, current_user.id, web_path)

    # 返回最新的用户详情
    return UserDetail(
        id=updated_user.id,
        username=updated_user.username,
        gender=updated_user.gender,
        avatar_url=updated_user.avatar_url,
        cover_url=updated_user.cover_url,
        bio=updated_user.bio,
        created_at=str(updated_user.created_at),
        follower_count=updated_user.follower_count,
        following_count=updated_user.following_count,
        liked_total_count=updated_user.liked_total_count
    )


@router.get("/{user_id}/avatar", response_model=UserAvatar)
def get_user_avatar(
        user_id: int,
        session: Session = Depends(get_session)
):
    """
    通过用户 ID 获取其头像 URL
    """
    # 1. 直接从数据库获取用户对象
    user = session.get(User, user_id)

    # 2. 校验用户是否存在
    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")

    # 3. 返回头像 URL
    return UserAvatar(avatar_url=user.avatar_url)


@router.get("/{user_id}/followers", response_model=List[UserOut])
def get_user_followers(
        user_id: int,
        session: Session = Depends(get_session)
):
    """
    获取指定用户的【粉丝列表】
    """
    user = session.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")

    # 利用 SQLModel 的 Relationship 自动查询
    return user.followers


@router.get("/{user_id}/following", response_model=List[UserOut])
def get_user_following(
        user_id: int,
        session: Session = Depends(get_session)
):
    """
    获取指定用户的【关注列表】
    """
    user = session.get(User, user_id)
    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")

    return user.following


# 更新背景图
COVER_UPLOAD_ROOT = Path("static/uploads/covers")
COVER_UPLOAD_ROOT.mkdir(parents=True, exist_ok=True)


@router.post("/me/cover", response_model=UserDetail)
def update_my_cover(
        file: UploadFile = File(...),
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    上传并更新当前用户的个人主页背景图
    """
    # 校验文件类型
    if not file.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="只能上传图片文件")

    # 生成唯一文件名
    ext = file.filename.split(".")[-1] if "." in file.filename else "png"
    new_filename = f"cover_{current_user.id}_{uuid.uuid4().hex[:8]}.{ext}"

    disk_path = COVER_UPLOAD_ROOT / new_filename
    web_path = f"/static/uploads/covers/{new_filename}"

    # 保存文件
    try:
        with disk_path.open("wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"背景图保存失败: {str(e)}")
    finally:
        file.file.close()

    # 更新数据库
    updated_user = crud_user.update_cover(session, current_user.id, web_path)

    # 返回最新的用户详情
    return UserDetail(
        id=updated_user.id,
        username=updated_user.username,
        gender=updated_user.gender,
        avatar_url=updated_user.avatar_url,
        cover_url=updated_user.cover_url,
        bio=updated_user.bio,
        created_at=str(updated_user.created_at),
        follower_count=updated_user.follower_count,
        following_count=updated_user.following_count,
        liked_total_count=updated_user.liked_total_count
    )


@router.patch("/me", response_model=UserDetail)
def update_user_me(
        user_in: UserUpdate,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    更新当前用户的基本信息 (昵称、简介、性别)
    """
    # 尝试修改用户名，检查唯一性
    if user_in.username and user_in.username != current_user.username:
        existing_user = crud_user.get_user_by_username(session, username=user_in.username)
        if existing_user:
            raise HTTPException(status_code=400, detail="该用户名已被使用")

    # 执行更新
    try:
        updated_user = crud_user.update_profile(session, current_user.id, user_in)
    except Exception as e:
        # 捕获可能的枚举错误或其他数据库错误
        raise HTTPException(status_code=400, detail=f"更新失败: {str(e)}")

    # 返回最新信息
    return UserDetail(
        id=updated_user.id,
        username=updated_user.username,
        gender=updated_user.gender,
        avatar_url=updated_user.avatar_url,
        cover_url=updated_user.cover_url,
        bio=updated_user.bio,
        created_at=str(updated_user.created_at),
        follower_count=updated_user.follower_count,
        following_count=updated_user.following_count,
        liked_total_count=updated_user.liked_total_count
    )
