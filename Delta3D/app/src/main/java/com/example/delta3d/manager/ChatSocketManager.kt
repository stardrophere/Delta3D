package com.example.delta3d.manager

import android.util.Log
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.api.WSEvent
import com.example.delta3d.api.WSMessageSend
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*

object ChatSocketManager {
    private var webSocket: WebSocket? = null
    private val gson = Gson()

    private var currentUserId: Int? = null

    private val _messageFlow = MutableSharedFlow<WSEvent>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val messageFlow = _messageFlow.asSharedFlow()


    fun connect(token: String, myUserId: Int) {
        // 连接判断逻辑
        if (webSocket != null) {
            if (currentUserId == myUserId) {
                Log.d("ChatSocket", "Socket already connected for user $myUserId, reusing.")
                return
            } else {
                Log.d(
                    "ChatSocket",
                    "User changed ($currentUserId -> $myUserId), disconnecting old socket."
                )
                disconnect()
            }
        }


        currentUserId = myUserId

        // ws:// 地址
        val wsUrl = RetrofitClient.BASE_URL
            .replace("http://", "ws://")
            .replace("https://", "wss://") + "api/v1/chat/ws/$myUserId"


        val request = Request.Builder().url(wsUrl).build()

        webSocket = RetrofitClient.client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("ChatSocket", "Connected for user: $myUserId")
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
                webSocket.close(1000, null)
                Log.d("ChatSocket", "Closing: $reason")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("ChatSocket", "Error: ${t.message}")
                this@ChatSocketManager.webSocket = null

            }
        })
    }

    fun sendMessage(msg: WSMessageSend) {
        val json = gson.toJson(msg)
        webSocket?.send(json)
    }

    //断开连接
    fun disconnect() {
        try {
            webSocket?.close(1000, "User Logout")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        webSocket = null
        currentUserId = null
    }
}