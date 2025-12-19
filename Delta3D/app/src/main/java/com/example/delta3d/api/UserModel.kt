package com.example.delta3d.api

import com.google.gson.annotations.SerializedName

data class UserDetail(
    val id: Int,
    val username: String,
    val gender: String?,
    @SerializedName("avatar_url") val avatarUrl: String?,
    @SerializedName("cover_url") val coverUrl: String?,
    val bio: String?,
    @SerializedName("created_at") val createdAt: String,

    // 统计数据
    @SerializedName("follower_count") val followerCount: Int,
    @SerializedName("following_count") val followingCount: Int,
    @SerializedName("liked_total_count") val likedTotalCount: Int
)

data class UserAvatarResponse(
    @SerializedName("avatar_url") val avatarUrl: String?
)

// 数据模型补充
data class UserUpdate(
    val username: String? = null,
    val bio: String? = null,
    val gender: String? = null
)

// 用于粉丝/关注列表的返回项
data class UserOut(
    val id: Int,
    val username: String,
    @SerializedName("avatar_url") val avatarUrl: String?,

    val gender: String?,
    val bio: String?
)