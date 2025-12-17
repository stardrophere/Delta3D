import sys
import time
from pathlib import Path

from app.process_manager.utils import ExternalCommandRunner  # 如果你类名不同，这里改成你的实际类名


def run_ngp_gui_with_rtsp_stream(
    scene_path: str,
    snapshot_path: str,
    *,
    venv_python: str,
    ngp_script_path: str,
    stream_seconds: int = 700,
    startup_delay: int = 3,
    window_title: str = "Instant Neural Graphics Primitives",
    rtsp_url: str = "rtsp://127.0.0.1:8554/live",
    ffmpeg_bin: str = "ffmpeg",
    wait_for_gui_close: bool = True,
) -> None:
    """
    启动 Instant-NGP GUI 并用 ffmpeg 抓取指定窗口推 RTSP。
    - 只需传 scene_path + snapshot_path（但 venv_python/ngp_script_path 你需要从外部传入或写死）
    """

    scene_p = Path(scene_path).resolve()
    snap_p = Path(snapshot_path).resolve()
    venv_p = Path(venv_python).resolve()
    ngp_p = Path(ngp_script_path).resolve()

    if not scene_p.exists():
        raise FileNotFoundError(f"scene 路径不存在: {scene_p}")
    if not snap_p.exists():
        raise FileNotFoundError(f"snapshot 路径不存在: {snap_p}")
    if not venv_p.exists():
        raise FileNotFoundError(f"venv python 不存在: {venv_p}")
    if not ngp_p.exists():
        raise FileNotFoundError(f"ngp script 不存在: {ngp_p}")

    ngp_command = [
        str(venv_p),
        str(ngp_p),
        "--scene", str(scene_p),
        "--load_snapshot", str(snap_p),
        "--gui",
    ]

    ffmpeg_command = [
        ffmpeg_bin,
        "-fflags", "+genpts+flush_packets+nobuffer",
        "-probesize", "32",
        "-analyzeduration", "0",
        "-use_wallclock_as_timestamps", "1",
        "-f", "gdigrab",
        "-framerate", "30",
        "-draw_mouse", "0",
        "-i", f"title={window_title}",
        "-vf", "format=yuv420p",
        "-vcodec", "h264_nvenc",
        "-preset", "llhq",
        "-tune", "ll",
        "-bf", "0",
        "-g", "15",
        "-keyint_min", "15",
        "-rc-lookahead", "0",
        "-rc", "constqp",
        "-b:v", "0",
        "-qp", "19",
        "-movflags", "frag_keyframe+empty_moov",
        "-rtsp_transport", "tcp",
        "-rtsp_flags", "prefer_tcp",
        "-f", "rtsp",
        rtsp_url,
    ]

    try:
        with ExternalCommandRunner(ngp_command) as ngp_runner:
            if not ngp_runner.is_running():
                raise RuntimeError("NGP GUI 启动失败（进程未在运行）")

            print(f"GUI 进程 {ngp_runner.process.pid} 正在运行...")
            time.sleep(startup_delay)

            print("----- 启动 FFMPEG 推流任务 -----")
            with ExternalCommandRunner(ffmpeg_command) as ffmpeg_runner:
                if not ffmpeg_runner.is_running():
                    raise RuntimeError("FFMPEG 启动失败（进程未在运行）")

                print(f"FFMPEG 进程 {ffmpeg_runner.process.pid} 已启动。")
                print(f"推流将在后台持续进行 {stream_seconds} 秒...")
                time.sleep(stream_seconds)
                print("推流时间到，停止推流...")

            print("FFMPEG 已通过 stop()/with 块安全关闭。")

            if wait_for_gui_close:
                print("等待你手动关闭 Instant-NGP GUI 窗口...")
                ngp_runner.process.wait()

        print("GUI 进程已清理。")

    except KeyboardInterrupt:
        print("\n主程序中断。")
        sys.exit(0)
    except Exception as e:
        print(f"发生错误: {e}")
        sys.exit(1)