package com.example.delta3d.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 弹窗类型枚举
enum class FeedbackType {
    SUCCESS, ERROR, INFO
}

// 状态管理类
class FeedbackState {
    var isVisible by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    var type by mutableStateOf(FeedbackType.INFO)
        private set

    // 显示弹窗的方法
    suspend fun show(msg: String, feedbackType: FeedbackType = FeedbackType.INFO, duration: Long = 2000) {
        // 如果正在显示，先重置
        if (isVisible) {
            isVisible = false
            delay(150) // 等待退出动画稍微进行
        }
        message = msg
        type = feedbackType
        isVisible = true
        delay(duration)
        isVisible = false
    }

    // 快捷方法：成功
    suspend fun showSuccess(msg: String) = show(msg, FeedbackType.SUCCESS)

    // 快捷方法：错误
    suspend fun showError(msg: String) = show(msg, FeedbackType.ERROR)
}


@Composable
fun rememberFeedbackState() = remember { FeedbackState() }

// --- 核心 UI 组件 ---
@Composable
fun GlassyFeedbackPopup(
    state: FeedbackState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(tween(300)),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(tween(300)),
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .statusBarsPadding()
            .padding(top = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            val (icon, color, borderColor) = when (state.type) {
                FeedbackType.SUCCESS -> Triple(Icons.Default.CheckCircle, Color(0xFF4CAF50), Color(0xFF81C784))
                FeedbackType.ERROR -> Triple(Icons.Default.Error, Color(0xFFEF5350), Color(0xFFE57373))
                FeedbackType.INFO -> Triple(Icons.Default.Info, Color(0xFF29B6F6), Color(0xFF4FC3F7))
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier

                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .border(1.dp, borderColor.copy(alpha = 0.5f), RoundedCornerShape(50)) // 边框现在会紧贴卡片
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp) // 控制文字离边框的距离
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = state.message,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}