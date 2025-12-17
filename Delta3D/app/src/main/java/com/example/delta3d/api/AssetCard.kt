package com.example.delta3d.api
import com.google.gson.annotations.SerializedName

// 对应 Python 的 AssetCard
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
    val createdAt: String, // 简化处理，直接用字符串接收时间

    val status: String
) {

    val randomHeightDp: Int
        get() = (130..220).random()
}
