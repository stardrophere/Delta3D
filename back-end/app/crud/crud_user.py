from sqlmodel import Session, select
from app.models import User, UserFollow
from app.schemas import UserCreate, UserUpdate
from app.core.security import get_password_hash


def toggle_follow(session: Session, follower_id: int, followed_id: int) -> tuple[bool, int]:
    """
    切换关注状态

    Returns:
        is_following: bool (当前是否关注)
        new_follower_count: int (目标用户最新的粉丝数，用于前端更新 UI)
    """
    if follower_id == followed_id:
        return False, 0  # 不能关注自己

    # 查关注记录
    statement = select(UserFollow).where(
        UserFollow.follower_id == follower_id,
        UserFollow.followed_id == followed_id
    )
    link = session.exec(statement).first()

    # 获取两个用户实体
    follower = session.get(User, follower_id)  # 我
    target_user = session.get(User, followed_id)  # 我要关注的人

    if not follower or not target_user:
        return False, 0

    if link:
        # 取消关注
        session.delete(link)

        # 更新计数
        follower.following_count = max(0, follower.following_count - 1)
        target_user.follower_count = max(0, target_user.follower_count - 1)

        is_following = False
    else:
        # --- 关注 ---
        new_link = UserFollow(follower_id=follower_id, followed_id=followed_id)
        session.add(new_link)

        # 更新计数
        follower.following_count += 1
        target_user.follower_count += 1

        is_following = True


    session.add(follower)
    session.add(target_user)
    session.commit()

    # 刷新目标用户数据，返回最新的粉丝数
    session.refresh(target_user)

    return is_following, target_user.follower_count


DEFAULT_AVATAR = "/static/avatars/default.png"
DEFAULT_COVER = "/static/avatars/default_cover.png"


def create_user(session: Session, user_in: UserCreate) -> User:
    """创建新用户"""
    # 将明文密码加密
    hashed_password = get_password_hash(user_in.password)

    # 创建数据库对象
    db_user = User(
        username=user_in.username,
        password_hash=hashed_password,
        avatar_url=DEFAULT_AVATAR,
        cover_url=DEFAULT_COVER
    )


    session.add(db_user)
    session.commit()
    session.refresh(db_user)
    return db_user


# 更新头像
def update_avatar(session: Session, user_id: int, new_avatar_url: str) -> User:
    """更新用户头像"""
    user = session.get(User, user_id)
    if user:
        user.avatar_url = new_avatar_url
        session.add(user)
        session.commit()
        session.refresh(user)
    return user


# 更新背景图
def update_cover(session: Session, user_id: int, cover_url: str) -> User:
    """
    更新用户背景图 URL
    """
    user = session.get(User, user_id)
    if user:
        user.cover_url = cover_url
        session.add(user)
        session.commit()
        session.refresh(user)
    return user


def get_user_by_username(session: Session, username: str) -> User | None:
    """通过用户名查找用户"""
    statement = select(User).where(User.username == username)
    return session.exec(statement).first()


def update_profile(session: Session, user_id: int, user_in: UserUpdate) -> User:
    """
    更新用户基本信息
    """
    user = session.get(User, user_id)
    if not user:
        return None

    # 排除没有传的字段
    update_data = user_in.model_dump(exclude_unset=True)

    for key, value in update_data.items():
        setattr(user, key, value)

    session.add(user)
    session.commit()
    session.refresh(user)
    return user
