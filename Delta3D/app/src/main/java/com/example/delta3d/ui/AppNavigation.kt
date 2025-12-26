package com.example.delta3d.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.delta3d.config.AppConfig
import com.example.delta3d.config.NetworkMode
import com.example.delta3d.ui.components.BottomNavBar
import com.example.delta3d.ui.components.NetworkStatusDialog
import com.example.delta3d.ui.screens.auth.LoginScreen
import com.example.delta3d.ui.screens.auth.RegisterScreen
import com.example.delta3d.ui.screens.chat.ChatListScreen
import com.example.delta3d.ui.screens.chat.ChatScreen
import com.example.delta3d.ui.screens.community.CommunityScreen
import com.example.delta3d.ui.screens.detail.AssetDetailScreen
import com.example.delta3d.ui.screens.detail.PostDetailScreen
import com.example.delta3d.ui.screens.home.HomeScreen
import com.example.delta3d.ui.screens.onboarding.Delta3DLogoSplash
import com.example.delta3d.ui.screens.onboarding.DeltaTreeScreen
import com.example.delta3d.ui.screens.preview.StreamPreviewScreen
import com.example.delta3d.ui.screens.profile.DownloadHistoryScreen
import com.example.delta3d.ui.screens.profile.GlassCard
import com.example.delta3d.ui.screens.profile.PlanSettingsScreen
import com.example.delta3d.ui.screens.profile.ProfileScreen
import com.example.delta3d.ui.screens.profile.SavedAssetsScreen
import com.example.delta3d.ui.screens.profile.SavedPostsScreen
import com.example.delta3d.ui.screens.profile.UserListScreen
import com.example.delta3d.ui.screens.publish.PublishPostScreen
import com.example.delta3d.ui.screens.upload.UploadScreen
import com.example.delta3d.ui.session.SessionViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val AccentColor = Color(0xFF64FFDA)

@Composable
fun AppNavigation(sessionVm: SessionViewModel, isFirstLaunch: Boolean) {
    val navController = rememberNavController()
    val token by sessionVm.token.collectAsState()
    val loaded by sessionVm.loaded.collectAsState()


    // 监听网络检测完成信号
    val networkCheckFinished by sessionVm.networkCheckFinished.collectAsState()
    val currentMode = AppConfig.currentMode

    var showNetworkDialog by remember { mutableStateOf(false) }

    // 当 ViewModel 通知检测完成，且状态不是未知时，显示弹窗
    LaunchedEffect(networkCheckFinished) {
        if (networkCheckFinished && currentMode != NetworkMode.UNKNOWN) {
            showNetworkDialog = true
        }
    }

    if (!loaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AccentColor)
        }
        return
    }

    //起始页逻辑
    val startDestination = if (isFirstLaunch) {
        "delta_tree?isReplay=false"
    } else if (token.isNullOrBlank()) {
        "login"
    } else {
        "home"
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf("home", "community", "profile")

    // 监听 Token 失效/退出登录
    LaunchedEffect(token) {
        if (token.isNullOrBlank() && !isFirstLaunch) {
            val current = navController.currentBackStackEntry?.destination?.route
            if (current != "login" && current != "register") {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    BottomNavBar(
                        currentRoute = currentRoute ?: "home",
                        onNavigate = { targetRoute ->
                            try {
                                if (currentRoute != targetRoute) {
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.startDestinationId) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("AppNavigation", "Navigation error: ${e.message}")
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier
            ) {

                //树页
                composable(
                    route = "delta_tree?isReplay={isReplay}",
                    arguments = listOf(
                        navArgument("isReplay") {
                            defaultValue = false
                            type = NavType.BoolType
                        }
                    )
                ) { backStackEntry ->
                    val isReplay = backStackEntry.arguments?.getBoolean("isReplay") ?: false

                    DeltaTreeScreen(
                        onDismiss = {
                            if (isReplay) {
                                //回看直接返回
                                navController.popBackStack()
                            } else {
                                // 首次启动
                                navController.navigate("logo") {
                                    popUpTo("delta_tree") { inclusive = true }
                                }
                            }
                        }
                    )
                }

                // Logo 页
                composable(
                    route = "logo?isReplay={isReplay}",
                    arguments = listOf(
                        navArgument("isReplay") {
                            defaultValue = false
                            type = NavType.BoolType
                        }
                    )
                ) { backStackEntry ->
                    val isReplay = backStackEntry.arguments?.getBoolean("isReplay") ?: false

                    Delta3DLogoSplash(
                        onAnimationComplete = {
                            if (isReplay) {
                                // 如回看直接返回
                                navController.popBackStack()
                            } else {
                                // 首次启动标记完成并去登录页
                                sessionVm.completeOnboarding()
                                navController.navigate("login") {
                                    popUpTo("logo") { inclusive = true }
                                }
                            }
                        }
                    )
                }

                //  登录/注册
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

                // 详情页
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
                            Log.d("TRACK_ID", "[Detail->Preview] 跳转 ID: $currentId")
                            navController.navigate("preview/$currentId")
                        },
                        onNavigateToPublish = { targetAssetId ->
                            navController.navigate("publish_post?assetId=$targetAssetId")
                        }
                    )
                }

                //  主业务模块
                composable("home") {
                    HomeScreen(
                        sessionVm = sessionVm,
                        innerPadding = innerPadding,
                        onAssetClick = { assetId -> navController.navigate("detail/$assetId") },
                        onNavigateToUpload = { uri ->
                            val uriString = uri.toString()
                            // URL 编码
                            val encodedUri =
                                URLEncoder.encode(uriString, StandardCharsets.UTF_8.toString())
                            navController.navigate("upload/$encodedUri")
                        },

                        // 跳转到全屏树
                        onNavigateToTree = {
                            navController.navigate("delta_tree?isReplay=true")
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


                //个人页跳转
                composable("profile") {
                    ProfileScreen(
                        sessionVm = sessionVm,
                        onNavigateToUserList = { type ->
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
                        },
                        // 启动页
                        onNavigateToTree = {
                            navController.navigate("delta_tree?isReplay=true")
                        },
                        onNavigateToLogo = {
                            navController.navigate("logo?isReplay=true")
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
                        // 接收 id 和 username 并跳转
                        onUserClick = { targetId, targetName ->
                            val currentUserId = sessionVm.currentUser.value?.id

                            if (targetId != currentUserId) {
                                // 跳转到聊天页面
                                navController.navigate("chat/$targetId/$targetName")
                            }
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

                //付费页面
                composable("profile/plan") {
                    PlanSettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        //全局弹窗
        if (showNetworkDialog) {
            NetworkStatusDialog(
                mode = currentMode,
                onDismiss = { showNetworkDialog = false }
            )
        }
    }
}


