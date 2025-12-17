import win32gui
import pyautogui
import time
import threading
import math
from typing import Optional, Tuple

# --- 配置部分 ---
TARGET_WINDOW_TITLE = "Instant Neural Graphics Primitives"
SMOOTHING_FUNCTION = pyautogui.easeInOutQuad
EDGE_MARGIN = 30  # 鼠标放到窗口边缘时与窗口边界的内边距（像素）


def find_target_window_info(title_part: str) -> Optional[Tuple[int, Tuple[int, int, int, int]]]:
    """通过标题查找目标窗口，将其置于前台，并返回窗口信息。"""

    def callback(hwnd, extra):
        if win32gui.IsWindowVisible(hwnd):
            if title_part in win32gui.GetWindowText(hwnd):
                extra.append((hwnd, win32gui.GetWindowRect(hwnd)))

    results = []
    win32gui.EnumWindows(callback, results)

    if results:
        hwnd, rect = results[0]
        try:
            win32gui.SetForegroundWindow(hwnd)
            time.sleep(0.1)
        except Exception as e:
            print(f"警告：无法将窗口置于前台。{e}")

        left, top, right, bottom = rect
        OFFSET_Y = 50
        adjusted_rect = (left, top + OFFSET_Y, right, bottom)
        return hwnd, adjusted_rect
    print(f"错误：未找到包含 '{title_part}' 的窗口。")
    return None


def get_window_center(rect: Tuple[int, int, int, int]) -> Tuple[int, int]:
    """计算窗口矩形的中心点坐标。"""
    left, top, right, bottom = rect
    return (left + right) // 2, (top + bottom) // 2


def _clamp_point_to_rect(x: int, y: int, rect: Tuple[int, int, int, int], margin: int = 0) -> Tuple[int, int]:
    """把 (x,y) 限制到 rect 内，保留 margin 的内边距。"""
    left, top, right, bottom = rect
    x = max(left + margin, min(right - margin, x))
    y = max(top + margin, min(bottom - margin, y))
    return x, y


class ContinuousController:
    """
    管理连续、线程化的用户界面操作，并支持通过命令来启动和停止。
    """

    def __init__(self):
        self._action_thread: Optional[threading.Thread] = None
        self._stop_event = threading.Event()

    def _start_action(self, target_func, *args):
        """启动任意连续操作线程的通用启动器。"""
        if self._action_thread and self._action_thread.is_alive():
            self.stop()

        self._stop_event.clear()
        self._action_thread = threading.Thread(target=target_func, args=args, daemon=True)
        self._action_thread.start()

    def stop(self):
        """向当前运行的动作线程发送停止信号，并等待其干净退出。"""
        if self._action_thread and self._action_thread.is_alive():
            print("--- 发送停止信号中... ---")
            self._stop_event.set()
            self._action_thread.join()
            self._action_thread = None
            print("--- 动作已停止。 ---")

    def start_zoom(self, direction: str, scrolls_per_step: int = 10, delay: float = 0.05):
        """开始连续放大或缩小。"""
        self._start_action(self._continuous_zoom, direction, scrolls_per_step, delay)

    def start_pan(self, direction: str, distance_per_step: int = 20, duration: float = 0.05):
        """开始连续、平滑的平移操作。"""
        self._start_action(self._continuous_drag_robust, direction, 'middle', distance_per_step, duration)

    def start_rotate(self, direction: str, distance_per_step: int = 10, duration: float = 0.05):
        """开始连续、平滑的旋转操作。"""
        self._start_action(self._continuous_drag_robust, direction, 'left', distance_per_step, duration)

    def _continuous_zoom(self, direction: str, scrolls_per_step: int, delay: float):
        """连续缩放的工作线程函数。"""
        print(f"开始连续缩放：{direction.upper()}")
        scroll_amount = scrolls_per_step if direction.lower() == "in" else -scrolls_per_step

        window_info = find_target_window_info(TARGET_WINDOW_TITLE)
        if not window_info: return
        _, rect = window_info
        center_x, center_y = get_window_center(rect)
        pyautogui.moveTo(center_x, center_y, duration=0.1)

        while not self._stop_event.is_set():
            pyautogui.scroll(scroll_amount)
            time.sleep(delay)

    # --- 替换后的稳定版方法（带自动放到反向边缘以最大化活动空间） ---
    def _human_move_to(self, from_x: int, from_y: int, to_x: int, to_y: int, duration: float):
        """
        把一次移动拆成多个微步，使用 SMOOTHING_FUNCTION 作为缓动（加速-减速）曲线。
        - duration: 总时长（秒），会被拆分成多小步。
        - 返回最后的位置（应为 to_x, to_y）。
        """
        # 每秒 60 帧为基准，乘以一个缩放器以更平滑
        HUMAN_STEP_SCALER = 2.0
        steps = max(3, int(max(1, duration * 60 * HUMAN_STEP_SCALER)))
        sx, sy = from_x, from_y
        for i in range(1, steps + 1):
            # t 从 0 -> 1
            t = i / steps
            # 使用 pyautogui 的 easing（传入 t 得到系数），pyautogui 的 easing 是函数形式需要手动呼叫
            try:
                ease_val = SMOOTHING_FUNCTION(t)  # many pyautogui easing funcs accept t in [0,1]
            except Exception:
                # 若 SMOOTHING_FUNCTION 不是直接可调用（向后兼容），退回到简单平滑
                ease_val = t * t * (3 - 2 * t)  # smoothstep fallback

            nx = int(round(from_x + (to_x - from_x) * ease_val))
            ny = int(round(from_y + (to_y - from_y) * ease_val))

            # 避免连续调用 moveTo 同位置（浪费）
            if (nx, ny) != (sx, sy):
                # 每小步时长：把总时长拆成 steps 段
                step_duration = duration / steps
                # limit minimal step duration so OS/目标程序有时间处理事件
                if step_duration < 0.002:
                    step_duration = 0.002
                pyautogui.moveTo(nx, ny, duration=step_duration, tween=pyautogui.linear)
                sx, sy = nx, ny

        return sx, sy

    def _continuous_drag_robust(self, direction: str, button: str, distance_per_step: int, duration_per_step: float):
        """
        连续/丝滑的拖拽（基于速度曲线而非每小段缓动）：
        - 在开始时加速（accel_time），在停止/重置时减速（decel）。
        - 在匀速段以恒定像素/秒移动（更像人手连续拖拽）。
        - tick 为每帧更新时间（秒），移动量 = speed(t) * tick。
        """
        print(f"开始连续拖拽（速度曲线版）：{direction.upper()}，按钮：{button}")

        window_info = find_target_window_info(TARGET_WINDOW_TITLE)
        if not window_info:
            return

        _, rect = window_info
        left, top, right, bottom = rect
        width = right - left
        height = bottom - top

        # direction -> 单步移动向量（用于确定方向）
        direction_map = {
            'up': (0, distance_per_step), 'down': (0, -distance_per_step),
            'left': (distance_per_step, 0), 'right': (-distance_per_step, 0),
            'clockwise': (-distance_per_step, 0), 'counter_clockwise': (distance_per_step, 0)
        }
        raw_dx, raw_dy = direction_map.get(direction.lower(), (0, 0))

        # 规范化方向单位向量（只在单轴上移动，避免对角）
        if raw_dx != 0:
            ux = 1 if raw_dx > 0 else -1
            uy = 0
            movement_axis = 'x'
        else:
            ux = 0
            uy = 1 if raw_dy > 0 else -1
            movement_axis = 'y'

        # 起始点（反侧边缘），与之前逻辑一致
        center_x, center_y = get_window_center(rect)
        start_x, start_y = center_x, center_y

        if raw_dx < 0:
            start_x = right - EDGE_MARGIN
        elif raw_dx > 0:
            start_x = left + EDGE_MARGIN

        if raw_dy < 0:
            start_y = bottom - EDGE_MARGIN
        elif raw_dy > 0:
            start_y = top + EDGE_MARGIN

        # 旋转时锁 Y 到中心以避免垂直漂移
        is_rotation = direction.lower() in ('clockwise', 'counter_clockwise')
        if is_rotation:
            start_y = center_y

        start_x, start_y = _clamp_point_to_rect(start_x, start_y, rect, margin=EDGE_MARGIN)

        # 轴向最大可移动距离（从起点到对侧边缘，沿对应轴）
        if movement_axis == 'x':
            if ux > 0:
                max_travel_along_axis = (right - EDGE_MARGIN) - start_x
            else:
                max_travel_along_axis = start_x - (left + EDGE_MARGIN)
        else:
            if uy > 0:
                max_travel_along_axis = (bottom - EDGE_MARGIN) - start_y
            else:
                max_travel_along_axis = start_y - (top + EDGE_MARGIN)
        max_travel_along_axis = max(0, max_travel_along_axis)

        # 参数（可调）
        # 基准速度：以你给的 distance_per_step/duration_per_step 作为最大速度参考（像素/秒）
        # 如果 duration_per_step 非常小则速度会很快，这与之前行为对齐
        baseline_v = max(1.0, abs(distance_per_step) / max(1e-6, duration_per_step))
        MAX_SPEED = baseline_v  # pixels per second
        ACCEL_TIME = min(0.18, max(0.06, duration_per_step * 2.0))  # 加速时间（秒），可调
        DECEL_TIME = ACCEL_TIME  # 减速时间
        TICK = 0.010  # 每帧更新（秒），越小越平滑/CPU 开销越高
        RESET_RATIO = 0.95

        # 把鼠标放到起始点并按下
        pyautogui.moveTo(start_x, start_y, duration=0.12, tween=SMOOTHING_FUNCTION)
        pyautogui.mouseDown(button=button)
        time.sleep(0.03)

        # 状态变量：用于保持小数部分累积，避免像素舍入导致抖动
        frac_x = 0.0
        frac_y = 0.0

        t_since_start = 0.0
        at_constant_speed = False

        try:
            while not self._stop_event.is_set():
                # 每个 TICK 更新一次速度 factor（基于加速阶段）
                # factor ∈ [0,1] 描述当前速度占 MAX_SPEED 的比例
                if t_since_start < ACCEL_TIME:
                    # ramp-up using easing: ease(t/ACCEL_TIME)
                    factor = SMOOTHING_FUNCTION(min(1.0, t_since_start / ACCEL_TIME))
                else:
                    factor = 1.0
                    at_constant_speed = True

                # 当前速度（pixels/sec）
                cur_speed = MAX_SPEED * factor

                # 一帧要移动的像素
                move_px = cur_speed * TICK

                # 把移动按轴分配
                dx = ux * move_px
                dy = uy * move_px

                # 累计并取整移动量（保留小数）
                frac_x += dx
                frac_y += dy
                int_dx = int(math.trunc(frac_x))
                int_dy = int(math.trunc(frac_y))
                frac_x -= int_dx
                frac_y -= int_dy

                # 若旋转锁定 Y，则把 int_dy 强制为 0
                if is_rotation:
                    int_dy = 0

                if int_dx == 0 and int_dy == 0:
                    # 若本帧移动不足 1 像素，仍要等待下一帧
                    time.sleep(TICK)
                    t_since_start += TICK
                    continue

                # 计算目标点并夹紧
                cur_x, cur_y = pyautogui.position()
                target_x = cur_x + int_dx
                target_y = cur_y + int_dy
                target_x, target_y = _clamp_point_to_rect(target_x, target_y, rect, margin=EDGE_MARGIN)

                # 实际执行移动（短时长以保持流畅）
                # 使用 moveTo 能配合 duration，使得鼠标移动看起来更平滑；用 TICK 作为小段 duration
                pyautogui.moveTo(target_x, target_y, duration=TICK, tween=pyautogui.linear)

                # 更新时间计数
                t_since_start += TICK

                # 轴向判断：已行进与剩余空间（以当前鼠标位置为准）
                if movement_axis == 'x':
                    traveled_along_axis = abs(pyautogui.position()[0] - start_x)
                    if ux > 0:
                        remaining_along_axis = (right - EDGE_MARGIN) - pyautogui.position()[0]
                    else:
                        remaining_along_axis = pyautogui.position()[0] - (left + EDGE_MARGIN)
                else:
                    traveled_along_axis = abs(pyautogui.position()[1] - start_y)
                    if uy > 0:
                        remaining_along_axis = (bottom - EDGE_MARGIN) - pyautogui.position()[1]
                    else:
                        remaining_along_axis = pyautogui.position()[1] - (top + EDGE_MARGIN)

                # 触发重置（接近边缘或下一步会撞墙）
                if (max_travel_along_axis > 0 and traveled_along_axis >= max_travel_along_axis * RESET_RATIO) \
                        or (remaining_along_axis <= abs(distance_per_step)):
                    # 先做减速（decel）以避免突然停止的视觉断层
                    decel_time = DECEL_TIME
                    decel_t = 0.0
                    # 在 DECEL_TIME 内按逆向 easing 把速度降为 0
                    while decel_t < decel_time:
                        ratio = 1.0 - (decel_t / decel_time)
                        decel_factor = SMOOTHING_FUNCTION(max(0.0, min(1.0, ratio)))
                        cur_speed = MAX_SPEED * decel_factor
                        move_px = cur_speed * TICK
                        dx = ux * move_px
                        dy = uy * move_px
                        frac_x += dx
                        frac_y += dy
                        int_dx = int(math.trunc(frac_x))
                        int_dy = int(math.trunc(frac_y))
                        frac_x -= int_dx
                        frac_y -= int_dy
                        if is_rotation:
                            int_dy = 0
                        if int_dx != 0 or int_dy != 0:
                            cur_x, cur_y = pyautogui.position()
                            target_x = cur_x + int_dx
                            target_y = cur_y + int_dy
                            target_x, target_y = _clamp_point_to_rect(target_x, target_y, rect, margin=EDGE_MARGIN)
                            pyautogui.moveTo(target_x, target_y, duration=TICK, tween=pyautogui.linear)
                        decel_t += TICK
                        time.sleep(TICK)
                    # 触发稳定重置（松开、回到起点再按下）
                    print("轴向达到极限或剩余空间不足，执行稳定重置（含减速）...")
                    pyautogui.mouseUp(button=button)
                    time.sleep(0.04)
                    # 丝滑回到起始点（用短的加速-匀速-减速）
                    # 直接使用 moveTo 带 duration 做平滑回撤
                    pyautogui.moveTo(start_x, start_y, duration=0.18, tween=SMOOTHING_FUNCTION)
                    time.sleep(0.04)
                    pyautogui.mouseDown(button=button)
                    # 重置时间与小数累积
                    t_since_start = 0.0
                    frac_x = 0.0
                    frac_y = 0.0

                # 小延迟（已包含在 moveTo 的 duration，但为保险仍 sleep）
                # time.sleep(TICK)  # 已在 moveTo 的 duration 中等待
        finally:
            # 在退出时平滑减速到 0（优雅停止）
            decel_time = DECEL_TIME
            decel_t = 0.0
            while decel_t < decel_time:
                ratio = 1.0 - (decel_t / decel_time)
                decel_factor = SMOOTHING_FUNCTION(max(0.0, min(1.0, ratio)))
                cur_speed = MAX_SPEED * decel_factor
                move_px = cur_speed * TICK
                dx = ux * move_px
                dy = uy * move_px
                frac_x += dx
                frac_y += dy
                int_dx = int(math.trunc(frac_x))
                int_dy = int(math.trunc(frac_y))
                frac_x -= int_dx
                frac_y -= int_dy
                if is_rotation:
                    int_dy = 0
                if int_dx != 0 or int_dy != 0:
                    cur_x, cur_y = pyautogui.position()
                    target_x = cur_x + int_dx
                    target_y = cur_y + int_dy
                    target_x, target_y = _clamp_point_to_rect(target_x, target_y, rect, margin=EDGE_MARGIN)
                    pyautogui.moveTo(target_x, target_y, duration=TICK, tween=pyautogui.linear)
                decel_t += TICK
                time.sleep(TICK)
            pyautogui.mouseUp(button=button)
            print("鼠标按键已释放（平滑停止）。")


# --- 示例：如何使用 (调用了新的 `_continuous_drag_robust` 方法) ---
if __name__ == '__main__':
    print("--- 开始连续控制测试 ---")
    print(">>> 你有 3 秒时间将目标窗口置于前台！ <<<")
    time.sleep(3)

    controller = ContinuousController()

    try:
        controller.start_zoom("in")
        time.sleep(9)
        controller.start_zoom('out')
        time.sleep(9)
        print("\n--- 测试连续平移（向右）4 秒 ---")
        controller.start_pan("right", distance_per_step=20)
        time.sleep(4)
        controller.stop()
        time.sleep(1)

        print("\n--- 测试连续旋转（顺时针）4 秒 ---")
        controller.start_rotate("clockwise", distance_per_step=20)
        time.sleep(4)

    finally:
        controller.stop()
        print("\n--- 测试结束。 ---")
