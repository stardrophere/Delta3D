import os
import subprocess
import time
from typing import Optional


class BackgroundProcessManager:
    """
    用于启动、管理和安全关闭单个后台命令行进程的类。
    适用于像 instant-ngp.exe 这样需要持续运行的程序。
    """

    def __init__(self, exe_path: str, scene_path: str):
        """
        初始化管理器。

        Args:
            exe_path (str): 可执行文件的完整路径（如 instant-ngp.exe）。
            scene_path (str): 传递给 --scene 参数的路径。
        """
        self.exe_path = exe_path
        self.scene_path = scene_path
        self.process: Optional[subprocess.Popen] = None

        # 构造命令列表
        self.command_list = [self.exe_path, "--scene", self.scene_path]
        self.cwd = os.path.dirname(self.exe_path)

    def is_running(self) -> bool:
        """检查进程是否仍在运行。"""
        if self.process is None:
            return False
        # poll() 返回 None 表示进程仍在运行
        return self.process.poll() is None

    def start(self) -> bool:
        """
        启动后台进程。

        **【已修改】不再静默运行，而是将子进程的输出打印到主控制台。**

        Returns:
            bool: 进程是否成功启动。
        """
        if self.process and self.process.poll() is None:
            print("警告：进程已经在运行中。")
            return True

        if not os.path.exists(self.exe_path):
            print(f"错误：找不到可执行文件: {self.exe_path}")
            return False

        try:
            print(f"--- 启动进程: {self.exe_path} ---")
            self.process = subprocess.Popen(
                self.command_list,
                cwd=self.cwd,
                stdout=subprocess.DEVNULL,
                stderr=subprocess.DEVNULL,
                shell=False
            )
            print(f"进程已启动 (PID: {self.process.pid})")

            # 检查进程是否立即死亡
            if self.process.poll() is not None:
                print(f"!!! 警告：进程在启动后立即退出，退出码: {self.process.returncode} !!!")
                return False

            return True

        except Exception as e:
            print(f"启动进程失败: {e}")
            self.process = None
            return False

    def stop(self, timeout=5):
        """
        尝试友好地关闭子进程，如果失败则强制杀死。

        Args:
            timeout (int): 等待进程友好退出的秒数。
        """
        if not self.is_running():
            if self.process:
                print(f"进程 (PID: {self.process.pid}) 已经结束。")
            else:
                print("进程未启动或已关闭。")
            return

        print(f"--- 正在尝试终止进程 (PID: {self.process.pid})... ---")
        self.process.terminate()  # 友好终止

        try:
            # 等待进程退出
            self.process.wait(timeout=timeout)
            print("进程已成功关闭。")
        except subprocess.TimeoutExpired:
            # 如果超时，则强制杀死
            print("进程未及时响应，强制杀死...")
            self.process.kill()
            self.process.wait()  # 确保进程完全被清理
            print("进程已被强制关闭。")

        self.process = None  # 清理引用

    def __enter__(self):
        """支持 with 语句，用于启动进程。"""
        self.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """支持 with 语句，确保进程在退出时被清理（无论是否发生异常）。"""
        self.stop()


def run_and_stream(cmd_list, input_data):
    """
    Args:
    cmd_list (list): 包含要执行的命令和所有参数的列表。
                     列表的第一个元素必须是 Python 解释器的路径。
    input_data (str): 包含要发送给子进程的标准输入的字符串，
                      多个输入以换行符 ('\n') 分隔。

    Returns:
        tuple: (success, tip, frame_count)
            success (bool): 脚本是否成功执行完成 (退出码为 0)。
            tip (int): 是否检测到 "No Convergence" 关键字。
                       - 1: 未检测到 (默认值)。
                       - 0: 已检测到。
            frame_count (int): 从脚本输出中解析到的 "frames" 数量。
    """
    print(f"--- 正在启动子进程并实时查看输出: {cmd_list[0]} ---")
    tip = 1
    frame_count = 0

    try:
        # 1. 使用 Popen 启动进程
        process = subprocess.Popen(
            cmd_list,
            shell=False,
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            # 关键：将标准错误 STDOUT 重定向到标准输出 STDOUT，统一读取
            stderr=subprocess.STDOUT,
            text=True  # 确保输入/输出以文本形式处理
        )
        print("PID: ", process.pid)

        # 2. 发送预先准备好的输入数据 (非阻塞方式)
        if input_data:
            print(f"--- 正在发送输入数据：\n{input_data.strip()} ---")
            # 立即发送数据，不会阻塞主线程
            process.stdin.write(input_data)
            process.stdin.flush()
            process.stdin.close()  # 输入发送完毕，关闭管道

        # 3. 实时循环读取输出
        start_time = time.time()
        print("\n[ 开始运行 ]")

        # process.stdout.readline() 阻塞性地读取一行
        # for line in process.stdout:
        #     # 实时打印子进程的输出
        #     sys.stdout.write(line)
        #     sys.stdout.flush()  # 立即刷新，确保信息立刻显示

        for line in process.stdout:
            if "No Convergence" in line:
                print(line)
                tip = 0
            if "frames" in line and "..." not in line and "num_reg" not in line:
                frame_count = int(line[:-8])
                # print("在这：", frame_count)

        # 4. 等待进程完成并获取最终返回码
        process.wait()

        end_time = time.time()
        print(f"[ 运行结束 ], 耗时{end_time - start_time}s")

        # 5. 检查返回码
        if process.returncode != 0:
            print(f"\n!!! 命令执行失败 (退出码: {process.returncode}) !!!")
            return False, tip, frame_count

        print("\n--- colmap2nerf脚本成功执行完成 ---")
        return True, tip, frame_count

    except Exception as e:
        print(f"\n!!! 发生错误: {e} !!!")
        # 如果进程还在运行，尝试清理
        if 'process' in locals() and process.poll() is None:
            process.kill()
        return False, tip, frame_count


class ExternalCommandRunner:
    """
    用于启动、管理和安全关闭单个外部命令行进程的通用类。
    适用于像 'python run.py --gui' 这样需要持续运行的程序。
    """

    def __init__(self, command_parts, cwd: Optional[str] = None):
        """
        初始化管理器。

        Args:
            command_parts (List[str]): 包含所有命令和参数的列表。
                                       例如: ['python', 'path/to/run.py', '--arg1', 'value1']
            cwd (Optional[str]): 子进程的工作目录。
        """
        self.command_parts = command_parts
        self.process: Optional[subprocess.Popen] = None

        # 默认工作目录为命令列表中第一个文件（脚本或可执行文件）的目录
        if cwd:
            self.cwd = cwd
        else:
            first_part_dir = os.path.dirname(self.command_parts[0])
            self.cwd = first_part_dir if first_part_dir else None

    def is_running(self) -> bool:
        """检查进程是否仍在运行。"""
        if self.process is None:
            return False
        # poll() 返回 None 表示进程仍在运行
        return self.process.poll() is None

    def start(self) -> bool:
        """
        启动后台进程。

        **【注意】Instant NGP GUI 需要看到其初始化日志，因此我们将 stdout/stderr
        设置为 None，让其继承主进程的控制台。**

        Returns:
            bool: 进程是否成功启动。
        """
        if self.process and self.process.poll() is None:
            print("警告：进程已经在运行中。")
            return True

        if not self.command_parts:
            print("错误：命令列表为空。")
            return False

        try:
            command_str = " ".join(self.command_parts)
            print(f"--- 启动命令: {command_str} ---")
            print(f"工作目录 (CWD): {self.cwd if self.cwd else 'Current'}")

            self.process = subprocess.Popen(
                self.command_parts,
                cwd=self.cwd,
                # 继承主进程的控制台输出，以便查看 Instant NGP 的初始化日志
                stdout=None,
                stderr=None,
                shell=False
            )
            print(f"进程已启动 (PID: {self.process.pid})。请查看弹出的 GUI 窗口。")

            # 检查进程是否立即死亡 (GUI 程序通常不会立即死亡)
            time.sleep(1)
            if self.process.poll() is not None:
                print(f"!!! 警告：进程在启动后立即退出，退出码: {self.process.returncode} !!!")
                return False

            return True

        except FileNotFoundError:
            print(f"错误：找不到可执行文件或脚本: {self.command_parts[0]}")
            self.process = None
            return False
        except Exception as e:
            print(f"启动进程失败: {e}")
            self.process = None
            return False

    # ( stop, __enter__, __exit__ 方法与原模板保持一致，保持其安全关闭的特性 )

    def stop(self, timeout=5):
        """
        尝试友好地关闭子进程，如果失败则强制杀死。

        Args:
            timeout (int): 等待进程友好退出的秒数。
        """
        if not self.is_running():
            if self.process:
                print(f"进程 (PID: {self.process.pid}) 已经结束。")
            else:
                print("进程未启动或已关闭。")
            return

        print(f"--- 正在尝试终止进程 (PID: {self.process.pid})... ---")
        self.process.terminate()  # 友好终止

        try:
            # 等待进程退出
            self.process.wait(timeout=timeout)
            print("进程已成功关闭。")
        except subprocess.TimeoutExpired:
            # 如果超时，则强制杀死
            print("进程未及时响应，强制杀死...")
            self.process.kill()
            self.process.wait()  # 确保进程完全被清理
            print("进程已被强制关闭。")

        self.process = None  # 清理引用

    def __enter__(self):
        """支持 with 语句，用于启动进程。"""
        self.start()
        return self

    def __exit__(self, exc_type, exc_val, exc_tb):
        """支持 with 语句，确保进程在退出时被清理（无论是否发生异常）。"""
        self.stop()


class NonBlockingCommandRunner:
    """
    用于启动、运行并等待单个非持续性（运行完毕即退出）命令行任务的类。
    适用于 Instant NGP 的训练脚本 (run.py --n_steps ...)。
    """

    def __init__(self, command_parts, cwd: str = None):
        """
        初始化运行器。

        Args:
            command_parts (List[str]): 包含所有命令和参数的列表。
                                       例如: ['python', 'path/to/run.py', '--arg1', 'value1']
            cwd (str, optional): 子进程的工作目录。如果为 None，则使用当前目录。
        """
        self.command_parts = command_parts
        self.cwd = cwd

    def run(self) -> subprocess.CompletedProcess:
        """
        执行命令，等待其完成，并将子进程的输出流式传输到主控制台。

        Returns:
            subprocess.CompletedProcess: 包含子进程结果的对象。
        """
        command_str = " ".join(self.command_parts)
        print("-" * 50)
        print(f"--- 启动非持续性任务 ---")
        print(f"命令: {command_str}")
        print(f"工作目录 (CWD): {self.cwd if self.cwd else 'Current'}")
        print("-" * 50)

        try:
            # subprocess.run 是用于运行命令并等待其完成的最佳函数
            # check=True: 如果子进程返回非零退出码，则抛出 CalledProcessError 异常
            # stdout=None, stderr=None: 继承主进程的控制台，实现实时日志流式输出
            result = subprocess.run(
                self.command_parts,
                cwd=self.cwd,
                check=True,
                stdout=None,
                stderr=None,
                shell=False
            )

            print("-" * 50)
            print(f"SUCCESS: 任务已完成。退出码: {result.returncode}")
            print("-" * 50)
            return result

        except subprocess.CalledProcessError as e:
            print("-" * 50)
            print(f"!!! ERROR: 命令执行失败。退出码: {e.returncode} !!!")
            print("-" * 50)
            raise
        except FileNotFoundError:
            print("-" * 50)
            print(f"!!! ERROR: 找不到可执行文件或脚本: {self.command_parts[0]} !!!")
            print("-" * 50)
            raise
        except Exception as e:
            print(f"!!! 发生意外错误: {e} !!!")
            raise
