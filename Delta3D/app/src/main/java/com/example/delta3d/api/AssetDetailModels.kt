package com.example.delta3d.api


import com.google.gson.annotations.SerializedName

data class AssetDetail(
    val id: Int,
    val title: String,
    val description: String?,
    val remark: String?,
    val tags: List<String> = emptyList(),

    @SerializedName("video_url")
    val videoUrl: String?,

    @SerializedName("model_url")
    val modelUrl: String?,

    val status: String,

    @SerializedName("created_at")
    val createdAt: String
)