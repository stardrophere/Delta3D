package com.example.delta3d.api
import com.google.gson.annotations.SerializedName

// 注册请求体
data class RegisterRequest(
    val username: String,
    val password: String
)

// 注册响应
data class RegisterResponse(
    val id: Int,
    val username: String,
    @SerializedName("avatar_url") val avatarUrl: String?
)

// 登录响应
data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String
)
