package com.example.delta3d.ui.screens.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.api.UserOut
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.session.SessionViewModel
import androidx.compose.material.icons.rounded.Female
import androidx.compose.material.icons.rounded.Male
import androidx.compose.material.icons.rounded.Transgender


private val ListCardShape = RoundedCornerShape(16.dp)
private val ListCardBackground = Color(0xFF1E1E1E).copy(alpha = 0.85f)
private val GlassBorder = Brush.verticalGradient(
    colors = listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))
)

@Composable
fun UserListScreen(
    sessionVm: SessionViewModel,
    userId: Int,
    listType: String,
    onBack: () -> Unit,
    onUserClick: (Int) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val token by sessionVm.token.collectAsState()
    val userList by viewModel.userList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 初始加载
    LaunchedEffect(userId, listType) {
        token?.let {
            viewModel.fetchUserList(userId, listType, it)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 统一背景
        AnimatedGradientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 头部栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassyBackBtn(onClick = onBack)

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = if (listType == "followers") "Followers" else "Following",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // 列表内容
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF64FFDA))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = 8.dp,
                        bottom = 20.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(userList) { user ->
                        UserListItem(
                            user = user,
                            onUserClick = { onUserClick(user.id) },
                            // 仅在 Following 列表中显示操作逻辑
                            showUnfollow = listType == "following",
                            onUnfollow = {
                                token?.let { viewModel.toggleFollowInList(user.id, it) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UserListItem(
    user: UserOut,
    onUserClick: () -> Unit,
    showUnfollow: Boolean,
    onUnfollow: () -> Unit
) {
    val avatarUrl = remember(user.avatarUrl) {
        user.avatarUrl?.let {
            if (it.startsWith("http")) it else "${RetrofitClient.BASE_URL.removeSuffix("/")}${it}"
        }
    }


    val (genderIcon, genderColor) = when (user.gender?.lowercase()) {
        "male" -> Icons.Rounded.Male to Color(0xFF42A5F5)
        "female" -> Icons.Rounded.Female to Color(0xFFEC407A)
        else -> Icons.Rounded.Transgender to Color.White.copy(alpha = 0.6f)
    }


    // 容器
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(ListCardShape)
            .border(1.dp, GlassBorder, ListCardShape)
            .background(ListCardBackground)
            .clickable(onClick = onUserClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像区域
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.1f))
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = user.username.firstOrNull()?.toString()?.uppercase() ?: "?",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // 中间信息区域
            Column(modifier = Modifier.weight(1f)) {
                // 用户名 + 性别图标
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.username,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false) // 防止挤压图标
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // 性别图标
                    Icon(
                        imageVector = genderIcon,
                        contentDescription = null,
                        tint = genderColor,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                val bioText = if (!user.bio.isNullOrBlank()) user.bio else "No bio yet..."
                Text(
                    text = bioText,
                    color = Color.White.copy(0.5f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }


            if (showUnfollow) {
                Spacer(Modifier.width(8.dp))

                // 本地状态用于即时反馈 UI 变化
                var isFollowed by remember { mutableStateOf(true) }

                ListActionBtn(
                    isFollowing = isFollowed,
                    onClick = {
                        onUnfollow()
                        isFollowed = !isFollowed
                    }
                )
            }
        }
    }
}


@Composable
fun ListActionBtn(isFollowing: Boolean, onClick: () -> Unit) {
    val bgColor = if (isFollowing) Color.White.copy(0.1f) else Color(0xFF64FFDA).copy(0.2f)
    val textColor = if (isFollowing) Color.White.copy(0.6f) else Color(0xFF64FFDA)
    val text = if (isFollowing) "Following" else "+ Follow"

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

// 返回按钮
@Composable
fun GlassyBackBtn(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
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
        Icon(
            imageVector = Icons.Rounded.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}