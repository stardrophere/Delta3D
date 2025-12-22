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


# from app.api.deps import get_current_user

router = APIRouter()


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
            # 接收消息 (JSON 格式: {"receiver_id": 2, "content": "你好"})
            data = await websocket.receive_text()
            msg_data = json.loads(data)

            receiver_id = msg_data.get("receiver_id")
            content = msg_data.get("content")

            if not receiver_id or not content:
                continue

            # 存入数据库
            new_msg = Message(
                sender_id=user_id,
                receiver_id=receiver_id,
                content=content,
                created_at=datetime.utcnow(),
                is_read=False
            )
            session.add(new_msg)
            session.commit()
            session.refresh(new_msg)

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

            # 推送给接收者
            await manager.send_personal_message(response_json, receiver_id)

            # 回显给自己
            await manager.send_personal_message(response_json, user_id)

    except WebSocketDisconnect:
        manager.disconnect(user_id)
    except Exception as e:
        print(f"Error: {e}")
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
    """
    获取消息列表：返回每个对话对象的最后一条消息、时间和未读数
    """
    user_id = current_user.id

    # 找出所有和我有关的消息 (发给我的 OR 我发出的)
    statement = select(Message.sender_id, Message.receiver_id, Message.created_at) \
        .where(or_(Message.sender_id == user_id, Message.receiver_id == user_id)) \
        .order_by(Message.created_at.desc())

    all_interactions = session.exec(statement).all()

    #提取所有有过对话的“对方用户ID” (保持顺序，最近的在前面)
    contact_ids = []
    seen = set()
    for msg in all_interactions:
        # 确定对方是谁
        other_id = msg.sender_id if msg.receiver_id == user_id else msg.receiver_id
        if other_id not in seen:
            seen.add(other_id)
            contact_ids.append(other_id)

    # 组装结果列表
    conversations = []
    for contact_id in contact_ids:
        # 3.1 获取对方用户信息
        contact_user = session.get(User, contact_id)
        if not contact_user:
            continue

        # 获取最后一条具体消息内容
        #    条件：(我发给TA) OR (TA发给我) -> 按时间倒序 -> 取第1条
        last_msg_stmt = select(Message).where(
            or_(
                (Message.sender_id == user_id) & (Message.receiver_id == contact_id),
                (Message.sender_id == contact_id) & (Message.receiver_id == user_id)
            )
        ).order_by(Message.created_at.desc()).limit(1)

        last_msg = session.exec(last_msg_stmt).first()
        if not last_msg:
            continue

        # 统计发给我且未读的数量
        unread_count = session.exec(
            select(Message).where(
                Message.sender_id == contact_id,  # TA发的
                Message.receiver_id == user_id,  # 我收的
                Message.is_read == False  # 没读的
            )
        ).all()

        conversations.append(ChatConversation(
            user_id=contact_user.id,
            username=contact_user.username,
            avatar_url=contact_user.avatar_url,
            last_message=last_msg.content,
            last_message_time=last_msg.created_at,
            unread_count=len(unread_count)
        ))

    return conversations


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