package com.example.delta3d.ui.screens.onboarding

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.delta3d.R
import com.example.delta3d.utils.WebViewPool
import kotlinx.coroutines.delay

private const val TAG = "Delta3DLogoSplash"
private const val LOGO_URL = "file:///android_asset/delta3d_logo.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Delta3DLogoSplash(
    onAnimationComplete: () -> Unit
) {
    var isFinished by remember { mutableStateOf(false) }
    val onCompleteCallback = remember { { isFinished = true } }

    val context = LocalContext.current
    val splashColor = ContextCompat.getColor(context, R.color.splash_background)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.splash_background))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                // 复用池子
                WebViewPool.obtain(ctx).apply {
                    setBackgroundColor(splashColor)

                    // 重新注入 JS 接口
                    addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onAnimationComplete() {
                                Log.d(TAG, "JS -> onAnimationComplete()")
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    onCompleteCallback()
                                }
                            }
                        },
                        "Android"
                    )

                    webChromeClient = WebChromeClient()

                    loadUrl(LOGO_URL)
                    Log.d(TAG, "Logo 页开始加载")
                }
            },
            onRelease = { webView ->
                // 移除接口
                webView.removeJavascriptInterface("Android")
                WebViewPool.recycle(webView)
            }
        )
    }

    // 监听完成状态
    LaunchedEffect(isFinished) {
        if (isFinished) {
            Log.d(TAG, "动画完成，执行跳转")
            onAnimationComplete()
        }
    }

    // 兜底超时机制
    LaunchedEffect(Unit) {
        delay(15000)
        if (!isFinished) {
            Log.w(TAG, "Splash 超时，强制跳转")
            isFinished = true
        }
    }
}