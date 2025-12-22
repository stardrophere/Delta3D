package com.example.delta3d.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.delta3d.api.ChatMessage
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.manager.ChatSocketManager
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.session.SessionViewModel
import com.example.delta3d.utils.ShareLinkUtils
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// 样式常量
private val AccentColor = Color(0xFF64FFDA)
private val MyBubbleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF64FFDA), Color(0xFF00B8D4))
)
private val OtherBubbleColor = Color(0xFF2A2A2A).copy(alpha = 0.6f)
private val GlassBorder = Brush.verticalGradient(
    colors = listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))
)


@Composable
fun ChatScreen(
    targetUserId: Int,
    targetUserName: String = "Chat",
    sessionVm: SessionViewModel,
    onBack: () -> Unit,
    onNavigateToPost: (Int) -> Unit
) {
    val user by sessionVm.currentUser.collectAsState()
    val token by sessionVm.token.collectAsState()

    val viewModel: ChatViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(
                    socketManager = ChatSocketManager,
                    myUserId = user?.id ?: 0,
                    targetUserId = targetUserId,
                    token = token ?: ""
                ) as T
            }
        }
    )

    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isLoadingHistory by viewModel.isLoadingHistory.collectAsState()

    val myAvatar by viewModel.myAvatarUrl.collectAsState()
    val targetAvatar by viewModel.targetAvatarUrl.collectAsState()

    // 数据反转
    val uniqueMessages = remember(messages) { messages.distinctBy { it.id } }
    val reversedMessages = remember(uniqueMessages) { uniqueMessages.reversed() }

    val listState = rememberLazyListState()

    // 自动滚动逻辑
    LaunchedEffect(reversedMessages.firstOrNull()?.id) {
        if (reversedMessages.isNotEmpty()) {
            val isAtBottom = listState.firstVisibleItemIndex < 2
            val lastMsg = reversedMessages.first()
            val isMe = lastMsg.isMe(user?.id ?: 0)

            // 滚动显示最新消息
            if (isMe || isAtBottom) {
                listState.animateScrollToItem(0)
            }
        }
    }

    // 加载更多历史记录逻辑
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            (lastVisibleItemIndex >= totalItems - 3) to (totalItems > 0)
        }
            .collect { (shouldLoad, hasItems) ->
                if (shouldLoad && hasItems) {
                    viewModel.loadMoreHistory()
                }
            }
    }

    // 进入页面时标记已读
    LaunchedEffect(Unit) {
        token?.let { rawToken ->
            try {
                val authHeader =
                    if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"
                RetrofitClient.api.markAsRead(authHeader, targetUserId)
                sessionVm.refreshUnreadCount()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground()

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0.dp),

            bottomBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .imePadding()
                ) {
                    GlassChatInputBar(
                        value = inputText,
                        onValueChange = viewModel::onInputChange,
                        onSend = viewModel::sendMessage
                    )
                }
            }
        ) { innerPadding ->

            // 计算顶部避让距离
            val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

            LazyColumn(
                state = listState,
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = statusBarHeight + 64.dp + 20.dp,
                    bottom = 100.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 消息列表
                items(
                    items = reversedMessages,
                    key = { it.id }
                ) { msg ->
                    val isMe = msg.isMe(user?.id ?: 0)
                    MessageBubble(
                        msg = msg,
                        isMe = isMe,
                        avatarUrl = if (isMe) myAvatar else targetAvatar,
                        onPostClick = { postId ->
                            onNavigateToPost(postId)
                        }
                    )
                }

                // 加载指示器
                if (isLoadingHistory) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AccentColor.copy(alpha = 0.7f),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }


        GlassChatHeader(
            title = targetUserName,
            onBack = onBack,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun MessageBubble(msg: ChatMessage, isMe: Boolean, avatarUrl: String?, onPostClick: (Int) -> Unit) {
    // 处理时间显示
    val timeString = remember(msg.createdAt) {
        formatChatDetailTime(msg.createdAt)
    }

    //拼接 URL 逻辑
    val finalAvatarUrl = remember(avatarUrl) {
        avatarUrl?.let { url ->
            if (url.startsWith("http")) url
            else {
                val base = RetrofitClient.BASE_URL.removeSuffix("/")
                val path = url.removePrefix("/")
                "$base/$path"
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isMe) {
            ChatAvatar(url = finalAvatarUrl)
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (ShareLinkUtils.isPostShareLink(msg.getSafeContent())) {
                // 解析数据
                val shareData =
                    remember(msg.content) { ShareLinkUtils.parsePostLink(msg.getSafeContent()) }

                if (shareData != null) {
                    PostShareCard(
                        data = shareData,
                        isMe = isMe,
                        onClick = { onPostClick(shareData.postId) }
                    )
                } else {
                    // 解析失败兜底显示文本
                    BubbleContent(msg.getSafeContent(), isMe)
                }
            } else {
                // 普通文本
                BubbleContent(msg.getSafeContent(), isMe)
            }

            // 时间显示
            if (timeString.isNotEmpty()) {
                Text(
                    text = timeString,
                    color = Color.White.copy(alpha = 0.3f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp, end = 4.dp)
                )
            }
        }

        if (isMe) {
            Spacer(Modifier.width(8.dp))
            ChatAvatar(url = finalAvatarUrl)
        }
    }
}

@Composable
fun ChatAvatar(url: String?) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // 默认头像占位符
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Gray.copy(0.3f))
            )
        }
    }
}

//文本气泡样式
@Composable
fun BubbleContent(text: String, isMe: Boolean) {
    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(
                    topStart = 20.dp, topEnd = 20.dp,
                    bottomStart = if (isMe) 20.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 20.dp
                )
            )
            .background(if (isMe) MyBubbleGradient else SolidColor(OtherBubbleColor))
            .then(
                if (!isMe) Modifier.border(0.5.dp, GlassBorder, RoundedCornerShape(20.dp))
                else Modifier
            )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            color = if (isMe) Color.Black else Color.White.copy(0.9f),
            style = TextStyle(fontSize = 15.sp, lineHeight = 22.sp)
        )
    }
}


//卡片组件
@Composable
fun PostShareCard(
    data: com.example.delta3d.utils.PostShareData,
    isMe: Boolean,
    onClick: () -> Unit
) {
    val fullCoverUrl = remember(data.coverUrl) {
        data.coverUrl?.let { url ->
            if (url.startsWith("http")) url
            else {
                val base = RetrofitClient.BASE_URL.removeSuffix("/")
                val path = url.removePrefix("/")
                "$base/$path/images/0001.jpg"
            }
        }
    }

    Column(
        modifier = Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF252525))
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color.White.copy(0.2f), Color.Transparent)),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // 封面图区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color.Black)
        ) {
            if (!fullCoverUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = fullCoverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 右上角标签
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "3D MODEL",
                    color = Color(0xFF64FFDA), // AccentColor
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 底部信息区域
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = data.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    tint = Color.White.copy(0.5f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Click to view details",
                    color = Color.White.copy(0.5f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun GlassChatHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF121212).copy(alpha = 0.8f))
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White.copy(0.1f), Color.Transparent)
                ),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
    ) {

        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                )
            }

        }
    }
}

@Composable
fun GlassChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.Transparent)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF1E1E1E).copy(alpha = 0.8f))
                .border(1.dp, GlassBorder, RoundedCornerShape(28.dp))
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 输入框
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        "Type a message...",
                        color = Color.White.copy(0.3f),
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 15.sp
                    ),
                    cursorBrush = SolidColor(AccentColor),
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 发送按钮
            val canSend = value.isNotBlank()

            Spacer(Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) MyBubbleGradient
                        else SolidColor(Color.White.copy(0.1f))
                    )
                    .clickable(enabled = canSend, onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send",
                    tint = if (canSend) Color.Black else Color.White.copy(0.2f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// 辅助函数：解析时间并转为东八区 HH:mm
fun formatChatDetailTime(timeStr: String): String {
    if (timeStr.isBlank()) return ""
    return try {
        val zoneShanghai = ZoneId.of("Asia/Shanghai")


        LocalDateTime.parse(timeStr)
            .atZone(ZoneId.of("UTC"))
            .withZoneSameInstant(zoneShanghai)
            .format(DateTimeFormatter.ofPattern("HH:mm"))

    } catch (e: Exception) {
        try {
            ZonedDateTime.parse(timeStr)
                .withZoneSameInstant(ZoneId.of("Asia/Shanghai"))
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        } catch (e2: Exception) {
            ""
        }
    }
}