import threading
import time
import sys
from pathlib import Path
from typing import Optional
import shutil

from app.window_controller.continuous import ContinuousController
from app.process_manager.utils import ExternalCommandRunner


class InteractiveStreamSession:
    """
    管理单次推流会话：
    1. 启动 Instant-NGP
    2. 启动 FFMPEG
    3. 接收鼠标控制指令
    """

    def __init__(self):
        self.is_running = False
        self.process_thread: Optional[threading.Thread] = None
        self.stop_event = threading.Event()
        self.controller = ContinuousController()

        # 状态记录
        self.current_asset_id: int | None = None
        self.rtsp_url: str = "rtsp://127.0.0.1:8555/live"

    def start(self, asset_id: int, scene_path: str, snapshot_path: str):
        """启动推流会话（如果已有会话则先停止）"""
        if self.is_running:
            self.stop()

        self.current_asset_id = asset_id
        self.stop_event.clear()
        self.is_running = True

        # 在后台线程启动 NGP 和 FFMPEG，防止阻塞 API
        self.process_thread = threading.Thread(
            target=self._run_processes,
            args=(scene_path, snapshot_path),
            daemon=True
        )
        self.process_thread.start()

    def stop(self):
        """停止当前会话"""
        if not self.is_running:
            return

        print("正在停止推流会话...")
        # 停止鼠标操作
        self.controller.stop()

        # 信号通知后台线程退出
        self.stop_event.set()

        # 等待线程结束
        if self.process_thread and self.process_thread.is_alive():
            self.process_thread.join(timeout=5)

        self.is_running = False
        self.current_asset_id = None
        print("推流会话已结束")

    def control(self, action: str, direction: str, mode: str):
        """处理控制指令"""
        if not self.is_running:
            return

        # 停止
        if mode == "stop":
            self.controller.stop()
            return

        # 开始
        # --- 速度配置参数 ---
        # 旋转: 距离越大越快，时间越短越丝滑
        ROTATE_DIST = 15
        ROTATE_TIME = 0.05

        # 平移
        PAN_DIST = 20
        PAN_TIME = 0.05

        # 缩放: 滚轮格数
        ZOOM_STEP = 20
        ZOOM_TIME = 0.05

        # -----------------------------------

        # 开始动作
        if action == "rotate":
            # 旋转 = 左键拖拽
            self.controller.start_rotate(
                direction,
                distance_per_step=ROTATE_DIST,
                duration=ROTATE_TIME
            )

        elif action == "pan":
            # 平移 = 中键拖拽
            self.controller.start_pan(
                direction,
                distance_per_step=PAN_DIST,
                duration=PAN_TIME
            )

        elif action == "zoom":
            # 缩放 = 滚轮
            self.controller.start_zoom(
                direction,
                scrolls_per_step=ZOOM_STEP,
                delay=ZOOM_TIME
            )

    def _run_processes(self, scene_path: str, snapshot_path: str):
        venv_python = r"D:\ProgramData\miniconda3\envs\instantNGP\python.exe"
        ngp_script = r"D:\InstantNgp\instant-ngp\scripts\run.py"
        window_title = "Instant Neural Graphics Primitives"

        # 保险：确认 ffmpeg 在 PATH 里
        ffmpeg_bin = shutil.which("ffmpeg") or "ffmpeg"

        ngp_cmd = [
            venv_python, ngp_script,
            "--scene", scene_path,
            "--load_snapshot", snapshot_path,
            "--gui",

        ]

        ffmpeg_cmd = [
            ffmpeg_bin,
            "-hide_banner",
            "-loglevel", "info",
            "-stats",

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

            "-muxdelay", "0",
            "-muxpreload", "0",

            "-f", "rtsp",
            self.rtsp_url,
        ]

        try:
            with ExternalCommandRunner(ngp_cmd) as ngp_runner:
                if not ngp_runner.is_running():
                    print("NGP 启动失败")
                    return

                print("NGP 启动成功，等待窗口加载...")
                time.sleep(3)

                with ExternalCommandRunner(ffmpeg_cmd) as ffmpeg_runner:
                    if not ffmpeg_runner.is_running():
                        print("FFMPEG 启动失败（很可能参数错误或找不到 ffmpeg）")
                        return

                    print("FFMPEG 推流开始...")

                    while not self.stop_event.is_set():
                        if not ngp_runner.is_running():
                            print("NGP 意外退出")
                            break
                        if not ffmpeg_runner.is_running():
                            print("FFMPEG 意外退出（请看 ffmpeg stderr 日志）")
                            break
                        time.sleep(0.5)

                print("FFMPEG 退出")
            print("NGP 退出")

        except Exception as e:
            print(f"推流后台线程出错: {e}")
        finally:
            self.is_running = False


# 全局单例
stream_session = InteractiveStreamSession()
