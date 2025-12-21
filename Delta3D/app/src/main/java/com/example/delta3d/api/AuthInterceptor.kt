package com.example.delta3d.api

import com.example.delta3d.utils.AuthEvents
import okhttp3.Interceptor
import okhttp3.Response

//检查401
class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (response.code == 401) {
            // 触发全局登出事件
            AuthEvents.triggerUnauthorized()


        }

        return response
    }
}