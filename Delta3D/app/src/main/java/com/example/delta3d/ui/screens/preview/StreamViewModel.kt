package com.example.delta3d.ui.screens.preview

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.delta3d.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class StreamUiState {
    object Idle : StreamUiState()
    object Loading : StreamUiState()
    data class Streaming(val url: String) : StreamUiState()
    data class Error(val msg: String) : StreamUiState()
}

class StreamViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<StreamUiState>(StreamUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // 开启推流
    fun startStreamSession(token: String, assetId: Int) {
        viewModelScope.launch {
            _uiState.value = StreamUiState.Loading
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"

                Log.d("TRACK_STREAM", "1. 请求启动推流: AssetId=$assetId")

                val status = RetrofitClient.api.startStream(authHeader, assetId)

                // 🟢 打印拿到的 URL
                Log.d(
                    "TRACK_STREAM",
                    "2. 后端返回状态: Active=${status.isActive}, URL=${status.rtspUrl}"
                )

                if (status.isActive && !status.rtspUrl.isNullOrEmpty()) {
                    _uiState.value = StreamUiState.Streaming(status.rtspUrl)
                } else {
                    Log.e("TRACK_STREAM", "❌ 推流启动失败: URL为空或状态非Active")
                    _uiState.value = StreamUiState.Error("Stream failed to start")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("TRACK_STREAM", "❌ 网络/API异常: ${e.message}")
                _uiState.value = StreamUiState.Error(e.message ?: "Connection error")
            }
        }
    }

    // 停止推流 (通常在页面退出时调用)
    fun stopStreamSession(token: String) {
        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                RetrofitClient.api.stopStream(authHeader)
                _uiState.value = StreamUiState.Idle
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 发送控制指令 (按下 mode="start", 松开 mode="stop")
    fun sendControl(
        token: String,
        action: StreamActionType,
        direction: StreamDirection,
        mode: String
    ) {
        viewModelScope.launch {
            try {
                val authHeader = if (token.startsWith("Bearer ")) token else "Bearer $token"
                val cmd = ControlCommand(action, direction, mode)
                RetrofitClient.api.sendControl(authHeader, cmd)
            } catch (e: Exception) {
                // 控制指令失败通常不需要阻断 UI，可以选择记录日志或轻提示
                e.printStackTrace()
            }
        }
    }
}