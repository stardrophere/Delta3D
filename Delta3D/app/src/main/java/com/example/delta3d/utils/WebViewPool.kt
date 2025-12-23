package com.example.delta3d.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.os.Looper
import android.os.MessageQueue
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import com.example.delta3d.data.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.LinkedList

/**
 * WebView 预热池
 * 使用 IdleHandler 避免主线程卡顿
 * 提前加载 HTML
 * 只在首次启动时预热
 */
@SuppressLint("SetJavaScriptEnabled")
object WebViewPool {

    private const val TAG = "WebViewPool"
    private val pool = LinkedList<WebView>()
    private var isInitialized = false

    /**
     * 在 Application.onCreate() 中调用
     */
    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val store = TokenStore(context)
                val isFirstLaunch = store.isFirstLaunchFlow.first()

                if (isFirstLaunch) {
                    Log.d(TAG, "首次启动: 注册 WebView 预热任务")
                    registerIdleHandler(context)
                } else {
                    Log.d(TAG, "非首次启动: 跳过 WebView 预热")
                }
            } catch (e: Exception) {
                Log.e(TAG, "检查首次启动状态失败", e)
                registerIdleHandler(context)
            }
        }
    }

    private fun registerIdleHandler(context: Context) {
        Looper.myQueue().addIdleHandler(object : MessageQueue.IdleHandler {
            override fun queueIdle(): Boolean {
                Log.d(TAG, "主线程空闲，开始执行预加载...")
                // 预加载两个 WebView
                prepareWebView(context, "file:///android_asset/delta_tree.html")
                prepareWebView(context, "file:///android_asset/delta3d_logo.html")
                return false
            }
        })
    }

    private fun prepareWebView(context: Context, url: String) {
        try {
            val tempContext = MutableContextWrapper(context.applicationContext)
            val webView = createWebView(tempContext)
            // 提前加载 URL
            webView.loadUrl(url)

            synchronized(pool) {
                pool.add(webView)
            }
            Log.d(TAG, "WebView 预加载完成: $url")
        } catch (e: Exception) {
            Log.e(TAG, "WebView 预加载失败", e)
        }
    }

    /**
     * 获取预热的WebView
     */
    fun obtain(context: Context): WebView {
        synchronized(pool) {
            if (pool.isNotEmpty()) {
                val webView = pool.removeFirst()
                (webView.context as? MutableContextWrapper)?.baseContext = context
                Log.d(TAG, "命中缓存池，获取 WebView")
                return webView
            }
        }
        // 兜底
        Log.w(TAG, "缓存池为空，新建 WebView")
        return createWebView(context)
    }

    /**
     * 回收 WebView
     */
    fun recycle(webView: WebView) {
        try {
            val contextWrapper = webView.context as? MutableContextWrapper
            contextWrapper?.baseContext = webView.context.applicationContext

            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            (webView.parent as? ViewGroup)?.removeView(webView)

            synchronized(pool) {
                // 如果当前不是首次启动，接销毁
                if (pool.size < 3) {
                    pool.add(webView)
                    Log.d(TAG, "WebView 已回收")
                } else {
                    webView.destroy()
                    Log.d(TAG, "WebView 已销毁")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "回收失败", e)
        }
    }

    private fun createWebView(context: Context): WebView {
        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true

                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true


                mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                mediaPlaybackRequiresUserGesture = false
            }

            // 保持硬件加速开启
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        }
    }

    fun clear() {
        synchronized(pool) {
            pool.forEach { it.destroy() }
            pool.clear()
        }
    }
}