import win32gui
import pyautogui
import time
import threading
import math
from typing import Optional, Tuple

# --- 配置部分 ---
TARGET_WINDOW_TITLE = "Instant Neural Graphics Primitives"
SMOOTHING_FUNCTION = pyautogui.easeInOutQuad
EDGE_MARGIN = 30  # 鼠标放到窗口边缘时与窗口边界的内边距


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

    def start_zoom(self, direction: str, scrolls_per_step: int = 40, delay: float = 0.01):
        """开始连续放大或缩小。scrolls_per_step 越大越快。"""
        self._start_action(self._continuous_zoom, direction, scrolls_per_step, delay)

    def start_pan(self, direction: str, distance_per_step: int = 80, duration: float = 0.01):
        """开始连续平移。distance_per_step 越大越快。"""
        self._start_action(self._continuous_drag_robust, direction, 'middle', distance_per_step, duration)

    def start_rotate(self, direction: str, distance_per_step: int = 60, duration: float = 0.01):
        """开始连续旋转。distance_per_step 越大越快。"""
        self._start_action(self._continuous_drag_robust, direction, 'left', distance_per_step, duration)

    def _continuous_zoom(self, direction: str, scrolls_per_step: int, delay: float):
        """连续缩放的工作线程函数。"""
        print(f"开始连续缩放：{direction.upper()}")
        scroll_amount = scrolls_per_step if direction.lower() == "in" else -scrolls_per_step

        window_info = find_target_window_info(TARGET_WINDOW_TITLE)
        if not window_info: return
        _, rect = window_info
        center_x, center_y = get_window_center(rect)

        # 快速归位
        pyautogui.moveTo(center_x, center_y, duration=0.05)

        while not self._stop_event.is_set():
            pyautogui.scroll(scroll_amount)
            time.sleep(delay)

    def _continuous_drag_robust(self, direction: str, button: str, distance_per_step: int, duration_per_step: float):
        """
        连续的拖拽：
        """
        print(f"开始连续拖拽：{direction.upper()}，按钮：{button}")

        window_info = find_target_window_info(TARGET_WINDOW_TITLE)
        if not window_info:
            return

        _, rect = window_info
        left, top, right, bottom = rect

        # 方向映射
        direction_map = {
            'up': (0, -distance_per_step),
            'down': (0, distance_per_step),
            'left': (-distance_per_step, 0),
            'right': (distance_per_step, 0),
            'clockwise': (distance_per_step, 0),
            'counter_clockwise': (-distance_per_step, 0)
        }
        raw_dx, raw_dy = direction_map.get(direction.lower(), (0, 0))

        # 规范化方向单位向量
        if raw_dx != 0:
            ux = 1 if raw_dx > 0 else -1
            uy = 0
            movement_axis = 'x'
        else:
            ux = 0
            uy = 1 if raw_dy > 0 else -1
            movement_axis = 'y'

        # 计算起始点
        center_x, center_y = get_window_center(rect)
        start_x, start_y = center_x, center_y

        # 左拖鼠标应从右边开始
        if raw_dx < 0:
            start_x = right - EDGE_MARGIN
        elif raw_dx > 0:
            start_x = left + EDGE_MARGIN

        # 向上拖鼠标应从底部开始
        if raw_dy < 0:
            start_y = bottom - EDGE_MARGIN
        elif raw_dy > 0:
            start_y = top + EDGE_MARGIN

        # 防止飘移
        is_rotation = direction.lower() in ('clockwise', 'counter_clockwise')
        if is_rotation:
            start_y = center_y

        start_x, start_y = _clamp_point_to_rect(start_x, start_y, rect, margin=EDGE_MARGIN)

        # 计算单次拖拽的最大物理行程
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

        # 速度参数调优
        # 基准速度：像素/秒
        baseline_v = max(1.0, abs(distance_per_step) / max(1e-6, duration_per_step))
        MAX_SPEED = baseline_v * 1.5

        # 极短的加速时间，消除启动延迟
        ACCEL_TIME = 0.05
        DECEL_TIME = 0.05

        TICK = 0.010
        RESET_RATIO = 0.95

        # 快速归位并按下
        pyautogui.moveTo(start_x, start_y, duration=0.05, tween=SMOOTHING_FUNCTION)
        pyautogui.mouseDown(button=button)
        # 极短的去抖动等待
        time.sleep(0.01)

        frac_x = 0.0
        frac_y = 0.0
        t_since_start = 0.0

        try:
            while not self._stop_event.is_set():
                # 极速加速逻辑
                if t_since_start < ACCEL_TIME:
                    factor = SMOOTHING_FUNCTION(min(1.0, t_since_start / ACCEL_TIME))
                else:
                    factor = 1.0

                cur_speed = MAX_SPEED * factor
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

                if int_dx == 0 and int_dy == 0:
                    time.sleep(TICK)
                    t_since_start += TICK
                    continue

                # 移动
                cur_x, cur_y = pyautogui.position()
                target_x = cur_x + int_dx
                target_y = cur_y + int_dy
                target_x, target_y = _clamp_point_to_rect(target_x, target_y, rect, margin=EDGE_MARGIN)

                pyautogui.moveTo(target_x, target_y, duration=TICK, tween=pyautogui.linear)

                t_since_start += TICK

                # 界检查与快速重置
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

                # 触发重置
                if (max_travel_along_axis > 0 and traveled_along_axis >= max_travel_along_axis * RESET_RATIO) \
                        or (remaining_along_axis <= abs(distance_per_step)):

                    # 快速减速
                    decel_t = 0.0
                    while decel_t < DECEL_TIME:
                        ratio = 1.0 - (decel_t / DECEL_TIME)
                        decel_factor = SMOOTHING_FUNCTION(max(0.0, min(1.0, ratio)))
                        cur_speed = MAX_SPEED * decel_factor
                        move_px = cur_speed * TICK

                        dx = ux * move_px
                        dy = uy * move_px

                        if is_rotation: dy = 0
                        pyautogui.moveRel(int(dx), int(dy), duration=TICK, tween=pyautogui.linear)

                        decel_t += TICK

                    # 快速重置回起点
                    pyautogui.mouseUp(button=button)
                    time.sleep(0.01)
                    # 极速回弹
                    pyautogui.moveTo(start_x, start_y, duration=0.1, tween=SMOOTHING_FUNCTION)
                    time.sleep(0.01)
                    pyautogui.mouseDown(button=button)

                    t_since_start = 0.0
                    frac_x = 0.0
                    frac_y = 0.0

        finally:
            pyautogui.mouseUp(button=button)
            print("按键已释放")


# --- 示例
if __name__ == '__main__':
    print("--- 开始连续控制测试 ---")
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
