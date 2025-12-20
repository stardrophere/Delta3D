// 文件: com/example/delta3d/ui/session/SessionViewModel.kt

package com.example.delta3d.ui.session

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.api.UserDetail
import com.example.delta3d.data.TokenStore
import com.example.delta3d.manager.ChatSocketManager
import com.example.delta3d.utils.AuthEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SessionViewModel(app: Application) :
    AndroidViewModel(app) {

    private val tokenStore = TokenStore(app.applicationContext)

    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token

    // 定义当前用户的状态
    private val _currentUser = MutableStateFlow<UserDetail?>(null)
    val currentUser: StateFlow<UserDetail?> = _currentUser

    // 全局未读消息数状态
    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    init {
        // 冷启动
        viewModelScope.launch {
            val savedToken = tokenStore.accessTokenFlow.first()
            _token.value = savedToken
            _loaded.value = true

            // 如果有 Token，自动拉取用户信息
            if (!savedToken.isNullOrBlank()) {
                fetchCurrentUser(savedToken)
            }
        }

        viewModelScope.launch {
            AuthEvents.unauthorizedEvent.collect {
                // 收到 401 信号，直接执行登出逻辑
                if (_token.value != null) {
                    logout()
                }
            }
        }
    }

    fun login(token: String) {
        //  内存立刻可用
        _token.value = token
        // 持久化
        viewModelScope.launch { tokenStore.saveAccessToken(token) }

        // 登录成功后，立刻拉取用户信息
        fetchCurrentUser(token)
    }

    fun logout() {
        _token.value = null
        _currentUser.value = null // 登出时清空信息
        _totalUnreadCount.value = 0 // 出时清空未读数
        ChatSocketManager.disconnect() //中断连接
        viewModelScope.launch { tokenStore.clear() }
    }

    // 更新未读数的方法
    fun updateTotalUnread(count: Int) {
        _totalUnreadCount.value = count
    }

    // 主动刷新未读数
    fun refreshUnreadCount() {
        val currentToken = _token.value ?: return
        viewModelScope.launch {
            try {
                val authHeader =
                    if (currentToken.startsWith("Bearer ")) currentToken else "Bearer $currentToken"
                val conversations = RetrofitClient.api.getConversations(authHeader)
                // 累加所有会话的未读数
                val total = conversations.sumOf { it.unreadCount }
                _totalUnreadCount.value = total
            } catch (e: Exception) {
                Log.e("SessionViewModel", "Failed to refresh unread count: ${e.message}")
            }
        }
    }

    // 拉取用户信息的具体逻辑
    public fun fetchCurrentUser(tokenStr: String) {
        viewModelScope.launch {
            try {

                val authHeader =
                    if (tokenStr.startsWith("Bearer ")) tokenStr else "Bearer $tokenStr"

                // 调用 API
                val user = RetrofitClient.api.getMe(authHeader)

                // 更新状态
                _currentUser.value = user
                Log.d("SessionViewModel", "User loaded: ${user.username}")

                // 获取用户信息成功后，顺便拉取一下未读消息数
                refreshUnreadCount()

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SessionViewModel", "Failed to fetch user info: ${e.message}")
            }
        }
    }

    //更新用户信息
    fun refreshUser(user: UserDetail) {
        _currentUser.value = user
    }

    fun refreshUserInfo() {
        val currentToken = _token.value
        if (!currentToken.isNullOrBlank()) {
            fetchCurrentUser(currentToken)
        }
    }
}