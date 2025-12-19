package com.example.delta3d.ui.screens.publish

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ViewInAr

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.delta3d.api.AssetCard
import com.example.delta3d.api.PostCreateRequest
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.ui.components.GlassyFeedbackPopup
import com.example.delta3d.ui.components.rememberFeedbackState
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.screens.home.TagCapsule
import com.example.delta3d.ui.screens.home.TagColorBinder
import com.example.delta3d.ui.screens.home.TagPalette
import com.example.delta3d.ui.screens.upload.GlassyInput
import com.example.delta3d.ui.session.SessionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// 样式常量
private val ListCardShape = RoundedCornerShape(16.dp)
private val ListCardBackground = Color(0xFF1E1E1E).copy(alpha = 0.8f)
private val GlassBorder = Brush.verticalGradient(
    colors = listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))
)
private val AccentColor = Color(0xFF64FFDA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublishPostScreen(
    sessionVm: SessionViewModel,
    initialAssetId: Int? = null,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val token by sessionVm.token.collectAsState()
    val scope = rememberCoroutineScope()
    // val context = LocalContext.current
    val tagColorBinder = remember { TagColorBinder(TagPalette) }


    val feedbackState = rememberFeedbackState()

    // --- 状态 ---
    var myAssets by remember { mutableStateOf<List<AssetCard>>(emptyList()) }
    var content by remember { mutableStateOf("") }

    // 权限状态
    var visibility by remember { mutableStateOf("public") }
    var allowDownload by remember { mutableStateOf(true) }

    var isSubmitting by remember { mutableStateOf(false) }

    // 传入id
    var selectedAssetId by remember { mutableStateOf(initialAssetId) }

    // 加载数据
    LaunchedEffect(token) {
        token?.let { t ->
            val auth = if (t.startsWith("Bearer ")) t else "Bearer $t"
            try {
                val assets = RetrofitClient.api.getAssets(auth)
                myAssets = assets.filter { it.status == "completed" }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Create Post", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                Button(
                    onClick = {
                        val currentToken = token ?: ""
                        if (token != null && selectedAssetId != null) {
                            scope.launch {
                                isSubmitting = true
                                try {
                                    val auth =
                                        if (currentToken.startsWith("Bearer ")) currentToken else "Bearer $currentToken"
                                    val req = PostCreateRequest(
                                        assetId = selectedAssetId!!,
                                        content = content,
                                        visibility = visibility,
                                        allowDownload = allowDownload
                                    )
                                    RetrofitClient.api.publishPost(auth, req)

                                    // 显示成功弹窗并延迟跳转
                                    feedbackState.showSuccess("Published Successfully!")
                                    delay(100)
                                    onSuccess()

                                } catch (e: Exception) {
                                    // 显示错误弹窗
                                    feedbackState.showError("Failed: ${e.message}")
                                } finally {
                                    isSubmitting = false
                                }
                            }
                        }
                    },
                    enabled = selectedAssetId != null && !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 36.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentColor,
                        disabledContainerColor = Color.White.copy(0.1f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = Color.Black)
                    } else {
                        Text(
                            "PUBLISH NOW",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {

                // 模型选择器 ---
                item {
                    SectionHeader("SELECT MODEL")
                    Spacer(modifier = Modifier.height(8.dp))

                    if (myAssets.isEmpty()) {
                        Text("No completed models found.", color = Color.White.copy(0.5f))
                    } else {

                        ModelSelectorDropdown(
                            assets = myAssets,
                            selectedId = selectedAssetId,
                            tagColorBinder = tagColorBinder,
                            onSelect = { selectedAssetId = it }
                        )
                    }
                }

                // 内容输入区
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SectionHeader("DESCRIPTION")
                        GlassyInput(
                            value = content,
                            onValueChange = { content = it },
                            label = "Write something you want to say...",
                            icon = Icons.Default.Add,
                            singleLine = false,
                            minLines = 3
                        )
                    }
                }

                // 设置区
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionHeader("SETTINGS")

                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(0.3f))
                                .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(16.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Visibility
                            Text(
                                "Visibility",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VisibilityChip("Public", "public", visibility) { visibility = it }
                                VisibilityChip("Followers", "followers", visibility) {
                                    visibility = it
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(0.1f))

                            // Allow Download
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Allow Download",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Others can download source files",
                                        color = Color.White.copy(0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                                Switch(
                                    checked = allowDownload,
                                    onCheckedChange = { allowDownload = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.Black,
                                        checkedTrackColor = AccentColor,
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color.White.copy(0.2f)
                                    )
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(60.dp)) }
            }
        }


        GlassyFeedbackPopup(
            state = feedbackState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

// 下拉框组件
@Composable
fun ModelSelectorDropdown(
    assets: List<AssetCard>,
    selectedId: Int?,
    tagColorBinder: TagColorBinder,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // 旋转动画
    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "ArrowRotation"
    )

    // 找到当前选中的对象
    val selectedAsset = assets.find { it.id == selectedId }

    Column(
        modifier = Modifier
            // 外部容器样式
            .fillMaxWidth()
            .clip(ListCardShape)
            .background(ListCardBackground)
            .border(
                width = if (expanded) 1.dp else 0.5.dp,
                brush = if (expanded) Brush.verticalGradient(
                    listOf(
                        AccentColor,
                        AccentColor
                    )
                ) else GlassBorder,
                shape = ListCardShape
            )
            .animateContentSize()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedAsset != null) {

                Box(modifier = Modifier.weight(1f)) {
                    SelectableAssetItem(
                        item = selectedAsset,
                        isSelected = true,
                        tagColorBinder = tagColorBinder,
                        onClick = { expanded = !expanded },
                        isInsideDropdown = true
                    )
                }
            } else {
                // 如果未选中，显示提示文本
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Outlined.ViewInAr,
                        contentDescription = null,
                        tint = AccentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Select a model...",
                        color = Color.White.copy(0.7f),
                        fontSize = 16.sp
                    )
                }
            }

            // 箭头图标
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Expand",
                tint = if (expanded || selectedAsset != null) AccentColor else Color.White.copy(0.5f),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(28.dp)
                    .rotate(rotationState)
            )
        }

        // --- 下拉列表
        if (expanded) {
            HorizontalDivider(color = Color.White.copy(0.1f), thickness = 1.dp)

            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                assets.forEach { asset ->

                    // if (asset.id != selectedId) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        SelectableAssetItem(
                            item = asset,
                            isSelected = asset.id == selectedId,
                            tagColorBinder = tagColorBinder,
                            onClick = {
                                onSelect(asset.id)
                                expanded = false
                            },
                            isInsideDropdown = true
                        )
                    }
                    // }
                }
            }
        }
    }
}


@Composable
fun SelectableAssetItem(
    item: AssetCard,
    isSelected: Boolean,
    tagColorBinder: TagColorBinder,
    onClick: () -> Unit,
    isInsideDropdown: Boolean = false
) {
    val fullImageUrl = remember(item.coverUrl) {
        item.coverUrl?.let { url ->
            val base = RetrofitClient.BASE_URL.removeSuffix("/")
            val path = url.removePrefix("/").removeSuffix("/")
            "$base/$path/images/0001.jpg"
        }
    }


    // 如果是“选中状态”高亮
    val showBorder = !isInsideDropdown || isSelected
    val borderBrush =
        if (isSelected) Brush.linearGradient(listOf(AccentColor, AccentColor)) else GlassBorder
    val borderWidth = if (isSelected) 1.dp else 0.dp


    val bgColor = if (isInsideDropdown && isSelected) Color.Transparent else ListCardBackground

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .clip(ListCardShape)
            .then(
                if (showBorder) Modifier.border(
                    borderWidth,
                    borderBrush,
                    ListCardShape
                ) else Modifier
            )
            .background(bgColor)
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // 封面
            Box(
                modifier = Modifier
                    .width(115.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(0.3f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fullImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 内容
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            color = if (isSelected) AccentColor else Color.White.copy(0.95f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        if (!isInsideDropdown || isSelected) {
                            Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = if (isSelected) AccentColor else Color.White.copy(0.2f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description ?: "No description",
                        color = Color.White.copy(0.5f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.tags.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(item.tags, key = { it }) { tag ->
                            TagCapsule(text = tag, baseColor = tagColorBinder.colorFor(tag))
                        }
                    }
                } else {
                    Text(
                        text = item.createdAt.substringBefore(" "),
                        color = Color.White.copy(0.3f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// 辅助组件
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        color = AccentColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
fun RowScope.VisibilityChip(
    label: String,
    value: String,
    currentValue: String,
    onSelect: (String) -> Unit
) {
    val isSelected = value == currentValue
    val bgColor = if (isSelected) AccentColor else Color.White.copy(0.05f)
    val textColor = if (isSelected) Color.Black else Color.White.copy(0.7f)
    val borderColor = if (isSelected) AccentColor else Color.White.copy(0.2f)

    Box(
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onSelect(value) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}