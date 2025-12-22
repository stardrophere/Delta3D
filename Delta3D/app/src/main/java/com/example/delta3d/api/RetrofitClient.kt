package com.example.delta3d.api

import com.example.delta3d.config.AppConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// 动态主机拦截器
class HostSelectionInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()

        // 获取当前应该使用的 Base URL
        val currentBaseUrl = AppConfig.currentBaseUrl.toHttpUrlOrNull()

        if (currentBaseUrl != null) {
            val newUrl = request.url.newBuilder()
                .scheme(currentBaseUrl.scheme)
                .host(currentBaseUrl.host)
                .port(currentBaseUrl.port)
                .build()

            request = request.newBuilder()
                .url(newUrl)
                .build()
        }

        return chain.proceed(request)
    }
}

object RetrofitClient {
    const val BASE_URL = AppConfig.WAN_URL

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val hostSelectionInterceptor = HostSelectionInterceptor()

    val client = OkHttpClient.Builder()
        .addInterceptor(hostSelectionInterceptor) // 动态 URL 拦截器
        .addInterceptor(loggingInterceptor)
        .addInterceptor(AuthInterceptor())
        .pingInterval(15, TimeUnit.SECONDS) //心跳包

        .connectTimeout(10, TimeUnit.SECONDS) // 设置超时
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}