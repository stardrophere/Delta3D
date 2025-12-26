package com.example.delta3d.ui.screens.home

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

// 样式常量
private val CardShape = RoundedCornerShape(24.dp)
private val GlassBorder = Brush.verticalGradient(
    colors = listOf(Color.White.copy(alpha = 0.3f), Color.White.copy(alpha = 0.05f))
)

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun HomeTreeCard(
    modifier: Modifier = Modifier,
    // 收缩进度
    expansionFraction: Float = 1f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .border(1.dp, GlassBorder, CardShape)
            .background(Color(0xFF0F2027))
    ) {
        // WebView 层
        Box(modifier = Modifier.alpha(0.3f + (0.7f * expansionFraction))) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        setBackgroundColor(0xFF0F2027.toInt())
                        setOnTouchListener { v, event ->
                            when (event.action) {
                                MotionEvent.ACTION_DOWN -> v.parent.requestDisallowInterceptTouchEvent(
                                    true
                                )

                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.parent.requestDisallowInterceptTouchEvent(
                                    false
                                )
                            }
                            false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                val jsCode = """
                                    document.getElementById('welcome-overlay').style.display = 'none';
                                    document.getElementById('loading').style.display = 'none';
                                    document.getElementById('controls').style.opacity = '0';
                                    document.getElementById('hint').style.display = 'none';
                                    if(typeof state !== 'undefined') {
                                        state.cameraZ = 45; 
                                        state.targetCameraZ = 45;
                                    }
                                """.trimIndent()
                                view?.evaluateJavascript(jsCode, null)
                            }
                        }
                        loadUrl("file:///android_asset/delta_tree.html")
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 渐变遮罩层
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = (1f - expansionFraction) * 0.5f))
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(0.7f)
                    )
                )
        )

        // 内容层
        content()
    }
}