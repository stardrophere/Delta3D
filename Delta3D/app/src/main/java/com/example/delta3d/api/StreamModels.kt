package com.example.delta3d.api

import com.google.gson.annotations.SerializedName

enum class StreamActionType(val value: String) {
    @SerializedName("rotate")
    ROTATE("rotate"),
    @SerializedName("pan")
    PAN("pan"),
    @SerializedName("zoom")
    ZOOM("zoom")
}

enum class StreamDirection(val value: String) {
    @SerializedName("up")
    UP("up"),
    @SerializedName("down")
    DOWN("down"),
    @SerializedName("left")
    LEFT("left"),
    @SerializedName("right")
    RIGHT("right"),
    @SerializedName("in")
    IN("in"),
    @SerializedName("out")
    OUT("out")
}

data class ControlCommand(
    val action: StreamActionType,
    val direction: StreamDirection,
    val mode: String // "start" or "stop"
)

data class StreamStatus(
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("rtsp_url") val rtspUrl: String?,
    @SerializedName("current_asset_id") val currentAssetId: Int?
)