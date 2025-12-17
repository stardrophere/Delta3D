package com.example.delta3d.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.delta3d.ui.session.SessionViewModel
import android.widget.Toast

// 直接复用登录/注册的背景（建议后续挪到 ui/components 里）
// import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground
import com.example.delta3d.ui.screens.auth.AnimatedGradientBackground

data class Product(
    val title: String,
    val subtitle: String,
    val imageHeight: Dp
)

@Composable
fun HomeScreen(innerPadding: PaddingValues = PaddingValues(0.dp), sessionVm: SessionViewModel) {
    val context = LocalContext.current

    // ✅ 读取 token（StateFlow / Flow 都行，按你 SessionViewModel 的 token 类型来）
    val token by sessionVm.token.collectAsState()
    // 首次进入的小动画开关
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    val headerAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "headerAlpha"
    )
    val headerOffset by animateFloatAsState(
        targetValue = if (entered) 0f else 18f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "headerOffset"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
        ) {
            // 顶部区域：标题 + 玻璃搜索框（和登录页同一套颜色逻辑）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .offset(y = headerOffset.dp)
                    .alpha(headerAlpha)
            ) {
                Text(
                    text = "Δ 3D",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

//                Spacer(modifier = Modifier.height(14.dp))
////                Spacer(modifier = Modifier.height(10.dp))
//
//                // ✅ 测试按钮：点一下看看 token
//                Button(
//                    onClick = {
//                        sessionVm.logout()
//                    }
//                ) {
//                    Text("查看Token")
//                }

                Spacer(modifier = Modifier.height(14.dp))
                GlassySearchBar(
                    placeholder = "搜索模型",
                    onClick = { /* TODO: 打开搜索页/弹窗 */ }
                )
            }

            // 瀑布流
            ProductStaggeredGrid(
                bottomPadding = innerPadding.calculateBottomPadding()
            )
        }
    }
}

@Composable
private fun GlassySearchBar(
    placeholder: String,
    onClick: () -> Unit
) {
    // 这里做成“可点的假输入框”，视觉更像你登录页的玻璃输入框
    Surface(
        onClick = onClick,
        color = Color.Black.copy(alpha = 0.28f),
        shape = RoundedCornerShape(50),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = placeholder,
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ProductStaggeredGrid(bottomPadding: Dp) {
    val items = listOf(
        Product("玩偶熊", "学院送的熊", 200.dp),
        Product("狐狸标本", "家里的狐狸标本", 150.dp),
        Product("高达", "高大帅", 220.dp),
        Product("鼠标", "记录下新鼠标", 180.dp),
        Product("耳机", "索尼大法好", 160.dp),
        Product("相机", "复古旁轴", 190.dp),
        Product("台灯", "护眼灯", 140.dp),
    )

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
        contentPadding = PaddingValues(
            start = 5.dp,
            end = 12.dp,
            top = 6.dp,
            bottom = bottomPadding + 88.dp // 给底部导航留空间
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items) { item ->
            ProductCard(item)
        }
    }
}

@Composable
fun ProductCard(item: Product) {
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { show = true }

    val alpha by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "cardAlpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (show) 1f else 0.96f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label = "cardScale"
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.30f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .scale(scale)
    ) {
        Column {
            // 图片占位：也做成玻璃风（深一点的透明层）
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(item.imageHeight)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Color.White.copy(alpha = 0.10f))
            )

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.subtitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
