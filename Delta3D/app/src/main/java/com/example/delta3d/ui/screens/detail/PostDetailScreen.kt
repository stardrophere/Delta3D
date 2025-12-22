package com.example.delta3d.ui.screens.detail

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.delta3d.api.AssetDetail
import com.example.delta3d.api.CommentOut
import com.example.delta3d.api.PostDetail
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.screens.community.formatCount
import com.example.delta3d.ui.screens.community.formatToChinaTime
import com.example.delta3d.ui.screens.home.TagColorBinder
import com.example.delta3d.ui.screens.home.TagPalette
import com.example.delta3d.ui.session.SessionViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.delta3d.api.ChatConversation
import com.example.delta3d.config.AppConfig
import com.example.delta3d.ui.components.GlassyFeedbackPopup
import com.example.delta3d.ui.components.rememberFeedbackState
import com.example.delta3d.ui.screens.chat.ChatAvatar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- 样式常量 ---
private val AccentColor = Color(0xFF64FFDA)
private val GlassDockColor = Color(0xFF1E1E1E).copy(alpha = 0.90f)
private val GlassBorder = Brush.verticalGradient(
    colors = listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))
)

// 随机设备列表
private val DEVICE_MODELS = listOf(
    "iPhone 15 Pro Max", "Samsung Galaxy S24 Ultra", "Pixel 9 Pro",
    "Xiaomi 14 Ultra", "Sony Xperia 1 V", "Delta3D Workstation", "iPad Pro M4"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: Int,
    onBack: () -> Unit,
    sessionVm: SessionViewModel,
    onNavigateToPreview: (Int) -> Unit,
    viewModel: PostDetailViewModel = viewModel()
) {
    val currentUser by sessionVm.currentUser.collectAsState()
    val token by sessionVm.token.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    // 引入 Feedback 状态
    val feedbackState = rememberFeedbackState()

    // 随机生成一个设备名称
    val randomDevice = remember { DEVICE_MODELS.random() }

    LaunchedEffect(postId, token) {
        token?.let { viewModel.loadPostDetail(it, postId) }
    }

    var showDownloadDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    val tagColorBinder = remember { TagColorBinder(TagPalette) }

    //联系人
    var showShareSheet by remember { mutableStateOf(false) }
    val recentContacts = remember { mutableStateOf<List<ChatConversation>>(emptyList()) }


    // 获取最近联系人逻辑
    LaunchedEffect(showShareSheet) {
        if (showShareSheet && token != null) {
            val authHeader = if (token!!.startsWith("Bearer ")) token!! else "Bearer $token"
            try {
                withContext(Dispatchers.IO) {
                    val list = RetrofitClient.api.getConversations(authHeader)
                    recentContacts.value = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    LaunchedEffect(Unit) {
        viewModel.downloadEvent.collect { event ->
            when (event) {
                is DownloadEvent.Success -> {
                    // 调用系统下载
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
        AnimatedGradientBackground()

        when (val state = uiState) {
            is PostUiState.Loading -> {
                CircularProgressIndicator(
                    color = AccentColor,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            is PostUiState.Error -> {
                ErrorView(
                    msg = state.msg,
                    onRetry = { token?.let { viewModel.loadPostDetail(it, postId) } })
            }

            is PostUiState.Success -> {
                val post = state.data
                val asset = post.asset

                // 主内容滚动区
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 顶部视频/图片轮播
                    item {
                        Box {
                            ImageCarouselHeader(
                                videoUrl = asset.videoUrl,
                                status = asset.status,
                                modifier = Modifier.height(300.dp),
                            )

                            // 返回按钮
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .padding(top = 45.dp, start = 16.dp)
                                    .size(40.dp)
                                    .background(Color.Black.copy(0.4f), CircleShape)
                                    .border(1.dp, Color.White.copy(0.2f), CircleShape)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // 作者信息栏
                    item {
                        AuthorInfoRow(
                            ownerName = post.ownerName,
                            ownerAvatar = post.ownerAvatar,
                            postTime = post.publishedAt,
                            isFollowing = post.isFollowing,
                            isSelf = post.ownerId == currentUser?.id,
                            onFollowClick = { token?.let { viewModel.toggleFollow(it) } }
                        )
                    }

                    // 帖子内容与模型信息
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            // 标题
                            Text(
                                text = asset.title,
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 32.sp
                            )
                            Spacer(Modifier.height(16.dp))

                            // 帖子正文 (User Content)
                            if (!post.content.isNullOrBlank()) {
                                Text(
                                    text = post.content,
                                    color = Color.White.copy(0.9f),
                                    fontSize = 15.sp,
                                    lineHeight = 24.sp
                                )
                                Spacer(Modifier.height(20.dp))
                            }

                            // 模型描述
                            if (!asset.description.isNullOrBlank() && asset.description != post.content) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(0.08f))
                                        .border(
                                            width = 1.dp,
                                            color = Color.White.copy(0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Outlined.Info,
                                            null,
                                            tint = AccentColor.copy(0.8f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "About Model",
                                            color = AccentColor.copy(0.8f),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = asset.description,
                                        color = Color.White.copy(0.6f),
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                                Spacer(Modifier.height(16.dp))
                            }

                            // 标签
                            if (asset.tags.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    asset.tags.take(4).forEach { tag ->
                                        TagCapsuleHere(
                                            text = tag,
                                            baseColor = tagColorBinder.colorFor(tag)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            // 随机设备显示
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Smartphone,
                                    contentDescription = null,
                                    tint = Color.White.copy(0.3f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Posted from $randomDevice",
                                    color = Color.White.copy(0.3f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    // 互数据栏 (Like, Collect, Views)
                    item {
                        InteractionStatsBar(
                            post = post,
                            onLike = { token?.let { viewModel.toggleLike(it) } },
                            onCollect = { token?.let { viewModel.toggleCollect(it) } }
                        )
                        Spacer(Modifier.height(24.dp))
                    }

                    // 技术参数
                    item {

                        val tempDetail = AssetDetail(
                            id = asset.id,
                            title = asset.title,
                            description = asset.description,
                            remark = null,
                            tags = asset.tags,
                            videoUrl = asset.videoUrl,
                            modelUrl = asset.modelUrl,
                            status = asset.status,
                            createdAt = "",
                            estimatedGenSeconds = asset.estimatedGenSeconds
                        )
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            TechSpecsCard(detail = tempDetail)
                            Spacer(Modifier.height(24.dp))
                        }
                    }

                    // 评论区
                    item {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            HorizontalDivider(color = Color.White.copy(0.1f))
                            Spacer(Modifier.height(20.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "Comments",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "(${post.comments.size})",
                                    color = Color.White.copy(0.5f),
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            // 评论输入框
                            CommentInputArea(
                                currentUserAvatar = currentUser?.avatarUrl,
                                onSendComment = { text ->
                                    token?.let { userToken ->
                                        viewModel.sendComment(userToken, text)
                                    }
                                }
                            )
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    // 评论列表
                    if (post.comments.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No comments yet.\nBe the first to share your thoughts!",
                                    color = Color.White.copy(0.3f),
                                    fontSize = 14.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(post.comments) { comment ->
                            CommentItem(comment)
                        }
                    }
                }

                // 底部操作栏
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 30.dp)
                ) {
                    PostBottomDock(
                        allowDownload = post.allowDownload,
                        onPreview = { onNavigateToPreview(asset.id) },
                        onDownload = {
                            if (post.allowDownload) showDownloadDialog = true
                            else {
                                scope.launch { feedbackState.showError("The author has disabled downloading.") }
                            }
                        },
                        onShare = { showShareSheet = true }
                    )
                }

                // 下载弹窗
                if (showDownloadDialog) {
                    DownloadFormatDialog(
                        title = asset.title,
                        onDismiss = { showDownloadDialog = false },
                        onDownload = { format ->
                            showDownloadDialog = false
                            token?.let {
                                viewModel.downloadAsset(it, asset.id, format)
                            }
                        }
                    )
                }

                //分享弹窗
                if (showShareSheet) {
                    ModalBottomSheet(
                        onDismissRequest = { showShareSheet = false },
                        sheetState = sheetState,
                        containerColor = Color(0xFF1E1E1E),
                        contentColor = Color.White
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 32.dp)
                        ) {
                            Text(
                                "Share to...",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = FontWeight.Bold
                            )

                            if (recentContacts.value.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = AccentColor)
                                }
                            } else {
                                LazyColumn {
                                    items(recentContacts.value) { contact ->
                                        ShareContactItem(
                                            contact = contact,
                                            onClick = {
                                                // 1. 调用 ViewModel 发送
                                                token?.let {
                                                    viewModel.sharePostToUser(it, contact.userId)
                                                }

                                                // 2. 反馈提示 (使用你现有的 feedbackState)
                                                scope.launch {
                                                    feedbackState.showSuccess("Sent to ${contact.username}")
                                                    sheetState.hide()
                                                    showShareSheet = false
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        GlassyFeedbackPopup(
            state = feedbackState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

// ---------------- UI 组件 ----------------

// 互栏，包含动画按钮
@Composable
fun InteractionStatsBar(
    post: PostDetail,
    onLike: () -> Unit,
    onCollect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(72.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(0.15f),
                        Color.White.copy(0.05f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .background(Color(0xFF1A1A1A).copy(alpha = 0.75f)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Views (Static) ---
        Column(
            modifier = Modifier.fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Visibility,
                null,
                tint = Color.White.copy(0.5f),
                modifier = Modifier.size(24.dp)
            )
//            Spacer(Modifier.height(1.dp))
            Text(
                formatCount(post.viewCount),
                color = Color.White.copy(0.5f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        VerticalDivider(
            Modifier
                .height(32.dp)
                .width(1.dp),
            color = Color.White.copy(0.1f)
        )


        AnimatedInteractionButton(
            isActive = post.isLiked,
            count = post.likeCount,
            activeIcon = Icons.Rounded.Favorite,
            inactiveIcon = Icons.Rounded.FavoriteBorder,
            activeColor = Color(0xFFFF4081),
            onClick = onLike
        )

        VerticalDivider(
            Modifier
                .height(32.dp)
                .width(1.dp),
            color = Color.White.copy(0.1f)
        )

        // --- Collect (Animated) ---
        AnimatedInteractionButton(
            isActive = post.isCollected,
            count = post.collectCount,
            activeIcon = Icons.Filled.Star,
            inactiveIcon = Icons.Outlined.StarBorder,
            activeColor = Color(0xFFFFC107),
            onClick = onCollect
        )
    }
}

// \用的带弹簧动画的按钮组件
@Composable
fun AnimatedInteractionButton(
    isActive: Boolean,
    count: Int,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    activeColor: Color,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.2f else 1.0f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
        label = "scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isActive) activeColor else Color.White.copy(0.8f),
        label = "color"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Icon(
            imageVector = if (isActive) activeIcon else inactiveIcon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier
                .size(24.dp)
                .scale(if (isActive) scale else 1f)
        )
//        Spacer(Modifier.height(2.dp))
        Text(
            text = formatCount(count),
            color = if (isActive) activeColor else Color.White.copy(0.8f),
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}


@Composable
fun CommentInputArea(
    currentUserAvatar: String?,
    onSendComment: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val fullAvatarUrl = remember(currentUserAvatar) {
        currentUserAvatar?.let { url ->
            if (url.startsWith("http")) url
            else "${AppConfig.currentBaseUrl.removeSuffix("/")}/${url.removePrefix("/")}"
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- 当前用户头像 ---
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White.copy(0.1f))
                .border(1.dp, Color.White.copy(0.1f), CircleShape)
        ) {
            if (!fullAvatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = fullAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White.copy(0.5f),
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // --- 输入框容器 ---
        Row(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(0.08f))
                .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(24.dp))
                .padding(horizontal = 16.dp), // 容器控制左右间距
            verticalAlignment = Alignment.CenterVertically
        ) {
            // BasicTextField
            androidx.compose.foundation.text.BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    color = Color.White, // 显式设置文字颜色
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(AccentColor),

                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text(
                                "Add a comment...",
                                color = Color.White.copy(0.4f),
                                fontSize = 14.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )


            val canSend = text.isNotBlank()
            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (canSend) AccentColor else Color.Transparent)
                    .clickable(enabled = canSend) {
                        onSendComment(text)
                        text = ""
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.Black else Color.White.copy(0.2f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// UI 组件

@Composable
fun AuthorInfoRow(
    ownerName: String,
    ownerAvatar: String?,
    postTime: String,
    isFollowing: Boolean,
    isSelf: Boolean,
    onFollowClick: () -> Unit
) {

    val fullAvatarUrl = remember(ownerAvatar) {
        ownerAvatar?.let { url ->
            if (url.startsWith("http")) url
            else "${AppConfig.currentBaseUrl.removeSuffix("/")}/${url.removePrefix("/")}"
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(0.3f))
        ) {
            if (!fullAvatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = fullAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = ownerName.firstOrNull()?.toString() ?: "?",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(ownerName, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                formatToChinaTime(postTime),
                color = Color.White.copy(0.5f),
                fontSize = 12.sp
            )
        }

        if (!isSelf) {
            Button(
                onClick = onFollowClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isFollowing) Color.White.copy(0.1f) else AccentColor
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = if (isFollowing) "Following" else "+ Follow",
                    color = if (isFollowing) Color.White.copy(0.7f) else Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
fun CommentItem(comment: CommentOut) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        val fullAvatarUrl = remember(comment.avatarUrl) {
            comment.avatarUrl?.let { url ->
                if (url.startsWith("http")) url
                else "${AppConfig.currentBaseUrl.removeSuffix("/")}/${url.removePrefix("/")}"
            }
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color.Gray.copy(0.3f))
        ) {
            if (!fullAvatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = fullAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = comment.username.firstOrNull()?.toString() ?: "?",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    comment.username,
                    color = Color.White.copy(0.9f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Spacer(Modifier.width(8.dp))

                Text(
                    formatToChinaTime(comment.createdAt),
                    color = Color.White.copy(0.4f),
                    fontSize = 11.sp
                )
            }
//            Spacer(Modifier.height(2.dp))
            Text(
                comment.content,
                color = Color.White.copy(0.7f),
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
fun PostBottomDock(
    allowDownload: Boolean,
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
        IconButton(
            onClick = onDownload,
            enabled = allowDownload,
            modifier = Modifier.size(64.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.CloudDownload,
                    null,
                    tint = if (allowDownload) Color.White.copy(0.8f) else Color.White.copy(0.2f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    if (allowDownload) "Save" else "Locked",
                    color = if (allowDownload) Color.White.copy(0.6f) else Color.White.copy(0.2f),
                    fontSize = 10.sp
                )
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
fun ErrorView(msg: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            null,
            tint = Color(0xFFFF5252),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(msg, color = Color.White)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

// 联系人列表项
@Composable
fun ShareContactItem(contact: ChatConversation, onClick: () -> Unit) {
    // 处理头像
    val finalAvatarUrl = remember(contact.avatarUrl) {
        contact.avatarUrl?.let { url ->
            if (url.startsWith("http")) url
            else "${AppConfig.currentBaseUrl.removeSuffix("/")}/${url.removePrefix("/")}"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChatAvatar(url = finalAvatarUrl)

        Spacer(Modifier.width(12.dp))
        Text(contact.username, color = Color.White, fontSize = 16.sp)
        Spacer(Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.Send, null, tint = AccentColor)
    }
}