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
    val lifecycleOwner = LocalLifecycleOwner.current

    // 重试计数器
    var retryCount by remember { mutableIntStateOf(0) }
    val maxRetries = 6

    // 初始化推流
    LaunchedEffect(assetId) {
        Log.d("TRACK_ID", "3. [PreviewScreen] 页面初始化, 接收到的 ID: $assetId")
        retryCount = 0
        token?.let { streamVm.startStreamSession(it, assetId) }
    }

    // 退出页面时停止推流
    DisposableEffect(Unit) {
        onDispose {
            token?.let { streamVm.stopStreamSession(it) }
        }
    }

    // 配置极低延迟的 LoadControl
    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                100,
                200,
                50,
                50
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl) // 应用低延迟策略
            .build().apply {
                // 允许跳帧以保持低延迟
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT

                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("TRACK_STREAM", "🔥 ExoPlayer 播放出错: ${error.message}", error)
                        // 自动重试逻辑
                        if (retryCount < maxRetries) {
                            retryCount++
                            Log.d(
                                "TRACK_STREAM",
                                "🔄 检测到播放失败，准备执行第 $retryCount 次重试..."
                            )
                        } else {
                            Log.e("TRACK_STREAM", "超过最大重试次数，放弃播放")
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        // 如果播放成功开始 (STATE_READY)，重置重试计数
                        if (playbackState == Player.STATE_READY) {
                            retryCount = 0
                        }
                    }
                })
            }
    }

    // 处理重试逻辑
    LaunchedEffect(retryCount) {
        if (retryCount > 0) {
            Log.d("TRACK_STREAM", "⏳ 等待 1.5秒后重试...")
            delay(2000)

            if (uiState is StreamUiState.Streaming) {
                val url = (uiState as StreamUiState.Streaming).url
                Log.d("TRACK_STREAM", "🔄 执行重试: $url")

                //低延迟 MediaItem
                val mediaItem = MediaItem.Builder()
                    .setUri(url)
                    .setLiveConfiguration(
                        MediaItem.LiveConfiguration.Builder()
                            .setMaxPlaybackSpeed(1.1f)
                            .setMinPlaybackSpeed(1.0f)
                            .setTargetOffsetMs(50)
                            .build()
                    )
                    .build()

                val mediaSource = RtspMediaSource.Factory()
                    .setForceUseRtpTcp(false) // 使用 UDP
                    .setTimeoutMs(3000)
                    .createMediaSource(mediaItem)

                exoPlayer.setMediaSource(mediaSource)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            }
        }
    }

    // 监听 RTSP URL 变化并首次播放
    LaunchedEffect(uiState) {
        if (uiState is StreamUiState.Streaming && retryCount == 0) {
            val url = (uiState as StreamUiState.Streaming).url
            Log.d("TRACK_STREAM", "ExoPlayer 首次准备播放: $url")

            // 🟢 配置低延迟 MediaItem
            val mediaItem = MediaItem.Builder()
                .setUri(url)
                .setLiveConfiguration(
                    MediaItem.LiveConfiguration.Builder()
                        .setMaxPlaybackSpeed(1.1f)
                        .setMinPlaybackSpeed(1.0f)
                        .setTargetOffsetMs(50)
                        .build()
                )
                .build()

            val mediaSource = RtspMediaSource.Factory()
                .setForceUseRtpTcp(false)
                .setTimeoutMs(3000)
                .createMediaSource(mediaItem)

            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        }
    }

    // 生命周期管理
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                Lifecycle.Event.ON_DESTROY -> exoPlayer.release()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 主要内容层 (视频 或 Loading/Error)
        when (uiState) {
            is StreamUiState.Loading -> {
                CircularProgressIndicator(
                    color = AccentColor,
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
                Box(modifier = Modifier.fillMaxSize()) {
                    // 视频层
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                // ZOOM 模式，裁剪多余部分以填满屏幕
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 重试加载层
                    if (retryCount > 0 && retryCount < maxRetries) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.75f))
                                .pointerInput(Unit) {},
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = AccentColor,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = "Connecting to Stream...",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Retrying ($retryCount/$maxRetries)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
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

// --- 以下组件代码保持原样 ---

@Composable
fun StreamControlOverlay(
    modifier: Modifier = Modifier,
    onControlEvent: (StreamActionType, StreamDirection, String) -> Unit
) {
    var isPanMode by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {

        // 模式切换
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            GlassButton(
                onClick = { isPanMode = !isPanMode },
                active = true
            ) {
                Icon(
                    if (isPanMode) Icons.Default.OpenWith else Icons.Default.Refresh,
                    null,
                    tint = AccentColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (isPanMode) "PAN" else "ROTATE",
                    color = AccentColor,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

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

        // 方向键
        GlassContainer(shape = CircleShape, padding = 10.dp) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                val currentAction = if (isPanMode) StreamActionType.PAN else StreamActionType.ROTATE

                Box(modifier = Modifier.align(Alignment.TopCenter)) {
                    DPadButton(Icons.Default.KeyboardArrowUp) { mode ->
                        onControlEvent(currentAction, StreamDirection.UP, mode)
                    }
                }
                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    DPadButton(Icons.Default.KeyboardArrowDown) { mode ->
                        onControlEvent(currentAction, StreamDirection.DOWN, mode)
                    }
                }
                Box(modifier = Modifier.align(Alignment.CenterStart)) {
                    DPadButton(Icons.Default.KeyboardArrowLeft) { mode ->
                        onControlEvent(currentAction, StreamDirection.LEFT, mode)
                    }
                }
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    DPadButton(Icons.Default.KeyboardArrowRight) { mode ->
                        onControlEvent(currentAction, StreamDirection.RIGHT, mode)
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
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) GlassControlColor else Color.Transparent
        ),
        border = if (active) BorderStroke(1.dp, GlassBorder) else null,
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

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (isPressed) AccentColor.copy(0.3f) else Color.Transparent)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onPressStart()
                        tryAwaitRelease()
                        isPressed = false
                        onPressEnd()
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