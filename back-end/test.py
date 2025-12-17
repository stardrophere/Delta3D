from fastapi import FastAPI, Request, WebSocket
from fastapi.responses import HTMLResponse, JSONResponse
import json

from window_controller.continuous import ContinuousController

controller = ContinuousController()
app = FastAPI()


# --- HTTP: 提供前端页面 ---
@app.get("/www")
def get_page():
    html = """
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="UTF-8">
        <title>控制界面</title>
        <style>
            body { font-family: sans-serif; padding: 20px; }
            .grid {
                display: grid;
                grid-template-columns: repeat(4, 100px);
                grid-gap: 8px;
                justify-content: center;
                margin-top: 20px;
            }
            button {
                padding: 10px;
                font-size: 14px;
                cursor: pointer;
                user-select: none;
            }
        </style>
    </head>
    <body>
        <h2>🕹 控制界面</h2>
        <div>
            <h3>旋转控制</h3>
            <div class="grid">
                <button onmousedown="send('rotate_up')" onmouseup="stop()" onmouseleave="stop()">↑</button>
                <button onmousedown="send('rotate_down')" onmouseup="stop()" onmouseleave="stop()">↓</button>
                <button onmousedown="send('rotate_left')" onmouseup="stop()" onmouseleave="stop()">←</button>
                <button onmousedown="send('rotate_right')" onmouseup="stop()" onmouseleave="stop()">→</button>
            </div>
        </div>

        <div>
            <h3>平移控制</h3>
            <div class="grid">
                <button onmousedown="send('pan_up')" onmouseup="stop()" onmouseleave="stop()">↑</button>
                <button onmousedown="send('pan_down')" onmouseup="stop()" onmouseleave="stop()">↓</button>
                <button onmousedown="send('pan_left')" onmouseup="stop()" onmouseleave="stop()">←</button>
                <button onmousedown="send('pan_right')" onmouseup="stop()" onmouseleave="stop()">→</button>
            </div>
        </div>

        <div>
            <h3>前进控制</h3>
            <div class="grid" style="grid-template-columns: repeat(2, 100px);">
                <button onmousedown="send('zoom_in')" onmouseup="stop()" onmouseleave="stop()">前进</button>
                <button onmousedown="send('zoom_out')" onmouseup="stop()" onmouseleave="stop()">后退</button>
            </div>
        </div>

        <script>
        const ws = new WebSocket("ws://localhost:8000/ws");
        ws.onopen = () => console.log("✅ WebSocket 连接成功");
        ws.onclose = () => console.log("❌ WebSocket 连接断开");

        function send(cmd) {
            ws.send(JSON.stringify({type: 'command', value: cmd}));
        }

        function stop() {
            
            setTimeout(() => {
ws.send(JSON.stringify({type: 'command', value: 'stop'}));
    }, 500);
        }
        </script>
    </body>
    </html>
    """
    return HTMLResponse(html)


# --- WebSocket: 实时控制 ---
@app.websocket("/ws")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    print("✅ WebSocket 客户端已连接")
    try:
        while True:
            data = await websocket.receive_text()
            msg = json.loads(data)
            if msg["type"] == "command":
                cmd = msg["value"]
                print(f"收到命令: {cmd}")

                if cmd == "stop":
                    controller.stop()

                # --- 平移 ---
                elif cmd.startswith("pan_"):
                    direction = cmd.replace("pan_", "")
                    controller.start_pan(direction)

                # --- 旋转 ---
                elif cmd.startswith("rotate_"):
                    direction = cmd.replace("rotate_", "")
                    controller.start_rotate(direction)

                # --- 缩放 ---
                elif cmd == "zoom_in":
                    controller.start_zoom("in")
                elif cmd == "zoom_out":
                    controller.start_zoom("out")

    except Exception as e:
        print(f"❌ WebSocket 断开: {e}")
    finally:
        controller.stop()
        print("🧹 控制器已清理")


# --- HTTP: 提供前端控制页面 ---
@app.get("/")
def get_page():
    html = """
  <!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>控制界面</title>
    <style>
        body { font-family: sans-serif; padding: 20px; }
        .grid {
            display: grid;
            grid-template-columns: repeat(4, 100px);
            grid-gap: 8px;
            justify-content: center;
            margin-top: 20px;
        }
        button {
            padding: 10px;
            font-size: 14px;
            cursor: pointer;
            user-select: none;
        }
        .pause-btn {
            display: block;
            margin: 30px auto 0;
            background-color: #f44336;
            color: white;
            font-size: 16px;
            width: 150px;
        }
    </style>
</head>
<body>
    <h2>🕹 控制界面（HTTP版）</h2>

    <div>
        <h3>旋转控制</h3>
        <div class="grid">
            <button onmousedown="send('rotate_up')">↑</button>
            <button onmousedown="send('rotate_down')">↓</button>
            <button onmousedown="send('rotate_left')">←</button>
            <button onmousedown="send('rotate_right')">→</button>
        </div>
    </div>

    <div>
        <h3>平移控制</h3>
        <div class="grid">
            <button onmousedown="send('pan_up')">↑</button>
            <button onmousedown="send('pan_down')">↓</button>
            <button onmousedown="send('pan_left')">←</button>
            <button onmousedown="send('pan_right')">→</button>
        </div>
    </div>

    <div>
        <h3>前进控制</h3>
        <div class="grid" style="grid-template-columns: repeat(2, 100px);">
            <button onmousedown="send('zoom_in')">前进</button>
            <button onmousedown="send('zoom_out')">后退</button>
        </div>
    </div>

    <button class="pause-btn" onclick="send('stop')">⏸ 暂停</button>

    <script>
    async function send(cmd) {
        try {
            await fetch('/command', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ value: cmd })
            });
            console.log('发送命令:', cmd);
        } catch (e) {
            console.error('发送失败', e);
        }
    }
    </script>
</body>
</html>

    """
    return HTMLResponse(html)


# --- HTTP: 处理控制命令 ---
@app.post("/command")
async def handle_command(request: Request):
    data = await request.json()
    cmd = data.get("value")
    print(f"收到命令: {cmd}")

    if cmd == "stop":
        controller.stop()

    # --- 平移 ---
    elif cmd.startswith("pan_"):
        direction = cmd.replace("pan_", "")
        controller.start_pan(direction)

    # --- 旋转 ---
    elif cmd.startswith("rotate_"):
        direction = cmd.replace("rotate_", "")
        controller.start_rotate(direction)

    # --- 缩放 ---
    elif cmd == "zoom_in":
        controller.start_zoom("in")
    elif cmd == "zoom_out":
        controller.start_zoom("out")

    return JSONResponse({"status": "ok", "cmd": cmd})
