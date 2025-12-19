package com.example.delta3d.ui

import android.util.Log
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.delta3d.ui.components.BottomNavBar
import com.example.delta3d.ui.screens.home.HomeScreen
import com.example.delta3d.ui.screens.auth.LoginScreen
import com.example.delta3d.ui.screens.auth.RegisterScreen
import androidx.compose.runtime.collectAsState
import com.example.delta3d.manager.ChatSocketManager
import com.example.delta3d.ui.screens.chat.ChatListScreen
import com.example.delta3d.ui.screens.chat.ChatScreen
import com.example.delta3d.ui.screens.community.CommunityScreen
import com.example.delta3d.ui.session.SessionViewModel
import com.example.delta3d.ui.screens.detail.AssetDetailScreen
import com.example.delta3d.ui.screens.detail.PostDetailScreen
import com.example.delta3d.ui.screens.preview.StreamPreviewScreen
import com.example.delta3d.ui.screens.profile.DownloadHistoryScreen
import com.example.delta3d.ui.screens.profile.PlanSettingsScreen
import com.example.delta3d.ui.screens.profile.ProfileScreen
import com.example.delta3d.ui.screens.profile.SavedAssetsScreen
import com.example.delta3d.ui.screens.profile.SavedPostsScreen
import com.example.delta3d.ui.screens.profile.UserListScreen
import com.example.delta3d.ui.screens.publish.PublishPostScreen
import com.example.delta3d.ui.screens.upload.UploadScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

val chatSocketManager = ChatSocketManager()

@Composable
fun AppNavigation(sessionVm: SessionViewModel) {
    val navController = rememberNavController()
    val token by sessionVm.token.collectAsState()
    val loaded by sessionVm.loaded.collectAsState()


    val start = if (token.isNullOrBlank()) "login" else "home"
//    val start = "login"
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf("home", "community", "profile")

    if (!loaded) return


    // 监听 Token 失效/退出登录
    LaunchedEffect(token) {
        if (token.isNullOrBlank()) {
            val current = navController.currentBackStackEntry?.destination?.route
            if (current != "login" && current != null) {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute ?: "home",
                    onNavigate = { targetRoute ->
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = start,
            modifier = Modifier
        ) {
            //  登录/注册/详情页
            composable("login") {
                LoginScreen(
                    sessionVm = sessionVm,
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("login") {
                                inclusive = true
                            }
                        }
                    },
                    onGoToRegister = { navController.navigate("register") }
                )
            }
            composable("register") {
                RegisterScreen(
                    { navController.popBackStack() },
                    { navController.popBackStack() })
            }

            composable(
                route = "detail/{assetId}",
                arguments = listOf(navArgument("assetId") { type = NavType.IntType })
            ) { backStackEntry ->
                val currentId = backStackEntry.arguments?.getInt("assetId") ?: 0

                AssetDetailScreen(
                    assetId = currentId,
                    onBack = { navController.popBackStack() },
                    sessionVm = sessionVm,
                    onPreviewClick = {
                        Log.d("TRACK_ID", "1. [Detail->Preview] 跳转 ID: $currentId") // 加个日志双重保险
                        navController.navigate("preview/$currentId")
                    },
                    onNavigateToPublish = { targetAssetId ->
                        navController.navigate("publish_post?assetId=$targetAssetId")
                    }
                )
            }

            // --- 主业务模块 ---
            composable("home") {
                HomeScreen(
                    sessionVm = sessionVm,
                    innerPadding = innerPadding,
                    onAssetClick = { assetId -> navController.navigate("detail/$assetId") },
                    // 处理上传跳转
                    onNavigateToUpload = { uri ->
                        val encodedUri =
                            URLEncoder.encode(uri.toString(), StandardCharsets.UTF_8.toString())
                        navController.navigate("upload/$encodedUri")
                    }
                )
            }
            composable(
                route = "preview/{assetId}",
                arguments = listOf(navArgument("assetId") { type = NavType.IntType })
            ) { backStackEntry ->
                val assetId = backStackEntry.arguments?.getInt("assetId") ?: 0
                Log.d("TRACK_ID", "2. [Navigation] 路由解析完成, 获取到的 ID: $assetId")
                StreamPreviewScreen(
                    assetId = assetId,
                    sessionVm = sessionVm,
                    onBack = { navController.popBackStack() }
                )
            }

            // 上传页面路由
            composable(
                route = "upload/{videoUri}",
                arguments = listOf(navArgument("videoUri") { type = NavType.StringType })
            ) { backStackEntry ->
                val uriString = backStackEntry.arguments?.getString("videoUri") ?: ""
                val uri = android.net.Uri.parse(uriString)

                UploadScreen(
                    videoUri = uri,
                    sessionVm = sessionVm,
                    onBack = { navController.popBackStack() },
                    onUploadSuccess = {
                        // 上传成功后返回首页并刷新
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "chat/{userId}/{username}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.IntType },
                    navArgument("username") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val targetId = backStackEntry.arguments?.getInt("userId") ?: 0
                val targetName = backStackEntry.arguments?.getString("username") ?: "Chat"

                ChatScreen(
                    targetUserId = targetId,
                    targetUserName = targetName,
                    sessionVm = sessionVm,
                    socketManager = chatSocketManager, // 传入单例
                    onBack = { navController.popBackStack() },
                    onNavigateToPost = { postId ->
                        // 跳转到帖子详情
                        navController.navigate("post/$postId")
                    }
                )
            }

            // 消息列表页
            composable("chat_list") {
                ChatListScreen(
                    sessionVm = sessionVm,
                    onBack = { navController.popBackStack() },
                    onNavigateToChat = { userId, username ->
                        // 跳转到具体的聊天页
                        navController.navigate("chat/$userId/$username")
                    }
                )
            }

            //社区主页
            composable("community") {
                CommunityScreen(
                    sessionVm = sessionVm,
                    innerPadding = innerPadding,
                    onNavigateToDetail = { postId ->
                        navController.navigate("post/$postId")
                    },
                    onNavigateToPublish = {
                        navController.navigate("publish_post")
                    },

                    onNavigateToChat = { navController.navigate("chat_list") },
                    onNavigateToDirectChat = { userId, userName ->
                        navController.navigate("chat/$userId/$userName")
                    }
                )
            }

//发布帖子页面
            composable(
                route = "publish_post?assetId={assetId}", // ? 可选参数
                arguments = listOf(
                    navArgument("assetId") {
                        defaultValue = -1 // 默认值为 -1，代表没有预选
                        type = NavType.IntType
                    }
                )
            ) { backStackEntry ->
                // 获取参数
                val assetIdArg = backStackEntry.arguments?.getInt("assetId") ?: -1

                val initialId = if (assetIdArg != -1) assetIdArg else null

                PublishPostScreen(
                    sessionVm = sessionVm,
                    initialAssetId = initialId,
                    onBack = { navController.popBackStack() },
                    onSuccess = {
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "post/{postId}",
                arguments = listOf(navArgument("postId") { type = NavType.IntType })
            ) { backStackEntry ->
                val postId = backStackEntry.arguments?.getInt("postId") ?: 0

                PostDetailScreen(
                    postId = postId,
                    onBack = { navController.popBackStack() },
                    sessionVm = sessionVm,
                    onNavigateToPreview = { assetId ->
                        // 跳转到Preview 页面
                        Log.d("TRACK_NAV", "From PostDetail -> Preview: AssetId=$assetId")
                        navController.navigate("preview/$assetId")
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    sessionVm = sessionVm,
                    onNavigateToUserList = { type ->
                        // 获取当前用户ID并跳转
                        val userId = sessionVm.currentUser.value?.id ?: 0
                        navController.navigate("profile/list/$userId/$type")
                    },
                    onNavigateToCollections = {
                        navController.navigate("profile/asset/collections")
                    },
                    onNavigateToSavedPosts = {
                        navController.navigate("profile/post/collections")
                    },
                    onNavigateToDownloads = {
                        navController.navigate("profile/asset/downloads")
                    },
                    onNavigateToPlanSettings = {
                        navController.navigate("profile/plan")
                    }
                )
            }

            // 粉丝/关注列表页
            composable(
                route = "profile/list/{userId}/{type}",
                arguments = listOf(
                    navArgument("userId") { type = NavType.IntType },
                    navArgument("type") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val userId = backStackEntry.arguments?.getInt("userId") ?: 0
                val type = backStackEntry.arguments?.getString("type") ?: "followers"

                UserListScreen(
                    sessionVm = sessionVm,
                    userId = userId,
                    listType = type,
                    onBack = { navController.popBackStack() },
                    onUserClick = { targetId ->
                    }
                )
            }

            //收藏列表页
            composable("profile/asset/collections") {
                SavedAssetsScreen(
                    sessionVm = sessionVm,
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { assetId ->
                        navController.navigate("detail/$assetId")
                    }
                )
            }

            //帖子收藏页
            composable("profile/post/collections") {
                SavedPostsScreen(
                    sessionVm = sessionVm,
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { postId ->
                        navController.navigate("post/$postId")
                    }
                )
            }

            //下载页
            composable("profile/asset/downloads") {
                DownloadHistoryScreen(
                    sessionVm = sessionVm,
                    onBack = { navController.popBackStack() },
                    onNavigateToDetail = { assetId ->
                        navController.navigate("detail/$assetId")
                    }
                )
            }

            //付费页面吗
            composable("profile/plan") {
                PlanSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}