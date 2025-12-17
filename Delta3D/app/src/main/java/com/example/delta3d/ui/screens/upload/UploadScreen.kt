package com.example.delta3d.ui.screens.upload

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.session.SessionViewModel

// --- 样式常量 (复用 AssetDetail 风格) ---
private val AccentColor = Color(0xFF64FFDA) // 青色高亮
private val GlassContainerColor = Color(0xFF1E1E1E).copy(alpha = 0.6f) // 半透明背景
private val TextWhite = Color.White
private val TextGray = Color.White.copy(alpha = 0.6f)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UploadScreen(
    videoUri: Uri,
    sessionVm: SessionViewModel,
    onBack: () -> Unit,
    onUploadSuccess: () -> Unit,
    viewModel: UploadViewModel = viewModel()
) {
    val token by sessionVm.token.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    val suggestedTags by viewModel.suggestedTags.collectAsState()

    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }

    // 标签系统
    var currentTagInput by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>() }

    LaunchedEffect(token) {
        if (token != null && token!!.isNotBlank()) {
            viewModel.fetchUserTags(token!!)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 全局动态背景
        AnimatedGradientBackground()

        Scaffold(
            containerColor = Color.Transparent, // 透明以显示背景
            topBar = {
                TopAppBar(
                    title = { Text("New Model", color = TextWhite, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextWhite)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 2. 视频文件卡片 (玻璃拟态)
                GlassCard {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(AccentColor.copy(0.1f), CircleShape)
                                .border(1.dp, AccentColor.copy(0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.VideoFile,
                                null,
                                tint = AccentColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Text(
                                "Selected Video",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite
                            )
                            Text(
                                "Ready to process",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                    }
                }

                // 3. 基础信息表单
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "BASIC INFO",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentColor,
                        letterSpacing = 1.sp
                    )

                    GlassyInput(
                        value = title,
                        onValueChange = { title = it },
                        label = "Model Title",
                        icon = Icons.Default.Title
                    )

                    GlassyInput(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description",
                        icon = Icons.Default.Description,
                        singleLine = false,
                        minLines = 1
                    )
                }

                // 4. 标签系统 (彩色风格)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "TAGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentColor,
                        letterSpacing = 1.sp
                    )

                    // 4.1 已选标签展示 (带删除功能的彩色胶囊)
                    if (tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 修改：使用 forEachIndexed 传入索引
                            tags.forEachIndexed { index, tag ->
                                DismissibleColorTag(
                                    text = tag,
                                    index = index,
                                    onDelete = { tags.remove(tag) }
                                )
                            }
                        }
                    }

                    // 4.2 标签输入 + 添加按钮
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.weight(1f)) {
                            GlassyInput(
                                value = currentTagInput,
                                onValueChange = { currentTagInput = it },
                                label = "Add new tag...",
                                icon = Icons.Default.Label
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = {
                                if (currentTagInput.isNotBlank()) {
                                    tags.add(currentTagInput.trim())
                                    currentTagInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color.White.copy(0.1f), CircleShape)
                                .border(1.dp, Color.White.copy(0.2f), CircleShape)
                        ) {
                            Icon(Icons.Default.Add, "Add Tag", tint = TextWhite)
                        }
                    }

                    // 4.3 推荐标签 (点击添加)
                    if (suggestedTags.isNotEmpty()) {
                        Text(
                            "Suggestions:",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestedTags
                                .filter { it !in tags }
                                .take(8)
                                // 修改：使用 forEachIndexed 传入索引
                                .forEachIndexed { index, tag ->
                                    SuggestionColorTag(
                                        text = tag,
                                        index = index,
                                        onClick = { tags.add(tag) }
                                    )
                                }
                        }
                    }
                }

                // 5. 备注 (Optional)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    GlassyInput(
                        value = remark,
                        onValueChange = { remark = it },
                        label = "Remarks (Optional)",
                        icon = Icons.Default.Description,
                        minLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 6. 提交按钮 (高亮风格)
                Button(
                    onClick = {
                        token?.let {
                            viewModel.uploadFile(
                                context,
                                videoUri,
                                it,
                                title,
                                description,
                                remark,
                                tags,
                                onUploadSuccess
                            )
                        }
                    },
                    enabled = title.isNotEmpty() && uploadState !is UploadState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentColor,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White.copy(0.1f),
                        disabledContentColor = Color.White.copy(0.3f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    if (uploadState is UploadState.Loading) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = Color.Black)
                        Spacer(Modifier.width(12.dp))
                        Text("Uploading...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("UPLOAD MODEL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                // 底部留白，防止被导航栏遮挡
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// -----------------------------------------------------------
// ✨ 自定义组件库 (美化核心)
// -----------------------------------------------------------

/**
 * 玻璃拟态输入框 (基于 AuthScreens 改造，支持多行)
 */
@Composable
fun GlassyInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextWhite.copy(alpha = 0.7f)) },
        leadingIcon = {
            Icon(icon, contentDescription = null, tint = TextWhite.copy(0.7f))
        },
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(20.dp), // 稍微圆一点，但不是完全胶囊，适合多行
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Black.copy(alpha = 0.3f), // 聚焦时深色半透
            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
            focusedBorderColor = AccentColor,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            cursorColor = AccentColor,
            focusedLabelColor = AccentColor
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * 玻璃卡片容器
 */
@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GlassContainerColor,
        border = BorderStroke(1.dp, Color.White.copy(0.1f)),
        content = content
    )
}

/**
 * 🎨 带删除功能的彩色标签 (已选状态)
 * 修改：增加 index 参数，用于顺序取色
 */
@Composable
fun DismissibleColorTag(text: String, index: Int, onDelete: () -> Unit) {
    val color = getColorByIndex(index) // 根据索引取色
    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.clickable { onDelete() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = color.copy(alpha = 1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = color.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 💡 推荐标签 (点击添加状态)
 * 修改：增加 index 参数，用于顺序取色
 */
@Composable
fun SuggestionColorTag(text: String, index: Int, onClick: () -> Unit) {
    val color = getColorByIndex(index) // 根据索引取色
    Surface(
        color = Color.Transparent, // 背景透明
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)), // 仅边框有颜色
        modifier = Modifier
            .clickable { onClick() }
            .clip(RoundedCornerShape(50))
    ) {
        Text(
            text = "+ $text",
            color = color.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/**
 * 🎲 简单的标签颜色生成器
 * 修改：根据索引 (index) 取色，确保排列时颜色循环且不重复
 */
fun getColorByIndex(index: Int): Color {
    val colors = listOf(
        Color(0xFF64FFDA), // 青
        Color(0xFFFF4081), // 粉
        Color(0xFFB388FF), // 紫
        Color(0xFFFFD740), // 黄
        Color(0xFF69F0AE), // 绿
        Color(0xFF40C4FF), // 蓝

        Color(0xFF7C4DFF), // 深紫（Royal Purple）
        Color(0xFF00BFA5), // 深青（Teal）
        Color(0xFFFF6D00), // 橙（Amber Orange）
        Color(0xFF1DE9B6), // 薄荷青（Mint）
        Color(0xFF536DFE), // 靛蓝（Indigo）
        Color(0xFFFF5252), // 珊瑚红（Coral Red）
        Color(0xFF26C6DA), // 青蓝（Cyan）
        Color(0xFFAED581), // 鼠尾草绿（Sage）
        Color(0xFFEF5350), // 柔红（Soft Red）
        Color(0xFF90CAF9)  // 雾蓝（Mist Blue）
    )
    val safeIndex = if (index < 0) 0 else index // 防止负数索引
    return colors[safeIndex % colors.size]
}