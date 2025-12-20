package com.example.delta3d.ui.screens.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


private val AccentColor = Color(0xFF64FFDA)
private val GoldColor = Color(0xFFFFD700)
private val DeepDark = Color(0xFF121212)

@Composable
fun PlanSettingsScreen(onBack: () -> Unit) {
    // 浮动动画
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDark)
    ) {
        // 动态背景光晕
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(AccentColor.copy(0.15f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.2f),
                    radius = 500f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(GoldColor.copy(0.1f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.8f),
                    radius = 600f
                )
            )
        }

        // 顶部导航栏
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White.copy(0.1f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White)
            }
        }

        //主要内容卡片
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .offset(y = (-20).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 图标
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        Brush.linearGradient(listOf(AccentColor, GoldColor)),
                        CircleShape
                    )
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DeepDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Diamond,
                        null,
                        tint = GoldColor,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 标题
            Text(
                text = "EARLY ADOPTER",
                style = MaterialTheme.typography.labelSmall,
                color = AccentColor,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Premium Plan",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(32.dp))

            // 权益列表
            FeatureItem(text = "Unlimited Downloads", icon = Icons.Rounded.AutoAwesome)
//            FeatureItem(text = "4K Model Preview", icon = Icons.Rounded.AutoAwesome)
            FeatureItem(text = "Priority Support", icon = Icons.Rounded.AutoAwesome)

            Spacer(Modifier.height(32.dp))

            // 分割线
            Divider(
                color = Color.White.copy(0.1f),
                modifier = Modifier.padding(horizontal = 40.dp)
            )

            Spacer(Modifier.height(32.dp))

            //  彩蛋
            Text(
                text = buildAnnotatedString {
                    append("To be honest, we haven't built a payment system yet. \n\n")
                    append("But more importantly, ")
                    withStyle(SpanStyle(color = AccentColor, fontWeight = FontWeight.Bold)) {
                        append("we are just glad you are here. ")
                    }
                    append("\nDelta3D is a dream driven by passion, not just profit. As an early supporter, you get everything on the house.")
                },
                color = Color.White.copy(0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(40.dp))

            // 价格按钮
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentColor
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$0.00 / Forever",
                        color = Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Rounded.Favorite,
                        null,
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "Thank you for creating with us.",
                color = Color.White.copy(0.3f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun FeatureItem(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Rounded.Check,
            null,
            tint = AccentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White.copy(0.9f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium
        )
    }
}