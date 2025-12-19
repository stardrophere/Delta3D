package com.example.delta3d.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.delta3d.api.ChatConversation
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.session.SessionViewModel
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// --- 样式常量 ---
private val ListCardShape = RoundedCornerShape(16.dp)
private val ListCardBackground = Color(0xFF1E1E1E).copy(alpha = 0.6f) // 稍微通透一点
private val GlassBorder = Brush.verticalGradient(
    colors = listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))
)

@Composable
fun ChatListScreen(
    sessionVm: SessionViewModel,
    onBack: () -> Unit,
    onNavigateToChat: (Int, String) -> Unit
) {
    val token by sessionVm.token.collectAsState()
    val viewModel: ChatListViewModel = viewModel()
    val conversations by viewModel.conversations.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 每次进入页面刷新数据
    LaunchedEffect(token) {
        token?.let { viewModel.loadConversations(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {

            GlassChatHeader(title = "Messages", onBack = onBack)

            if (isLoading && conversations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF64FFDA))
                }
            } else if (conversations.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No messages yet.",
                        color = Color.White.copy(0.5f),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(conversations, key = { it.userId }) { chat ->
                        ConversationItem(
                            chat = chat,
                            onClick = {
                                onNavigateToChat(chat.userId, chat.username)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConversationItem(
    chat: ChatConversation,
    onClick: () -> Unit
) {
    val avatarUrl = remember(chat.avatarUrl) {
        chat.avatarUrl?.let { url ->
            if (url.startsWith("http")) url else "${RetrofitClient.BASE_URL.removeSuffix("/")}/${
                url.removePrefix("/")
            }"
        }
    }

    val formattedTime = remember(chat.lastMessageTime) {
        formatMessageTime(chat.lastMessageTime)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(ListCardShape)
            .background(ListCardBackground)
            .border(1.dp, GlassBorder, ListCardShape)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 头像
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.1f))
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = chat.username.take(1).uppercase(),
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 右侧内容区域
            Column(modifier = Modifier.weight(1f)) {

                // 第一行：用户名 + 时间
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = chat.username,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 第二行：消息内容 + 未读红点
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = chat.lastMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (chat.unreadCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))

                        // --- 修复红点居中问题 ---
                        Box(
                            modifier = Modifier
                                .height(18.dp)
                                .defaultMinSize(minWidth = 18.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color(0xFFFF5252)),
                            contentAlignment = Alignment.Center // 确保 Box 内容居中
                        ) {
                            Text(
                                text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                                color = Color.White,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 10.sp,
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                ),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 格式化时间
fun formatMessageTime(timeStr: String): String {
    if (timeStr.isBlank()) return ""
    return try {
        val zoneShanghai = ZoneId.of("Asia/Shanghai")
        val nowShanghai = ZonedDateTime.now(zoneShanghai)


        val targetTime = try {
            // 尝试作为带时区的时间解析
            ZonedDateTime.parse(timeStr).withZoneSameInstant(zoneShanghai)
        } catch (e: Exception) {

            LocalDateTime.parse(timeStr).atZone(ZoneId.of("UTC")).withZoneSameInstant(zoneShanghai)
        }

        val daysDiff = ChronoUnit.DAYS.between(targetTime.toLocalDate(), nowShanghai.toLocalDate())

        when {
            daysDiff == 0L -> targetTime.format(DateTimeFormatter.ofPattern("HH:mm"))
            daysDiff == 1L -> "Yesterday"
            daysDiff < 7L -> targetTime.format(DateTimeFormatter.ofPattern("EEEE")) // Tuesday
            else -> targetTime.format(DateTimeFormatter.ofPattern("MM-dd"))
        }
    } catch (e: Exception) {
        timeStr
    }
}