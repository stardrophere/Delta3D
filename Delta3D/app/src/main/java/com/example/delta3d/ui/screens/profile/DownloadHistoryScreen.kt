package com.example.delta3d.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.delta3d.api.AssetCard
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.ui.components.FeedbackType
import com.example.delta3d.ui.components.GlassyFeedbackPopup
import com.example.delta3d.ui.components.rememberFeedbackState
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.session.SessionViewModel
import com.example.delta3d.utils.TimeUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadHistoryScreen(
    sessionVm: SessionViewModel,
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: DownloadHistoryViewModel = viewModel()
) {
    val token by sessionVm.token.collectAsState()
    val currentUser by sessionVm.currentUser.collectAsState()
    val assets by viewModel.assets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 弹窗状态管理
    val feedbackState = rememberFeedbackState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(token) {
        token?.let { viewModel.loadDownloadHistory(it) }
    }

    // 统一检查权限的逻辑
    fun handleItemClick(asset: AssetCard) {
        // 如果当前用户不是模型的所有者
        if (currentUser?.id != asset.ownerId) {
            scope.launch {
                feedbackState.show(
                    msg = "Access Denied: Only the owner can view details.",
                    feedbackType = FeedbackType.ERROR
                )
            }
        } else {
            onNavigateToDetail(asset.id)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground() // 复用背景

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Download History",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF64FFDA)
                    )
                } else if (assets.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.DownloadDone,
                            contentDescription = null,
                            tint = Color.White.copy(0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No downloads yet",
                            color = Color.White.copy(0.5f),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(assets) { asset ->
                            DownloadHistoryItem(
                                asset = asset,
                                currentUserId = currentUser?.id ?: -1,
                                onClick = { handleItemClick(asset) }
                            )
                        }
                    }
                }
            }
        }

        // 反馈弹窗
        GlassyFeedbackPopup(state = feedbackState, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
fun DownloadHistoryItem(
    asset: AssetCard,
    currentUserId: Int,
    onClick: () -> Unit
) {
    val isOwner = asset.ownerId == currentUserId

    // 玻璃拟态卡片容器
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1E1E1E).copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color.White.copy(0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 缩略图
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(0.3f))
            ) {
                val imageUrl = if (asset.coverUrl?.startsWith("http") == true) {
                    asset.coverUrl
                } else {
                    "${RetrofitClient.BASE_URL.removeSuffix("/")}${asset.coverUrl}/images/0001.jpg"
                }

                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 如果不是Owner，显示一个小锁图标提示
                if (!isOwner) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            null,
                            tint = Color.White.copy(0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 信息区域
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = asset.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))

                // 显示下载时间 (东八区)
                Text(
                    text = "Downloaded on:",
                    color = Color.White.copy(0.5f),
                    fontSize = 11.sp
                )
                Text(
                    text = TimeUtils.formatToEastEight(asset.downloadedAt), // 格式化时间
                    color = Color(0xFF64FFDA), // 高亮时间
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 箭头
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(0.3f)
            )
        }
    }
}