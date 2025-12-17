# app/api/v1/endpoints/posts.py
from typing import List
from fastapi import APIRouter, Depends, HTTPException
from sqlmodel import Session

from app.database import get_session
from app.schemas import PostCard, PostCreate,ToggleResponse, CommentCreate, CommentOut
from app.models import (
    CommunityPost,
    ModelAsset,
    InteractionLike,
    PostCollection,
    Comment,
    UserFollow,
    User,
    Visibility
)
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


@router.post("/publish", response_model=PostCard)
def publish_post(
        post_in: PostCreate,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    发布帖子
    """
    # 1. 检查资产是否存在，且属于当前用户
    asset = session.get(ModelAsset, post_in.asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="模型资产不存在")

    if asset.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="你只能发布属于自己的模型")

    # 2. 创建帖子
    new_post = crud_post.create_post(session, current_user.id, post_in)

    # 3. 手动组装返回结果 (PostCard)
    return PostCard(
        post_id=new_post.id,
        asset_id=asset.id,
        title=asset.title,  # 标题沿用模型的标题
        cover_url=asset.video_path,  # 封面沿用视频
        description=new_post.content,  # 描述使用帖子的文案
        tags=asset.tags,
        published_at=str(new_post.published_at),
        like_count=0,
        view_count=0,
        owner_id=current_user.id,
        owner_name=current_user.username,
        owner_avatar=current_user.avatar_url,
        is_liked=False,
        is_collected=False,
        has_commented=False,
        is_following=False
    )




 # 导入


# --- 点赞帖子 ---
@router.post("/{post_id}/like", response_model=ToggleResponse)
def like_post(
        post_id: int,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """点赞/取消点赞 帖子"""
    is_liked, new_count = crud_post.toggle_like(session, current_user.id, post_id)
    return ToggleResponse(is_active=is_liked, new_count=new_count)


# --- 收藏帖子 ---
@router.post("/{post_id}/collect", response_model=ToggleResponse)
def collect_post(
        post_id: int,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """收藏/取消收藏 帖子"""
    is_collected, new_count = crud_post.toggle_collection(session, current_user.id, post_id)
    return ToggleResponse(is_active=is_collected, new_count=new_count)


# --- 评论帖子 ---
@router.post("/{post_id}/comments", response_model=CommentOut)
def comment_post(
        post_id: int,
        comment_in: CommentCreate,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """发表评论"""
    comment = crud_post.create_comment(
        session,
        current_user.id,
        post_id,
        comment_in.content,
        comment_in.parent_id
    )
    if not comment:
        raise HTTPException(status_code=404, detail="帖子不存在")

    return CommentOut(
        id=comment.id,
        user_id=current_user.id,
        username=current_user.username,
        avatar_url=current_user.avatar_url,
        content=comment.content,
        created_at=str(comment.created_at)
    )