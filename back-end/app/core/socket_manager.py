from typing import Dict
from fastapi import WebSocket


class ConnectionManager:
    def __init__(self):

        self.active_connections: Dict[int, WebSocket] = {}

    async def connect(self, user_id: int, websocket: WebSocket):
        await websocket.accept()
        self.active_connections[user_id] = websocket
        print(f"用户 {user_id} 已连接")

    def disconnect(self, user_id: int):
        if user_id in self.active_connections:
            del self.active_connections[user_id]
            print(f"用户 {user_id} 已断开")

    async def send_personal_message(self, message: str, user_id: int):
        if user_id in self.active_connections:
            websocket = self.active_connections[user_id]
            await websocket.send_text(message)


# 全局对象
manager = ConnectionManager()
