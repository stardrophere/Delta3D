package com.example.delta3d.api

import com.google.gson.annotations.SerializedName


data class AssetCard(
    val id: Int,
    val title: String,

    @SerializedName("cover_url")
    val coverUrl: String?, // 对应 Python 的 video_path/cover_url

    val description: String?,

    val tags: List<String> = emptyList(),

    @SerializedName("is_collected")
    val isCollected: Boolean,

    @SerializedName("created_at")
    val createdAt: String,

    val status: String,

    val height: Int,

    @SerializedName("owner_id")
    val ownerId: Int,

    @SerializedName("downloaded_at")
    val downloadedAt: String? = null

) {

    val randomHeightDp: Int
        get() = (130..220).random()
}
