package com.example.delta3d.ui

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
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
import com.example.delta3d.ui.session.SessionViewModel
import com.example.delta3d.ui.screens.detail.AssetDetailScreen
import com.example.delta3d.ui.screens.preview.StreamPreviewScreen
// 🟢 引入上传页
import com.example.delta3d.ui.screens.upload.UploadScreen
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation(sessionVm: SessionViewModel) {
    val navController = rememberNavController()
    val token by sessionVm.token.collectAsState()
    val loaded by sessionVm.loaded.collectAsState()

    if (!loaded) return

//    val start = if (token.isNullOrBlank()) "login" else "home"
    val start = "login"
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf("home", "community", "profile")

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
            // ... 登录/注册/详情页 (保持不变) ...
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
                // ✅ 第一步：先提取变量
                val currentId = backStackEntry.arguments?.getInt("assetId") ?: 0

                AssetDetailScreen(
                    assetId = currentId, // ✅ 第二步：这里传进去
                    onBack = { navController.popBackStack() },
                    sessionVm = sessionVm,
                    // ✅ 第三步：这里跳转也用同一个变量
                    onPreviewClick = {
                        Log.d("TRACK_ID", "1. [Detail->Preview] 跳转 ID: $currentId") // 加个日志双重保险
                        navController.navigate("preview/$currentId")
                    }
                )
            }

            // --- 主业务模块 ---
            composable("home") {
                HomeScreen(
                    sessionVm = sessionVm,
                    innerPadding = innerPadding,
                    onAssetClick = { assetId -> navController.navigate("detail/$assetId") },
                    // 🟢 核心修改：处理上传跳转
                    onNavigateToUpload = { uri ->
                        // 必须对 URI 进行编码，否则特殊字符会破坏路由结构
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

            // 🟢 新增：上传页面路由
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
                        // 上传成功后返回首页并刷新 (HomeScreen 会因 LaunchedEffect 自动重载)
                        navController.popBackStack()
                    }
                )
            }

            composable("community") { androidx.compose.material3.Text("社区页面建设中...") }
            composable("profile") { androidx.compose.material3.Text("个人中心建设中...") }
        }
    }
}