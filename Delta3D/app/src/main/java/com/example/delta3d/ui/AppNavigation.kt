package com.example.delta3d.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.delta3d.ui.components.BottomNavBar
import com.example.delta3d.ui.screens.home.HomeScreen
import com.example.delta3d.ui.screens.auth.LoginScreen
import com.example.delta3d.ui.screens.auth.RegisterScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import com.example.delta3d.data.TokenStore
import androidx.compose.runtime.remember
import com.example.delta3d.ui.session.SessionViewModel

@Composable
fun AppNavigation(sessionVm: SessionViewModel) {
    val navController = rememberNavController()
    //token保存
    val token by sessionVm.token.collectAsState()
    val loaded by sessionVm.loaded.collectAsState()


    if (!loaded) return

//    val start = if (token.isNullOrBlank()) "login" else "home"
    val start = "login"

    // 获取当前路由，用于判断是否显示底部栏
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "login"

    // 定义底部栏页面
    val showBottomBar = currentRoute in listOf("home", "community", "profile")



    Scaffold(
        bottomBar = {
            // ✅ 关键逻辑：只有在主界面才显示底部栏
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
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
            startDestination = start, // 起点
            modifier = Modifier
        ) {
            // --- 认证模块 ---
            composable("login") {
                LoginScreen(
                    sessionVm = sessionVm,
                    onLoginSuccess = {
                        // 🎉 登录成功跳转逻辑
                        navController.navigate("home") {
                            // 弹出登录页，这样按返回键不会回到登录页，而是直接退出 App
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onGoToRegister = {
                        navController.navigate("register")
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        // 注册成功返回登录页
                        navController.popBackStack()
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            // --- 主业务模块 ---
            composable("home") { HomeScreen(sessionVm = sessionVm, innerPadding = innerPadding) }
            composable("community") { androidx.compose.material3.Text("社区页面建设中...") }
            composable("profile") { androidx.compose.material3.Text("个人中心建设中...") }
        }
    }
}