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
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.material.icons.filled.Warning

// --- 样式常量
private val AccentColor = Color(0xFF64FFDA) // 青色高亮
private val GlassContainerColor = Color(0xFF1E1E1E).copy(alpha = 0.6f) // 半透明背景
private val TextWhite = Color.White
private val TextGray = Color.White.copy(alpha = 0.6f)
private val ErrorColor = Color(0xFFFF5252)

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

    // 订阅文件信息
    val fileSize by viewModel.fileSizeStr.collectAsState()
    val estimatedTime by viewModel.estimatedTimeStr.collectAsState()

    // 订阅资源错误信息
    val resourceError by viewModel.resourceError.collectAsState()

    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var currentTagInput by remember { mutableStateOf("") }
    val tags = remember { mutableStateListOf<String>() }

    // 获取标签
    LaunchedEffect(token) {
        if (token != null && token!!.isNotBlank()) {
            viewModel.fetchUserTags(token!!)
        }
    }
    // 计算文件大小和时间
    LaunchedEffect(videoUri) {
        viewModel.calculateFileInfo(context, videoUri)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground()

        Scaffold(
            containerColor = Color.Transparent,
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
                //频信息卡片
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
                            Spacer(modifier = Modifier.height(4.dp))
                            // 显示文件大小
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = fileSize,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (resourceError != null && fileSize.contains("Limit")) ErrorColor else TextGray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("•", color = TextGray)
                                Spacer(modifier = Modifier.width(8.dp))
                                // 显示预估时间
                                Icon(
                                    Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = if (resourceError != null && estimatedTime.contains("Limit")) ErrorColor else AccentColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "~$estimatedTime",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (resourceError != null && estimatedTime.contains("Limit")) ErrorColor else AccentColor
                                )
                            }
                        }
                    }
                }

                // 错误提示卡片
                if (resourceError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ErrorColor.copy(0.1f), RoundedCornerShape(16.dp))
                            .border(1.dp, ErrorColor.copy(0.3f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = ErrorColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = resourceError ?: "",
                                color = ErrorColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                //基础信息表单
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

                // 标签系统
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "TAGS",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentColor,
                        letterSpacing = 1.sp
                    )

                    if (tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tags.forEachIndexed { index, tag ->
                                DismissibleColorTag(
                                    text = tag,
                                    index = index,
                                    onDelete = { tags.remove(tag) }
                                )
                            }
                        }
                    }

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

                //备注
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

                //提交
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
                    //如果 resourceError 不为空，禁用按钮
                    enabled = title.isNotEmpty() && uploadState !is UploadState.Loading && resourceError == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentColor,
                        contentColor = Color.Black,
                        disabledContainerColor = Color.White.copy(0.05f),
                        disabledContentColor = Color.White.copy(0.2f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    if (uploadState is UploadState.Loading) {
                        CircularProgressIndicator(Modifier.size(24.dp), color = Color.Black)
                        Spacer(Modifier.width(12.dp))
                        Text("Uploading...", fontWeight = FontWeight.Bold)
                    } else {
                        // 如果有错误，显示 Unable，否则显示 UPLOAD
                        Text(
                            if (resourceError != null) "UNABLE TO UPLOAD" else "UPLOAD MODEL",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// -----------------------------------------------------------
// 辅助组件
// -----------------------------------------------------------

@Composable
fun SpecRow(label: String, value: String, valueColor: Color = Color.White.copy(0.7f)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(0.5f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

// -----------------------------------------------------------
// 自定义组件库
// -----------------------------------------------------------

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
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
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

@Composable
fun DismissibleColorTag(text: String, index: Int, onDelete: () -> Unit) {
    val color = getColorByIndex(index)
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

@Composable
fun SuggestionColorTag(text: String, index: Int, onClick: () -> Unit) {
    val color = getColorByIndex(index)
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
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

fun getColorByIndex(index: Int): Color {
    val colors = listOf(
        Color(0xFF64FFDA), Color(0xFFFF4081), Color(0xFFB388FF), Color(0xFFFFD740),
        Color(0xFF69F0AE), Color(0xFF40C4FF), Color(0xFF7C4DFF), Color(0xFF00BFA5),
        Color(0xFFFF6D00), Color(0xFF1DE9B6), Color(0xFF536DFE), Color(0xFFFF5252),
        Color(0xFF26C6DA), Color(0xFFAED581), Color(0xFFEF5350), Color(0xFF90CAF9)
    )
    val safeIndex = if (index < 0) 0 else index
    return colors[safeIndex % colors.size]
}