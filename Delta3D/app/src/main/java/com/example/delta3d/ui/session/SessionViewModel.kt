package com.example.delta3d.ui.session

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.api.UserDetail
import com.example.delta3d.config.AppConfig
import com.example.delta3d.config.NetworkMode
import com.example.delta3d.data.TokenStore
import com.example.delta3d.manager.ChatSocketManager
import com.example.delta3d.utils.AuthEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class SessionViewModel(app: Application) : AndroidViewModel(app) {

    private val tokenStore = TokenStore(app.applicationContext)


    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token
    private val _isFirstLaunch = MutableStateFlow(true)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch
    private val _currentUser = MutableStateFlow<UserDetail?>(null)
    val currentUser: StateFlow<UserDetail?> = _currentUser
    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount
    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    // 网络检测状态通知
    private val _networkCheckFinished = MutableStateFlow(false)
    val networkCheckFinished: StateFlow<Boolean> = _networkCheckFinished

    // 用于避免网络波动导致频繁检测的防抖 Job
    private var networkCheckJob: Job? = null

    // 网络管理器
    private val connectivityManager =
        app.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // 网络回调对象
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // 当有新网络可用时
            Log.d("NetworkMonitor", "Network available, triggering check...")
            triggerNetworkCheck()
        }

        override fun onLost(network: Network) {

            Log.d("NetworkMonitor", "Network lost")
        }
    }

    init {
        // 注册网络监听
        monitorNetworkChanges()

        viewModelScope.launch {
            // 加载本地数据
            val savedToken = tokenStore.accessTokenFlow.first()
            _token.value = savedToken
            val firstLaunchStatus = tokenStore.isFirstLaunchFlow.first()
            _isFirstLaunch.value = firstLaunchStatus
            _loaded.value = true

            if (savedToken != null) {
                fetchCurrentUser(savedToken)
            }
        }

        // 监听 401
        viewModelScope.launch {
            AuthEvents.unauthorizedEvent.collect {
                if (_token.value != null) logout()
            }
        }
    }

    // 在 ViewModel 销毁时取消监听
    override fun onCleared() {
        super.onCleared()
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun monitorNetworkChanges() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // 兜底
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
    }

    // 防抖动检测
    private fun triggerNetworkCheck() {
        networkCheckJob?.cancel()
        networkCheckJob = viewModelScope.launch {
            delay(500) // 等待 500ms
            checkAndSelectBestNetwork()
        }
    }

    // 核心检测逻辑
    private suspend fun checkAndSelectBestNetwork() = withContext(Dispatchers.IO) {


        val lanHost = AppConfig.LAN_HOST
        val lanPort = AppConfig.LAN_PORT
        val timeoutMs = 2000 // 超时时间

        Log.d("NetworkMonitor", "Pinging LAN: $lanHost...")
        val isLanAvailable = isHostReachable(lanHost, lanPort, timeoutMs)

        val newMode = if (isLanAvailable) NetworkMode.LAN else NetworkMode.WAN

        // 只有当模式真正改变时，才去触发状态更新和 UI 提示
        if (AppConfig.currentMode != newMode) {
            AppConfig.currentMode = newMode
            Log.i("NetworkMonitor", "Switched to -> $newMode")

            // 通知 UI 弹窗
            _networkCheckFinished.value = false
            delay(50)
            _networkCheckFinished.value = true
        } else {
            Log.i("NetworkMonitor", "Mode remains -> $newMode")
        }
    }

    private fun isHostReachable(host: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), timeout)
                true
            }
        } catch (e: Exception) {
            false
        }
    }


    // 引导页
    fun completeOnboarding() {
        viewModelScope.launch {
            tokenStore.setFirstLaunchCompleted()
            _isFirstLaunch.value = false
        }
    }

    fun login(token: String) {
        _token.value = token
        // 持久化
        viewModelScope.launch { tokenStore.saveAccessToken(token) }

        // 拉取用户信息
        fetchCurrentUser(token)
    }

    fun logout() {
        _token.value = null
        _currentUser.value = null // 登出时清空信息
        _totalUnreadCount.value = 0 // 登出时清空未读数
        ChatSocketManager.disconnect() // 中断连接
        viewModelScope.launch { tokenStore.clear() }
    }

    // 更新未读数
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
    fun fetchCurrentUser(tokenStr: String) {
        viewModelScope.launch {
            try {
                val authHeader =
                    if (tokenStr.startsWith("Bearer ")) tokenStr else "Bearer $tokenStr"

                // 调用 API
                val user = RetrofitClient.api.getMe(authHeader)

                // 更新状态
                _currentUser.value = user
                Log.d("SessionViewModel", "User loaded: ${user.username}")

                // 获取用户信息成功后，拉取一下未读消息数
                refreshUnreadCount()

            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("SessionViewModel", "Failed to fetch user info: ${e.message}")
            }
        }
    }

    // 更新用户信息
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