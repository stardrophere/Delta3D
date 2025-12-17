from typing import List, Optional
from datetime import datetime
from enum import Enum
from sqlmodel import Field, Relationship, SQLModel, JSON


# =============================================================================
# Enums (枚举)
# =============================================================================

class Visibility(str, Enum):
    """可见性：公开、粉丝可见、私密"""
    PUBLIC = "public"
    FOLLOWERS = "followers"
    PRIVATE = "private"


class AssetStatus(str, Enum):
    """资产状态：排队中、处理中、完成、失败"""
    PENDING = "pending"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"


# =============================================================================
# Association Tables (中间表)
# =============================================================================

class UserFollow(SQLModel, table=True):
    """用户关注关联表"""
    follower_id: int = Field(foreign_key="user.id", primary_key=True)
    followed_id: int = Field(foreign_key="user.id", primary_key=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)


class PostCollection(SQLModel, table=True):
    """帖子收藏关联表"""
    user_id: int = Field(foreign_key="user.id", primary_key=True)
    post_id: int = Field(foreign_key="communitypost.id", primary_key=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)


class ModelCollection(SQLModel, table=True):
    """模型资产收藏关联表"""
    user_id: int = Field(foreign_key="user.id", primary_key=True)
    asset_id: int = Field(foreign_key="modelasset.id", primary_key=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)


class InteractionLike(SQLModel, table=True):
    """点赞关联表"""
    user_id: int = Field(foreign_key="user.id", primary_key=True)
    post_id: int = Field(foreign_key="communitypost.id", primary_key=True)
    created_at: datetime = Field(default_factory=datetime.utcnow)


# =============================================================================
# Core Models (核心实体)
# =============================================================================

class User(SQLModel, table=True):
    """用户实体"""
    id: Optional[int] = Field(default=None, primary_key=True)

    username: str = Field(index=True, unique=True, description="用户登录唯一标识")
    password_hash: str = Field(description="加密后的密码")
    avatar_url: Optional[str] = Field(default=None, description="头像 URL")
    bio: Optional[str] = Field(default=None, max_length=500, description="个人简介")
    created_at: datetime = Field(default_factory=datetime.utcnow)

    # 统计缓存
    follower_count: int = Field(default=0, description="粉丝数")
    following_count: int = Field(default=0, description="关注数")
    liked_total_count: int = Field(default=0, description="获赞总数")

    # 关系
    following: List["User"] = Relationship(
        back_populates="followers",
        link_model=UserFollow,
        sa_relationship_kwargs={
            "primaryjoin": "User.id==UserFollow.follower_id",
            "secondaryjoin": "User.id==UserFollow.followed_id",
        },
    )
    followers: List["User"] = Relationship(
        back_populates="following",
        link_model=UserFollow,
        sa_relationship_kwargs={
            "primaryjoin": "User.id==UserFollow.followed_id",
            "secondaryjoin": "User.id==UserFollow.follower_id",
        },
    )
    assets: List["ModelAsset"] = Relationship(back_populates="owner")
    posts: List["CommunityPost"] = Relationship(back_populates="author")
    collected_posts: List["CommunityPost"] = Relationship(
        back_populates="collected_by_users", link_model=PostCollection
    )
    collected_assets: List["ModelAsset"] = Relationship(
        back_populates="collected_by_users", link_model=ModelCollection
    )
    downloads: List["DownloadRecord"] = Relationship(back_populates="downloader")
    sent_messages: List["Message"] = Relationship(
        sa_relationship_kwargs={"primaryjoin": "Message.sender_id==User.id"}
    )
    received_messages: List["Message"] = Relationship(
        sa_relationship_kwargs={"primaryjoin": "Message.receiver_id==User.id"}
    )


class ModelAsset(SQLModel, table=True):
    """模型资产 (私有库)"""
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id", description="拥有者ID")

    video_path: str = Field(description="原始视频路径")
    model_path: Optional[str] = Field(default=None, description="模型文件路径")

    title: str = Field(max_length=100)
    description: Optional[str] = None
    remark: Optional[str] = Field(default=None, description="私有备注")
    tags: List[str] = Field(default=[], sa_type=JSON, description="标签列表")

    status: AssetStatus = Field(default=AssetStatus.PENDING)
    created_at: datetime = Field(default_factory=datetime.utcnow)

    # 关系
    owner: User = Relationship(back_populates="assets")
    posts: List["CommunityPost"] = Relationship(back_populates="asset")
    collected_by_users: List[User] = Relationship(
        back_populates="collected_assets", link_model=ModelCollection
    )


class CommunityPost(SQLModel, table=True):
    """社区帖子 (公开展示)"""
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id")
    asset_id: int = Field(foreign_key="modelasset.id")

    content: Optional[str] = Field(default=None, description="帖子正文")
    visibility: Visibility = Field(default=Visibility.PUBLIC)
    allow_download: bool = Field(default=True)
    published_at: datetime = Field(default_factory=datetime.utcnow)

    # 统计
    view_count: int = 0
    like_count: int = 0
    collect_count: int = 0
    download_count: int = 0
    comment_count: int = 0

    # 关系
    author: User = Relationship(back_populates="posts")
    asset: ModelAsset = Relationship(back_populates="posts")
    comments: List["Comment"] = Relationship(back_populates="post")
    download_records: List["DownloadRecord"] = Relationship(back_populates="post")
    collected_by_users: List[User] = Relationship(
        back_populates="collected_posts", link_model=PostCollection
    )
    liked_by_users: List[User] = Relationship(link_model=InteractionLike)


class Comment(SQLModel, table=True):
    """评论"""
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id")
    post_id: int = Field(foreign_key="communitypost.id")
    parent_id: Optional[int] = Field(default=None, description="父评论ID")

    content: str
    created_at: datetime = Field(default_factory=datetime.utcnow)

    post: CommunityPost = Relationship(back_populates="comments")
    user: User = Relationship()


class DownloadRecord(SQLModel, table=True):
    """下载记录"""
    id: Optional[int] = Field(default=None, primary_key=True)
    user_id: int = Field(foreign_key="user.id")
    post_id: int = Field(foreign_key="communitypost.id")
    created_at: datetime = Field(default_factory=datetime.utcnow)

    downloader: User = Relationship(back_populates="downloads")
    post: CommunityPost = Relationship(back_populates="download_records")


class Message(SQLModel, table=True):
    """私信"""
    id: Optional[int] = Field(default=None, primary_key=True)
    sender_id: int = Field(foreign_key="user.id")
    receiver_id: int = Field(foreign_key="user.id")

    content: str
    is_read: bool = False
    created_at: datetime = Field(default_factory=datetime.utcnow)