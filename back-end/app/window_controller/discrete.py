import win32gui
import win32api
import win32con
import pyautogui
import time
from typing import Optional, Tuple

# --- 配置 ---
TARGET_WINDOW_TITLE = "Instant Neural Graphics Primitives"


# pyautogui.linear - 恒定速度
# pyautogui.easeInQuad - 开始慢，然后加速
# pyautogui.easeOutQuad - 开始快，然后减速
# pyautogui.easeInOutQuad - 开始慢，加速
SMOOTHING_FUNCTION = pyautogui.easeInOutQuad


def find_target_window_info(title_part: str) -> Optional[Tuple[int, Tuple[int, int, int, int]]]:
    """
    通过标题的一部分查找窗口并将其置于前台
    （这里不需要更改，这个函数很高效）
    """

    def callback(hwnd, extra):
        if win32gui.IsWindowVisible(hwnd):
            title = win32gui.GetWindowText(hwnd)
            if title_part in title:
                extra.append((hwnd, win32gui.GetWindowRect(hwnd)))

    results = []
    win32gui.EnumWindows(callback, results)

    if results:
        hwnd, rect = results[0]

        try:
            win32gui.SetForegroundWindow(hwnd)

            time.sleep(0.1)
        except Exception as e:

            print(f"警告：无法设置前台窗口。{e}")
        return hwnd, rect

    print(f"错误：找不到标题包含 '{title_part}' 的窗口。")
    return None


def get_window_center(rect: Tuple[int, int, int, int]) -> Tuple[int, int]:
    """计算窗口矩形的中心坐标"""
    left, top, right, bottom = rect
    return (left + right) // 2, (top + bottom) // 2


# ----------------------------------------------------
# --- 优化的模拟函数 ---
# ----------------------------------------------------

def simulate_zoom(direction: str, scrolls: int = 3):
    """
    模拟鼠标滚轮滚动进行缩放
    （这个函数已经很好了，因为滚动是离散的，不是拖动动作）
    """
    window_info = find_target_window_info(TARGET_WINDOW_TITLE)
    if not window_info:
        return

    hwnd, rect = window_info
    center_x, center_y = get_window_center(rect)
    pyautogui.moveTo(center_x, center_y, duration=0.1)  # 快速移动到中心

    scroll_amount = scrolls if direction.lower() == "in" else -scrolls
    pyautogui.scroll(scroll_amount)
    print(f"执行缩放：方向={direction}，滚动量={scroll_amount}")


def simulate_pan_smooth(direction: str, pan_distance: int = 150, duration: float = 0.3):
    """
    优化版：通过按住鼠标中键并拖动来模拟平滑平移
    使用 pyautogui 的内置 duration 和缓动功能实现平滑效果
    """
    window_info = find_target_window_info(TARGET_WINDOW_TITLE)
    if not window_info:
        return

    hwnd, rect = window_info
    center_x, center_y = get_window_center(rect)

    # 计算目标坐标
    target_x, target_y = center_x, center_y
    # 在这里，我们已经有了 'up' (0, -pan_distance) 和 'down' (0, pan_distance)
    direction_map = {
        'up': (0, -pan_distance),
        'down': (0, pan_distance),
        'left': (-pan_distance, 0),
        'right': (pan_distance, 0)
    }
    if direction.lower() not in direction_map:
        print(f"错误：无效的平移方向 '{direction}'。")
        return

    dx, dy = direction_map[direction.lower()]
    target_x += dx
    target_y += dy

    # 执行平滑拖动
    pyautogui.moveTo(center_x, center_y, duration=0.1)  # 移动到起始位置
    pyautogui.dragTo(
        target_x,
        target_y,
        duration=duration,
        button='middle',
        tween=SMOOTHING_FUNCTION
    )
    print(f"执行平滑平移：方向={direction}，距离={pan_distance}px，持续时间={duration}s")


def simulate_rotate_smooth(direction: str, angle_distance: int = 200, duration: float = 0.3):
    """
    优化版：通过按住鼠标左键并拖动来模拟平滑旋转
    增加：垂直旋转（上下看/俯仰）
    """
    window_info = find_target_window_info(TARGET_WINDOW_TITLE)
    if not window_info:
        return

    hwnd, rect = window_info
    center_x, center_y = get_window_center(rect)

    # 根据方向计算目标坐标
    target_x, target_y = center_x, center_y

    # 定义旋转方向和对应的目标坐标偏移
    rotate_map = {
        # 水平旋转 (左右)
        'clockwise': (angle_distance, 0),  # 顺时针/右旋转：增加X
        'counter_clockwise': (-angle_distance, 0),  # 逆时针/左旋转：减少X
        # 垂直旋转 (上下)
        'up_rotate': (0, -angle_distance),  # 向上看/俯仰：减少Y (屏幕坐标系)
        'down_rotate': (0, angle_distance)  # 向下看/俯仰：增加Y (屏幕坐标系)
    }

    if direction.lower() not in rotate_map:
        print(f"错误：无效的旋转方向 '{direction}'。")
        return

    dx, dy = rotate_map[direction.lower()]
    target_x += dx
    target_y += dy

    # 执行平滑拖动
    pyautogui.moveTo(center_x, center_y, duration=0.1)
    pyautogui.dragTo(
        target_x,
        target_y,
        duration=duration,
        button='left',
        tween=SMOOTHING_FUNCTION
    )
    print(f"执行平滑旋转：方向={direction}，距离={angle_distance}px，持续时间={duration}s")


# ----------------------------------------------------

if __name__ == '__main__':
    print("--- 开始优化模拟测试 ---")
    # 2秒延迟，让你有时间切换到目标窗口
    print("你有2秒时间聚焦目标窗口...")
    time.sleep(2)

    # --- 缩放测试 ---
    # print("\n--- 测试缩放（放大）---")
    # simulate_zoom("in", scrolls=20)
    # time.sleep(1)
    #
    # print("\n--- 测试缩放（缩小）---")
    # simulate_zoom("out", scrolls=20)
    # time.sleep(1)

    # --- 平移测试 ---
    print("\n--- 测试平滑平移（右）---")
    simulate_pan_smooth("right", pan_distance=250, duration=0.4)
    time.sleep(1)

    print("\n--- 测试平滑平移（左）---")
    simulate_pan_smooth("left", pan_distance=250, duration=0.4)
    time.sleep(1)

    # 你的要求: 上下平移 (已存在)
    print("\n--- 测试平滑平移（上）---")
    simulate_pan_smooth("up", pan_distance=250, duration=0.4)
    time.sleep(1)

    print("\n--- 测试平滑平移（下）---")
    simulate_pan_smooth("down", pan_distance=250, duration=0.4)
    time.sleep(1)

    # --- 旋转测试 ---
    # 左右旋转 (水平)
    print("\n--- 测试平滑旋转（顺时针/右）---")
    simulate_rotate_smooth("clockwise", angle_distance=250, duration=0.5)
    time.sleep(1)

    print("\n--- 测试平滑旋转（逆时针/左）---")
    simulate_rotate_smooth("counter_clockwise", angle_distance=250, duration=0.5)
    time.sleep(1)

    # 你的要求: 上下旋转 (垂直)
    print("\n--- 测试平滑旋转（向上看/俯仰）---")
    simulate_rotate_smooth("up_rotate", angle_distance=250, duration=0.5)
    time.sleep(1)

    print("\n--- 测试平滑旋转（向下看/俯仰）---")
    simulate_rotate_smooth("down_rotate", angle_distance=250, duration=0.5)

    print("\n--- 测试完成 ---")