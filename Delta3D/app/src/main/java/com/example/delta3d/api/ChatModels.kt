package com.example.delta3d.api

import com.google.gson.annotations.SerializedName

// 接收到的消息模型
data class ChatMessage(
    val id: Int,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("receiver_id") val receiverId: Int,
    val content: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_read") val isRead: Boolean
) {
    fun isMe(myUserId: Int): Boolean = (senderId == myUserId)
}

// 发送给 WebSocket 的数据
data class WSMessageSend(
    @SerializedName("receiver_id") val receiverId: Int,
    val content: String
)

// WebSocket 推送过来的事件包装
data class WSEvent(
    val type: String,
    val data: ChatMessage
)

// 聊天总览
data class ChatConversation(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("username") val username: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("last_message") val lastMessage: String,
    @SerializedName("last_message_time") val lastMessageTime: String,
    @SerializedName("unread_count") val unreadCount: Int
)

// 分享专用
data class SendMessageRequest(
    @SerializedName("receiver_id") val receiverId: Int,
    val content: String
)