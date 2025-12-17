package com.example.delta3d.ui.screens.detail

import android.net.Uri
import android.util.Log
import android.widget.MediaController
import android.widget.VideoView
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.delta3d.api.AssetDetail
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.screens.home.TagCapsule
import com.example.delta3d.ui.screens.home.TagColorBinder
import com.example.delta3d.ui.screens.home.TagPalette
import com.example.delta3d.ui.session.SessionViewModel
import kotlin.random.Random
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Info
import coil.compose.AsyncImage
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState


// --- 样式常量 ---
private val AccentColor = Color(0xFF64FFDA) // 青色高亮
private val WarningColor = Color(0xFFFFAB40) // 橙色警告/处理中
private val ErrorColor = Color(0xFFFF5252)   // 红色错误
private val SurfaceColor = Color(0xFF1E1E1E) // 深色背景
private val GlassDockColor = Color(0xFF1E1E1E).copy(alpha = 0.90f)
private val CardBgColor = Color(0xFF2C2C2C).copy(alpha = 0.6f) // 半透明卡片底

private val GlassBorder = Brush.verticalGradient(
    colors = listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))
)

@Composable
fun AssetDetailScreen(
    assetId: Int,
    onBack: () -> Unit,
    sessionVm: SessionViewModel,
    onPreviewClick: () -> Unit,
    detailVm: AssetDetailViewModel = viewModel()
) {
    val token by sessionVm.token.collectAsState()
    val uiState by detailVm.uiState.collectAsState()

    // 弹窗状态
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    // 菜单状态
    var showMenu by remember { mutableStateOf(false) }

    // 初始化加载
    LaunchedEffect(assetId, token) {
        token?.let { if (it.isNotEmpty()) detailVm.loadDetail(it, assetId) }
    }
    val tagColorBinder = remember { TagColorBinder(TagPalette) }

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
                        .padding(bottom = 140.dp) // 给底部悬浮栏留出更多空间
                ) {
                    // 1. 视频区域
                    ImageCarouselHeader(videoUrl = detail.videoUrl)

                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {

                        // 1) 标题
                        Text(
                            text = detail.title,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 34.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2) 标签（调大 + 加粗）
                        if (detail.tags.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                detail.tags.forEach { tag ->
                                    TagCapsuleHere(
                                        text = tag,
                                        baseColor = tagColorBinder.colorFor(tag),
                                        // 如果你的 TagCapsule 还没支持这俩参数，就看下面“TagCapsule 改法”
//                                        fontSize = 14.sp,
//                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                        }

                        // 3) 时间线
                        ProcessingTimeline(
                            status = detail.status,
                            createdAt = detail.createdAt
                        )

                        Spacer(modifier = Modifier.height(22.dp))

                        // 4) Tech Specs
                        TechSpecsCard(detail)

                        Spacer(modifier = Modifier.height(18.dp))

                        //描述
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

                // --- 顶部导航栏 (透明覆盖) ---
                TopNavBar(
                    onBack = onBack,
                    onMenuClick = { showMenu = true },
                    showMenu = showMenu,
                    onDismissMenu = { showMenu = false },
                    onMenuItemClick = { action ->
                        showMenu = false
                        when (action) {
                            "edit" -> showEditDialog = true
                            "delete" -> { /* TODO: 删除逻辑 */
                            }

                            "rerun" -> { /* TODO: 重新运行任务 */
                            }
                        }
                    }
                )

                // --- 底部悬浮操作栏 ---
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 24.dp, vertical = 40.dp) // 往上提了一点
                ) {
                    GlassBottomDock(
                        onPreview = {
                            Log.d(
                                "TRACK_ID",
                                "1. [DetailScreen] 点击按钮, 准备跳转 ID: $assetId"
                            ) // 🟢 加在这里
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
                            // TODO: 触发真实下载逻辑，带上格式
                        }
                    )
                }

                if (showShareDialog) {
                    CustomGlassDialog(
                        title = "Share Asset",
                        text = "Public Link: ${RetrofitClient.BASE_URL}share/${detail.id}",
                        confirmText = "Copy Link",
                        onDismiss = { showShareDialog = false },
                        onConfirm = { showShareDialog = false }
                    )
                }

                if (showEditDialog) {
                    // TODO: 这里可以放一个输入框弹窗，简化起见复用 CustomGlassDialog 示意
                    CustomGlassDialog(
                        title = "Edit Info",
                        text = "Edit title, tags and description functionality would go here.",
                        confirmText = "Save",
                        onDismiss = { showEditDialog = false },
                        onConfirm = { showEditDialog = false }
                    )
                }
            }
        }
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
            .padding(top = 40.dp, start = 16.dp, end = 16.dp), // 适配状态栏
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
                DropdownMenuItem(
                    text = { Text("Delete", color = ErrorColor) },
                    onClick = { onMenuItemClick("delete") },
                    leadingIcon = { Icon(Icons.Default.DeleteForever, null, tint = ErrorColor) }
                )
            }
        }
    }
}

// ------------------------------------
// 组件：状态时间线 (Timeline) - prettier version
// ------------------------------------
private data class TimelineStep(
    val title: String,
    val icon: ImageVector
)

@Composable
fun ProcessingTimeline(status: String, createdAt: String) {
    val s = status.trim().lowercase()

    val steps = remember {
        listOf(
            TimelineStep("Queued", Icons.Outlined.Schedule),
            TimelineStep("Processing", Icons.Outlined.Timelapse),
            TimelineStep("Ready", Icons.Outlined.CheckCircle)
        )
    }

    val isFailed = s == "failed" || s == "error"
    val currentStepIndex = when (s) {
        "pending" -> 0
        "processing" -> 1
        "completed", "ready", "done", "success" -> 2
        "failed", "error" -> 1 // 失败一般发生在 processing；你也可以按后端语义改
        else -> 0
    }.coerceIn(0, steps.lastIndex)

    val progressTarget =
        if (steps.size <= 1) 0f else (currentStepIndex.toFloat() / (steps.size - 1).toFloat())
    val progress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "timelineProgress"
    )

    val pillColor = when {
        isFailed -> ErrorColor
        currentStepIndex == 0 -> Color.White.copy(0.65f)
        currentStepIndex == 1 -> WarningColor
        else -> AccentColor
    }
    val pillText = when {
        isFailed -> "FAILED"
        currentStepIndex == 0 -> "QUEUED"
        currentStepIndex == 1 -> "PROCESSING"
        else -> "READY"
    }
    val pillIcon = when {
        isFailed -> Icons.Outlined.ErrorOutline
        currentStepIndex == 0 -> Icons.Outlined.Schedule
        currentStepIndex == 1 -> Icons.Outlined.Timelapse
        else -> Icons.Outlined.CheckCircle
    }

    val displayTime = runCatching {
        createdAt.replace("T", " ").substringBeforeLast(".")
    }.getOrElse { createdAt }

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

        Spacer(modifier = Modifier.height(14.dp))

        // Track + Nodes
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            // Track (behind nodes)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = steps.size.coerceAtLeast(2)
                val startX = size.width / (n * 2f)
                val endX = size.width - startX
                val y = size.height / 2f

                // base line
                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = Offset(startX, y),
                    end = Offset(endX, y),
                    strokeWidth = 6f
                )

                val progressX = startX + (endX - startX) * progress

                if (isFailed) {
                    drawLine(
                        color = ErrorColor.copy(alpha = 0.9f),
                        start = Offset(startX, y),
                        end = Offset(progressX, y),
                        strokeWidth = 6f
                    )
                } else {
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                AccentColor.copy(alpha = 0.35f),
                                AccentColor.copy(alpha = 0.95f)
                            )
                        ),
                        start = Offset(startX, y),
                        end = Offset(progressX, y),
                        strokeWidth = 6f
                    )
                }
            }

            // Nodes
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                steps.forEachIndexed { index, step ->
                    val isActive = index <= currentStepIndex && !isFailed
                    val isCurrent = index == currentStepIndex
                    val isDim = !isActive && !isCurrent

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TimelineNode(
                            icon = step.icon,
                            active = isActive,
                            current = isCurrent,
                            failed = isFailed && isCurrent
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = step.title,
                            color = when {
                                isFailed && isCurrent -> ErrorColor
                                isDim -> Color.White.copy(0.35f)
                                else -> Color.White
                            },
                            fontSize = 11.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Footer time
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = Color.White.copy(0.5f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isFailed) "Failed at $displayTime" else "Last updated: $displayTime",
                color = if (isFailed) ErrorColor else Color.White.copy(0.55f),
                fontSize = 11.sp
            )
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

    // 呼吸光圈（只给当前节点）
    val infinite = rememberInfiniteTransition(label = "nodePulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.55f,
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = when {
                        failed -> Color.Black
                        active || current -> Color.Black
                        else -> Color.White.copy(0.55f)
                    },
                    modifier = Modifier.size(if (current) 16.dp else 14.dp)
                )
            }
        }
    }
}


// ------------------------------------
// 组件：技术参数卡片 (Tech Specs)
// ------------------------------------
// ------------------------------------
// 组件：增强版技术参数卡片 (Tech Specs)
// ------------------------------------
@Composable
fun TechSpecsCard(detail: AssetDetail) {
    // --- 1. 模拟更丰富的硬核数据 ---
    // 使用 remember(detail.id) 确保数据对于同一个物品是固定的，不会乱跳
    val rnd = remember(detail.id) { kotlin.random.Random(detail.id.hashCode()) }

    val isRigged = remember(detail.id) { rnd.nextFloat() < 0.45f }
    val hasAnim = remember(detail.id) { isRigged && rnd.nextFloat() < 0.75f }

    val textureRes = remember(detail.id) {
        listOf("512x512", "1024x1024", "2048x2048", "4096x4096", "4096x2048").random(rnd)
    }

    val trianglesK = remember(detail.id) {
        // 分段更像真实：大多数在中间段
        val bucket = rnd.nextInt(100)
        when {
            bucket < 25 -> rnd.nextInt(20, 220)        // 轻量 20k-220k
            bucket < 85 -> rnd.nextInt(220, 1800)      // 常见 220k-1800k
            else -> rnd.nextInt(1800, 9000)            // 重型 1.8M-9M
        }
    }

    val verticesK = remember(detail.id) {
        val ratio = 0.48 + rnd.nextDouble() * 0.25    // 0.48~0.73
        (trianglesK * ratio).toInt().coerceAtLeast(10)
    }

    val materials = remember(detail.id) {
        // 1~16，偏向 1~6
        val bucket = rnd.nextInt(100)
        when {
            bucket < 60 -> rnd.nextInt(1, 7)
            bucket < 90 -> rnd.nextInt(7, 11)
            else -> rnd.nextInt(11, 17)
        }
    }

    val animClips = remember(detail.id) {
        if (!hasAnim) 0 else {
            val bucket = rnd.nextInt(100)
            when {
                bucket < 40 -> rnd.nextInt(1, 8)
                bucket < 85 -> rnd.nextInt(8, 28)
                else -> rnd.nextInt(28, 61)
            }
        }
    }

    val uvLayout = remember(detail.id) {
        listOf(
            "Non-Overlapping",
            "Overlapping (Mirrored)",
            "UDIM (2 Tiles)",
            "UDIM (4 Tiles)",
            "UDIM (8 Tiles)",
            "Triplanar (No UV)"
        ).random(rnd)
    }

    val shader = remember(detail.id) {
        listOf(
            "Standard Surface",
            "PBR Metallic-Roughness",
            "Unlit",
            "Toon",
            "Glass / Transmission",
            "SSS (Skin)"
        ).random(rnd)
    }

    val format = remember(detail.id) {
        listOf(".msgpack (v1.0)", ".msgpack (v2.0)", ".msgpack (v2.1)").random(rnd)
    }

    val fileSizeMb = remember(detail.id) {
        // 粗略估：基础 + (贴图档位) + (面数档位) + (动画开销)
        val texFactor = when {
            textureRes.startsWith("512") -> 10.0
            textureRes.startsWith("1024") -> 18.0
            textureRes.startsWith("2048") -> 35.0
            textureRes.startsWith("4096") -> 90.0
            textureRes.startsWith("8192") -> 220.0
            else -> 70.0 // 4096x2048
        }
        val geoFactor = trianglesK / 35.0               // 三角面越多越大
        val animFactor = if (hasAnim) animClips * 2.8 else 0.0
        val matFactor = materials * 6.5

        val base = 8.0 + rnd.nextDouble() * 10.0
        (base + texFactor + geoFactor + animFactor + matFactor).coerceIn(8.0, 650.0)
    }

    val specs = remember(detail.id) {
        listOf(
            SpecItemData("Format", format),
            SpecItemData("File Size", String.format("%.2f MB", fileSizeMb)),

            SpecItemData("Triangles", "${trianglesK}k"),
            SpecItemData("Vertices", "${verticesK}k"),

            SpecItemData("Materials", "$materials PBR Sets"),
            SpecItemData("Texture Res", textureRes),

            SpecItemData(
                "Rigging",
                if (isRigged) "Humanoid (${rnd.nextInt(45, 150)} Bones)" else "Static Mesh"
            ),
            SpecItemData("Animation", if (hasAnim) "$animClips Clips" else "N/A"),

            SpecItemData("UV Layout", uvLayout),
            SpecItemData("Shader", shader)
        )
    }


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(16.dp))
            .background(CardBgColor) // 你的深色半透明背景
            .padding(20.dp)
    ) {
        // 卡片标题
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
            // 可以加个小图标表示数据来源
            Icon(
                Icons.Outlined.Info,
                null,
                tint = Color.White.copy(0.3f),
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. 渲染网格 ---
        // 这里使用简单的 Column + Row 模拟 Grid，每行放2个，保证对齐
        val chunkedSpecs = specs.chunked(2)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            chunkedSpecs.forEach { rowItems ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    rowItems.forEach { item ->
                        // 每一个格子占据一半宽度 (weight 1f)
                        TechSpecGridItem(item, modifier = Modifier.weight(1f))
                    }
                    // 如果最后一行只有一个，补一个空的占位符保持对齐
                    if (rowItems.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// 数据类，方便管理
data class SpecItemData(val label: String, val value: String)

// 单个格子的 UI
@Composable
fun TechSpecGridItem(item: SpecItemData, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(0.2f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = item.label.uppercase(),
            color = Color.White.copy(0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.value,
            color = Color.White.copy(0.9f),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun SpecItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = label, color = Color.White.copy(0.4f), fontSize = 11.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = Color.White.copy(0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
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
    val formats = listOf("OBJ (Universal)", "GLB (Web/AR)", "PLY (Point Cloud)")
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
                    "Download Assets",
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
                                onClick = { selectedOption = format }
                            )
                            .background(if (selectedOption == format) AccentColor.copy(0.1f) else Color.Transparent)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == format),
                            onClick = null,
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
// 组件：顶部图片轮播 (Carousel)
// ------------------------------------
@Composable
fun ImageCarouselHeader(
    videoUrl: String?,
    modifier: Modifier = Modifier
) {
    // 1. 生成 5 张随机图片 URL
    val imageUrls = remember(videoUrl) {
        if (videoUrl == null) emptyList()
        else generateRandomImageUrls(videoUrl, count = 5)
    }

    // 如果没有图片（URL为空），显示占位
    if (imageUrls.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(250.dp) // 固定高度
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

    // 2. Pager 状态
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })

    // 3. 自动轮播逻辑
    LaunchedEffect(pagerState) {
        while (true) {
            kotlinx.coroutines.delay(1300) // 2s切换
            try {
                val nextPage = (pagerState.currentPage + 1) % imageUrls.size
                pagerState.animateScrollToPage(nextPage)
            } catch (e: Exception) {
                // 忽略页面销毁时的异常
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp) // ✅ 固定高度，你可以根据需要调整 (如 300.dp)
    ) {
        // 4. 轮播内容
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            AsyncImage(
                model = imageUrls[page],
                contentDescription = "Preview Image $page",
                contentScale = ContentScale.Crop, // ✅ 裁剪填满
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black) // 图片加载前的底色
            )
        }

        // 5. 底部渐变遮罩 (让指示器更清晰)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
        )

        // 6. 指示器 (Dots)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(imageUrls.size) { iteration ->
                val isSelected = pagerState.currentPage == iteration
                // 选中的是长条，未选中是圆点
                val width = if (isSelected) 24.dp else 8.dp
                val color = if (isSelected) AccentColor else Color.White.copy(alpha = 0.5f)

                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(color)
                        .animateContentSize() // 宽度变化的动画
                )
            }
        }
    }
}

// 辅助逻辑：生成图片 URL 列表
private fun generateRandomImageUrls(baseUrlRaw: String, count: Int): List<String> {
    // 1. 处理 Base URL (确保是绝对路径)
    val baseUrl = if (baseUrlRaw.startsWith("http", ignoreCase = true)) {
        baseUrlRaw
    } else {
        "${RetrofitClient.BASE_URL.removeSuffix("/")}/${baseUrlRaw.removePrefix("/")}"
    }

    // 2. 移除可能存在的 .mp4 后缀或 query 参数，获取纯净目录路径
    // 假设 videoUrl 类似 ".../assets/123/video.mp4" 或 ".../assets/123"
    // 我们需要的是 ".../assets/123"
    val cleanBase = baseUrl.substringBefore("?").substringBeforeLast("/video.mp4")

    // 3. 随机生成 5 个不重复的序号 (1~100)
    // 使用 seed 确保每次 recompose 不会乱变，但这里因为是 remember(videoUrl) 所以只会在进入页面时生成一次
    val indices = (1..100).shuffled().take(count).sorted()

    // 4. 拼接 URL
    return indices.map { index ->
        // 格式化为 0001.jpg, 0045.jpg 等
        val fileName = "%04d.jpg".format(index)
        "$cleanBase/images/$fileName"
    }
}

// ------------------------------------
// 复用你之前的 Glass Dock，微调样式
// ------------------------------------
@Composable
fun GlassBottomDock(
    onPreview: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
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

// ------------------------------------
// 组件：通用玻璃拟态弹窗
// ------------------------------------
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
                    ) {
                        Text("Cancel", color = Color.White.copy(0.7f))
                    }
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(confirmText, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 🏷️ 彩色胶囊标签（Detail页专用：更大更醒目）
 */
@Composable
fun TagCapsuleHere(
    text: String,
    baseColor: Color
) {
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
