package com.example.delta3d.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.screens.community.CommunityFeedItem
import com.example.delta3d.ui.screens.home.TagColorBinder
import com.example.delta3d.ui.screens.home.TagPalette
import com.example.delta3d.ui.session.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPostsScreen(
    sessionVm: SessionViewModel,
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: SavedPostsViewModel = viewModel()
) {
    val token by sessionVm.token.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentUser by sessionVm.currentUser.collectAsState()


    val tagColorBinder = remember { TagColorBinder(TagPalette) }


    LaunchedEffect(Unit) {
        token?.let { viewModel.loadCollectedPosts(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        AnimatedGradientBackground()

        Column(modifier = Modifier.fillMaxSize()) {


            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Post Collections",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // 内容列表
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF64FFDA))
                }
            } else if (posts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No collections yet.",
                        color = Color.White.copy(0.5f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(posts, key = { it.postId }) { post ->

                        CommunityFeedItem(
                            post = post,
                            currentUserId = currentUser?.id ?: -1,
                            tagColorBinder = tagColorBinder,
                            onClick = { onNavigateToDetail(post.postId) },
                            onLike = { token?.let { viewModel.toggleLike(post.postId, it) } },
                            onCollect = { token?.let { viewModel.toggleCollect(post.postId, it) } },
                            onFollow = { token?.let { viewModel.toggleFollow(post.ownerId, it) } },
                            onAvatarClick = {
                            }
                        )
                    }
                }
            }
        }
    }
}