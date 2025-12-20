package com.example.delta3d.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object AuthEvents {
    // 发送“未授权”事件
    private val _unauthorizedEvent = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val unauthorizedEvent = _unauthorizedEvent.asSharedFlow()

    // 触发
    fun triggerUnauthorized() {
        _unauthorizedEvent.tryEmit(Unit)
    }
}