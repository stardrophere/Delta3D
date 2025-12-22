package com.example.delta3d.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.delta3d.api.AssetCard
import com.example.delta3d.api.RetrofitClient
import com.example.delta3d.config.AppConfig

// --- 样式常量 ---
private val ListCardShape = RoundedCornerShape(16.dp)
private val ListCardBackground = Color(0xFF1E1E1E).copy(alpha = 0.8f)
private val GlassBorder = Brush.verticalGradient(
    colors = listOf(Color.White.copy(0.15f), Color.White.copy(0.05f))
)

val TagPalette = listOf(
    Color(0xFF64B5F6),
    Color(0xFF81C784),
    Color(0xFFFFB74D),
    Color(0xFFE57373),
    Color(0xFFBA68C8),
    Color(0xFF4DD0E1),
    Color(0xFFFFD54F),
    Color(0xFFA1887F)
)

@Stable
class TagColorBinder(
    private val palette: List<Color>
) {
    private val map = mutableStateMapOf<String, Color>()
    private var nextIndex = 0

    fun colorFor(tag: String): Color {
        return map.getOrPut(tag) {
            val idx = nextIndex++
            if (idx < palette.size) palette[idx] else generateExtraColor(idx - palette.size)
        }
    }

    private fun generateExtraColor(i: Int): Color {
        val hue = (i * 137.508f) % 360f
        return Color.hsl(hue = hue, saturation = 0.55f, lightness = 0.65f)
    }
}


@Composable
fun ProductListView(
    dataList: List<AssetCard>,
    bottomPadding: Dp,
    onItemClick: (Int) -> Unit,
    onCollectClick: (Int) -> Unit
) {

    val tagColorBinder = remember { TagColorBinder(TagPalette) }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = bottomPadding + 100.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(dataList, key = { it.id }) { item ->
            ProductListItem(
                item = item,
                tagColorBinder = tagColorBinder,
                onClick = { onItemClick(item.id) },
                onCollectClick = { onCollectClick(item.id) }
            )
        }
    }
}


@Composable
fun ProductListItem(
    item: AssetCard,
    tagColorBinder: TagColorBinder,
    onClick: () -> Unit,
    onCollectClick: () -> Unit
) {
    val fullImageUrl = remember(item.coverUrl) {
        item.coverUrl?.let { url ->
            val base = AppConfig.currentBaseUrl.removeSuffix("/")
            val path = url.removePrefix("/").removeSuffix("/")
            "$base/$path/images/0001.jpg"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .clip(ListCardShape)
            .border(0.5.dp, GlassBorder, ListCardShape)
            .background(ListCardBackground)
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxSize()) {

            // 左侧封面
            Box(
                modifier = Modifier
                    .width(115.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(0.3f))
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(fullImageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // 右侧内容区域
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .padding(start = 16.dp, end = 12.dp)
            ) {

                // 中间：标题、描述、标签/日期
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = item.title,
                            color = Color.White.copy(0.95f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.description ?: "暂无描述",
                            color = Color.White.copy(0.5f),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 标签栏
                    if (item.tags.isNotEmpty()) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(item.tags, key = { it }) { tag ->
                                TagCapsule(
                                    text = tag,
                                    baseColor = tagColorBinder.colorFor(tag)
                                )
                            }
                        }
                    } else {
                        // 没标签时显示日期
                        Text(
                            text = item.createdAt.substringBefore("T"),
                            color = Color.White.copy(0.3f),
                            fontSize = 11.sp
                        )
                    }
                }

                // 收藏按钮
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(start = 4.dp)
                ) {
                    CollectButton(
                        isCollected = item.isCollected,
                        onClick = onCollectClick
                    )
                }
            }
        }
    }
}

@Composable
fun TagCapsule(text: String, baseColor: Color) {
    Surface(
        color = baseColor.copy(alpha = 0.15f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(0.5.dp, baseColor.copy(alpha = 0.3f)),
    ) {
        Text(
            text = text,
            color = baseColor.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// onClick 参数
@Composable
fun CollectButton(isCollected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable(
                indication = ripple(color = Color.White),
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }, //响应点击
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isCollected) Icons.Filled.Star else Icons.Outlined.Star,
            contentDescription = "Collect",
            tint = if (isCollected) Color(0xFFFFC107) else Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(26.dp)
        )
    }
}