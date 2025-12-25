package com.example.delta3d.ui.screens.preview

import android.util.Log
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.rtsp.RtspMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.delta3d.api.StreamActionType
import com.example.delta3d.api.StreamDirection
import com.example.delta3d.ui.session.SessionViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.delta3d.utils.WebViewPool
import kotlinx.coroutines.delay

// --- 样式定义 ---
private val GlassControlColor = Color(0xFF1E1E1E).copy(alpha = 0.65f)
private val AccentColor = Color(0xFF64FFDA)
private val GlassBorder = Brush.verticalGradient(
    colors = listOf(Color.White.copy(0.2f), Color.White.copy(0.05f))
)

@OptIn(UnstableApi::class)
@Composable
fun StreamPreviewScreen(
    assetId: Int,
    sessionVm: SessionViewModel,
    onBack: () -> Unit,
    streamVm: StreamViewModel = viewModel()
) {
    val context = LocalContext.current
    val token by sessionVm.token.collectAsState()
    val uiState by streamVm.uiState.collectAsState()


    val webView = remember { WebViewPool.obtain(context) }

    // 初始化推流
    LaunchedEffect(assetId) {
        Log.d("TRACK_ID", "[PreviewScreen] 页面初始化, 接收到的 ID: $assetId")
        token?.let { streamVm.startStreamSession(it, assetId) }
    }

    // 2监听 URL 变化并触发 WebRTC 播放
    LaunchedEffect(uiState) {
        if (uiState is StreamUiState.Streaming) {
            val streamUrl = (uiState as StreamUiState.Streaming).url
            Log.d("TRACK_STREAM", "WebRTC 准备加载: $streamUrl")


            webView.loadUrl("file:///android_asset/webrtc_player.html")

            delay(500)

            Log.d("TRACK_STREAM", "注入 JS 启动播放...")

            webView.evaluateJavascript("start('$streamUrl')", null)
        }
    }

    // 生命周期管理
    DisposableEffect(Unit) {
        onDispose {
            Log.d("TRACK_STREAM", "页面销毁，回收 WebView 并停止推流")

            token?.let { streamVm.stopStreamSession(it) }

            WebViewPool.recycle(webView)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 主要内容层
        when (uiState) {
            is StreamUiState.Loading -> {
                CircularProgressIndicator(
                    color = Color(0xFF64FFDA),
                    modifier = Modifier.align(Alignment.Center)
                )
                Text(
                    "Starting Server Instance...",
                    color = Color.White.copy(0.7f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 80.dp)
                )
            }

            is StreamUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Connect Failed",
                        color = Color.Red,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        (uiState as StreamUiState.Error).msg,
                        color = Color.White.copy(0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            is StreamUiState.Streaming -> {

                AndroidView(
                    factory = { webView },
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {}
        }

        // 顶部返回按钮
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 40.dp, start = 16.dp)
                .align(Alignment.TopStart)
                .background(Color.Black.copy(0.4f), CircleShape)
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }

        // 底部悬浮控制面板
        if (uiState is StreamUiState.Streaming) {
            StreamControlOverlay(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp, start = 20.dp, end = 20.dp),
                onControlEvent = { action, dir, mode ->
                    token?.let { streamVm.sendControl(it, action, dir, mode) }
                }
            )
        }
    }
}

// 控制面板组件

@Composable
fun StreamControlOverlay(
    modifier: Modifier = Modifier,
    onControlEvent: (StreamActionType, StreamDirection, String) -> Unit
) {
    // 默认是旋转模式
    var isPanMode by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {

        // 模式切换区域
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // 视觉反馈
            GlassButton(
                onClick = { isPanMode = !isPanMode },
                active = isPanMode
            ) {
                Icon(
                    if (isPanMode) Icons.Default.OpenWith else Icons.Default.Refresh,
                    null,
                    tint = if (isPanMode) Color.Black else AccentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (isPanMode) "PAN" else "ROTATE",
                    color = if (isPanMode) Color.Black else AccentColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 缩放控制 (Zoom) - 通常不需要反转
            GlassContainer {
                Column {
                    RepeatButton(
                        onPressStart = {
                            onControlEvent(
                                StreamActionType.ZOOM,
                                StreamDirection.IN,
                                "start"
                            )
                        },
                        onPressEnd = {
                            onControlEvent(
                                StreamActionType.ZOOM,
                                StreamDirection.IN,
                                "stop"
                            )
                        }
                    ) {
                        Icon(Icons.Default.ZoomIn, null, tint = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    RepeatButton(
                        onPressStart = {
                            onControlEvent(
                                StreamActionType.ZOOM,
                                StreamDirection.OUT,
                                "start"
                            )
                        },
                        onPressEnd = {
                            onControlEvent(
                                StreamActionType.ZOOM,
                                StreamDirection.OUT,
                                "stop"
                            )
                        }
                    ) {
                        Icon(Icons.Default.ZoomOut, null, tint = Color.White)
                    }
                }
            }
        }

        // 方向键区域
        GlassContainer(shape = CircleShape, padding = 10.dp) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentAction = if (isPanMode) StreamActionType.PAN else StreamActionType.ROTATE

                // 反转所有方向


                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    DPadButton(Icons.Default.KeyboardArrowUp) { mode ->
                        // 界面按 上 -> 发送 DOWN
                        onControlEvent(currentAction, StreamDirection.DOWN, mode)
                    }
                }
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    DPadButton(Icons.Default.KeyboardArrowDown) { mode ->
                        // 界面按 下 -> 发送 UP
                        onControlEvent(currentAction, StreamDirection.UP, mode)
                    }
                }
                Box(modifier = Modifier.align(Alignment.CenterStart)) {
                    DPadButton(Icons.Default.KeyboardArrowLeft) { mode ->
                        // 界面按 左 -> 发送 RIGHT
                        onControlEvent(currentAction, StreamDirection.RIGHT, mode)
                    }
                }
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    DPadButton(Icons.Default.KeyboardArrowRight) { mode ->
                        // 界面按 右 -> 发送 LEFT
                        onControlEvent(currentAction, StreamDirection.LEFT, mode)
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(0.1f), CircleShape)
                        .border(1.dp, Color.White.copy(0.1f), CircleShape)
                )
            }
        }
    }
}

@Composable
fun GlassContainer(
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(24.dp),
    padding: androidx.compose.ui.unit.Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(shape)
            .background(GlassControlColor)
            .border(1.dp, GlassBorder, shape)
            .padding(padding)
    ) {
        content()
    }
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    active: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    // 根据 active 状态改变背景色和文字颜色逻辑
    val bgColor = if (active) AccentColor else Color.Transparent // 激活变绿
    val border = if (active) null else BorderStroke(1.dp, GlassBorder)

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = bgColor
        ),
        border = border,
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        content()
    }
}

@Composable
fun RepeatButton(
    modifier: Modifier = Modifier,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    content: @Composable () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    // 使用 rememberUpdatedState 包装回调，确保在 pointerInput 中总是获取到最新的 lambda
    val currentOnPressStart by rememberUpdatedState(onPressStart)
    val currentOnPressEnd by rememberUpdatedState(onPressEnd)


    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isPressed) AccentColor.copy(0.3f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        // 调用包装后的最新状态
                        currentOnPressStart()
                        tryAwaitRelease()
                        isPressed = false
                        // 调用包装后的最新状态
                        currentOnPressEnd()
                    }
                )
            }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun DPadButton(
    icon: ImageVector,
    onAction: (String) -> Unit
) {
    RepeatButton(
        onPressStart = { onAction("start") },
        onPressEnd = { onAction("stop") }
    ) {
        Icon(
            icon,
            null,
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
    }
}