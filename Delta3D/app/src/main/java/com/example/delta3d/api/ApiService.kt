package com.example.delta3d.api

import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*


// 🟢 新增：收藏状态响应
data class ToggleResponse(
    val is_active: Boolean,
    val new_count: Int
)

interface ApiService {

    // 🟢 窗口一：注册 (JSON 格式)
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    // 🔵 窗口二：登录 (表单格式)
    @FormUrlEncoded
    @POST("api/v1/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): LoginResponse

    // 🟢 窗口三：获取模型列表
    @GET("api/v1/assets/me")
    suspend fun getAssets(
        @Header("Authorization") token: String
    ): List<AssetCard>

    // 🟢 窗口四：获取单个模型详情
    @GET("api/v1/assets/{id}")
    suspend fun getAssetDetail(
        @Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: Int
    ): AssetDetail

    // 🟢 新增：收藏切换接口
    @POST("api/v1/assets/{id}/collect")
    suspend fun toggleCollect(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ToggleResponse

    // 🟢 新增：上传模型接口
    // 注意：对应后端 file: UploadFile, title: str, description: str...
    @Multipart
    @POST("api/v1/assets/upload")
    suspend fun uploadAsset(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("tags") tags: RequestBody?,
        @Part("remark") remark: RequestBody?
    ): AssetCard

    // 1. 开启推流
    @POST("api/v1/stream/start/{assetId}") // 注意核对你的后端路径前缀，如果是 /stream/start 就去掉 api/v1
    suspend fun startStream(
        @Header("Authorization") token: String,
        @Path("assetId") assetId: Int
    ): StreamStatus

    // 2. 停止推流
    @POST("api/v1/stream/stop")
    suspend fun stopStream(
        @Header("Authorization") token: String
    ): Map<String, String>

    // 3. 发送控制指令 (旋转/平移/缩放)
    @POST("api/v1/stream/control")
    suspend fun sendControl(
        @Header("Authorization") token: String,
        @Body cmd: ControlCommand
    ): Map<String, Any>
}