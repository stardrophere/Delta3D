package com.example.delta3d.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.ChatMessage
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.api.WSMessageSend
import com.example.delta3d.manager.ChatSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ChatViewModel(
    private val socketManager: ChatSocketManager,
    private val myUserId: Int,
    private val targetUserId: Int,
    private val token: String
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText = _inputText.asStateFlow()

    // 双方头像的状态
    private val _myAvatarUrl = MutableStateFlow<String?>(null)
    val myAvatarUrl = _myAvatarUrl.asStateFlow()

    private val _targetAvatarUrl = MutableStateFlow<String?>(null)
    val targetAvatarUrl = _targetAvatarUrl.asStateFlow()

    private var currentOffset = 0
    private val pageSize = 20 // 每次加载20条，避免卡顿

    private val _isLoadingHistory = MutableStateFlow(false) // 是否正在加载历史
    val isLoadingHistory = _isLoadingHistory.asStateFlow()

    private var isEndReached = false // 是否已经没有更多历史消息了

    init {
        loadMoreHistory() // 初始加载
        loadAvatars()
        markMessagesAsRead()

        // Socket 监听逻辑
        viewModelScope.launch {
            socketManager.messageFlow.collect { event ->
                if (event.type == "new_message") {
                    val msg = event.data
                    if (msg.senderId == targetUserId || msg.receiverId == targetUserId) {
                        addNewMessage(msg)
                        if (msg.senderId == targetUserId) markMessagesAsRead()
                    }
                }
            }
        }
        socketManager.connect(token, myUserId)
    }

    //并行获取头像
    private fun loadAvatars() {
        val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

        // 加载我的头像
        viewModelScope.launch {
            try {
                val res = RetrofitClient.api.getUserAvatar(authHeader, myUserId)
                _myAvatarUrl.value = res.avatarUrl
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 加载对方头像
        viewModelScope.launch {
            try {
                val res = RetrofitClient.api.getUserAvatar(authHeader, targetUserId)
                _targetAvatarUrl.value = res.avatarUrl
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val history = RetrofitClient.api.getChatHistory(authHeader, targetUserId)
                _messages.value = history.reversed()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onInputChange(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val content = _inputText.value.trim()
        if (content.isBlank()) return





        // 只负责发送指令给 WebSocket
        val msgSend = WSMessageSend(receiverId = targetUserId, content = content)
        socketManager.sendMessage(msgSend)


        // 清空输入框，等待 WebSocket 的回显自动上屏
        _inputText.value = ""
    }

    private fun addMessage(msg: ChatMessage) {
        val currentList = _messages.value.toMutableList()
        currentList.add(msg)
        _messages.value = currentList
    }

    private fun markMessagesAsRead() {
        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                // 调用后端接口把该用户的消息设为 is_read = true
                RetrofitClient.api.markAsRead(authHeader, targetUserId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //分页加载
    fun loadMoreHistory() {
        if (_isLoadingHistory.value || isEndReached) return

        _isLoadingHistory.value = true
        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

                // 调用 API，传入 offset 和 limit
                val newMessagesDesc = RetrofitClient.api.getChatHistory(
                    token = authHeader,
                    targetUserId = targetUserId,
                    limit = pageSize,
                    offset = currentOffset
                )

                if (newMessagesDesc.isEmpty()) {
                    isEndReached = true
                } else {
                    val newMessagesAsc = newMessagesDesc.reversed()

                    // 更新 Offset
                    currentOffset += newMessagesDesc.size


                    val currentList = _messages.value
                    _messages.value = (newMessagesAsc + currentList).distinctBy { it.id }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingHistory.value = false
            }
        }
    }


    //处理新消息
    private fun addNewMessage(msg: ChatMessage) {
        val currentList = _messages.value.toMutableList()
        currentList.add(msg)
        _messages.value = currentList
        currentOffset++
    }

}