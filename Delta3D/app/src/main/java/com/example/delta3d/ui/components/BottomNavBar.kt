package com.example.delta3d.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Shader
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- 1) 玻璃拟态配色 ---
private val BarGlass = Color.Black.copy(alpha = 0.9f)          // 加深一点透明度，配合噪点更有质感
private val BarStroke = Color.White.copy(alpha = 0.15f)
private val UnselectedTint = Color.White.copy(alpha = 0.65f)
private val SelectedTint = Color.White

// 选中“内核”的渐变
private val BubbleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF7C4DFF), Color(0xFF00E5FF))
)

// --- 2) 尺寸常量 ---
private val BarShapeHeight = 65.dp
private val CutoutRadius = 28.dp

// --- 3) 数据模型 ---
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem("home", "主页", Icons.Filled.Home, Icons.Outlined.Home)
    object Community : BottomNavItem("community", "社区", Icons.Filled.Whatshot, Icons.Outlined.Whatshot)
    object Profile : BottomNavItem("profile", "我的", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun BottomNavBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(BottomNavItem.Home, BottomNavItem.Community, BottomNavItem.Profile)

    val selectedIndex = remember(currentRoute) {
        val index = items.indexOfFirst { it.route == currentRoute }
        if (index == -1) 0 else index
    }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val totalHeight = BarShapeHeight + bottomInset

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalHeight),
        contentAlignment = Alignment.BottomCenter
    ) {
        val width = constraints.maxWidth.toFloat()
        val tabWidth = width / items.size

        val animatedOffsetX by animateFloatAsState(
            targetValue = (selectedIndex * tabWidth) + (tabWidth / 2),
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 320f),
            label = "OffsetAnimation"
        )

        // 🟢 获取噪点 Brush (关键：模拟磨砂质感)
        val noiseBrush = rememberNoiseBrush()

        // 🟢 获取光感渐变 (关键：模拟光线在玻璃表面的漫反射)
        val highlightGradient = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.15f), // 顶部稍微亮一点
                Color.White.copy(alpha = 0.02f)  // 底部透明
            )
        )

        // 1) 玻璃导航栏
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .align(Alignment.BottomCenter)
                // 阴影稍微柔和一点
                .shadow(
                    elevation = 12.dp,
                    spotColor = Color.Black.copy(alpha = 0.2f),
                    shape = object : Shape {
                        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
                            return Outline.Generic(getCutoutPath(size, animatedOffsetX, with(density) { CutoutRadius.toPx() }))
                        }
                    }
                )
        ) {
            val path = getCutoutPath(size, animatedOffsetX, CutoutRadius.toPx())

            // A. 底层：半透明黑背景
            drawPath(path = path, color = BarGlass, style = Fill)

            // B. 中层：噪点纹理 (这就是“磨砂”的来源)
            // 使用 SRC_OVER 混合模式，低透明度覆盖
            drawPath(path = path, brush = noiseBrush, alpha = 0.2f, style = Fill)

            // C. 顶层：漫反射光感 (让它看起来不像平面的塑料)
            drawPath(path = path, brush = highlightGradient, style = Fill)

            // D. 描边：玻璃边缘反光
            drawPath(
                path = path,
                color = BarStroke,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // 2) 悬浮球 (保持不变)
        GlassBubble(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = bottomInset)
                .offset(y = -(BarShapeHeight - CutoutRadius) + 4.dp)
                .graphicsLayer { translationX = animatedOffsetX - CutoutRadius.toPx() }
                .size(CutoutRadius * 2),
            fill = BubbleGradient,
            icon = items[selectedIndex].selectedIcon
        )

        // 3) 图标层 (保持不变)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .padding(bottom = bottomInset)
                .align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, item ->
                val isSelected = index == selectedIndex
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onNavigate(item.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isSelected) {
                        Spacer(modifier = Modifier.height(30.dp))
                    } else {
                        Icon(
                            imageVector = item.unselectedIcon,
                            contentDescription = item.title,
                            tint = UnselectedTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        color = if (isSelected) SelectedTint else UnselectedTint
                    )
                }
            }
        }
    }
}

// --- 辅助函数：生成噪点 Shader ---
@Composable
fun rememberNoiseBrush(): ShaderBrush {
    return remember {
        val size = 64 // 噪点图大小，越小性能越好，64足够了
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val random = java.util.Random()

        for (i in pixels.indices) {
            // 生成随机的灰度噪点
            // alpha 值控制噪点的“颗粒感”强弱，30~50 左右比较像磨砂
            val alpha = (random.nextInt(40) + 10)
            val color = android.graphics.Color.argb(alpha, 255, 255, 255)
            pixels[i] = color
        }
        bitmap.setPixels(pixels, 0, size, 0, 0, size, size)

        val shader = BitmapShader(bitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        ShaderBrush(shader)
    }
}

// --- 路径计算 (保持你的优化版) ---
private fun getCutoutPath(size: Size, cutoutCenterX: Float, cutoutRadius: Float): Path {
    val path = Path()
    val height = size.height
    val width = size.width
    val sideOffset = cutoutRadius * 1.3f
    val startX = cutoutCenterX - sideOffset

    path.moveTo(0f, 0f)
    path.lineTo(startX, 0f)
    path.cubicTo(
        cutoutCenterX - cutoutRadius, 0f,
        cutoutCenterX - (cutoutRadius * 0.5f), cutoutRadius,
        cutoutCenterX, cutoutRadius
    )
    path.cubicTo(
        cutoutCenterX + (cutoutRadius * 0.5f), cutoutRadius,
        cutoutCenterX + cutoutRadius, 0f,
        cutoutCenterX + sideOffset, 0f
    )
    path.lineTo(width, 0f)
    path.lineTo(width, height)
    path.lineTo(0f, height)
    path.close()
    return path
}

// 悬浮球 GlassBubble
@Composable
private fun GlassBubble(
    modifier: Modifier = Modifier,
    fill: Brush,
    icon: ImageVector,
    iconSize: androidx.compose.ui.unit.Dp = 34.dp,
    iconTint: Color = Color.White
) {
    Box(
        modifier = modifier
            .shadow(10.dp, CircleShape, clip = false)
            .clip(CircleShape)
            .background(fill)
            .drawWithCache {
                val strokeW = 1.2.dp.toPx()
                val highlight = Brush.radialGradient(
                    0.0f to Color.White.copy(alpha = 0.5f),
                    0.55f to Color.Transparent,
                    center = Offset(size.width * 0.28f, size.height * 0.22f),
                    radius = size.minDimension * 0.9f
                )
                val rim = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.35f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                val innerShadow = Brush.radialGradient(
                    0.0f to Color.Transparent,
                    0.70f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.22f),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.minDimension * 0.55f
                )
                val glare = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.18f),
                        Color.Transparent
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )
                onDrawWithContent {
                    drawContent()
                    drawCircle(brush = innerShadow)
                    drawCircle(brush = highlight)
                    withTransform({
                        rotate(degrees = -22f, pivot = center)
                        translate(left = -size.width * 0.15f, top = size.height * 0.05f)
                    }) {
                        drawRect(brush = glare, size = Size(size.width * 1.3f, size.height * 0.32f))
                    }
                    drawCircle(brush = rim, style = Stroke(width = strokeW))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(iconSize))
    }
}