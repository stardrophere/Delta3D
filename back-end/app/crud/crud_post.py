from app.schemas import PostCreate
from fastapi import HTTPException
from app.schemas import PostDetail, PostAssetInfo, CommentOut
from sqlmodel import Session, select, or_, and_, col
from typing import List
from app.models import (
    CommunityPost, ModelAsset, InteractionLike,
    PostCollection, Comment, UserFollow, User, Visibility
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
    # 查询该用户发布的所有帖子
    statement = (
        select(CommunityPost)
        .where(CommunityPost.user_id == target_user_id)
        .order_by(CommunityPost.published_at.desc())
    )
    posts = session.exec(statement).all()

    results = []
    for post in posts:
        # 获取关联的model信息
        asset = post.asset
        author = post.author

        # 检查当前用户是否点赞
        # 查询 InteractionLike 表
        like_stat = select(InteractionLike).where(
            InteractionLike.user_id == current_user_id,
            InteractionLike.post_id == post.id
        )
        is_liked = session.exec(like_stat).first() is not None

        # 组装数据
        results.append({
            "post_id": post.id,
            "asset_id": asset.id,
            "title": asset.title,
            "cover_url": asset.video_path,
            "description": post.content or asset.description,
            "tags": asset.tags,
            "like_count": post.like_count,
            "is_liked": is_liked,
            "owner_name": author.username,
            "owner_avatar": author.avatar_url
        })

    return results







def get_community_posts(session: Session, current_user_id: int) -> List[dict]:
    """
    【社区首页】获取帖子流
    逻辑更新：
    1. 显示 PUBLIC 帖子
    2. 显示 FOLLOWERS 帖子 (如果当前用户关注了作者)
    3. 显示当前用户自己的帖子
    """

    # =========================================================
    # 获取我关注的所有用户 ID
    # =========================================================
    following_subquery = select(UserFollow.followed_id).where(
        UserFollow.follower_id == current_user_id
    )

    statement = (
        select(CommunityPost)
        .where(
            or_(
                # 帖子是公开的
                CommunityPost.visibility == Visibility.PUBLIC,

                # 帖子仅粉丝可见，且作者ID在我的关注列表里
                and_(
                    CommunityPost.visibility == Visibility.FOLLOWERS,
                    col(CommunityPost.user_id).in_(following_subquery)
                ),

                CommunityPost.user_id == current_user_id
            )
        )
        .order_by(CommunityPost.published_at.desc())
    )

    # 执行查询
    posts = session.exec(statement).all()

    if not posts:
        return []

    # 我点赞过的帖子ID
    liked_ids_stmt = select(InteractionLike.post_id).where(InteractionLike.user_id == current_user_id)
    my_liked_ids = set(session.exec(liked_ids_stmt).all())

    # 我收藏过的帖子ID
    collected_ids_stmt = select(PostCollection.post_id).where(PostCollection.user_id == current_user_id)
    my_collected_ids = set(session.exec(collected_ids_stmt).all())

    # 我评论过的帖子ID
    commented_ids_stmt = select(Comment.post_id).where(Comment.user_id == current_user_id).distinct()
    my_commented_ids = set(session.exec(commented_ids_stmt).all())

    # 我关注的用户ID
    following_ids_stmt = select(UserFollow.followed_id).where(UserFollow.follower_id == current_user_id)
    my_following_ids = set(session.exec(following_ids_stmt).all())

    # 组装数据
    results = []
    for post in posts:
        asset = post.asset
        author = post.author

        display_desc = post.content if post.content else asset.description

        results.append({
            "post_id": post.id,
            "asset_id": asset.id,
            "title": asset.title,
            "cover_url": asset.video_path,
            "description": display_desc,
            "tags": asset.tags,
            "published_at": str(post.published_at),
            "view_count": post.view_count,
            "like_count": post.like_count,
            "collect_count": post.collect_count,
            "comment_count": post.comment_count,
            "owner_id": author.id,
            "owner_name": author.username,
            "owner_avatar": author.avatar_url,
            "is_liked": post.id in my_liked_ids,
            "is_collected": post.id in my_collected_ids,
            "has_commented": post.id in my_commented_ids,
            "is_following": author.id in my_following_ids
        })

    return results


def get_post_by_asset_id(session: Session, asset_id: int) -> CommunityPost | None:
    """检查某个 asset 是否已经被发布过"""
    statement = select(CommunityPost).where(CommunityPost.asset_id == asset_id)
    return session.exec(statement).first()


def create_post(session: Session, user_id: int, post_in: PostCreate) -> CommunityPost:
    """创建新帖子"""
    db_post = CommunityPost(
        user_id=user_id,
        asset_id=post_in.asset_id,
        content=post_in.content,

        visibility=Visibility(post_in.visibility),
        allow_download=post_in.allow_download,
        # published_at
    )
    session.add(db_post)
    session.commit()
    session.refresh(db_post)
    return db_post


# 点赞触发
def toggle_like(session: Session, user_id: int, post_id: int) -> tuple[bool, int]:
    """
    切换帖子点赞状态 (Toggle Like)

    Returns:
        (is_liked: bool, new_like_count: int)
    """
    # 查帖子
    post = session.get(CommunityPost, post_id)
    if not post:
        return False, 0

    # 查当前用户的点赞记录
    statement = select(InteractionLike).where(
        InteractionLike.user_id == user_id,
        InteractionLike.post_id == post_id
    )
    like_record = session.exec(statement).first()

    # 查作者
    author = session.get(User, post.user_id)

    if like_record:
        # 已点赞 -> 执行取消
        session.delete(like_record)
        post.like_count = max(0, post.like_count - 1)
        if author:
            author.liked_total_count = max(0, author.liked_total_count - 1)
        is_active = False
    else:
        # 未点赞 -> 执行点赞
        new_record = InteractionLike(user_id=user_id, post_id=post_id)
        session.add(new_record)
        post.like_count += 1
        if author:
            author.liked_total_count += 1
        is_active = True

    # 提交事务
    session.add(post)
    if author:
        session.add(author)

    session.commit()
    session.refresh(post)

    return is_active, post.like_count


def toggle_collection(session: Session, user_id: int, post_id: int) -> tuple[bool, int]:
    """
    切换帖子收藏状态 (Toggle Collection)

    Returns:
        (is_collected: bool, new_collect_count: int)
    """
    post = session.get(CommunityPost, post_id)
    if not post:
        return False, 0

    statement = select(PostCollection).where(
        PostCollection.user_id == user_id,
        PostCollection.post_id == post_id
    )
    collect_record = session.exec(statement).first()

    if collect_record:
        # 取消收藏
        session.delete(collect_record)
        post.collect_count = max(0, post.collect_count - 1)
        is_active = False
    else:
        # 收藏
        new_record = PostCollection(user_id=user_id, post_id=post_id)
        session.add(new_record)
        post.collect_count += 1
        is_active = True

    session.add(post)
    session.commit()
    session.refresh(post)

    return is_active, post.collect_count


# 评论
def create_comment(session: Session, user_id: int, post_id: int, content: str) -> Comment:
    """发布评论"""
    post = session.get(CommunityPost, post_id)
    if not post:
        return None

    # 创建评论
    comment = Comment(
        user_id=user_id,
        post_id=post_id,
        content=content,
        # parent_id=parent_id
    )
    session.add(comment)

    # 帖子评论数 +1
    post.comment_count += 1
    session.add(post)

    session.commit()
    session.refresh(comment)
    return comment


def get_post_detail(session: Session, post_id: int, current_user_id: int) -> PostDetail:
    """
    获取帖子详情，包含：
    1. 帖子内容与权限
    2. 关联的模型信息
    3. 交互状态 (是否关注/点赞/收藏)
    4. 评论列表
    5. 自动增加浏览量 (+1 view count)
    """

    # 查询帖子 (包含关联对象)
    post = session.get(CommunityPost, post_id)
    if not post:
        raise HTTPException(status_code=404, detail="帖子不存在")

    # 权限/可见性检查
    if post.visibility == Visibility.PRIVATE and post.user_id != current_user_id:
        raise HTTPException(status_code=403, detail="该帖子为私密状态，无法查看")



    # 增加浏览量
    post.view_count += 1
    session.add(post)
    session.commit()
    session.refresh(post)  # 刷新以获取最新数据

    # 获取关联对象
    asset = post.asset
    author = post.author

    # 查询交互状态 (User -> Post/Author)
    # 是否点赞
    is_liked = session.exec(
        select(InteractionLike).where(
            InteractionLike.user_id == current_user_id,
            InteractionLike.post_id == post_id
        )
    ).first() is not None

    # 是否收藏
    is_collected = session.exec(
        select(PostCollection).where(
            PostCollection.user_id == current_user_id,
            PostCollection.post_id == post_id
        )
    ).first() is not None

    # 是否关注作者
    is_following = False
    if author.id != current_user_id:
        is_following = session.exec(
            select(UserFollow).where(
                UserFollow.follower_id == current_user_id,
                UserFollow.followed_id == author.id
            )
        ).first() is not None

    # 获取评论列表 (按时间倒序或正序)
    comment_list = []
    stmt_comments = (
        select(Comment)
        .where(Comment.post_id == post_id)
        .order_by(Comment.created_at.desc())
    )
    db_comments = session.exec(stmt_comments).all()

    for c in db_comments:
        c_user = c.user
        comment_list.append(CommentOut(
            id=c.id,
            user_id=c_user.id,
            username=c_user.username,
            avatar_url=c_user.avatar_url,
            content=c.content,
            created_at=str(c.created_at),
            # parent_id=c.parent_id
        ))

    return PostDetail(
        # Post Info
        post_id=post.id,
        content=post.content,
        published_at=str(post.published_at),
        visibility=post.visibility.value,
        allow_download=post.allow_download,

        # Stats
        like_count=post.like_count,
        collect_count=post.collect_count,
        view_count=post.view_count,
        comment_count=post.comment_count,

        # Author Info
        owner_id=author.id,
        owner_name=author.username,
        owner_avatar=author.avatar_url,

        # Interaction
        is_liked=is_liked,
        is_collected=is_collected,
        is_following=is_following,

        # Asset Info
        asset=PostAssetInfo(
            id=asset.id,
            title=asset.title,
            description=asset.description,
            tags=asset.tags,
            video_url=asset.video_path,
            model_url=asset.model_path,
            status=asset.status,
            height=asset.height,
            estimated_gen_seconds=asset.estimated_gen_seconds
        ),

        # Comments
        comments=comment_list
    )


def get_my_collected_posts(session: Session, current_user_id: int) -> List[dict]:
    """
    获取【我收藏】的所有帖子列表
    按收藏时间倒序排列
    """
    # 从 PostCollection 表找到对应的 CommunityPost
    # 倒序
    statement = (
        select(CommunityPost)
        .join(PostCollection, CommunityPost.id == PostCollection.post_id)
        .where(PostCollection.user_id == current_user_id)
        .order_by(PostCollection.created_at.desc())
    )

    posts = session.exec(statement).all()

    if not posts:
        return []


    # 我点赞过的帖子ID
    liked_ids_stmt = select(InteractionLike.post_id).where(InteractionLike.user_id == current_user_id)
    my_liked_ids = set(session.exec(liked_ids_stmt).all())

    # 我评论过的帖子ID
    commented_ids_stmt = select(Comment.post_id).where(Comment.user_id == current_user_id).distinct()
    my_commented_ids = set(session.exec(commented_ids_stmt).all())

    # 我关注的用户ID (用于判断是否关注了原作者)
    following_ids_stmt = select(UserFollow.followed_id).where(UserFollow.follower_id == current_user_id)
    my_following_ids = set(session.exec(following_ids_stmt).all())

    # 组装数据
    results = []
    for post in posts:
        asset = post.asset
        author = post.author

        # 优先显示帖子内容，没有则显示模型描述
        display_desc = post.content if post.content else asset.description

        results.append({
            "post_id": post.id,
            "asset_id": asset.id,
            "title": asset.title,
            "cover_url": asset.video_path,
            "description": display_desc,
            "tags": asset.tags,
            "published_at": str(post.published_at),

            # 统计数据
            "view_count": post.view_count,
            "like_count": post.like_count,
            "collect_count": post.collect_count,
            "comment_count": post.comment_count,

            # 作者信息
            "owner_id": author.id,
            "owner_name": author.username,
            "owner_avatar": author.avatar_url,

            # 交互状态
            "is_liked": post.id in my_liked_ids,
            "is_collected": True,
            "has_commented": post.id in my_commented_ids,
            "is_following": author.id in my_following_ids
        })

    return results