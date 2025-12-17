# app/api/v1/endpoints/posts.py
from typing import List
from fastapi import APIRouter, Depends, HTTPException
from sqlmodel import Session

from app.database import get_session
from app.schemas import PostCard
from app.models import User
from app.api.deps import get_current_user
from app.crud import crud_post

router = APIRouter()

@router.get("/users/{target_user_id}/posts", response_model=List[PostCard])
def read_user_posts(
    target_user_id: int,
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user) # 需要登录才能看
):
    """
    获取指定用户(target_user_id)的所有帖子列表
    """
    posts = crud_post.get_posts_by_user(
        session=session,
        target_user_id=target_user_id,
        current_user_id=current_user.id
    )
    return posts

@router.get("/users/me/posts", response_model=List[PostCard])
def read_my_posts(
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user)
):
    """
    获取【我自己】的所有帖子列表
    """
    posts = crud_post.get_posts_by_user(
        session=session,
        target_user_id=current_user.id,
        current_user_id=current_user.id
    )
    return posts


@router.get("/community", response_model=List[PostCard])
def read_community_posts(
    session: Session = Depends(get_session),
    current_user: User = Depends(get_current_user)
):
    """
    【社区首页】
    获取所有用户的帖子流，包含：
    - 帖子详情
    - 作者信息
    - 我是否点赞/收藏/评论/关注
    """
    posts = crud_post.get_community_posts(
        session=session,
        current_user_id=current_user.id
    )
    return posts