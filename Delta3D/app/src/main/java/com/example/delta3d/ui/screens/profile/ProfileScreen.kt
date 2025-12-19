package com.example.delta3d.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.session.SessionViewModel

// --- 样式常量 ---
private val AccentColor = Color(0xFF64FFDA)
private val GlassContainerColor = Color(0xFF1E1E1E).copy(alpha = 0.6f)
private val TextWhite = Color.White
private val TextGray = Color.White.copy(alpha = 0.6f)

@Composable
fun ProfileScreen(
    sessionVm: SessionViewModel,
    onNavigateToUserList: (String) -> Unit,
    onNavigateToCollections: () -> Unit,
    onNavigateToSavedPosts: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToPlanSettings: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val currentUser by sessionVm.currentUser.collectAsState()
    val token by sessionVm.token.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        sessionVm.refreshUserInfo()
    }

    var showEditDialog by remember { mutableStateOf(false) }


    val blurRadius by animateDpAsState(
        targetValue = if (showEditDialog) 20.dp else 0.dp,
        label = "backgroundBlur",
        animationSpec = tween(durationMillis = 300)
    )

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { token?.let { t -> viewModel.uploadImage(context, it, false, t, sessionVm) } }
    }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { token?.let { t -> viewModel.uploadImage(context, it, true, t, sessionVm) } }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius) // 背景模糊
        ) {
            AnimatedGradientBackground()

            if (currentUser == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AccentColor)
                }
            } else {
                val user = currentUser!!
                val coverUrl = remember(user.coverUrl) {
                    user.coverUrl?.let {
                        if (it.startsWith("http")) it else "${
                            RetrofitClient.BASE_URL.removeSuffix("/")
                        }${it}"
                    }
                }
                val avatarUrl = remember(user.avatarUrl) {
                    user.avatarUrl?.let {
                        if (it.startsWith("http")) it else "${
                            RetrofitClient.BASE_URL.removeSuffix("/")
                        }${it}"
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {

                    Box(modifier = Modifier.fillMaxWidth()) {
                        // 封面背景
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .background(Color.Gray.copy(0.3f))
                                .clickable {
                                    coverPicker.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = "Cover",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(0.3f))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(0.8f)
                                            ),
                                            startY = 300f
                                        )
                                    )
                            )

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .background(Color.Black.copy(0.4f), CircleShape)
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    "Edit Cover",
                                    tint = TextWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // 内容区域 (头像 + 信息)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 180.dp)
                                .padding(horizontal = 20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(84.dp)
                                        .clip(CircleShape)
                                        .border(3.dp, Color(0xFF121212), CircleShape)
                                        .background(Color.DarkGray)
                                        .clickable {
                                            avatarPicker.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        }
                                ) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .height(24.dp)
                                            .background(Color.Black.copy(0.6f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.CameraAlt,
                                            null,
                                            tint = TextWhite,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier
                                        .padding(bottom = 12.dp)
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = user.username,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val (genderIcon, genderColor) = when (user.gender) {
                                            "male" -> Icons.Rounded.Male to Color(0xFF42A5F5)
                                            "female" -> Icons.Rounded.Female to Color(0xFFEC407A)
                                            else -> Icons.Rounded.Transgender to TextGray
                                        }
                                        Icon(
                                            genderIcon,
                                            null,
                                            tint = genderColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = user.gender?.replaceFirstChar { it.uppercase() }
                                                ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextGray
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { showEditDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentColor),
                                    border = BorderStroke(1.dp, AccentColor.copy(0.5f)),
                                    shape = RoundedCornerShape(50),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .padding(bottom = 12.dp)
                                ) {
                                    Text("Edit", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            if (!user.bio.isNullOrBlank()) {
                                Text(
                                    text = user.bio,
                                    color = TextGray,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(20.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(Modifier.weight(1f)) {
                                    StatItem("Likes", user.likedTotalCount.toString(), {})
                                }
                                Box(Modifier.weight(1f)) {
                                    StatItem(
                                        "Following",
                                        user.followingCount.toString()
                                    ) { onNavigateToUserList("following") }
                                }
                                Box(Modifier.weight(1f)) {
                                    StatItem(
                                        "Followers",
                                        user.followerCount.toString()
                                    ) { onNavigateToUserList("followers") }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Divider(
                        color = TextWhite.copy(0.1f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Spacer(Modifier.height(20.dp))


                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "DASHBOARD",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentColor,
                            fontSize = 10.sp
                        )

                        FunctionCard(
                            Icons.Rounded.ViewInAr,
                            "Model Collections",
                            "View saved 3D models"
                        ) { onNavigateToCollections() }
                        FunctionCard(
                            Icons.Rounded.Bookmarks,
                            "Post Collections",
                            "Saved community posts"
                        ) { onNavigateToSavedPosts() }
                        FunctionCard(
                            Icons.Rounded.Download,
                            "Download History",
                            "Downloaded models"
                        ) { onNavigateToDownloads() }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "ACCOUNT",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentColor,
                            fontSize = 10.sp
                        )

                        FunctionCard(
                            Icons.Rounded.Diamond,
                            "Plan Settings",
                            "Upgrade to Pro",
                            Color(0xFFFFD700)
                        ) {onNavigateToPlanSettings()}

                        GlassCard {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { sessionVm.logout() }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.Logout,
                                    null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Log Out",
                                    color = Color(0xFFFF5252),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(130.dp))
                }
            }
        }


        AnimatedVisibility(
            visible = showEditDialog,
            enter = fadeIn(animationSpec = tween(durationMillis = 300)),

            exit = fadeOut(animationSpec = tween(durationMillis = 250)),
            modifier = Modifier.zIndex(10f)
        ) {

            BackHandler { showEditDialog = false }

            // 全屏遮罩层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.3f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showEditDialog = false }, // 点击空白处关闭
                contentAlignment = Alignment.Center
            ) {
                // 内容容器 (阻止点击事件穿透)
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clickable(enabled = false) {} // 拦截点击
                ) {
                    EditProfilePanel(
                        currentName = currentUser?.username ?: "",
                        currentBio = currentUser?.bio ?: "",
                        currentGender = currentUser?.gender ?: "secret",
                        onDismiss = { showEditDialog = false },
                        onConfirm = { name, bio, gender ->
                            token?.let { viewModel.updateProfile(name, bio, gender, it, sessionVm) }
                            showEditDialog = false
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(20f)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentColor)
            }
        }
    }
}

// ==========================================
// 辅助组件
// ==========================================

@Composable
fun StatItem(label: String, count: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White.copy(0.05f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp)
    ) {
        Text(text = count, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextWhite)
        Text(text = label, fontSize = 11.sp, color = TextGray)
    }
}

@Composable
fun FunctionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color = AccentColor,
    onClick: () -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(0.1f), CircleShape)
                    .border(1.dp, color.copy(0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(subtitle, color = TextGray, fontSize = 11.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = TextGray, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = GlassContainerColor,
        border = BorderStroke(1.dp, Color.White.copy(0.1f)),
        content = content
    )
}

@Composable
fun GlassyInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    singleLine: Boolean = true,
    minLines: Int = 1,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = TextWhite.copy(alpha = 0.7f), fontSize = 14.sp) },
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = TextWhite.copy(0.7f),
                modifier = Modifier.size(20.dp)
            )
        },
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        singleLine = singleLine,
        minLines = minLines,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Black.copy(alpha = 0.3f),
            unfocusedContainerColor = Color.Black.copy(alpha = 0.2f),
            focusedBorderColor = AccentColor,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            focusedTextColor = TextWhite,
            unfocusedTextColor = TextWhite,
            cursorColor = AccentColor,
            focusedLabelColor = AccentColor
        ),
        modifier = modifier.fillMaxWidth()
    )
}


@Composable
fun EditProfilePanel(
    currentName: String,
    currentBio: String,
    currentGender: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }
    var bio by remember { mutableStateOf(currentBio) }
    var gender by remember { mutableStateOf(currentGender) }

    GlassCard {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Edit Profile",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, "Close", tint = TextGray)
                }
            }
            Divider(color = Color.White.copy(0.1f))
            GlassyInput(
                value = name,
                onValueChange = { name = it },
                label = "Username",
                icon = Icons.Default.Person
            )
            GlassyInput(
                value = bio,
                onValueChange = { bio = it },
                label = "Bio",
                icon = Icons.Rounded.Description,
                singleLine = false,
                minLines = 3
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gender", color = AccentColor, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("male", "female", "secret").forEach { item ->
                        val isSelected = gender == item
                        val color = if (isSelected) AccentColor else Color.White.copy(0.3f)
                        Surface(
                            color = if (isSelected) AccentColor.copy(0.2f) else Color.Transparent,
                            shape = RoundedCornerShape(50), border = BorderStroke(1.dp, color),
                            modifier = Modifier
                                .clickable { gender = item }
                                .clip(RoundedCornerShape(50))
                        ) {
                            Text(
                                text = item.replaceFirstChar { it.uppercase() },
                                color = if (isSelected) AccentColor else TextGray,
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 6.dp
                                ),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { onConfirm(name, bio, gender) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentColor,
                    contentColor = Color.Black
                )
            ) {
                Text("SAVE CHANGES", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}