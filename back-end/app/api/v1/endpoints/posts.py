# app/api/v1/endpoints/posts.py
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlmodel import Session

from app.database import get_session
from app.schemas import PostCard, PostCreate, ToggleResponse, CommentCreate, CommentOut, PostDetail
from app.models import (
    CommunityPost,
    ModelAsset,
    InteractionLike,
    PostCollection,
    Comment,
    UserFollow,
    User,
    Visibility, AssetStatus
)
from app.api.deps import get_current_user
from app.crud import crud_post

router = APIRouter()


@router.get("/users/{target_user_id}/posts", response_model=List[PostCard])
def read_user_posts(
        target_user_id: int,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)  # 需要登录才能看
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
    - 模型信息（标题、封面）
    - 帖子数据（内容、时间、点赞数、评论数、收藏数）
    - 交互状态（是否已赞、是否已收藏、是否已关注作者）
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
    发布帖子 (Publish Post)
    逻辑：
    1. 检查资产是否存在
    2. 检查资产是否属于当前用户
    3. 检查资产状态是否为 Completed
    4. 检查该资产是否重复发布
    5. 创建帖子并返回 PostCard
    """

    # 获取model信息
    asset = session.get(ModelAsset, post_in.asset_id)

    # model不存在
    if not asset:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="找不到指定的模型资产"
        )

    # model不属于你
    if asset.user_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="你无法发布不属于你的模型"
        )

    # mdoel未就绪
    # 如果模型还在生成中(PROCESSING)或失败(FAILED)，不能发布
    if asset.status != AssetStatus.COMPLETED:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"模型状态为 {asset.status}，无法发布。请等待模型生成完成。"
        )

    # 防重复发布
    existing_post = crud_post.get_post_by_asset_id(session, asset.id)
    if existing_post:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="该模型已经发布过帖子，请勿重复发布"
        )

    # 创建帖子
    new_post = crud_post.create_post(session, current_user.id, post_in)

    # 组装返回数据
    return PostCard(
        post_id=new_post.id,
        asset_id=asset.id,
        title=asset.title,  # 沿用模型标题
        cover_url=asset.video_path,  # 沿用模型视频/封面
        description=new_post.content or asset.description,  # 优先显示帖子文案
        tags=asset.tags,  # 沿用模型标签
        published_at=str(new_post.published_at),

        # 初始数据
        like_count=0,
        view_count=0,
        collect_count=0,
        comment_count=0,

        # 作者信息
        owner_id=current_user.id,
        owner_name=current_user.username,
        owner_avatar=current_user.avatar_url,

        # 交互状态
        is_liked=False,
        is_collected=False,
        has_commented=False,
        is_following=False  # 自己不能关注自己
    )


# 点赞帖子
@router.post("/{post_id}/like", response_model=ToggleResponse)
def like_post(
        post_id: int,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    点赞/取消点赞
    - 如果当前未赞，则点赞（返回 is_active=True）
    - 如果当前已赞，则取消（返回 is_active=False）
    """
    # 先简单检查帖子是否存在
    post = session.get(CommunityPost, post_id)
    if not post:
        raise HTTPException(status_code=404, detail="帖子不存在")

    is_liked, new_count = crud_post.toggle_like(session, current_user.id, post_id)

    return ToggleResponse(
        is_active=is_liked,
        new_count=new_count
    )


@router.post("/{post_id}/collect", response_model=ToggleResponse)
def collect_post(
        post_id: int,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    收藏/取消收藏
    - 逻辑同点赞
    """
    post = session.get(CommunityPost, post_id)
    if not post:
        raise HTTPException(status_code=404, detail="帖子不存在")

    is_collected, new_count = crud_post.toggle_collection(session, current_user.id, post_id)

    return ToggleResponse(
        is_active=is_collected,
        new_count=new_count
    )


# 评论帖子
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
        comment_in.content
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


@router.get("/{post_id}", response_model=PostDetail)
def read_post_detail(
        post_id: int,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    【帖子详情页】
    获取帖子详情、关联模型信息、作者信息、所有评论及交互状态。
    访问此接口会自动增加浏览量。
    """
    post_detail = crud_post.get_post_detail(
        session=session,
        post_id=post_id,
        current_user_id=current_user.id
    )
    return post_detail


@router.get("/me/collected", response_model=List[PostCard])
def read_my_collections(
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    获取【我收藏】的所有帖子列表
    包含帖子详情、作者信息以及统计数据
    """
    posts = crud_post.get_my_collected_posts(
        session=session,
        current_user_id=current_user.id
    )
    return posts
