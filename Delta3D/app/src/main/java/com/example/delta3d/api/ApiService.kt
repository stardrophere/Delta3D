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


// 收藏状态响应
data class ToggleResponse(
    val is_active: Boolean,
    val new_count: Int
)

interface ApiService {

    // 注册
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    // 登录
    @FormUrlEncoded
    @POST("api/v1/auth/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): LoginResponse

    // 获取模型列表
    @GET("api/v1/assets/me")
    suspend fun getAssets(
        @Header("Authorization") token: String
    ): List<AssetCard>

    // 获取单个模型详情
    @GET("api/v1/assets/{id}")
    suspend fun getAssetDetail(
        @Header("Authorization") token: String,
        @retrofit2.http.Path("id") id: Int
    ): AssetDetail

    // 藏切换接口
    @POST("api/v1/assets/{id}/collect")
    suspend fun toggleCollect(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): ToggleResponse

    // 上传模型接口
    @Multipart
    @POST("api/v1/assets/upload")
    suspend fun uploadAsset(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("title") title: RequestBody,
        @Part("description") description: RequestBody?,
        @Part("tags") tags: RequestBody?,
        @Part("remark") remark: RequestBody?,
        @Part("estimated_time") estimatedTime: RequestBody?
    ): AssetCard

    // 开启推流
    @POST("api/v1/stream/start/{assetId}")
    suspend fun startStream(
        @Header("Authorization") token: String,
        @Path("assetId") assetId: Int
    ): StreamStatus

    // 停止推流
    @POST("api/v1/stream/stop")
    suspend fun stopStream(
        @Header("Authorization") token: String
    ): Map<String, String>

    //发送控制指令 (旋转/平移/缩放)
    @POST("api/v1/stream/control")
    suspend fun sendControl(
        @Header("Authorization") token: String,
        @Body cmd: ControlCommand
    ): Map<String, Any>

    // 社区帖子列表
    @GET("api/v1/posts/community")
    suspend fun getCommunityPosts(
        @Header("Authorization") token: String
    ): List<PostCard>

    // 发布帖子
    @POST("api/v1/posts/publish")
    suspend fun publishPost(
        @Header("Authorization") token: String,
        @Body request: PostCreateRequest
    ): PostCard

    // 点赞帖子
    @POST("api/v1/posts/{post_id}/like")
    suspend fun likePost(
        @Header("Authorization") token: String,
        @Path("post_id") postId: Int
    ): ToggleResponse

    // 收藏帖子
    @POST("api/v1/posts/{post_id}/collect")
    suspend fun collectPost(
        @Header("Authorization") token: String,
        @Path("post_id") postId: Int
    ): ToggleResponse

    //关注
    @POST("api/v1/user/{id}/follow")
    suspend fun followUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): ToggleResponse

    // 获取帖子详情 (包含模型信息、评论、交互状态)
    @GET("api/v1/posts/{post_id}")
    suspend fun getPostDetail(
        @Header("Authorization") token: String,
        @Path("post_id") postId: Int
    ): PostDetail

    @GET("api/v1/user/me")
    suspend fun getMe(
        @Header("Authorization") token: String,
    ): UserDetail

    // 发表评论接口
    @POST("api/v1/posts/{postId}/comments")
    suspend fun createComment(
        @Header("Authorization") token: String,
        @Path("postId") postId: Int,
        @Body request: CommentCreateRequest
    ): CommentOut

    @GET("api/v1/chat/history/{targetUserId}")
    suspend fun getChatHistory(
        @Header("Authorization") token: String,
        @Path("targetUserId") targetUserId: Int,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): List<ChatMessage>

    @GET("api/v1/user/{user_id}/avatar")
    suspend fun getUserAvatar(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ): UserAvatarResponse

    @GET("api/v1/chat/conversations")
    suspend fun getConversations(
        @Header("Authorization") token: String
    ): List<ChatConversation>


    //已读
    @POST("api/v1/chat/conversations/{userId}/read")
    suspend fun markAsRead(
        @Header("Authorization") token: String,
        @Path("userId") userId: Int
    ): Map<String, Any>

    // 上传头像
    @Multipart
    @POST("api/v1/user/me/avatar")
    suspend fun updateAvatar(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): UserDetail

    // 上传背景图
    @Multipart
    @POST("api/v1/user/me/cover")
    suspend fun updateCover(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part
    ): UserDetail

    // 更新基本信息
    @PATCH("api/v1/user/me")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body userIn: UserUpdate
    ): UserDetail

    // 获取粉丝列表
    @GET("api/v1/user/{user_id}/followers")
    suspend fun getUserFollowers(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ): List<UserOut>

    // 获取关注列表
    @GET("api/v1/user/{user_id}/following")
    suspend fun getUserFollowing(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ): List<UserOut>

    // 关注/取消关注
    @POST("api/v1/user/{id}/follow")
    suspend fun toggleFollowUser(
        @Header("Authorization") token: String,
        @Path("id") userId: Int
    ): ToggleResponse

    // 获取收藏列表
    @GET("api/v1/assets/me/collected")
    suspend fun getCollectedAssets(
        @Header("Authorization") token: String,
    ): List<AssetCard>

    // 帖子收藏
    @GET("api/v1/posts/me/collected")
    suspend fun getCollectedPosts(
        @Header("Authorization") token: String
    ): List<PostCard>


    //模型下载
    @POST("api/v1/assets/{id}/download")
    suspend fun downloadAsset(
        @Header("Authorization") token: String,
        @Path("id") assetId: Int,
        @Query("file_type") fileType: String
    ): DownloadResponse


    //下载记录
    @GET("api/v1/assets/me/downloads")
    suspend fun getMyDownloads(
        @Header("Authorization") token: String,
    ): List<AssetCard>


    @POST("api/v1/chat/send")
    suspend fun sendChatMessage(
        @Header("Authorization") token: String,
        @Body request: SendMessageRequest
    ): ChatMessage
}


