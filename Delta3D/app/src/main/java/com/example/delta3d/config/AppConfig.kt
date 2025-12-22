package com.example.delta3d.config


import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class NetworkMode {
    LAN, // 局域网
    WAN, // 公网
    UNKNOWN
}

object AppConfig {
    const val LAN_HOST = "10.252.130.135"
    const val LAN_PORT = 8000

    const val LAN_URL = "http://$LAN_HOST:$LAN_PORT/"

    const val WAN_URL = "http://47.107.130.88:29654/"


    // 局域网限制
    const val LIMIT_MB_LAN = 50.0

    // 公网限制
    const val LIMIT_MB_WAN = 20.0

    var currentMode by mutableStateOf(NetworkMode.UNKNOWN)

    // 获取当前生效的 Base URL
    val currentBaseUrl: String
        get() = when (currentMode) {
            NetworkMode.LAN -> LAN_URL
            NetworkMode.WAN -> WAN_URL
            else -> WAN_URL // 默认兜底
        }

    // 获取当前生效的上传限制
    val currentUploadLimit: Double
        get() = when (currentMode) {
            NetworkMode.LAN -> LIMIT_MB_LAN
            else -> LIMIT_MB_WAN
        }
}