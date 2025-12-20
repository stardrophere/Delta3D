package com.example.delta3d.ui.screens.community

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Visibility
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.delta3d.api.PostCard
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.screens.home.EditableGlassySearchBar
import com.example.delta3d.ui.screens.home.TagColorBinder
import com.example.delta3d.ui.screens.home.TagPalette
import com.example.delta3d.ui.session.SessionViewModel
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- 样式常量 ---
private val FeedCardShape = RoundedCornerShape(16.dp)
private val FeedCardBackground = Color(0xFF1E1E1E).copy(alpha = 0.85f)
private val GlassBorder = Brush.verticalGradient(
    colors = listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    sessionVm: SessionViewModel,
    innerPadding: PaddingValues,
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToPublish: () -> Unit,
    onNavigateToChat: () -> Unit = {},
    onNavigateToDirectChat: (Int, String) -> Unit,
    viewModel: CommunityViewModel = viewModel()
) {
    val token by sessionVm.token.collectAsState()
    val posts by viewModel.displayPosts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isFollowingMode by viewModel.onlyShowFollowing.collectAsState()

    val tagColorBinder = remember { TagColorBinder(TagPalette) }
    var toastMessage by remember { mutableStateOf<String?>(null) }
    val currentUser by sessionVm.currentUser.collectAsState()
    val myUserId = currentUser?.id ?: -1
    val unreadCount by sessionVm.totalUnreadCount.collectAsState()
    sessionVm.refreshUnreadCount()


    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            toastMessage = message
            delay(2000)
            toastMessage = null
        }
    }

    LaunchedEffect(token) {
        token?.let { viewModel.loadPosts(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground()

        Column(modifier = Modifier.padding(top = innerPadding.calculateTopPadding())) {


            // Explore/Following
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧 Tabs
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HeaderSwitchTab(
                        title = "Explore",
                        isActive = !isFollowingMode,
                        onClick = { viewModel.setFilterMode(false) }
                    )

                    Spacer(Modifier.width(20.dp))

                    HeaderSwitchTab(
                        title = "Following",
                        isActive = isFollowingMode,
                        onClick = { viewModel.setFilterMode(true) }
                    )
                }

                // 聊天按钮
                GlassyIconBtn(
                    icon = Icons.Rounded.ChatBubbleOutline,
                    unreadCount = unreadCount,
                    onClick = onNavigateToChat
                )
            }

            //搜索栏
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
            ) {
                EditableGlassySearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchInput(it) },
                    placeholder = "Search posts, tags or users...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
            // =====================================================================

            // --- 列表内容 ---
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { token?.let { viewModel.loadPosts(it) } },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (posts.isEmpty() && searchQuery.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No posts found.",
                                    color = Color.White.copy(0.5f)
                                )
                            }
                        }
                    }

                    items(posts, key = { it.postId }) { post ->
                        CommunityFeedItem(
                            post = post,
                            currentUserId = myUserId,
                            tagColorBinder = tagColorBinder,
                            onClick = { onNavigateToDetail(post.postId) },
                            onLike = { token?.let { viewModel.toggleLike(post.postId, it) } },
                            onCollect = { token?.let { viewModel.toggleCollect(post.postId, it) } },
                            onFollow = { token?.let { viewModel.toggleFollow(post.ownerId, it) } },
                            onAvatarClick = {
                                if (post.ownerId != myUserId) {
                                    onNavigateToDirectChat(post.ownerId, post.ownerName)
                                } else {
                                    viewModel.showToast("Cannot chat with yourself.")
                                }
                            }
                        )
                    }
                }
            }
        }

        // FAB
        val BubbleGradient = Brush.linearGradient(
            colors = listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF))
        )

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
                        onClick = onNavigateToPublish
                    ),
                fill = BubbleGradient,
                icon = Icons.Rounded.Add,
                iconSize = 44.dp
            )
        }

        GlassToast(
            message = toastMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 60.dp)
        )
    }
}


@Composable
fun GlassyIconBtn(
    icon: ImageVector,
    unreadCount: Int = 0, // 新增参数
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.2f))
            .border(
                width = 0.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(0.3f),
                        Color.White.copy(0.05f)
                    )
                ),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // 图标
        Icon(
            imageVector = icon,
            contentDescription = "Chat",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )

        //红点逻辑
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF5252))
                    .border(1.dp, Color(0x80000000), CircleShape) // 加个深色描边增加对比度
            )
        }
    }
}

// --- 头部切换标签 ---
@Composable
fun HeaderSwitchTab(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.9f,
        label = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.5f,
        label = "alpha"
    )
    val color = if (isActive) Color.White else Color.White.copy(alpha = 0.7f)
    val indicatorColor = Color(0xFF64FFDA)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .alpha(alpha)
    ) {
        Text(
            text = title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            style = if (isActive) MaterialTheme.typography.titleLarge.copy(
                shadow = androidx.compose.ui.graphics.Shadow(
                    color = indicatorColor.copy(alpha = 0.3f),
                    blurRadius = 15f
                )
            ) else MaterialTheme.typography.titleLarge
        )

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .height(8.dp),
                contentAlignment = Alignment.Center
            ) {
                val dotAlpha by animateFloatAsState(if (isActive) 1f else 0f, label = "dotAlpha")
                val dotScale by animateFloatAsState(if (isActive) 1f else 0.6f, label = "dotScale")

                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .scale(dotScale)
                        .alpha(dotAlpha)
                        .clip(CircleShape)
                        .background(indicatorColor)
                        .shadow(8.dp, spotColor = indicatorColor)
                )
            }
        }
    }
}

// --- Feed 列表项 ---
@Composable
fun CommunityFeedItem(
    post: PostCard,
    currentUserId: Int,
    tagColorBinder: TagColorBinder,
    onClick: () -> Unit,
    onLike: () -> Unit,
    onCollect: () -> Unit,
    onFollow: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val imageUrl = remember(post.coverUrl) {
        post.coverUrl?.let { url ->
            val base = RetrofitClient.BASE_URL.removeSuffix("/")
            val path = url.removePrefix("/").removeSuffix("/")
            "$base/$path/images/0001.jpg"
        }
    }
    val avatarUrl = remember(post.ownerAvatar) {
        post.ownerAvatar?.let { url ->
            if (url.startsWith("http")) {
                url
            } else {
                val base = RetrofitClient.BASE_URL.removeSuffix("/")
                val path = url.removePrefix("/")
                "$base/$path"
            }
        }
    }
    val formattedDate = remember(post.publishedAt) {
        formatToChinaTime(post.publishedAt)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(FeedCardShape)
            .border(1.dp, GlassBorder, FeedCardShape)
            .background(FeedCardBackground)
            .clickable(onClick = onClick)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(0.3f))
                        // 关键修改：单独响应点击，拦截事件
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null, // 头像点击一般不需要波纹，或者你可以去掉这行保留波纹
                            onClick = onAvatarClick
                        )
                ) {
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            post.ownerName.firstOrNull()?.toString() ?: "?",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        post.ownerName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Text(
                        formattedDate,
                        color = Color.White.copy(0.5f),
                        fontSize = 11.sp
                    )
                }

                if (post.ownerId != currentUserId) {
                    FollowButton(post.isFollowing, onFollow)
                } else {
                    YourselfBadge()
                }
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 12.dp)
                    .height(IntrinsicSize.Min)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(0.3f))
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp)
                ) {
                    Text(
                        post.title,
                        color = Color.White.copy(0.95f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        post.description ?: "",
                        color = Color.White.copy(0.6f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.weight(1f))
                    if (post.tags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(post.tags) { tag ->
                                TagCapsule(
                                    text = tag,
                                    baseColor = tagColorBinder.colorFor(tag)
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .align(Alignment.CenterVertically)
                ) {
                    CollectButton(
                        isCollected = post.isCollected,
                        onClick = onCollect
                    )
                }
            }
            PostBottomBar(
                viewCount = post.viewCount,
                commentCount = post.commentCount,
                likeCount = post.likeCount,
                isLiked = post.isLiked,
                onLikeClick = onLike
            )
        }
    }
}

// --- 辅助组件 ---

@Composable
fun CollectButton(isCollected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                indication = ripple(color = Color.White),
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isCollected) Icons.Filled.Star else Icons.Outlined.Star,
            contentDescription = "Collect",
            tint = if (isCollected) Color(0xFFFFC107) else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun TagCapsule(text: String, baseColor: Color) {
    Surface(
        color = baseColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.5.dp, baseColor.copy(alpha = 0.3f)),
    ) {
        Text(
            text = text,
            color = baseColor.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun FollowButton(isFollowing: Boolean, onClick: () -> Unit) {
    val bgColor = if (isFollowing) Color.White.copy(0.1f) else Color(0xFF64FFDA).copy(0.2f)
    val textColor = if (isFollowing) Color.White.copy(0.6f) else Color(0xFF64FFDA)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            if (isFollowing) "Following" else "+ Follow",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

fun formatCount(count: Int): String {
    return when {
        count < 1000 -> count.toString()
        count < 10000 -> String.format("%.1fk", count / 1000f)
        else -> String.format("%.1fw", count / 10000f)
    }
}

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

// --- 工具函数 ---
fun formatToChinaTime(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return try {
        val inputFmt =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS][.SSS]", Locale.getDefault())
        val ldt = LocalDateTime.parse(raw.trim(), inputFmt)
        val zdt: ZonedDateTime =
            ldt.atZone(ZoneOffset.UTC).withZoneSameInstant(ZoneId.of("Asia/Shanghai"))
        val outFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
        zdt.format(outFmt)
    } catch (e: Exception) {
        raw.trim().replace('T', ' ').let { s ->
            if (s.length >= 16) s.substring(0, 16) else s
        }
    }
}

@Composable
fun PostBottomBar(
    viewCount: Int,
    commentCount: Int,
    likeCount: Int,
    isLiked: Boolean,
    onLikeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 16.dp, bottom = 14.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Visibility,
                contentDescription = "Views",
                tint = Color.White.copy(0.35f),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = formatCount(viewCount),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(0.35f),
                fontSize = 11.5.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(0.85f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChatBubbleOutline,
                    contentDescription = "Comments",
                    tint = Color.White.copy(0.8f),
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = formatCount(commentCount),
                    fontSize = 12.5.sp,
                    color = Color.White.copy(0.8f)
                )
            }

            val scale by animateFloatAsState(
                targetValue = if (isLiked) 1.2f else 1.0f,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
                label = "LikeScale"
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onLikeClick() }
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) Color(0xFFFF4081) else Color.White.copy(0.8f),
                    modifier = Modifier
                        .size(19.dp)
                        .scale(if (isLiked) scale else 1f)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = formatCount(likeCount),
                    fontSize = 12.5.sp,
                    fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Medium,
                    color = if (isLiked) Color(0xFFFF4081) else Color.White.copy(0.8f)
                )
            }
        }
    }
}

@Composable
fun GlassToast(
    message: String?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (message != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFF5252).copy(0.6f),
                                Color.Transparent
                            )
                        ),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// 自己标志
@Composable
fun YourselfBadge() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.05f))
            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(50)) // 淡淡的描边
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Yourself",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}