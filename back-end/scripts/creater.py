import os
from pathlib import Path
from typing import Optional, Dict, Any
from app.process_manager.utils import run_and_stream, NonBlockingCommandRunner
from dotenv import load_dotenv

load_dotenv()

ENV_NGP_PYTHON = os.getenv("NGP_PYTHON_PATH")
ENV_COLMAP2NERF = os.getenv("COLMAP2NERF_SCRIPT_PATH")
ENV_NGP_RUN = os.getenv("NGP_RUN_SCRIPT_PATH")


def train_ngp_from_video(
        video_path: str,
        snapshot_path: str,
        *,
        venv_python: Optional[str] = None,
        colmap2nerf_script: Optional[str] = None,
        ngp_run_script: Optional[str] = None,
        video_fps: int = 5,
        n_steps: int = 5000,
        colmap_camera_model: str = "SIMPLE_RADIAL",
        aabb_scale: int = 8,
        colmap_matcher: str = "exhaustive",
        # sharpen_strength: float = 0.0,
        user_input: str = "y\ny\n",
        scene_dir: Optional[str] = None,
) -> Dict[str, Any]:
    """
    输入：
      - video_path: 视频文件路径
      - snapshot_path: 训练输出的模型快照路径（.msgpack）

    输出：
      - 返回 dict，包含 scene_dir、transforms.json 路径、抽帧数量等信息

    失败时：
      - 抛出 RuntimeError / FileNotFoundError
    """

    venv_python = venv_python or ENV_NGP_PYTHON
    colmap2nerf_script = colmap2nerf_script or ENV_COLMAP2NERF
    ngp_run_script = ngp_run_script or ENV_NGP_RUN

    # 完整性检查
    if not venv_python:
        raise ValueError("未配置 Python 路径: 请在 .env 设置 NGP_PYTHON_PATH 或在调用时传入")
    if not colmap2nerf_script:
        raise ValueError("未配置 colmap2nerf 路径: 请在 .env 设置 COLMAP2NERF_SCRIPT_PATH")
    if not ngp_run_script:
        raise ValueError("未配置 run.py 路径: 请在 .env 设置 NGP_RUN_SCRIPT_PATH")
    video_path = Path(video_path).resolve()

    video_dir = str(video_path.parent)
    # print(video_dir, type(video_path))
    video_path = str(video_path)
    # return

    if not Path(video_path).exists():
        raise FileNotFoundError(f"视频不存在: {video_path}")

    if not Path(venv_python).exists():
        raise FileNotFoundError(f"Python 不存在: {venv_python}")

    if not Path(colmap2nerf_script).exists():
        raise FileNotFoundError(f"colmap2nerf.py 不存在: {colmap2nerf_script}")

    if not Path(ngp_run_script).exists():
        raise FileNotFoundError(f"run.py 不存在: {ngp_run_script}")

    # if scene_dir is None:
    #     scene_path = snapshot_path.parent / f"{snapshot_path.stem}_scene"
    # else:
    #     scene_path = Path(scene_dir).resolve()
    #
    # scene_path.mkdir(parents=True, exist_ok=True)

    # transforms.json
    video_dir = Path(video_path).resolve().parent
    transforms_path = video_dir / "transforms.json"

    # colmap2nerf：视频 -> transforms.json
    colmap_cmd = [
        venv_python,
        colmap2nerf_script,
        "--colmap_camera_model", colmap_camera_model,
        "--aabb_scale", str(aabb_scale),
        "--video_in", str(video_path),
        "--video_fps", str(video_fps),
        "--run_colmap",
        "--colmap_matcher", colmap_matcher,
        "--out", str(transforms_path),
        "--overwrite",
        # "--sharpen_strength", str(sharpen_strength)
    ]

    success, tip, frames = run_and_stream(colmap_cmd, user_input, cwd=video_dir)

    if (not success) or (tip == 0):
        raise RuntimeError(
            f"colmap2nerf 失败或未收敛：success={success}, tip={tip}, frames={frames}\n"
            f"cmd={' '.join(colmap_cmd)}"
        )

    if not Path(transforms_path).exists():
        raise RuntimeError(f"transforms.json 未生成：{transforms_path}")

    # 练并保存 snapshot
    # snapshot_path.parent.mkdir(parents=True, exist_ok=True)

    train_cmd = [
        venv_python,
        str(Path(ngp_run_script).resolve()),
        "--scene", str(video_dir),
        "--n_steps", str(n_steps),
        "--save_snapshot", str(snapshot_path),
    ]
    print(train_cmd)

    runner = NonBlockingCommandRunner(train_cmd)
    runner.run()

    if not Path(snapshot_path).exists():
        raise RuntimeError(f"训练结束但未找到 snapshot：{snapshot_path}")

    return {
        "scene_dir": str(video_dir),
        "transforms_json": str(transforms_path),
        "snapshot_path": str(snapshot_path),
        "frames": frames,
        "video_path": video_path,
    }

# train_ngp_from_video(r"E:\ScnuProject\2025-Autumn-Aberdeen-10-Delta3D\back-end\static\uploads\b2c795e8ce8042068e521cf618abc02c\video.mp4",r"E:\ScnuProject\2025-Autumn-Aberdeen-10-Delta3D\back-end\static\uploads\b2c795e8ce8042068e521cf618abc02c\model.")
