from datetime import datetime
from enum import Enum
from sqlmodel import SQLModel
from typing import List


# 注册时，前端需要传的数据
class UserCreate(SQLModel):
    username: str
    password: str


# 登录成功后，后端返回的数据
class Token(SQLModel):
    access_token: str
    token_type: str


# 获取用户信息时，返回的数据
class UserOut(SQLModel):
    id: int
    username: str
    avatar_url: str | None = None
    gender: str | None = None
    bio: str | None = None


class AssetCard(SQLModel):
    """
    【模型卡片】用于“我的模型”页面展示
    对应你截图里的每一个小方块
    """
    id: int
    title: str  # 标题
    cover_url: str  # 封面图
    description: str | None  # 描述
    tags: list[str]  # 标签
    is_collected: bool
    created_at: datetime
    status: str  # 模型状态
    height: int
    downloaded_at: datetime | None = None  # 下载时间
    owner_id: int


class PostCard(SQLModel):
    """
    【社区帖子卡片】
    包含帖子基本信息、作者信息、以及当前用户与帖子的交互状态
    """
    # 帖子基本信息
    post_id: int
    asset_id: int
    title: str  # 模型标题
    cover_url: str  # 封面 (视频路径)
    description: str | None  # 优先显示帖子文案，没有则显示模型描述
    tags: list[str]  # 标签
    published_at: str  # 发布时间

    # 统计数据 (新增 collect_count 和 comment_count)
    view_count: int = 0  # 浏览量
    like_count: int = 0  # 点赞数
    collect_count: int = 0  # 收藏数
    comment_count: int = 0  # 评论数

    # 作者信息
    owner_id: int  # 作者ID
    owner_name: str  # 作者用户名
    owner_avatar: str | None  # 作者头像

    # 当前用户与帖子的交互状态
    is_liked: bool  # 我是否点赞
    is_collected: bool  # 我是否收藏
    is_following: bool  # 我是否关注了该作者
    has_commented: bool  # 我是否评论过


class AssetDetail(SQLModel):
    """
    【模型详情】用于点击进入单个模型页面时展示
    包含隐私字段 remark
    """
    id: int
    title: str
    description: str | None
    remark: str | None  # 只有详情页才返回这个
    tags: list[str]
    video_url: str  # 对应数据库的 video_path
    model_url: str | None  # 对应数据库的 model_path (生成的模型文件)
    status: str  # 状态 (pending/processing/completed/failed)
    created_at: str  # 时间字符串
    estimated_gen_seconds: int | None = None


class PostCreate(SQLModel):
    """创建帖子时的请求参数"""
    asset_id: int
    content: str | None = None  # 帖子正文（允许为空，为空时前端可以显示默认文案）
    visibility: str = "public"  # 默认公开
    allow_download: bool = True  # 是否允许他人下载模型


# 通用的点赞/收藏响应
class ToggleResponse(SQLModel):
    is_active: bool  # True=已点赞/已收藏, False=已取消
    new_count: int  # 最新的总数 (点赞数/收藏数)


# 创建评论的请求参数
class CommentCreate(SQLModel):
    content: str
    # parent_id: int | None = None


# 评论返回结构
class CommentOut(SQLModel):
    id: int
    user_id: int
    username: str
    avatar_url: str | None
    content: str
    created_at: str


# 控制流逻辑
class StreamActionType(str, Enum):
    ROTATE = "rotate"  # 对应鼠标左键
    PAN = "pan"  # 对应鼠标中键
    ZOOM = "zoom"  # 对应滚轮


class StreamDirection(str, Enum):
    # --- 通用方向 (用于旋转和平移) ---
    UP = "up"
    DOWN = "down"
    LEFT = "left"
    RIGHT = "right"

    # 原有方向
    CLOCKWISE = "clockwise"
    COUNTER_CLOCKWISE = "counter_clockwise"

    # --- 缩放方向 ---
    IN = "in"
    OUT = "out"


class ControlCommand(SQLModel):
    action: StreamActionType
    direction: StreamDirection
    mode: str = "start"  #


class StreamStatus(SQLModel):
    is_active: bool
    rtsp_url: str | None
    current_asset_id: int | None


class PostAssetInfo(SQLModel):
    id: int
    title: str
    description: str | None
    tags: list[str]
    video_url: str  # 对应 video_path
    model_url: str | None  # 对应 model_path (下载用)
    status: str
    height: int
    estimated_gen_seconds: int | None


# 帖子详情的完整返回结构
class PostDetail(SQLModel):
    # --- 帖子基础信息 ---
    post_id: int
    content: str | None
    published_at: str
    visibility: str
    allow_download: bool
    # --- 统计信息 ---
    like_count: int
    collect_count: int
    view_count: int
    comment_count: int

    # --- 作者信息 ---
    owner_id: int
    owner_name: str
    owner_avatar: str | None

    # --- 交互状态 ---
    is_liked: bool  # 我是否点赞
    is_collected: bool  # 我是否收藏
    is_following: bool  # 我是否关注作者

    # --- 模型信息 (嵌套) ---
    asset: PostAssetInfo

    # --- 评论列表 ---
    comments: List[CommentOut]


class UserDetail(SQLModel):
    """
    【用户详情】
    用于 /users/me 或 /users/{id} 接口，返回完整用户信息
    """
    id: int
    username: str
    gender: str
    avatar_url: str | None
    cover_url: str | None
    bio: str | None  # 个人简介
    created_at: str  # 注册时间

    # 统计数据
    follower_count: int  # 粉丝数
    following_count: int  # 关注数
    liked_total_count: int  # 获赞总数


class MessageCreate(SQLModel):
    """发送消息时的入参"""
    receiver_id: int
    content: str


class MessageOut(SQLModel):
    """返回给前端的消息结构"""
    id: int
    sender_id: int
    receiver_id: int
    content: str
    is_read: bool
    created_at: datetime


class ChatConversation(SQLModel):
    user_id: int  # 对方的用户ID
    username: str  # 对方的用户名
    avatar_url: str | None  # 对方的头像
    last_message: str  # 最后一句话
    last_message_time: datetime  # 最后一句话的时间
    unread_count: int  # 未读消息数


class UserAvatar(SQLModel):
    """仅返回头像"""
    avatar_url: str | None


class UserUpdate(SQLModel):
    """用户更新个人信息的数据模型"""
    username: str | None = None
    bio: str | None = None
    gender: str | None = None


class DownloadFileType(str, Enum):
    """
    下载文件类型枚举
    对应前端选项：
    - OBJ (Universal)   -> obj
    - GLB (Web/AR)      -> glb
    - PLY (Point Cloud) -> ply
    - SOURCE DATA       -> msgpack (现阶段源数据即为训练好的 msgpack)
    """
    OBJ = "obj"
    GLB = "glb"
    PLY = "ply"
    SOURCE = "msgpack"


class DownloadResponse(SQLModel):
    """下载接口响应，返回真实文件链接"""
    url: str
    filename: str


class AssetUpdate(SQLModel):
    """资产更新请求模型"""
    title: str | None = None
    description: str | None = None
    remark: str | None = None
    tags: List[str] | None = None


class AssetReport(SQLModel):
    """举报/反馈请求模型"""
    category: str  # 例如: "Bug", "Inappropriate", "Other"
    content: str
