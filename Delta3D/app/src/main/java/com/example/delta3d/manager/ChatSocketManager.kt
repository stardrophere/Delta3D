package com.example.delta3d.manager

import android.util.Log
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.api.WSEvent
import com.example.delta3d.api.WSMessageSend
import com.example.delta3d.config.AppConfig
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

object ChatSocketManager {
    private var webSocket: WebSocket? = null
    private val gson = Gson()

    // 缓存连接信息，用于断线重连
    private var cachedToken: String? = null
    private var cachedUserId: Int? = null

    // 状态标记
    private var isConnecting = false
    private var reconnectJob: Job? = null

    // 使用独立的协程作用域来处理重连倒计时
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _messageFlow = MutableSharedFlow<WSEvent>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val messageFlow = _messageFlow.asSharedFlow()

    /**
     * 发起连接
     */
    fun connect(token: String, myUserId: Int) {
        // 更新缓存，确保重连时用的是最新的 token
        cachedToken = token
        cachedUserId = myUserId

        // 如果已经连接且用户一致，直接复用
        if (webSocket != null && currentUserId() == myUserId) {
            Log.d("ChatSocket", "Socket already connected for user $myUserId, reusing.")
            return
        }

        // 如果正在连接中，跳过
        if (isConnecting) return

        // 如果切换账号，先断开旧的
        if (webSocket != null) {
            disconnectInternal()
        }

        startConnection(token, myUserId)
    }

    private fun startConnection(token: String, myUserId: Int) {
        isConnecting = true
        reconnectJob?.cancel() // 取消

        try {
            // 动态获取 WebSocket 地址
            val baseUrl = AppConfig.currentBaseUrl
            val wsUrl = baseUrl
                .replace("http://", "ws://")
                .replace("https://", "wss://") + "api/v1/chat/ws/$myUserId"

            Log.d("ChatSocket", "Connecting to: $wsUrl")

            val request = Request.Builder().url(wsUrl).build()

            // 设置心跳
            val socketClient = RetrofitClient.client.newBuilder()
                .pingInterval(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            webSocket = socketClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.d("ChatSocket", "Connected for user: $myUserId")
                    isConnecting = false
                    // 重置重连计数器
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    Log.d("ChatSocket", "Received: $text")
                    try {
                        val event = gson.fromJson(text, WSEvent::class.java)
                        _messageFlow.tryEmit(event)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    Log.w("ChatSocket", "Remote closing: $code / $reason")
                    webSocket.close(1000, null)
                    this@ChatSocketManager.webSocket = null
                    isConnecting = false
                    // 触发重连
                    scheduleReconnect()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e("ChatSocket", "Connection Error: ${t.message}")
                    this@ChatSocketManager.webSocket = null
                    isConnecting = false
                    // 触发重连
                    scheduleReconnect()
                }
            })

        } catch (e: Exception) {
            Log.e("ChatSocket", "Start connection failed: ${e.message}")
            isConnecting = false
            scheduleReconnect()
        }
    }

    /**
     * 发送消息
     */
    fun sendMessage(msg: WSMessageSend) {
        if (webSocket == null) {
            Log.e("ChatSocket", "Cannot send message: Socket is disconnected.")
            // 尝试触发一次重连
            scheduleReconnect()
            return
        }
        try {
            val json = gson.toJson(msg)
            val success = webSocket?.send(json) ?: false
            if (!success) {
                Log.e("ChatSocket", "Send returned false, maybe queue is full or socket closing.")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 断线重连机制
     */
    private fun scheduleReconnect() {
        // 如果 token 空了（说明用户主动登出了），就不重连了
        if (cachedToken == null || cachedUserId == null) return

        if (reconnectJob?.isActive == true) return

        reconnectJob = scope.launch {
            Log.d("ChatSocket", "Reconnecting in 3 seconds...")
            delay(3000) // 延迟 3 秒重连

            // 双重检查，防止在等待期间用户退出了
            if (cachedToken != null && cachedUserId != null) {
                Log.d("ChatSocket", "Executing reconnection...")
                connect(cachedToken!!, cachedUserId!!)
            }
        }
    }

    /**
     * 主动彻底断开（退出登录时调用）
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun disconnect() {
        Log.d("ChatSocket", "Disconnecting manually (User Logout)")
        // 清除缓存凭证，阻止自动重连
        cachedToken = null
        cachedUserId = null
        reconnectJob?.cancel()

        // 关闭连接
        disconnectInternal()

        // 清空消息流缓存，防止下一个登录用户收到上一个人的旧消息
        _messageFlow.resetReplayCache()
    }

    private fun disconnectInternal() {
        try {
            webSocket?.close(1000, "Logout/Switch")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        webSocket = null
        isConnecting = false
    }

    // 获取当前连接的用户ID
    private fun currentUserId(): Int? {
        return cachedUserId
    }
}