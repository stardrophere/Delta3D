from typing import List
from sqlmodel import Session, select, func
from app.models import ModelAsset, ModelCollection, AssetStatus, DownloadRecord


def get_my_assets(session: Session, user_id: int) -> List[dict]:
    """
    获取【我创建的】所有模型，并标记【我是否收藏】了它
    """
    # 查询
    statement = (
        select(ModelAsset)
        .where(ModelAsset.user_id == user_id)
        .order_by(ModelAsset.created_at.desc())
    )
    assets = session.exec(statement).all()

    # 查收藏状态
    collection_stmt = select(ModelCollection.asset_id).where(ModelCollection.user_id == user_id)
    my_collected_ids = set(session.exec(collection_stmt).all())

    # 组装数据
    results = []
    for asset in assets:
        results.append({
            "id": asset.id,
            "title": asset.title,
            "cover_url": asset.video_path,
            "description": asset.description,
            "tags": asset.tags,
            "is_collected": asset.id in my_collected_ids,
            "created_at": asset.created_at,
            "status": asset.status,
            'height': asset.height,
            "owner_id": asset.user_id
        })

    return results


def create_asset(
        session: Session,
        user_id: int,
        title: str,
        video_path: str,
        description: str | None,
        tags: List[str],
        remark: str | None,
        estimated_gen_seconds: int | None = None
) -> ModelAsset:
    """
    创建新的模型资产记录
    """
    db_asset = ModelAsset(
        user_id=user_id,
        title=title,
        video_path=video_path,
        description=description,
        tags=tags,
        remark=remark,
        estimated_gen_seconds=estimated_gen_seconds,
        status=AssetStatus.PENDING,

    )

    session.add(db_asset)
    session.commit()
    session.refresh(db_asset)
    return db_asset


def toggle_collection(session: Session, user_id: int, asset_id: int) -> bool:
    """
    切换模型收藏状态
    Returns: True(收藏成功), False(取消收藏)
    """
    # 查一下有没有收藏过
    statement = select(ModelCollection).where(
        ModelCollection.user_id == user_id,
        ModelCollection.asset_id == asset_id
    )
    link = session.exec(statement).first()

    if link:
        # 如果有 -> 删除 (取消收藏)
        session.delete(link)
        is_collected = False
    else:
        # 如果没有 -> 添加 (收藏)
        new_link = ModelCollection(user_id=user_id, asset_id=asset_id)
        session.add(new_link)
        is_collected = True

    session.commit()
    return is_collected


def record_download(session: Session, user_id: int, asset_id: int) -> None:
    """
    记录下载行为
    """
    # 记录到 DownloadRecord 表
    new_record = DownloadRecord(user_id=user_id, asset_id=asset_id)
    session.add(new_record)
    session.commit()


def get_my_downloaded_assets(session: Session, user_id: int) -> List[dict]:
    """
    获取【我下载过的】所有模型
    按【最近下载时间】倒序排列
    """
    # 同时查 ModelAsset 和 最近的下载时间
    statement = (
        select(ModelAsset, func.max(DownloadRecord.created_at).label("last_download_time"))
        .join(DownloadRecord, ModelAsset.id == DownloadRecord.asset_id)
        .where(DownloadRecord.user_id == user_id)
        .group_by(ModelAsset.id)  # 按模型分组，去重
        .order_by(func.max(DownloadRecord.created_at).desc())  # 按下载时间倒序，最近的在前面
    )

    # exec(statement).all() 返回的是 [(ModelAsset, datetime), (ModelAsset, datetime), ...]
    results_with_time = session.exec(statement).all()

    # 查收藏状态
    collection_stmt = select(ModelCollection.asset_id).where(ModelCollection.user_id == user_id)
    my_collected_ids = set(session.exec(collection_stmt).all())

    # 组装数据
    results = []
    for asset, last_download_time in results_with_time:
        results.append({
            "id": asset.id,
            "title": asset.title,
            "cover_url": asset.video_path,
            "description": asset.description,
            "tags": asset.tags,
            "is_collected": asset.id in my_collected_ids,
            "created_at": asset.created_at,
            "status": asset.status,
            "height": asset.height,
            "owner_id": asset.user_id,
            "downloaded_at": last_download_time
        })

    return results
