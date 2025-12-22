from fastapi import APIRouter, Depends, HTTPException
from sqlmodel import Session

from app.core.config import settings
from app.database import get_session
from app.models import User, ModelAsset, AssetStatus
from app.api.deps import get_current_user
from app.schemas import StreamStatus, ControlCommand
from app.core.stream_manager import stream_session
from pathlib import Path

router = APIRouter()

from pathlib import Path
from fastapi import Depends, HTTPException, Request


@router.post("/start/{asset_id}", response_model=StreamStatus)
def start_stream(
        asset_id: int,
        request: Request,
        session: Session = Depends(get_session),
        current_user: User = Depends(get_current_user)
):
    asset = session.get(ModelAsset, asset_id)
    if not asset:
        raise HTTPException(status_code=404, detail="模型不存在")

    if asset.status != "completed" or not asset.model_path:
        raise HTTPException(status_code=400, detail="模型尚未训练完成，无法预览")


    model_path_rel = Path(asset.model_path)  # 相对路径

    parts = model_path_rel.parts
    if not parts or parts[0].lower() != "static":
        raise HTTPException(status_code=500, detail=f"model_path 不合法(必须 static 开头): {asset.model_path}")


    under_static = Path(*parts[1:])


    static_root = Path("static").resolve()


    snapshot_path = static_root / under_static


    asset_dir = snapshot_path.parent
    scene_path = asset_dir / f"{asset_dir.name}_scene"

    # ---- debug 打印 ----
    print("\n========== [PATH DEBUG] ==========")
    print(f"asset.model_path = {asset.model_path}")
    print(f"static_root      = {static_root}")
    print(f"under_static     = {under_static}")
    print(f"snapshot_path    = {snapshot_path}")
    print(f"asset_dir        = {asset_dir}")
    print(f"scene_path       = {scene_path}")
    print("=================================\n")

    # 校验 snapshot
    if not snapshot_path.exists():
        print("!! snapshot_path NOT EXISTS")
        raise HTTPException(status_code=404, detail=f"模型文件丢失: {snapshot_path}")

    # 校验 scene
    if not scene_path.exists():
        print("!! scene_path(guess) NOT EXISTS")
        transforms_in_asset_dir = asset_dir / "transforms.json"
        print(f"transforms.json@asset_dir = {transforms_in_asset_dir} exists={transforms_in_asset_dir.exists()}")

        if transforms_in_asset_dir.exists():
            scene_path = asset_dir
            print(f"scene_path(fallback)= {scene_path}")
        else:
            print(f"调试路径(scene_path guess): {scene_path}")
            raise HTTPException(status_code=404, detail="场景数据丢失 (transforms.json)")

    print("========== [start_stream] PATH DEBUG END ==========\n")

    stream_session.start(
        asset_id=asset.id,
        scene_path=str(scene_path),
        snapshot_path=str(snapshot_path)
    )

    host = request.url.hostname
    # host = request.headers.get("host", "").split(":")[0]

    rtsp_url = f"rtsp://{host}:8555/live"

    return StreamStatus(
        is_active=True,
        rtsp_url=rtsp_url,
        current_asset_id=asset.id
    )


@router.post("/stop")
def stop_stream(
        current_user: User = Depends(get_current_user)
):
    """停止推流"""
    stream_session.stop()
    return {"message": "推流已停止"}


@router.post("/control")
def control_view(
        cmd: ControlCommand,
        current_user: User = Depends(get_current_user)
):
    """
    操控窗口 (旋转/缩放/平移)
    mode="start" 开始连续动作
    mode="stop" 停止动作
    """
    if not stream_session.is_running:
        raise HTTPException(status_code=400, detail="推流未启动")

    stream_session.control(
        action=cmd.action.value,
        direction=cmd.direction.value,
        mode=cmd.mode
    )
    return {"status": "ok", "cmd": cmd}
