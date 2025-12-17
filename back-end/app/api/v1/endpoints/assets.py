from typing import List
from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException, BackgroundTasks
from sqlmodel import Session

import shutil
import uuid
import os
from pathlib import Path

from app.database import get_session
from app.models import User, ModelAsset
from app.schemas import AssetCard
from app.api.deps import get_current_user
from app.crud import crud_asset
from app.core.config import settings
from app.ngp.worker import task_train_asset
from app.schemas import AssetDetail
from app.schemas import ToggleResponse

router = APIRouter()


@router.get("/me", response_model=List[AssetCard])
def read_my_assets(
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    获取【我的模型库】列表
    对应设计图中的网格展示
    """
    assets = crud_asset.get_my_assets(session=session, user_id=current_user.id)
    return assets


@router.post("/upload", response_model=AssetCard)
def upload_asset(
        background_tasks: BackgroundTasks,
        file: UploadFile = File(...),
        title: str = Form(...),
        description: str = Form(default=None),
        tags: str = Form(default=""),
        remark: str = Form(default=None),
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    upload_root = Path(settings.UPLOAD_DIR)  # 例如 ./static/uploads
    upload_root.mkdir(parents=True, exist_ok=True)

    # 1) 每个资产一个独立目录
    asset_uid = uuid.uuid4().hex
    asset_dir = upload_root / asset_uid
    asset_dir.mkdir(parents=True, exist_ok=False)

    # # （可选）预建一些产物目录
    # (asset_dir / "outputs").mkdir(exist_ok=True)
    # (asset_dir / "temp").mkdir(exist_ok=True)

    # 2) 视频统一叫 video.<ext>
    video_filename = f"video.mp4"
    video_disk_path = asset_dir / video_filename

    try:
        with video_disk_path.open("wb") as buffer:
            shutil.copyfileobj(file.file, buffer)
    except Exception:
        shutil.rmtree(asset_dir, ignore_errors=True)
        raise HTTPException(status_code=500, detail="文件保存失败")
    finally:
        try:
            file.file.close()
        except Exception:
            pass

    # 3) web 路径
    web_asset_base = f"/static/uploads/{asset_uid}"
    video_disk_path = str(video_disk_path)  # 真实磁盘路径
    snapshot_disk_path = str((asset_dir / "model.msgpack"))
    web_model_path = snapshot_disk_path

    # 4) tags
    tag_list = [t.strip() for t in tags.split(",") if t.strip()]

    # 5) 写库：video_path 只存 base
    new_asset = crud_asset.create_asset(
        session=session,
        user_id=current_user.id,
        title=title,
        video_path=web_asset_base,
        description=description,
        tags=tag_list,
        remark=remark
    )

    background_tasks.add_task(
        task_train_asset,
        new_asset.id,
        video_disk_path,
        snapshot_disk_path,
        web_model_path
    )

    # 6) 返回：cover_url
    return AssetCard(
        id=new_asset.id,
        title=new_asset.title,
        cover_url=web_asset_base,
        description=new_asset.description,
        tags=new_asset.tags,
        is_collected=False,
        created_at=str(new_asset.created_at),
        status=new_asset.status
    )


@router.get("/{asset_id}", response_model=AssetDetail)
def read_asset_detail(
        asset_id: int,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    获取单个模型资产的详细信息
    权限：仅拥有者可见（因为包含 remark）
    """
    # 1. 查询数据库
    asset = session.get(ModelAsset, asset_id)

    # 2. 判断是否存在
    if not asset:
        raise HTTPException(status_code=404, detail="模型资产不存在")

    # 3. 权限校验
    # 如果当前用户的ID 不等于 资产的主人ID，这就不是他的模型，不能给他看备注
    if asset.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="无权访问该资产")

    # 4. 返回数据
    return AssetDetail(
        id=asset.id,
        title=asset.title,
        description=asset.description,
        remark=asset.remark,
        tags=asset.tags,
        video_url=asset.video_path,
        model_url=asset.model_path,
        status=asset.status,
        created_at=str(asset.created_at)
    )


@router.post("/{asset_id}/collect", response_model=ToggleResponse)
def collect_asset(
        asset_id: int,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """收藏/取消收藏 模型"""
    # 检查是否存在
    asset = session.get(ModelAsset, asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="资产不存在")

    is_collected = crud_asset.toggle_collection(session, current_user.id, asset_id)

    # 模型表里没有专门的 collect_count 字段，所以 new_count 返回 0 或前端自己维护
    return ToggleResponse(is_active=is_collected, new_count=0)
