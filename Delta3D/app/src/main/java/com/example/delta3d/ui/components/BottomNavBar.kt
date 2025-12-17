package com.example.delta3d.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform

// --- 1) 玻璃拟态配色（和登录/主界面更搭） ---
private val BarGlass = Color.Black.copy(alpha = 0.28f)              // 导航栏主体玻璃
private val BarStroke = Color.White.copy(alpha = 0.14f)             // 导航栏描边
private val UnselectedTint = Color.White.copy(alpha = 0.65f)        // 未选中白色半透明
private val SelectedTint = Color.White                              // 选中纯白
private val BubbleGlass = Color.White.copy(alpha = 0.14f)           // 外圈玻璃球填充
private val BubbleStroke = Color.White.copy(alpha = 0.22f)          // 外圈玻璃球描边

// 选中“内核”的渐变：把这里换成 AnimatedGradientBackground 的主色最搭
private val BubbleGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xFF7C4DFF),  // TODO: 换成你的背景渐变色 A
        Color(0xFF00E5FF)   // TODO: 换成你的背景渐变色 B
    )
)

// --- 2) 尺寸常量 ---
private val BarShapeHeight = 65.dp
private val CircleRadius = 26.dp
private val CutoutRadius = 32.dp

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

        // 1) 玻璃导航栏（带凹槽）
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight)
                .align(Alignment.BottomCenter)
                .shadow(
                    elevation = 10.dp,
                    shape = object : Shape {
                        override fun createOutline(
                            size: Size,
                            layoutDirection: LayoutDirection,
                            density: Density
                        ): Outline {
                            return Outline.Generic(
                                getCutoutPath(size, animatedOffsetX, with(density) { CutoutRadius.toPx() })
                            )
                        }
                    }
                )
        ) {
            val path = getCutoutPath(size, animatedOffsetX, CutoutRadius.toPx())

            // 主体：半透明黑（透出背景渐变）
            drawPath(path = path, color = BarGlass, style = Fill)

            // 细描边：白色半透明，增强“玻璃边缘”
            drawPath(
                path = path,
                color = BarStroke,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // 2) 选中“浮动球”（外圈玻璃 + 内圈渐变）
//        Box(
//            modifier = Modifier
//                .align(Alignment.BottomStart)
//                .padding(bottom = bottomInset)
//                .offset(y = -(BarShapeHeight - CutoutRadius) + 4.dp)
//                .graphicsLayer {
//                    translationX = animatedOffsetX - CutoutRadius.toPx()
//                }
//                .size(CutoutRadius * 2)
//        ) {
//
//            // (B) 直接用渐变填满整个球体（不再有外圈）
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .shadow(elevation = 6.dp, shape = CircleShape)
//                    .clip(CircleShape)
//                    .background(BubbleGradient),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    imageVector = items[selectedIndex].selectedIcon,
//                    contentDescription = null,
//                    tint = SelectedTint,
//                    modifier = Modifier.size(34.dp) // 图标放大：28 -> 34（你想更大就继续加）
//                )
//            }
//        }
        GlassBubble(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = bottomInset)
                .offset(y = -(BarShapeHeight - CutoutRadius) + 4.dp)
                .graphicsLayer { translationX = animatedOffsetX - CutoutRadius.toPx() }
                .size(CutoutRadius * 2),
            fill = BubbleGradient, // 你现有的渐变
            icon = items[selectedIndex].selectedIcon,
            iconSize = 36.dp // 图标想多大就多大
        )


        // 3) 未选中图标 + 文案（统一白色体系）
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

                // 顶部偏左的“高光”
                val highlight = Brush.radialGradient(
                    0.0f to Color.White.copy(alpha = 0.5f),
                    0.55f to Color.Transparent,
                    center = Offset(size.width * 0.28f, size.height * 0.22f),
                    radius = size.minDimension * 0.9f
                )

                // 边缘亮边（玻璃边缘反光）
                val rim = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.55f),
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.35f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(size.width, size.height)
                )

                // 轻微内阴影（让球更立体）
                val innerShadow = Brush.radialGradient(
                    0.0f to Color.Transparent,
                    0.70f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.22f),
                    center = Offset(size.width * 0.5f, size.height * 0.5f),
                    radius = size.minDimension * 0.55f
                )

                // 斜向“光带”
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

                    // 内阴影
                    drawCircle(brush = innerShadow)

                    // 高光
                    drawCircle(brush = highlight)

                    // 光带（旋转一点更像玻璃反射）
                    withTransform({
                        rotate(degrees = -22f, pivot = center)
                        translate(left = -size.width * 0.15f, top = size.height * 0.05f)
                    }) {
                        drawRect(
                            brush = glare,
                            size = Size(size.width * 1.3f, size.height * 0.32f)
                        )
                    }

                    // 亮边描边
                    drawCircle(brush = rim, style = Stroke(width = strokeW))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

private fun getCutoutPath(size: Size, cutoutCenterX: Float, cutoutRadius: Float): Path {
    val path = Path()
    val height = size.height
    val width = size.width

    path.moveTo(0f, 0f)

    // --- 修改点 1：调整开口宽度 ---
    // 原来的 1.5f 有点太宽，显得曲线很趴。
    // 改为 1.3f 左右会更紧凑，贴合球体。
    val sideOffset = cutoutRadius * 1.3f
    val startX = cutoutCenterX - sideOffset
    path.lineTo(startX, 0f)

    // --- 修改点 2：修正贝塞尔曲线控制点 ---
    // 关键修正：底部控制点的 X 轴偏移量。
    // 原理：为了画出完美的圆弧，控制点距离终点不能是半径的 100%，而应该是约 55%。
    // 之前是 (cutoutCenterX - cutoutRadius)，现在改为 (cutoutCenterX - cutoutRadius * 0.5f)

    // 左半边曲线
    path.cubicTo(
        cutoutCenterX - cutoutRadius, 0f,              // 控制点1：顶部切线（保持不变，保证平滑过渡）
        cutoutCenterX - (cutoutRadius * 0.5f), cutoutRadius, // 控制点2：【关键】向内收缩，防止底部变平
        cutoutCenterX, cutoutRadius                    // 终点：最底端
    )

    // 右半边曲线（对称操作）
    path.cubicTo(
        cutoutCenterX + (cutoutRadius * 0.5f), cutoutRadius, // 控制点1：【关键】向内收缩
        cutoutCenterX + cutoutRadius, 0f,              // 控制点2
        cutoutCenterX + sideOffset, 0f                 // 终点
    )

    path.lineTo(width, 0f)
    path.lineTo(width, height)
    path.lineTo(0f, height)
    path.close()

    return path
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewFluidBottomNavBar() {
    var currentRoute by remember { mutableStateOf("home") }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B1020)) // 暗色背景预览更接近你主界面
    ) {
        BottomNavBar(
            currentRoute = currentRoute,
            onNavigate = { newRoute -> currentRoute = newRoute }
        )
    }
}
