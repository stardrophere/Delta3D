from datetime import datetime
from enum import Enum
from sqlmodel import SQLModel

# 注册时，前端需要传的数据
class UserCreate(SQLModel):
    username: str
    password: str

# 登录成功后，后端返回的数据
class Token(SQLModel):
    access_token: str
    token_type: str

# 获取用户信息时，返回的数据 (不包含密码)
class UserOut(SQLModel):
    id: int
    username: str
    avatar_url: str | None = None



class AssetCard(SQLModel):
    """
    【模型卡片】用于“我的模型”页面展示
    对应你截图里的每一个小方块
    """
    id: int
    title: str  # 标题 (如: 玩偶熊)
    cover_url: str  # 封面图 (暂时用视频地址 video_path)
    description: str | None  # 描述 (如: 学院送的熊)
    tags: list[str] # 标签
    is_collected: bool  # ★ 关键：那个星星有没有亮
    created_at: datetime
    status: str #模型状态


class PostCard(SQLModel):
    """
    【社区帖子卡片】
    包含帖子基本信息、作者信息、以及当前用户与帖子的交互状态
    """
    # 1. 帖子基本信息
    post_id: int
    asset_id: int
    title: str  # 标题
    cover_url: str  # 封面 (视频路径)
    description: str | None  # 描述
    tags: list[str]  # 标签
    published_at: str  # 发布时间

    # 2. 统计数据
    like_count: int
    view_count: int = 0  # 浏览量 (可选)

    # 3. 作者信息
    owner_id: int  # 作者ID (方便前端做跳转)
    owner_name: str  # 作者用户名
    owner_avatar: str | None  # 作者头像

    # 4. 当前用户与帖子的交互状态 (关键需求)
    is_liked: bool  # 我是否点赞
    is_collected: bool  # 我是否收藏
    has_commented: bool  # 我是否评论过
    is_following: bool  # 我是否关注了该作者

class AssetDetail(SQLModel):
    """
    【模型详情】用于点击进入单个模型页面时展示
    包含隐私字段 remark
    """
    id: int
    title: str
    description: str | None
    remark: str | None      # ★只有详情页才返回这个
    tags: list[str]
    video_url: str          # 对应数据库的 video_path
    model_url: str | None   # 对应数据库的 model_path (生成的模型文件)
    status: str             # 状态 (pending/processing/completed/failed)
    created_at: str         # 时间字符串

class PostCreate(SQLModel):
    """创建帖子时的请求参数"""
    asset_id: int
    content: str | None = None  # 帖子正文
    visibility: str = "public"  # 默认公开
    allow_download: bool = True

# 1. 通用的点赞/收藏响应
class ToggleResponse(SQLModel):
    is_active: bool  # True=已点赞/已收藏, False=已取消
    new_count: int   # 最新的总数 (点赞数/收藏数)

# 2. 创建评论的请求参数
class CommentCreate(SQLModel):
    content: str
    parent_id: int | None = None # 如果是回复别人的评论，填这个

# 3. 评论返回结构
class CommentOut(SQLModel):
    id: int
    user_id: int
    username: str
    avatar_url: str | None
    content: str
    created_at: str






class StreamActionType(str, Enum):
    ROTATE = "rotate"  # 对应鼠标左键
    PAN = "pan"  # 对应鼠标中键
    ZOOM = "zoom"  # 对应滚轮


class StreamDirection(str, Enum):
    # --- 新增通用方向 (用于旋转和平移) ---
    UP = "up"
    DOWN = "down"
    LEFT = "left"
    RIGHT = "right"

    # --- 原有方向 (保留) ---
    CLOCKWISE = "clockwise"
    COUNTER_CLOCKWISE = "counter_clockwise"

    # --- 缩放方向 ---
    IN = "in"
    OUT = "out"


class ControlCommand(SQLModel):
    action: StreamActionType
    direction: StreamDirection
    mode: str = "start"  # "start" (按下) 或 "stop" (松开)


# StreamStatus 保持不变
class StreamStatus(SQLModel):
    is_active: bool
    rtsp_url: str | None
    current_asset_id: int | None