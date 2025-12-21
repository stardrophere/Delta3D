package com.example.delta3d.ui.screens.auth


import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.delta3d.api.RegisterRequest
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.ui.components.GlassyFeedbackPopup
import com.example.delta3d.ui.components.rememberFeedbackState
import com.example.delta3d.ui.session.SessionViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --- 登录页面 ---
@Composable
fun LoginScreen(
    sessionVm: SessionViewModel,
    onLoginSuccess: () -> Unit,
    onGoToRegister: () -> Unit
) {
    // 状态管理
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val feedbackState = rememberFeedbackState()

    // 全局背景容器
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                "Δ 3D",
                fontSize = 40.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                style = TextStyle(
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.3f),
                        offset = Offset(4f, 4f),
                        blurRadius = 8f
                    )
                )
            )
            Text("Welcome Back", fontSize = 18.sp, color = Color.White.copy(alpha = 0.7f))

            Spacer(modifier = Modifier.height(48.dp))

            // 输入框卡片
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    GlassyTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = "Username",
                        icon = Icons.Default.Person
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    GlassyTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        icon = Icons.Default.Lock,
                        isPassword = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 登录按钮
            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        scope.launch { feedbackState.showError("Please enter your username and password") }
                        return@Button
                    }

                    // 发起登录请求
                    scope.launch {
                        isLoading = true
                        try {
                            val response = RetrofitClient.api.login(username, password)

                            // 成功后：
                            sessionVm.login(response.accessToken)
                            println("Login Token: ${response.accessToken}")
                            feedbackState.showSuccess("Welcome back, $username!")

//                            Toast.makeText(context, "登录成功！", Toast.LENGTH_SHORT).show()


                            onLoginSuccess()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            val errorMessage = when (e) {
                                is retrofit2.HttpException -> "Login failed: Incorrect username or password" // HTTP 4xx/5xx
                                is java.io.IOException -> "Network connection failed. Please check your server or network" // Cannot connect to server
                                else -> "An unknown error occurred"
                            }
                            feedbackState.showError(errorMessage)
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0F2027)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF0F2027)
                    )
                } else {
                    Text("LOGIN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onGoToRegister) {
                Text("Don't have an account? Register", color = Color.White)
            }
        }
        GlassyFeedbackPopup(state = feedbackState, modifier = Modifier.align(Alignment.TopCenter))
    }
}

// --- 注册页面 ---
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val feedbackState = rememberFeedbackState()

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground() // 复用背景

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Create Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    GlassyTextField(username, { username = it }, "Username", Icons.Default.Person)
                    Spacer(modifier = Modifier.height(16.dp))
                    GlassyTextField(
                        password,
                        { password = it },
                        "Password",
                        Icons.Default.Lock,
                        isPassword = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (username.isBlank() || password.isBlank()) {
                        scope.launch { feedbackState.showError("Please enter your username and password") }
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        try {
                            // 起注册请求
                            val request = RegisterRequest(username, password)
                            val response = RetrofitClient.api.register(request)

                            feedbackState.showSuccess("Registration success: ${response.username}")
                            delay(1000)
                            onRegisterSuccess() // 返回登录页
                        } catch (e: Exception) {
                            e.printStackTrace()
                            feedbackState.showError("Registration failed: the username may already exist")
                        } finally {
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFF0F2027)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF0F2027)
                    )
                } else {
                    Text("REGISTER NOW", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBack) {
                Text("Back to Login", color = Color.White.copy(alpha = 0.7f))
            }
        }
        GlassyFeedbackPopup(state = feedbackState, modifier = Modifier.align(Alignment.TopCenter))
    }
}


@Composable
fun AnimatedGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val targetOffset = with(LocalDensity.current) { 1000.dp.toPx() }
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = targetOffset,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset"
    )
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364), Color(0xFF0F2027)),
        start = Offset(offset, offset), end = Offset(offset + 1000f, offset + 1000f),
        tileMode = TileMode.Mirror
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false
) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = Color.White.copy(alpha = 0.8f)) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = Color.White) },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = {
                    passwordVisible = !passwordVisible
                }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null,
                        tint = Color.White
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true, shape = RoundedCornerShape(50),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.15f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.1f),
            focusedIndicatorColor = Color.White,
            unfocusedIndicatorColor = Color.White.copy(alpha = 0.5f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth()
    )
}