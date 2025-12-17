from typing import List
from sqlmodel import Session, select
from app.models import ModelAsset, ModelCollection,AssetStatus


def get_my_assets(session: Session, user_id: int) -> List[dict]:
    """
    获取【我创建的】所有模型，并标记【我是否收藏】了它
    """
    # 1. 查询
    statement = (
        select(ModelAsset)
        .where(ModelAsset.user_id == user_id)
        .order_by(ModelAsset.created_at.desc())
    )
    assets = session.exec(statement).all()

    # 2. 查收藏状态
    collection_stmt = select(ModelCollection.asset_id).where(ModelCollection.user_id == user_id)
    my_collected_ids = set(session.exec(collection_stmt).all())

    # 3. 组装数据
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
            "status": asset.status
        })

    return results


def create_asset(
        session: Session,
        user_id: int,
        title: str,
        video_path: str,
        description: str | None,
        tags: List[str],
        remark: str | None
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
    # 1. 查一下有没有收藏过
    statement = select(ModelCollection).where(
        ModelCollection.user_id == user_id,
        ModelCollection.asset_id == asset_id
    )
    link = session.exec(statement).first()

    if link:
        # 2. 如果有 -> 删除 (取消收藏)
        session.delete(link)
        is_collected = False
    else:
        # 3. 如果没有 -> 添加 (收藏)
        new_link = ModelCollection(user_id=user_id, asset_id=asset_id)
        session.add(new_link)
        is_collected = True

    session.commit()
    return is_collected