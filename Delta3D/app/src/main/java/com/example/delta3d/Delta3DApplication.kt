package com.example.delta3d
import com.example.delta3d.utils.WebViewPool
import android.app.Application

class Delta3DApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 预热WebView，减少首次加载卡顿
        WebViewPool.init(this)
    }
}