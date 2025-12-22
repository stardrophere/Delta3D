from datetime import datetime
from typing import List
from fastapi import APIRouter, Depends, UploadFile, File, Form, HTTPException, BackgroundTasks
from sqlmodel import Session

import shutil
import uuid
import os
from pathlib import Path

from app.database import get_session
from app.models import User, ModelAsset
from app.schemas import AssetCard, DownloadResponse, DownloadFileType, AssetUpdate, AssetReport
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
    # raise HTTPException(
    #     status_code=401,
    #     detail="Testing Token Expiration"
    # )
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
        estimated_time: int = Form(default=None),
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    upload_root = Path(settings.UPLOAD_DIR)
    upload_root.mkdir(parents=True, exist_ok=True)

    # 每个model一个独立目录
    asset_uid = uuid.uuid4().hex
    asset_dir = upload_root / asset_uid
    asset_dir.mkdir(parents=True, exist_ok=False)

    # (asset_dir / "outputs").mkdir(exist_ok=True)
    # (asset_dir / "temp").mkdir(exist_ok=True)

    # 视频统一叫 video.<ext>
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

    # web 路径
    web_asset_base = f"/static/uploads/{asset_uid}"
    video_disk_path = str(video_disk_path)
    snapshot_disk_path = str((asset_dir / "model.msgpack"))
    web_model_path = snapshot_disk_path

    # tags
    tag_list = [t.strip() for t in tags.split(",") if t.strip()]

    # video_path 只存 base
    new_asset = crud_asset.create_asset(
        session=session,
        user_id=current_user.id,
        title=title,
        video_path=web_asset_base,
        description=description,
        tags=tag_list,
        remark=remark,
        estimated_gen_seconds=estimated_time
    )

    background_tasks.add_task(
        task_train_asset,
        new_asset.id,
        video_disk_path,
        snapshot_disk_path,
        web_model_path
    )

    return AssetCard(
        id=new_asset.id,
        title=new_asset.title,
        cover_url=web_asset_base,
        description=new_asset.description,
        tags=new_asset.tags,
        is_collected=False,
        created_at=str(new_asset.created_at),
        status=new_asset.status,
        height=new_asset.height,
        owner_id=current_user.id
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
    # 查询数据库
    asset = session.get(ModelAsset, asset_id)

    # 断是否存在
    if not asset:
        raise HTTPException(status_code=404, detail="模型资产不存在")

    # 权限校验
    if asset.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="无权访问该资产")

    # 返回数据
    return AssetDetail(
        id=asset.id,
        title=asset.title,
        description=asset.description,
        remark=asset.remark,
        tags=asset.tags,
        video_url=asset.video_path,
        model_url=asset.model_path,
        status=asset.status,
        created_at=str(asset.created_at),
        estimated_gen_seconds=asset.estimated_gen_seconds
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

    return ToggleResponse(is_active=is_collected, new_count=0)


@router.get("/me/collected", response_model=List[AssetCard])
def read_my_collected_assets(
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    获取【我收藏的模型】列表
    """

    collected_assets = current_user.collected_assets

    # 将 ModelAsset 转换为 AssetCard 格式返回
    return [
        AssetCard(
            id=asset.id,
            title=asset.title,
            cover_url=asset.video_path,
            description=asset.description,
            tags=asset.tags,
            is_collected=True,
            created_at=asset.created_at,
            status=asset.status,
            height=asset.height,
            owner_id=asset.user_id
        )
        for asset in collected_assets
    ]


@router.post("/{asset_id}/download", response_model=DownloadResponse)
def download_asset_file(
        asset_id: int,
        file_type: DownloadFileType,  # 前端传递 obj/glb/ply/source
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    下载模型接口
    1. 校验资产是否存在
    2. 记录下载历史
    3. 返回文件真实链接 (目前统一返回 msgpack)
    """
    # 检查资产
    asset = session.get(ModelAsset, asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="资产不存在")

    if not asset.model_path:
        raise HTTPException(status_code=400, detail="模型文件尚未生成或已丢失")

    # 记录下载
    crud_asset.record_download(session, current_user.id, asset.id)

    # 返回的文件路径
    real_url = asset.model_path

    file_extension = ".msgpack"
    safe_title = asset.title.replace(" ", "_").replace("/", "_")
    filename = f"{safe_title}_{file_type.value}{file_extension}"

    return DownloadResponse(url=real_url, filename=filename)


@router.get("/me/downloads", response_model=List[AssetCard])
def read_my_downloads(
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    """
    获取【我下载过的模型】列表
    用于个人中心 -> 下载历史
    """
    assets = crud_asset.get_my_downloaded_assets(session=session, user_id=current_user.id)
    return assets


# 更新资产信息接口
@router.patch("/{asset_id}", response_model=AssetDetail)
def update_asset(
        asset_id: int,
        update_data: AssetUpdate,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    asset = session.get(ModelAsset, asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="Asset not found")

    if asset.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Permission denied")

    # 更新字段
    if update_data.title is not None:
        asset.title = update_data.title
    if update_data.description is not None:
        asset.description = update_data.description
    if update_data.remark is not None:
        asset.remark = update_data.remark
    if update_data.tags is not None:
        asset.tags = update_data.tags

    session.add(asset)
    session.commit()
    session.refresh(asset)

    # 返回更新后的详情结构
    return AssetDetail(
        id=asset.id,
        title=asset.title,
        description=asset.description,
        remark=asset.remark,
        tags=asset.tags,
        video_url=asset.video_path,
        model_url=asset.model_path,
        status=asset.status,
        created_at=str(asset.created_at),
        estimated_gen_seconds=asset.estimated_gen_seconds
    )


# 举报/反馈接口
@router.post("/{asset_id}/report")
def report_issue(
        asset_id: int,
        report: AssetReport,
        current_user: User = Depends(get_current_user)
):
    log_file = Path("asset_reports.log")
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

    log_entry = (
        f"[{timestamp}] REPORT | AssetID: {asset_id} | User: {current_user.username} (ID:{current_user.id}) | "
        f"Category: {report.category} | Content: {report.content}\n"
    )

    try:
        with open(log_file, "a", encoding="utf-8") as f:
            f.write(log_entry)
        return {"status": "success", "message": "Report logged."}
    except Exception as e:
        raise HTTPException(status_code=500, detail="Failed to log report")
