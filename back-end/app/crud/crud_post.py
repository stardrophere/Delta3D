# app/crud/crud_post.py
from typing import List
from sqlmodel import Session, select
from app.models import CommunityPost, ModelAsset, InteractionLike, User
from app.models import (
    CommunityPost, ModelAsset, InteractionLike,
    PostCollection, Comment, UserFollow, User
)


def get_posts_by_user(
        session: Session,
        target_user_id: int,
        current_user_id: int
) -> List[dict]:
    """
    查询 target_user_id 发布的所有帖子。
    同时计算 current_user_id 是否点过赞。
    """
    # 1. 查询该用户发布的所有帖子 (按时间倒序)
    statement = (
        select(CommunityPost)
        .where(CommunityPost.user_id == target_user_id)
        .order_by(CommunityPost.published_at.desc())
    )
    posts = session.exec(statement).all()

    results = []
    for post in posts:
        # 2. 获取关联的资产信息 (Asset)
        asset = post.asset
        author = post.author

        # 3. 检查当前用户是否点赞
        # 查询 InteractionLike 表，看有没有 (user_id, post_id) 的记录
        like_stat = select(InteractionLike).where(
            InteractionLike.user_id == current_user_id,
            InteractionLike.post_id == post.id
        )
        is_liked = session.exec(like_stat).first() is not None

        # 4. 组装数据
        results.append({
            "post_id": post.id,
            "asset_id": asset.id,
            "title": asset.title,
            "cover_url": asset.video_path,
            "description": post.content or asset.description,  # 优先用帖子文案
            "tags": asset.tags,
            "like_count": post.like_count,
            "is_liked": is_liked,
            "owner_name": author.username,
            "owner_avatar": author.avatar_url
        })

    return results


def get_community_posts(session: Session, current_user_id: int) -> List[dict]:
    """
    获取社区所有帖子，并填充当前用户的交互状态
    """
    # 1. 查出所有公开帖子 (按发布时间倒序)
    # 也可以在这里加 .limit(20) 做分页
    statement = (
        select(CommunityPost)
        .order_by(CommunityPost.published_at.desc())
    )
    posts = session.exec(statement).all()

    if not posts:
        return []

    # =========================================================
    # ★ 性能优化：预加载当前用户的交互数据 (避免 N+1 查询) ★
    # =========================================================

    # A. 我点赞过的帖子ID
    liked_ids_stmt = select(InteractionLike.post_id).where(InteractionLike.user_id == current_user_id)
    my_liked_ids = set(session.exec(liked_ids_stmt).all())

    # B. 我收藏过的帖子ID
    collected_ids_stmt = select(PostCollection.post_id).where(PostCollection.user_id == current_user_id)
    my_collected_ids = set(session.exec(collected_ids_stmt).all())

    # C. 我评论过的帖子ID (distinct去重)
    commented_ids_stmt = select(Comment.post_id).where(Comment.user_id == current_user_id).distinct()
    my_commented_ids = set(session.exec(commented_ids_stmt).all())

    # D. 我关注的用户ID
    # 注意 UserFollow 表结构: follower_id 是我, followed_id 是被关注的人
    following_ids_stmt = select(UserFollow.followed_id).where(UserFollow.follower_id == current_user_id)
    my_following_ids = set(session.exec(following_ids_stmt).all())

    # =========================================================
    # 3. 组装数据
    # =========================================================
    results = []
    for post in posts:
        asset = post.asset  # 关联查询
        author = post.author  # 关联查询

        results.append({
            # 基础信息
            "post_id": post.id,
            "asset_id": asset.id,
            "title": asset.title,
            "cover_url": asset.video_path,
            "description": post.content or asset.description,
            "tags": asset.tags,
            "published_at": str(post.published_at),
            "like_count": post.like_count,
            "view_count": post.view_count,

            # 作者信息
            "owner_id": author.id,
            "owner_name": author.username,
            "owner_avatar": author.avatar_url,

            # 交互状态 (O(1) 复杂度查找)
            "is_liked": post.id in my_liked_ids,
            "is_collected": post.id in my_collected_ids,
            "has_commented": post.id in my_commented_ids,

            # 这里的判断逻辑：如果作者就是我自己，算作 False 还是 True 均可，这里暂定 False
            "is_following": author.id in my_following_ids
        })

    return results

