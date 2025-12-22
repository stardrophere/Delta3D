from fastapi import APIRouter, Depends, WebSocket, WebSocketDisconnect, Query
from sqlmodel import Session, select, or_, and_
from typing import List
import json
from datetime import datetime

from app.api.deps import get_current_user
from app.database import get_session
from app.models import Message, User
from app.schemas import MessageOut, MessageCreate, ChatConversation
from app.core.socket_manager import manager
from sqlalchemy import func
import asyncio
from functools import partial

# from app.api.deps import get_current_user

router = APIRouter()


# 同步操作封锁
def save_message_sync(session: Session, sender_id: int, receiver_id: int, content: str):
    new_msg = Message(
        sender_id=sender_id,
        receiver_id=receiver_id,
        content=content,
        created_at=datetime.utcnow(),
        is_read=False
    )
    session.add(new_msg)
    session.commit()
    session.refresh(new_msg)
    return new_msg


# ============================
# WebSocket 实时聊天
# ============================
@router.websocket("/ws/{user_id}")
async def websocket_endpoint(
        websocket: WebSocket,
        user_id: int,
        session: Session = Depends(get_session)
):
    """
    WebSocket 连接端点
    """
    await manager.connect(user_id, websocket)
    try:
        while True:
            # 接收消息
            data = await websocket.receive_text()

            try:
                msg_data = json.loads(data)
            except json.JSONDecodeError:
                continue

            receiver_id = msg_data.get("receiver_id")
            content = msg_data.get("content")

            if not receiver_id or not content:
                continue

            # 获取当前的异步事件循环
            loop = asyncio.get_running_loop()

            # 使用 partial 包装函数和参数
            save_func = partial(save_message_sync, session, user_id, receiver_id, content)

            # 在默认线程池中执行同步函数，并 await 等待结果
            new_msg = await loop.run_in_executor(None, save_func)

            # 构造推送数据
            response_json = json.dumps({
                "type": "new_message",
                "data": {
                    "id": new_msg.id,
                    "sender_id": user_id,
                    "receiver_id": receiver_id,
                    "content": content,
                    "created_at": new_msg.created_at.isoformat()
                }
            })

            # 推送消息
            # 推给对方
            await manager.send_personal_message(response_json, receiver_id)
            # 回显给自己
            await manager.send_personal_message(response_json, user_id)

    except WebSocketDisconnect:
        manager.disconnect(user_id)
    except Exception as e:
        print(f"WebSocket Error: {e}")
        manager.disconnect(user_id)


# ============================
# HTTP 获取历史记录
# ============================
@router.get("/history/{other_user_id}", response_model=List[MessageOut])
def get_chat_history(
        other_user_id: int,
        current_user: User = Depends(get_current_user),
        session: Session = Depends(get_session),
        offset: int = 0,
        limit: int = 50
):
    """
    获取我和 other_user_id 的聊天记录
    """
    current_user_id = current_user.id  # 从注入的用户对象中拿 ID

    statement = select(Message).where(
        or_(
            and_(Message.sender_id == current_user_id, Message.receiver_id == other_user_id),
            and_(Message.sender_id == other_user_id, Message.receiver_id == current_user_id)
        )
    ).order_by(Message.created_at.desc()).offset(offset).limit(limit)

    messages = session.exec(statement).all()
    return messages


@router.get("/conversations", response_model=List[ChatConversation])
def get_conversations(
        current_user: User = Depends(get_current_user),
        session: Session = Depends(get_session)
):
    user_id = current_user.id

    subquery = select(
        func.max(Message.id).label("max_id")
    ).where(
        or_(Message.sender_id == user_id, Message.receiver_id == user_id)
    ).group_by(

    )

    # 查出所有涉及 current_user 的消息，按时间倒序
    stmt = select(Message).where(
        or_(Message.sender_id == user_id, Message.receiver_id == user_id)
    ).order_by(Message.created_at.desc())

    all_msgs = session.exec(stmt).all()

    conversations_map = {}  # target_user_id -> {last_msg, unread_count}

    for msg in all_msgs:
        target_id = msg.sender_id if msg.receiver_id == user_id else msg.receiver_id

        if target_id not in conversations_map:
            # 这是一个新发现的对话，因为是倒序
            conversations_map[target_id] = {
                "last_message": msg.content,
                "last_message_time": msg.created_at,
                "unread_count": 0
            }

        # 统计未读：如果是对方发给我的，且未读
        if msg.sender_id == target_id and not msg.is_read:
            conversations_map[target_id]["unread_count"] += 1

    # 批量查出所有联系人的用户信息
    target_ids = list(conversations_map.keys())
    if not target_ids:
        return []

    users_stmt = select(User).where(User.id.in_(target_ids))
    users = session.exec(users_stmt).all()
    user_map = {u.id: u for u in users}

    # 3. 组装结果
    result = []
    for uid in target_ids:  # 保持时间顺序
        user_info = user_map.get(uid)
        if not user_info: continue

        conv_data = conversations_map[uid]
        result.append(ChatConversation(
            user_id=user_info.id,
            username=user_info.username,
            avatar_url=user_info.avatar_url,
            last_message=conv_data["last_message"],
            last_message_time=conv_data["last_message_time"],
            unread_count=conv_data["unread_count"]
        ))

    return result


@router.post("/conversations/{other_user_id}/read")
def mark_messages_as_read(
        other_user_id: int,
        current_user: User = Depends(get_current_user),
        session: Session = Depends(get_session)
):
    """
    将 other_user_id 发给我的所有消息标记为已读
    """
    # 查出所有未读消息
    statement = select(Message).where(
        Message.sender_id == other_user_id,
        Message.receiver_id == current_user.id,
        Message.is_read == False
    )
    messages = session.exec(statement).all()

    # 批量更新
    for msg in messages:
        msg.is_read = True
        session.add(msg)

    session.commit()
    return {"status": "ok", "updated_count": len(messages)}


# ============================
# ：HTTP 发送消息接口 (用于分享/详情页发送)
# ============================
@router.post("/send", response_model=MessageOut)
async def send_message_http(
        msg_in: MessageCreate,
        current_user: User = Depends(get_current_user),
        session: Session = Depends(get_session)
):
    """
    通过 HTTP 接口发送消息。
    适用于：详情页分享、非聊天页面的快速发送。
    功能：
    1. 写入数据库
    2. 如果对方在线，通过 WebSocket 实时推送
    """

    # 写入数据库
    new_msg = Message(
        sender_id=current_user.id,
        receiver_id=msg_in.receiver_id,
        content=msg_in.content,
        created_at=datetime.utcnow(),
        is_read=False
    )
    session.add(new_msg)
    session.commit()
    session.refresh(new_msg)

    # 构造 WebSocket 推送的数据结构

    ws_payload = {
        "type": "new_message",
        "data": {
            "id": new_msg.id,
            "sender_id": current_user.id,
            "receiver_id": msg_in.receiver_id,
            "content": new_msg.content,
            "created_at": new_msg.created_at.isoformat()
        }
    }

    # 尝试实时推给接收者
    import json
    ws_text = json.dumps(ws_payload)

    # 调用 socket_manager 推送
    await manager.send_personal_message(ws_text, msg_in.receiver_id)

    # 推送给自己
    await manager.send_personal_message(ws_text, current_user.id)

    return new_msg
