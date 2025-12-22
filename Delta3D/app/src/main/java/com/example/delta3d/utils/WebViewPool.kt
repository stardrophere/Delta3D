package com.example.delta3d.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.MutableContextWrapper
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import java.util.LinkedList

/**
 * WebView 预热池
 * 在Application启动时预热，减少首次加载卡顿
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

        // 在后台线程预热
        Thread {
            try {
                // 预热WebView内核
                val tempContext = MutableContextWrapper(context.applicationContext)
                val webView = createWebView(tempContext)
                
                synchronized(pool) {
                    pool.add(webView)
                }
                Log.d(TAG, "WebView prewarmed")
            } catch (e: Exception) {
                Log.e(TAG, "Prewarm failed", e)
            }
        }.start()
    }

    /**
     * 获取预热的WebView，如果没有则创建新的
     */
    fun obtain(context: Context): WebView {
        synchronized(pool) {
            if (pool.isNotEmpty()) {
                val webView = pool.removeFirst()
                // 更换Context
                (webView.context as? MutableContextWrapper)?.baseContext = context
                Log.d(TAG, "Using prewarmed WebView")
                return webView
            }
        }
        Log.d(TAG, "Creating new WebView")
        return createWebView(context)
    }

    /**
     * 回收WebView（可选）
     */
    fun recycle(webView: WebView) {
        try {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            (webView.parent as? ViewGroup)?.removeView(webView)
            
            synchronized(pool) {
                if (pool.size < 2) {
                    pool.add(webView)
                    Log.d(TAG, "WebView recycled")
                } else {
                    webView.destroy()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recycle failed", e)
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
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(false)
                mediaPlaybackRequiresUserGesture = false
            }
            
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        }
    }

    /**
     * 清理所有
     */
    fun clear() {
        synchronized(pool) {
            pool.forEach { it.destroy() }
            pool.clear()
        }
    }
}
