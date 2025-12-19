package com.example.delta3d.api

import kotlinx.serialization.Serializable
import com.google.gson.annotations.SerializedName

// --- 社区帖子卡片 ---
data class PostCard(
    @SerializedName("post_id") val postId: Int,
    @SerializedName("asset_id") val assetId: Int,
    val title: String,

    @SerializedName("cover_url")
    val coverUrl: String?,

    val description: String?,
    val tags: List<String> = emptyList(),

    @SerializedName("published_at") val publishedAt: String,

    // 统计数据
    @SerializedName("view_count") val viewCount: Int = 0,
    @SerializedName("like_count") val likeCount: Int = 0,
    @SerializedName("collect_count") val collectCount: Int = 0,
    @SerializedName("comment_count") val commentCount: Int = 0,

    // 作者信息
    @SerializedName("owner_id") val ownerId: Int,
    @SerializedName("owner_name") val ownerName: String,
    @SerializedName("owner_avatar") val ownerAvatar: String?,

    // 交互状态
    @SerializedName("is_liked") val isLiked: Boolean,
    @SerializedName("is_collected") val isCollected: Boolean,
    @SerializedName("is_following") val isFollowing: Boolean,
    @SerializedName("has_commented") val hasCommented: Boolean
)

// --- 发布帖子的请求体 ---
data class PostCreateRequest(
    @SerializedName("asset_id") val assetId: Int,
    val content: String,
    val visibility: String = "public",
    @SerializedName("allow_download") val allowDownload: Boolean = true
)

// 帖子基础信息
data class PostDetail(
    // --- 帖子基础信息 ---
    @SerializedName("post_id") val postId: Int,
    val content: String?,
    @SerializedName("published_at") val publishedAt: String,
    val visibility: String,
    @SerializedName("allow_download") val allowDownload: Boolean,

    // --- 统计信息 ---
    @SerializedName("like_count") val likeCount: Int,
    @SerializedName("collect_count") val collectCount: Int,
    @SerializedName("view_count") val viewCount: Int,
    @SerializedName("comment_count") val commentCount: Int,

    // --- 作者信息 ---
    @SerializedName("owner_id") val ownerId: Int,
    @SerializedName("owner_name") val ownerName: String,
    @SerializedName("owner_avatar") val ownerAvatar: String?,

    // --- 交互状态 ---
    @SerializedName("is_liked") val isLiked: Boolean,
    @SerializedName("is_collected") val isCollected: Boolean,
    @SerializedName("is_following") val isFollowing: Boolean,

    // --- 嵌套数据 ---
    val asset: PostAssetInfo,
    val comments: List<CommentOut>
)

// 帖子内的模型信息
data class PostAssetInfo(
    val id: Int,
    val title: String,
    val description: String?,
    val tags: List<String>,

    @SerializedName("video_url") val videoUrl: String,
    @SerializedName("model_url") val modelUrl: String?,

    val status: String,
    val height: Int,

    @SerializedName("estimated_gen_seconds") val estimatedGenSeconds: Int?
)

// 评论信息
data class CommentOut(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    val username: String,
    @SerializedName("avatar_url") val avatarUrl: String?,
    val content: String,
    @SerializedName("created_at") val createdAt: String
)

//发布评论
data class CommentCreateRequest(
    val content: String
)

