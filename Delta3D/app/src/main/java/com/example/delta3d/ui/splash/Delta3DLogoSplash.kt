package com.example.delta3d.ui.screens.splash

import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

private const val TAG = "Delta3DLogoSplash"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Delta3DLogoSplash(
    onAnimationComplete: () -> Unit
) {
    var isFinished by remember { mutableStateOf(false) }
    var isPageLoaded by remember { mutableStateOf(false) }
    val onCompleteCallback = remember { { isFinished = true } }
    val onPageLoadedCallback = remember { { isPageLoaded = true } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F2027))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    setBackgroundColor(android.graphics.Color.parseColor("#0F2027"))

                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false

                    WebView.setWebContentsDebuggingEnabled(true)

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        allowFileAccess = true
                        allowContentAccess = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        setSupportZoom(false)
                        mediaPlaybackRequiresUserGesture = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                    }

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

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.d(TAG, "Page loaded: $url")
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onPageLoadedCallback()
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            Log.e(TAG, "WebView Error: ${error?.errorCode} - ${error?.description}")
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(msg: ConsoleMessage?): Boolean {
                            Log.d(TAG, "[JS] ${msg?.lineNumber()}: ${msg?.message()}")
                            return true
                        }
                    }

                    loadUrl("file:///android_asset/delta3d_logo.html")
                    Log.d(TAG, "Loading animation...")
                }
            }
        )
    }

    LaunchedEffect(isFinished) {
        if (isFinished) {
            Log.d(TAG, "Animation complete, navigating...")
            onAnimationComplete()
        }
    }

    // 备用计时器
    LaunchedEffect(isPageLoaded) {
        if (isPageLoaded && !isFinished) {
            delay(16000)
            if (!isFinished) {
                Log.w(TAG, "Backup timer triggered")
                isFinished = true
            }
        }
    }
}