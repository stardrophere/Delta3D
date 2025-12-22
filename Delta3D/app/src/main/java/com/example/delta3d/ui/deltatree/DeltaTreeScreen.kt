package com.example.delta3d.ui.deltatree

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "DeltaTree"

/**
 * Delta Tree 互动页面 - 稳定版（无摄像头）
 */
@Composable
fun DeltaTreeScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d(TAG, "DeltaTreeScreen 开始渲染")
    
    var webView by remember { mutableStateOf<WebView?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F2027))
    ) {
        // WebView
        AndroidView(
            factory = { ctx ->
                Log.d(TAG, "创建 WebView")
                WebView(ctx).apply {
                    setupWebView()
                    Log.d(TAG, "开始加载 HTML")
                    loadUrl("file:///android_asset/delta_tree.html")
                    webView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 关闭按钮
        CloseButton(
            onClick = {
                Log.d(TAG, "关闭按钮被点击")
                onDismiss()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            Log.d(TAG, "WebView 销毁")
            webView?.stopLoading()
            webView?.destroy()
        }
    }
}

@Composable
private fun CloseButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "关闭",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupWebView() {
    Log.d(TAG, "配置 WebView")
    
    WebView.setWebContentsDebuggingEnabled(true)
    setBackgroundColor(android.graphics.Color.parseColor("#0F2027"))

    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        mediaPlaybackRequiresUserGesture = false
        allowFileAccess = true
        allowContentAccess = true
        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        loadWithOverviewMode = true
        useWideViewPort = true
    }

    webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d(TAG, "页面加载完成: $url")
        }
        
        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            Log.e(TAG, "页面加载错误: $errorCode - $description - $failingUrl")
        }
    }

    webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            consoleMessage?.let {
                Log.d(TAG, "JS [${it.lineNumber()}]: ${it.message()}")
            }
            return true
        }
    }

    // 启用硬件加速
    Log.d(TAG, "WebView 配置完成")
}
