package com.example.delta3d.ui.screens.onboarding

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.delta3d.R
import com.example.delta3d.utils.WebViewPool

private const val TAG = "DeltaTree"
private const val TREE_URL = "file:///android_asset/delta_tree.html"

@Composable
fun DeltaTreeScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val splashColor = ContextCompat.getColor(context, R.color.splash_background)

    // 1. 记住 WebView 实例，以便在 DisposableEffect 中使用
    // 注意：这里不要直接创建，而是通过 remember 或者在 AndroidView 中获取。
    // 但为了生命周期绑定，我们通常在 AndroidView 的 factory 中处理，或者在这里定义一个变量
    // 由于 WebViewPool 的特殊性，我们在 AndroidView 内部处理更安全。

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.splash_background))
    ) {
        AndroidView(
            factory = { ctx ->
                // 从池子抽取实例
                WebViewPool.obtain(ctx).apply {
                    setupWebView(splashColor)

                    // 检查 URL 是否正确
                    if (this.url != TREE_URL) {
                        Log.d(TAG, "未命中预加载或 URL 不匹配，重新加载")
                        loadUrl(TREE_URL)
                    } else {
                        Log.d(TAG, "命中预加载，URL: $url")
                        // 2. 关键修复：即使命中了预加载，页面可能处于“暂停”状态
                        // 强制重新加载 JS 计时器和渲染
                        onResume()
                        resumeTimers()
                    }
                }
            },
            update = { webView ->
                // 3. 每次重组或更新时，确保它是活跃的
                webView.onResume()
                webView.resumeTimers()
            },
            onRelease = { webView ->
                Log.d(TAG, "回收 WebView 到池中")
                WebViewPool.recycle(webView)
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
private fun WebView.setupWebView(backgroundColor: Int) {
    setBackgroundColor(backgroundColor)

    // 确保这些设置在复用时也生效
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccessFromFileURLs = true
    settings.allowUniversalAccessFromFileURLs = true

    webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d(TAG, "页面加载完成: $url")
            view?.evaluateJavascript("console.log('JS 注入测试: 页面已加载');", null)
        }

        // 捕获资源加载错误
        override fun onReceivedError(
            view: WebView?,
            request: android.webkit.WebResourceRequest?,
            error: android.webkit.WebResourceError?
        ) {
            super.onReceivedError(view, request, error)

            Log.e(
                TAG,
                "资源加载失败: ${request?.url}, 错误码: ${error?.errorCode}, 描述: ${error?.description}"
            )
        }

        // HTTP 错误
        override fun onReceivedHttpError(
            view: WebView?,
            request: android.webkit.WebResourceRequest?,
            errorResponse: android.webkit.WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            Log.e(TAG, "HTTP 错误: ${request?.url}, 状态码: ${errorResponse?.statusCode}")
        }
    }

    webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            Log.d(
                "DeltaTree_JS",
                "${consoleMessage?.message()} -- line ${consoleMessage?.lineNumber()}"
            )
            return false
        }
    }
}