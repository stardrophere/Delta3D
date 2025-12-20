package com.example.delta3d.ui.screens.home


import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.delta3d.api.AssetCard
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.session.SessionViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cached
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Warning

// --- 样式常量 ---
private val CardShape = RoundedCornerShape(16.dp)
private val GlassBorder = Brush.verticalGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.02f)
    )
)
private val GlassBackground = Brush.linearGradient(
    colors = listOf(
        Color(0xFF2A2A2A).copy(alpha = 0.6f),
        Color(0xFF121212).copy(alpha = 0.6f)
    )
)

// FAB 专属渐变
private val FabGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF7C4DFF),
        Color(0xFF00E5FF)
    )
)

//限制数目
private const val MAX_PROCESSING_LIMIT = 1

private val AccentColor = Color(0xFF64FFDA)
private val TextWhite = Color.White
private val TextGray = Color.White.copy(alpha = 0.6f)
private val GlassContainerColor = Color(0xFF1E1E1E).copy(alpha = 0.95f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    sessionVm: SessionViewModel,
    homeVm: HomeViewModel = viewModel(),
    onAssetClick: (Int) -> Unit,
    onNavigateToUpload: (android.net.Uri) -> Unit
) {
    val token by sessionVm.token.collectAsState()

    // 观察 VM 处理好的数据
    val displayAssets by homeVm.displayAssets.collectAsState()
    val isRefreshing by homeVm.isRefreshing.collectAsState()
    val searchQuery by homeVm.searchQuery.collectAsState()
    val processingCount by homeVm.processingCount.collectAsState()

    // 网格/列表 视图切换状态
    var isGridView by remember { mutableStateOf(true) }
    val BubbleGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF))
    )
    var showUploadGuide by remember { mutableStateOf(false) }


    var showLimitDialog by remember { mutableStateOf(false) }

    // 定义媒体选择器 Launcher
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // 当用户选中视频后，回调 URI 给上层导航
        if (uri != null) {
            onNavigateToUpload(uri)
        }
    }

    LaunchedEffect(token) {
        token?.let { if (it.isNotEmpty()) homeVm.loadAssets(it) }
    }

    // 定义点击处理函数：调用 VM 的 toggleCollect
    val onCollectToggle: (Int) -> Unit = { id ->
        token?.let { homeVm.toggleCollect(id, it) }
    }

    // 进场动画状态
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val headerAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(800), label = "alpha")
    val headerOffset by animateFloatAsState(
        if (entered) 0f else 40f,
        tween(800, easing = LinearOutSlowInEasing),
        label = "offset"
    )

    // 如果正在搜索，强制显示列表视图
    val showListView = !isGridView || searchQuery.isNotEmpty()

    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() }
    ) {
        AnimatedGradientBackground()

        // 内容列
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            //顶部 Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .offset(y = headerOffset.dp)
                    .alpha(headerAlpha)
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Δ 3D",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        color = Color.White
                    )

                    AnimatedVisibility(
                        visible = processingCount > 0 && searchQuery.isEmpty(),
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        ProcessingStatusBadge(count = processingCount)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 搜索栏区域
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EditableGlassySearchBar(
                        query = searchQuery,
                        onQueryChange = { homeVm.onSearchInput(it) }, //VM 防抖
                        placeholder = "Search models or tags...",
                        modifier = Modifier.weight(1f)
                    )

                    AnimatedVisibility(
                        visible = searchQuery.isEmpty(),
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        Row {
                            Spacer(modifier = Modifier.width(10.dp))
                            GlassyIconButton(
                                icon = if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                                onClick = { isGridView = !isGridView }
                            )
                        }
                    }
                }
            }

            //列表内容
            Box(modifier = Modifier.weight(1f)) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { token?.let { homeVm.loadAssets(it) } },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (displayAssets.isEmpty() && searchQuery.isNotEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No results found for \"$searchQuery\"",
                                color = Color.White.copy(0.5f),
                                modifier = Modifier.offset(y = (-20).dp)
                            )
                        }
                    } else {
                        AnimatedContent(
                            targetState = showListView,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.98f))
                                    .togetherWith(fadeOut(animationSpec = tween(300)))
                            },
                            label = "ViewSwitchAnimation",
                            modifier = Modifier.fillMaxSize()
                        ) { isListMode ->
                            if (isListMode) {
                                // 列表视图
                                ProductListView(
                                    dataList = displayAssets,
                                    bottomPadding = innerPadding.calculateBottomPadding(),
                                    onItemClick = onAssetClick,
                                    onCollectClick = onCollectToggle
                                )
                            } else {
                                // 网格视图
                                ProductStaggeredGrid(
                                    dataList = displayAssets,
                                    bottomPadding = innerPadding.calculateBottomPadding(),
                                    onItemClick = onAssetClick,
                                    onCollectClick = onCollectToggle
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = innerPadding.calculateBottomPadding() + 20.dp, end = 20.dp)
        ) {
            GlassBubble(
                modifier = Modifier
                    .size(50.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            // 判断次数
                            if (processingCount >= MAX_PROCESSING_LIMIT) {
                                showLimitDialog = true
                            } else {
                                showUploadGuide = true
                            }
                        }
                    ),
                fill = FabGradient,
                icon = Icons.Rounded.Add,
                iconSize = 44.dp
            )
        }

        // 弹窗组件
        if (showUploadGuide) {
            GlassyUploadGuideDialog(
                onDismiss = { showUploadGuide = false },
                onConfirm = {
                    showUploadGuide = false
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
            )
        }

        //限制弹窗
        if (showLimitDialog) {
            GlassyLimitDialog(
                onDismiss = { showLimitDialog = false }
            )
        }

    }
}

// --- 组件定义 ---

@Composable
fun EditableGlassySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = if (query.isNotEmpty()) Color.White else Color.White.copy(0.6f),
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(10.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color.White.copy(0.4f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(Color.White),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = scaleIn(),
                exit = scaleOut()
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = Color.White.copy(0.7f),
                    modifier = Modifier
                        .size(20.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onQueryChange("")
                            focusManager.clearFocus()
                        }
                )
            }
        }
    }
}

@Composable
fun GlassyIconButton(
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = "Switch View",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(26.dp)
            )
        }
    }
}


@Composable
fun ProductStaggeredGrid(
    dataList: List<AssetCard>,
    bottomPadding: Dp,
    onItemClick: (Int) -> Unit,
    onCollectClick: (Int) -> Unit
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
        contentPadding = PaddingValues(
            start = 12.dp, end = 12.dp, top = 8.dp,
            bottom = bottomPadding + 80.dp
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        items(dataList, key = { it.id }) { item ->
            ProductCard(
                item = item,
                onClick = { onItemClick(item.id) },
                onCollectClick = { onCollectClick(item.id) }
            )
        }
    }
}

// 绑定到图标
@Composable
fun ProductCard(item: AssetCard, onClick: () -> Unit, onCollectClick: () -> Unit) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    val height = remember { item.height.dp }
    val fullImageUrl = remember(item.coverUrl) {
        item.coverUrl?.let { url ->
            val base = RetrofitClient.BASE_URL.removeSuffix("/")
            val path = url.removePrefix("/").removeSuffix("/")
            "$base/$path/images/0001.jpg"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .alpha(if (show) 1f else 0f)
            .scale(if (show) 1f else 0.95f)
            .clip(CardShape)
            .border(0.5.dp, GlassBorder, CardShape)
            .background(GlassBackground)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(Color.Black.copy(alpha = 0.2f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(fullImageUrl)
                        .crossfade(300)
                        .build(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.3f))
                        .border(0.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { onCollectClick() }, //触发点击回调
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isCollected) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Collect",
                        tint = if (item.isCollected) Color(0xFFFFC107) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    maxLines = 1,
                    letterSpacing = 0.5.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = item.description ?: "No description",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2,
                    minLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}


@Composable
fun ProcessingStatusBadge(count: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val accentColor = Color(0xFF64FFDA)

    Surface(
        color = Color.Black.copy(alpha = 0.3f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(
            1.dp, Brush.linearGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.3f),
                    accentColor.copy(alpha = 0.05f)
                )
            )
        ),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Sync,
                contentDescription = "Processing",
                tint = accentColor,
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { rotationZ = angle }
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "$count Model Processing...",
                color = accentColor.copy(alpha = 0.9f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// 用于 FAB
@Composable
private fun GlassBubble(
    modifier: Modifier = Modifier,
    fill: Brush,
    icon: ImageVector,
    iconSize: Dp = 34.dp,
    iconTint: Color = Color.White
) {
    Box(
        modifier = modifier
            .shadow(10.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(fill)
            .drawWithCache {
                val strokeW = 1.2.dp.toPx()
                val highlight = Brush.radialGradient(
                    0.0f to Color.White.copy(alpha = 0.5f),
                    0.55f to Color.Transparent,
                    center = Offset(size.width * 0.28f, size.height * 0.22f),
                    radius = size.minDimension * 0.9f
                )
                val rim = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.35f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                val innerShadow = Brush.radialGradient(
                    0.0f to Color.Transparent,
                    0.70f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.22f),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.minDimension * 0.55f
                )
                val glare = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                onDrawWithContent {
                    drawContent()
                    drawCircle(brush = innerShadow)
                    drawCircle(brush = highlight)
                    withTransform({
                        rotate(degrees = -22f, pivot = center)
                        translate(left = -size.width * 0.15f, top = size.height * 0.05f)
                    }) {
                        drawRect(brush = glare, size = Size(size.width * 1.3f, size.height * 0.32f))
                    }
                    drawCircle(brush = rim, style = Stroke(width = strokeW))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

//指引弹窗
@Composable
fun GlassyUploadGuideDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                // 边框渐变，增加质感
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .background(GlassContainerColor)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recording Tips",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, "Close", tint = TextGray)
                    }
                }

                // 分割线
                HorizontalDivider(color = Color.White.copy(0.1f))


                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GuideTipItem(
                        icon = Icons.Rounded.Cached,
                        title = "Orbit the Object",
                        desc = "Move slowly 360° around the subject."
                    )
                    GuideTipItem(
                        icon = Icons.Rounded.Timer,
                        title = "Keep it Short",
                        desc = "Duration must be under 30 seconds."
                    )
                    GuideTipItem(
                        icon = Icons.Rounded.SdStorage,
                        title = "File Size Limit",
                        desc = "File size must be under 20MB."
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // ---底部按钮
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel 按钮
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(0.05f), // 淡淡的灰色
                            contentColor = TextWhite.copy(0.8f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }

                    // Confirm 按钮 (高亮)
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentColor,
                            contentColor = Color.Black
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GuideTipItem(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(0.03f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        // 图标容器
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(AccentColor.copy(0.1f), CircleShape)
                .border(1.dp, AccentColor.copy(0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 文字
        Column {
            Text(
                text = title,
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = desc,
                color = TextGray,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}


@Composable
fun GlassyLimitDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF5252).copy(0.3f),
                            Color.Transparent
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .background(GlassContainerColor)
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 图标
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFFF5252).copy(0.1f), CircleShape)
                        .border(1.dp, Color(0xFFFF5252).copy(0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = "Server Busy",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Due to limited server resources, please wait for your current task to finish before uploading a new one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(0.1f),
                        contentColor = TextWhite
                    ),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Text("Understood", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}