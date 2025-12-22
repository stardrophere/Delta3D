package com.example.delta3d.ui.screens.detail

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.delta3d.api.AssetDetail
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.config.AppConfig
import com.example.delta3d.ui.components.FeedbackType
import com.example.delta3d.ui.components.GlassyFeedbackPopup
import com.example.delta3d.ui.components.rememberFeedbackState
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.screens.home.TagColorBinder
import com.example.delta3d.ui.screens.home.TagPalette
import com.example.delta3d.ui.session.SessionViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

// --- 样式常量 ---
private val AccentColor = Color(0xFF64FFDA) // 青色高亮
private val WarningColor = Color(0xFFFFAB40) // 橙色警告/处理中
private val ErrorColor = Color(0xFFFF5252)   // 红色错误
private val SurfaceColor = Color(0xFF1E1E1E) // 深色背景
private val GlassDockColor = Color(0xFF1E1E1E).copy(alpha = 0.90f)
private val CardBgColor = Color(0xFF2C2C2C).copy(alpha = 0.6f)

private val GlassBorder = Brush.verticalGradient(
    colors = listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))
)

@Composable
fun AssetDetailScreen(
    assetId: Int,
    onBack: () -> Unit,
    sessionVm: SessionViewModel,
    onPreviewClick: () -> Unit,
    onNavigateToPublish: (Int) -> Unit,
    detailVm: AssetDetailViewModel = viewModel()
) {
    val token by sessionVm.token.collectAsState()
    val uiState by detailVm.uiState.collectAsState()
    val context = LocalContext.current

    // 引入 Feedback
    val feedbackState = rememberFeedbackState()
    val scope = rememberCoroutineScope()

    // 弹窗状态
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    // 菜单状态
    var showMenu by remember { mutableStateOf(false) }

    // 初始化加载
    LaunchedEffect(assetId, token) {
        token?.let { if (it.isNotEmpty()) detailVm.loadDetail(it, assetId) }
    }
    val tagColorBinder = remember { TagColorBinder(TagPalette) }

    // 下载事件
    LaunchedEffect(Unit) {
        detailVm.downloadEvent.collect { event ->
            when (event) {
                is DownloadEvent.Success -> {
                    // 调用系统下载管理器
                    startSystemDownload(context, event.url, event.filename, onError = { msg ->
                        scope.launch { feedbackState.showError(msg) }
                    })
                    feedbackState.showSuccess("Starting download: ${event.filename}")


                }

                is DownloadEvent.Error -> {
                    feedbackState.showError(event.msg)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground() // 背景

        when (val state = uiState) {
            is DetailUiState.Loading -> {
                CircularProgressIndicator(
                    color = AccentColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is DetailUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        null,
                        tint = ErrorColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Load Failed: ${state.msg}", color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { token?.let { detailVm.loadDetail(it, assetId) } },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor)
                    ) {
                        Text("Retry", color = Color.Black)
                    }
                }
            }

            is DetailUiState.Success -> {
                val detail = state.data

                // --- 页面主体 ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 140.dp)
                ) {
                    // 视频区域
                    ImageCarouselHeader(videoUrl = detail.videoUrl, status = detail.status)

                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {

                        // 标题
                        Text(
                            text = detail.title,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 34.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 标签
                        if (detail.tags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                detail.tags.forEach { tag ->
                                    TagCapsuleHere(
                                        text = tag,
                                        baseColor = tagColorBinder.colorFor(tag)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // 时间线
                        ProcessingTimeline(
                            status = detail.status,
                            createdAt = detail.createdAt,
                            estimatedSeconds = detail.estimatedGenSeconds,
                            // 传入轮询触发器
                            onCheckStatus = {
                                token?.let { detailVm.refreshDetail(it, assetId) }
                            }
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // Tech Specs
                        TechSpecsCard(detail)

                        Spacer(modifier = Modifier.height(18.dp))

                        // 描述
                        Box(modifier = Modifier.padding(start = 10.dp)) {
                            DetailSection(
                                title = "Description",
                                content = detail.description ?: "No description provided."
                            )
                        }

                        if (!detail.remark.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(modifier = Modifier.padding(start = 10.dp)) {
                                DetailSection(
                                    title = "Remarks",
                                    content = detail.remark,
                                    isItalic = true
                                )
                            }
                        }
                    }
                }

                // --- 顶部导航栏 ---
                TopNavBar(
                    onBack = onBack,
                    onMenuClick = { showMenu = true },
                    showMenu = showMenu,
                    onDismissMenu = { showMenu = false },
                    onMenuItemClick = { action ->
                        showMenu = false
                        when (action) {
                            "edit" -> showEditDialog = true
                            "rerun" -> {
                                scope.launch {
                                    feedbackState.show(
                                        msg = "Server resource limited: Cannot re-run process.",
                                        feedbackType = FeedbackType.INFO
                                    )
                                }
                            }

                            "report" -> showReportDialog = true
                            // "delete"
                        }
                    }
                )

                // --- 底部悬浮操作栏 ---
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 40.dp)
                ) {
                    GlassBottomDock(
                        onPreview = {
                            Log.d("TRACK_ID", "[DetailScreen] 点击按钮, 准备跳转 ID: $assetId")
                            onPreviewClick()
                        },
                        onDownload = { showDownloadDialog = true },
                        onShare = { showShareDialog = true }
                    )
                }

                // --- 各种弹窗 ---
                if (showDownloadDialog) {
                    DownloadFormatDialog(
                        title = detail.title,
                        onDismiss = { showDownloadDialog = false },
                        onDownload = { format ->
                            showDownloadDialog = false
                            // 下载逻辑
                            token?.let {
                                detailVm.downloadAsset(it, assetId, format)
                            }
                        }
                    )
                }

                //分享弹窗

                if (showShareDialog) {
                    ShareActionDialog(
                        title = "Share Model",
                        link = "Wait for implementation...",
                        onDismiss = { showShareDialog = false },
                        onCopyLink = {
                            // 静态页面地址
                            val staticPlaceholderUrl =
                                "${AppConfig.currentBaseUrl}static/coming_soon.html"

                            //复制到剪贴板
                            val clipboard =
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText(
                                "Copied Link",
                                staticPlaceholderUrl
                            )
                            clipboard.setPrimaryClip(clip)

                            scope.launch {
                                feedbackState.showSuccess("Link copied! (Feature under construction)")
                            }

                            showShareDialog = false
                        },
                        onPostToCommunity = {
                            showShareDialog = false
                            // 触发跳转
                            onNavigateToPublish(assetId)
                        }
                    )
                }

                // 编辑弹窗
                if (showEditDialog) {
                    val currentData = (uiState as? DetailUiState.Success)?.data
                    if (currentData != null) {
                        GlassyEditAssetDialog(
                            initialTitle = currentData.title,
                            initialDesc = currentData.description ?: "",
                            initialRemark = currentData.remark ?: "",
                            initialTags = currentData.tags,
                            onDismiss = { showEditDialog = false },
                            onConfirm = { title, desc, remark, tags ->
                                token?.let {
                                    detailVm.updateAssetInfo(it, assetId, title, desc, remark, tags)
                                }
                                showEditDialog = false
                            }
                        )
                    }
                }

                // 举报弹窗
                if (showReportDialog) {
                    ReportIssueDialog(
                        onDismiss = { showReportDialog = false },
                        onSubmit = { category, content ->
                            token?.let {
                                detailVm.reportIssue(it, assetId, category, content) {
                                    Toast.makeText(
                                        context,
                                        "Report sent. Thank you.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            showReportDialog = false
                        }
                    )
                }
            }
        }

        // 反馈组件
        GlassyFeedbackPopup(
            state = feedbackState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}


@Composable
fun ShareActionDialog(
    title: String,
    link: String,
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit,
    onPostToCommunity: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.White.copy(0.1f), Color.White.copy(0.05f))),
                    RoundedCornerShape(24.dp)
                )
                .background(Color(0xFF252525).copy(alpha = 0.95f))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(24.dp))

                // 选项 1: 发布到社区 (高亮强调)
                Button(
                    onClick = onPostToCommunity,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64FFDA)), // 青色高亮
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Public, contentDescription = null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Post to Community",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(0.1f)
                    )
                    Text(
                        " OR ",
                        color = Color.White.copy(0.3f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(0.1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 选项 2: 复制链接
                Button(
                    onClick = onCopyLink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.08f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.Link, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Copy Public Link", color = Color.White)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.White.copy(0.5f))
                }
            }
        }
    }
}


// 系统下载管理器辅助函数
fun startSystemDownload(
    context: Context,
    relativeUrl: String,
    fileName: String,
    onError: (String) -> Unit
) {
    try {
        val baseUrl = AppConfig.currentBaseUrl.removeSuffix("/")


        val cleanPath = relativeUrl.replace("\\", "/").removePrefix("/")


        val fullUrl = "$baseUrl/$cleanPath"



        Log.d("Download", "Final URL: $fullUrl")

        val request = DownloadManager.Request(Uri.parse(fullUrl)).apply {
            setTitle(fileName)
            setDescription("Downloading 3D Asset...")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
        }

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)

    } catch (e: Exception) {
        e.printStackTrace()
        onError("System Download Failed: ${e.message}")
    }
}


// ------------------------------------
// 组件：顶部导航与菜单
// ------------------------------------
@Composable
fun TopNavBar(
    onBack: () -> Unit,
    onMenuClick: () -> Unit,
    showMenu: Boolean,
    onDismissMenu: () -> Unit,
    onMenuItemClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, start = 16.dp, end = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 返回按钮
        IconButton(
            onClick = onBack,
            modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }

        // 菜单按钮区
        Box {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.background(Color.Black.copy(0.3f), CircleShape)
            ) {
                Icon(Icons.Default.MoreVert, "Menu", tint = Color.White)
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = onDismissMenu,
                modifier = Modifier.background(Color(0xFF252525))
            ) {
                DropdownMenuItem(
                    text = { Text("Edit / Rename", color = Color.White) },
                    onClick = { onMenuItemClick("edit") },
                    leadingIcon = { Icon(Icons.Default.Edit, null, tint = AccentColor) }
                )
                DropdownMenuItem(
                    text = { Text("Re-run Process", color = Color.White) },
                    onClick = { onMenuItemClick("rerun") },
                    leadingIcon = { Icon(Icons.Default.Refresh, null, tint = AccentColor) }
                )
                HorizontalDivider(thickness = 0.5.dp, color = Color.Gray)
                DropdownMenuItem(
                    text = { Text("Report Issue", color = Color.White) },
                    onClick = { onMenuItemClick("report") },
                    leadingIcon = { Icon(Icons.Default.Flag, null, tint = Color.White) }
                )
                // Delete 移除
            }
        }
    }
}

// ------------------------------------
// 组件：状态时间线
// ------------------------------------
private data class TimelineStep(
    val title: String,
    val icon: ImageVector
)

@Composable
fun ProcessingTimeline(
    status: String,
    createdAt: String,
    estimatedSeconds: Int? = 60,
    onCheckStatus: () -> Unit = {}
) {
    val s = status.trim().lowercase()

    // --- 状态判定 ---
    val isCompleted = s == "completed" || s == "ready" || s == "done" || s == "success"
    val isFailed = s == "failed" || s == "error"
    val isPending = s == "pending" || s == "queued"
    val isProcessing = s == "processing"

    val safeEstimatedSeconds = estimatedSeconds ?: 60

    val steps = remember {
        listOf(
            TimelineStep("Queued", Icons.Outlined.Schedule),
            TimelineStep("Processing", Icons.Outlined.Timelapse),
            TimelineStep("Ready", Icons.Outlined.CheckCircle)
        )
    }

    // --- 核心进度逻辑 ---
    var progressTarget by remember { mutableFloatStateOf(0f) }
    var lastPollTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(s, createdAt, safeEstimatedSeconds) {
        when {
            isCompleted -> {
                progressTarget = 1.0f
            }

            isFailed -> {
                progressTarget = 0.1f
            }

            isPending -> {
                progressTarget = 0.05f
            }

            isProcessing -> {
                while (true) {
                    val rawRatio = calculateRawProgress(createdAt, safeEstimatedSeconds)

                    progressTarget = if (rawRatio <= 1.0f) {
                        0.05f + (rawRatio * 0.90f)
                    } else {
                        val overtime = rawRatio - 1.0f
                        val curve = overtime / (overtime + 3.0f)
                        0.95f + (curve * 0.04f)
                    }

                    if (rawRatio >= 1.0f) {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastPollTime > 3000) {
                            onCheckStatus()
                            lastPollTime = currentTime
                        }
                    }
                    delay(100)
                }
            }
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 500, easing = LinearEasing),
        label = "timelineProgress"
    )

    val currentStepIndex = when {
        isCompleted -> 2
        isProcessing -> 1
        else -> 0
    }

    val pillColor = when {
        isFailed -> ErrorColor
        isCompleted -> AccentColor
        isProcessing -> WarningColor
        else -> Color.White.copy(0.65f)
    }

    val pillText = when {
        isFailed -> "FAILED"
        isCompleted -> "READY"
        isProcessing -> "PROCESSING ${(animatedProgress * 100).toInt()}%"
        else -> "QUEUED"
    }

    val pillIcon = when {
        isFailed -> Icons.Outlined.ErrorOutline
        isCompleted -> Icons.Outlined.CheckCircle
        isProcessing -> Icons.Outlined.Timelapse
        else -> Icons.Outlined.Schedule
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .background(CardBgColor)
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "STATUS",
                    color = Color.White.copy(0.55f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Timeline",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            StatusPill(text = pillText, color = pillColor, icon = pillIcon)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Track + Nodes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = steps.size
                val startX = size.width / (n * 2f)
                val endX = size.width - startX
                val y = size.height / 2f
                val totalWidth = endX - startX

                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = 6f
                )

                val currentBarWidth = totalWidth * animatedProgress
                val progressEndX = startX + currentBarWidth

                if (isFailed) {
                    drawLine(Color.Red.copy(0.8f), Offset(startX, y), Offset(progressEndX, y), 6f)
                } else {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(AccentColor.copy(0.3f), AccentColor),
                            startX = startX, endX = progressEndX
                        ),
                        start = Offset(startX, y), end = Offset(progressEndX, y), strokeWidth = 6f
                    )
                }
            }

            Row(modifier = Modifier.fillMaxSize()) {
                steps.forEachIndexed { index, step ->
                    val nodePos = index.toFloat() / (steps.size - 1)
                    val isPassed = animatedProgress >= (nodePos - 0.05f)
                    val isActive = if (isFailed) index < currentStepIndex else isPassed

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimelineNode(
                            step.icon,
                            isActive,
                            index == currentStepIndex && !isCompleted && !isFailed,
                            isFailed && index == currentStepIndex
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            step.title,
                            color = if (isActive) Color.White else Color.White.copy(0.3f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Footer Text
        val infoText = remember(s, animatedProgress, safeEstimatedSeconds) {
            when {
                isFailed -> "Processing failed."
                isCompleted -> "Created at ${formatUtcToGmt8YmdHm(createdAt)}."
                isPending -> "Waiting for server..."
                isProcessing -> {
                    val rawRatio = calculateRawProgress(createdAt, safeEstimatedSeconds)
                    if (rawRatio >= 1.0f) {
                        "Please wait..."
                    } else {
                        val remaining =
                            ((1f - rawRatio) * safeEstimatedSeconds).toInt().coerceAtLeast(1)
                        "Estimated time remaining: ${remaining}s"
                    }
                }

                else -> "Status: $s"
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AccessTime,
                null,
                tint = Color.White.copy(0.5f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(infoText, color = Color.White.copy(0.55f), fontSize = 11.sp)
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color, icon: ImageVector) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text,
                color = Color.White.copy(0.9f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimelineNode(icon: ImageVector, active: Boolean, current: Boolean, failed: Boolean) {
    val fill = when {
        failed -> ErrorColor
        active || current -> AccentColor
        else -> Color.White.copy(0.18f)
    }
    val border = when {
        failed -> ErrorColor.copy(alpha = 0.65f)
        current -> fill.copy(alpha = 0.70f)
        else -> Color.White.copy(0.10f)
    }

    val infinite = rememberInfiniteTransition(label = "nodePulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.35f, targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infinite.animateFloat(
        initialValue = 1.0f, targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )

    Box(contentAlignment = Alignment.Center) {
        if (current && !failed) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                        alpha = pulseAlpha
                    }
                    .border(1.5.dp, fill.copy(alpha = 0.75f), CircleShape)
            )
        }

        Surface(
            shape = CircleShape,
            color = fill.copy(alpha = if (active || current || failed) 0.95f else 0.22f),
            border = BorderStroke(1.dp, border),
            shadowElevation = if (current) 6.dp else 0.dp
        ) {
            Box(
                modifier = Modifier.size(if (current) 28.dp else 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon, contentDescription = null,
                    tint = when {
                        failed || active || current -> Color.Black
                        else -> Color.White.copy(0.55f)
                    },
                    modifier = Modifier.size(if (current) 16.dp else 14.dp)
                )
            }
        }
    }
}

// ------------------------------------
// 组件：技术参数卡片
// ------------------------------------
@Composable
fun TechSpecsCard(detail: AssetDetail) {
    val s = detail.status.trim().lowercase()
    val isCompleted = s == "completed" || s == "ready" || s == "done" || s == "success"

    val specs = remember(detail.id, isCompleted) {
        if (!isCompleted) {
            listOf(
                SpecItemData("Format", "N/A"), SpecItemData("File Size", "N/A"),
                SpecItemData("Triangles", "N/A"), SpecItemData("Vertices", "N/A"),
                SpecItemData("Materials", "N/A"), SpecItemData("Texture Res", "N/A"),
                SpecItemData("Rigging", "N/A"), SpecItemData("Animation", "N/A"),
                SpecItemData("UV Layout", "N/A"), SpecItemData("Shader", "N/A")
            )
        } else {
            val rnd = kotlin.random.Random(detail.id.hashCode())
            val isRigged = rnd.nextFloat() < 0.45f
            val hasAnim = isRigged && rnd.nextFloat() < 0.75f
            val textureRes = listOf("512x512", "1024x1024", "2048x2048", "4096x4096").random(rnd)
            val trianglesK = rnd.nextInt(20, 9000)
            val verticesK =
                (trianglesK * (0.48 + rnd.nextDouble() * 0.25)).toInt().coerceAtLeast(10)
            val materials = rnd.nextInt(1, 17)
            val animClips = if (!hasAnim) 0 else rnd.nextInt(1, 60)
            val uvLayout = listOf("Non-Overlapping", "UDIM", "Triplanar").random(rnd)
            val shader = listOf("Standard", "PBR", "Unlit", "SSS").random(rnd)
            val format = ".msgpack"
            val fileSizeMb = rnd.nextDouble() * 100.0 + 10.0

            listOf(
                SpecItemData("Format", format),
                SpecItemData("File Size", String.format("%.2f MB", fileSizeMb)),
                SpecItemData("Triangles", "${trianglesK}k"),
                SpecItemData("Vertices", "${verticesK}k"),
                SpecItemData("Materials", "$materials Sets"),
                SpecItemData("Texture Res", textureRes),
                SpecItemData("Rigging", if (isRigged) "Humanoid" else "Static"),
                SpecItemData("Animation", if (hasAnim) "$animClips Clips" else "N/A"),
                SpecItemData("UV Layout", uvLayout),
                SpecItemData("Shader", shader)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
            .background(CardBgColor)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Analytics,
                null,
                tint = AccentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "TECHNICAL SPECIFICATIONS",
                color = AccentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Outlined.Info,
                null,
                tint = Color.White.copy(0.3f),
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        val chunkedSpecs = specs.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            chunkedSpecs.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowItems.forEach { item ->
                        TechSpecGridItem(
                            item,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

data class SpecItemData(val label: String, val value: String)

@Composable
fun TechSpecGridItem(item: SpecItemData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(0.2f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            item.label.uppercase(),
            color = Color.White.copy(0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            item.value,
            color = Color.White.copy(0.9f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ------------------------------------
// 组件：下载格式选择弹窗
// ------------------------------------
@Composable
fun DownloadFormatDialog(
    title: String,
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit
) {
    val formats = listOf("OBJ (Universal)", "GLB (Web/AR)", "PLY (Point Cloud)", "SOURCE DATA")
    var selectedOption by remember { mutableStateOf(formats[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .background(Color(0xFF252525).copy(alpha = 0.95f))
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Download Model",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Select target format for '$title'",
                    color = Color.White.copy(0.6f),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                formats.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .selectable(
                                selected = (selectedOption == format),
                                onClick = { selectedOption = format })
                            .background(if (selectedOption == format) AccentColor.copy(0.1f) else Color.Transparent)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == format), onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = AccentColor,
                                unselectedColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(format, color = Color.White, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White.copy(0.7f))
                    }
                    Button(
                        onClick = { onDownload(selectedOption) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Download", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ------------------------------------
// 组件：文本区域封装
// ------------------------------------
@Composable
fun DetailSection(title: String, content: String, isItalic: Boolean = false) {
    Column {
        Text(
            title.uppercase(),
            color = AccentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            color = Color.White.copy(0.8f),
            fontSize = 14.sp,
            lineHeight = 22.sp,
            fontStyle = if (isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal
        )
    }
}

// ------------------------------------
// 组件：顶部图片轮播
// ------------------------------------
@Composable
fun ImageCarouselHeader(
    videoUrl: String?,
    status: String,
    modifier: Modifier = Modifier
) {
    // 处理失败状态
    if (status == "failed" || status == "error") {
        val errorImageUrl = remember {
            val base = AppConfig.currentBaseUrl.removeSuffix("/")
            "$base/static/states/error.png"
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = errorImageUrl,
                contentDescription = "Processing Failed",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
//                    .padding(40.dp)
                    .alpha(0.8f)
            )
        }
        return
    }


    // 生成随机预览图链接
    val imageUrls = remember(videoUrl) {
        if (videoUrl == null) emptyList() else generateRandomImageUrls(videoUrl, count = 5)
    }

    if (imageUrls.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color(0xFF1E1E1E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.ImageNotSupported,
                null,
                tint = Color.White.copy(0.2f),
                modifier = Modifier.size(48.dp)
            )
        }
        return
    }

    // 轮播逻辑
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })

    LaunchedEffect(pagerState) {
        while (true) {
            delay(2500)
            try {
                val nextPage = (pagerState.currentPage + 1) % imageUrls.size
                pagerState.animateScrollToPage(nextPage)
            } catch (e: Exception) {

            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            AsyncImage(
                model = imageUrls[page],
                contentDescription = "Preview Image $page",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        // 轮播指示器
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(imageUrls.size) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                val width = if (isSelected) 24.dp else 8.dp
                val color = if (isSelected) AccentColor else Color.White.copy(alpha = 0.5f)
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                        .animateContentSize()
                )
            }
        }
    }
}

private fun generateRandomImageUrls(baseUrlRaw: String, count: Int): List<String> {
    val baseUrl = if (baseUrlRaw.startsWith("http", ignoreCase = true)) baseUrlRaw
    else "${AppConfig.currentBaseUrl.removeSuffix("/")}/${baseUrlRaw.removePrefix("/")}"

    val cleanBase = baseUrl.substringBefore("?").substringBeforeLast("/video.mp4")
    val indices = (1..60).shuffled().take(count).sorted()
    return indices.map { index ->
        val fileName = "%04d.jpg".format(index)
        "$cleanBase/images/$fileName"
    }
}

@Composable
fun GlassBottomDock(onPreview: () -> Unit, onDownload: () -> Unit, onShare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(32.dp))
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
            .background(GlassDockColor)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onDownload, modifier = Modifier.size(64.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.CloudDownload, null, tint = Color.White.copy(0.8f))
                Spacer(modifier = Modifier.height(2.dp))
                Text("Save", color = Color.White.copy(0.6f), fontSize = 10.sp)
            }
        }

        Button(
            onClick = onPreview,
            colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .padding(horizontal = 12.dp)
        ) {
            Icon(Icons.Default.ViewInAr, null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text("PREVIEW 3D", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        IconButton(onClick = onShare, modifier = Modifier.size(64.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.Share, null, tint = Color.White.copy(0.8f))
                Spacer(modifier = Modifier.height(2.dp))
                Text("Share", color = Color.White.copy(0.6f), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun CustomGlassDialog(
    title: String,
    text: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .background(Color(0xFF252525).copy(alpha = 0.95f))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text,
                    color = Color.White.copy(0.8f),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f)),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel", color = Color.White.copy(0.7f)) }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        modifier = Modifier.weight(1f)
                    ) { Text(confirmText, color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun TagCapsuleHere(text: String, baseColor: Color) {
    Surface(
        color = baseColor.copy(alpha = 0.16f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, baseColor.copy(alpha = 0.30f)),
    ) {
        Text(
            text = text,
            color = baseColor.copy(alpha = 0.95f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

fun calculateRawProgress(createdAtStr: String, estimatedSeconds: Int): Float {
    return try {
        val cleanTimeStr = createdAtStr.replace("T", " ").substringBefore(".")
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        val createdDate = format.parse(cleanTimeStr) ?: return 0f
        val now = Date().time
        val elapsedSeconds = (now - createdDate.time) / 1000f
        if (estimatedSeconds <= 0) return 0f
        elapsedSeconds / estimatedSeconds.toFloat()
    } catch (e: Exception) {
        0f
    }
}

fun formatUtcToGmt8YmdHm(createdAtStr: String): String {
    return try {
        val clean = createdAtStr.replace("T", " ").substringBefore(".")
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = parser.parse(clean) ?: return createdAtStr
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }
        formatter.format(date)
    } catch (e: Exception) {
        createdAtStr
    }
}

//输入框
@Composable
fun GlassyInputSimple(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(0.5f)) },
        singleLine = singleLine,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Black.copy(0.3f),
            unfocusedContainerColor = Color.Black.copy(0.2f),
            focusedBorderColor = AccentColor,
            unfocusedBorderColor = Color.White.copy(0.1f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = AccentColor,
            focusedLabelColor = AccentColor
        )
    )
}

@Composable
fun ReportIssueDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    var content by remember { mutableStateOf("") }
    val categories = listOf("Bug", "Inappropriate", "Copyright", "Other")
    var selectedCategory by remember { mutableStateOf(categories[0]) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(0.15f),
                            Color.White.copy(0.05f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .background(Color(0xFF1E1E1E).copy(alpha = 0.95f))
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Report Issue",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Why are you reporting this?",
                    color = Color.White.copy(0.7f),
                    fontSize = 14.sp
                )

                // Categories
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = (cat == selectedCategory),
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentColor,
                                selectedLabelColor = Color.Black,
                                containerColor = Color.White.copy(0.05f),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                GlassyInputSimple(
                    value = content,
                    onValueChange = { content = it },
                    label = "Describe the issue...",
                    singleLine = false,
                    minLines = 4
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(0.1f))
                    ) { Text("Cancel", color = Color.White) }

                    Button(
                        onClick = { onSubmit(selectedCategory, content) },
                        enabled = content.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)) // Red for report
                    ) { Text("Report", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}


//编辑栏
@Composable
fun GlassyEditAssetDialog(
    initialTitle: String,
    initialDesc: String,
    initialRemark: String,
    initialTags: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDesc) }
    var remark by remember { mutableStateOf(initialRemark) }

    // 标签状态
    val tags = remember { mutableStateListOf<String>().apply { addAll(initialTags) } }
    var currentTagInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(0.15f),
                            Color.White.copy(0.05f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                )
                .background(Color(0xFF1E1E1E).copy(alpha = 0.95f)) // 深色半透明背景
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Edit Model Info",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, "Close", tint = Color.White.copy(0.6f))
                    }
                }

                HorizontalDivider(color = Color.White.copy(0.1f))

                // Inputs
                GlassyInputSimple(value = title, onValueChange = { title = it }, label = "Title")

                GlassyInputSimple(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description",
                    singleLine = false,
                    minLines = 3
                )

                GlassyInputSimple(
                    value = remark,
                    onValueChange = { remark = it },
                    label = "Private Remark",
                    singleLine = false,
                    minLines = 2
                )

                // Tags Section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Tags",
                        color = AccentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEach { tag ->
                                Surface(
                                    color = AccentColor.copy(0.15f),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, AccentColor.copy(0.3f)),
                                    modifier = Modifier.clickable { tags.remove(tag) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 5.dp
                                        ), verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(tag, color = AccentColor, fontSize = 12.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            null,
                                            tint = AccentColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add Tag Input
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) {
                            GlassyInputSimple(
                                value = currentTagInput,
                                onValueChange = { currentTagInput = it },
                                label = "Add tag..."
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (currentTagInput.isNotBlank() && currentTagInput !in tags) {
                                    tags.add(currentTagInput.trim())
                                    currentTagInput = ""
                                }
                            },
                            modifier = Modifier.background(Color.White.copy(0.1f), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Save Button
                Button(
                    onClick = { onConfirm(title, description, remark, tags) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                    shape = RoundedCornerShape(25.dp)
                ) {
                    Text("SAVE CHANGES", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}