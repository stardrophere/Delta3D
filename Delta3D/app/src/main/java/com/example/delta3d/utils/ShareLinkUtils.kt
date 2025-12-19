package com.example.delta3d.utils

import android.net.Uri
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ShareLinkUtils {
    private const val SCHEME = "delta3d"
    private const val HOST = "share"
    private const val PATH_POST = "/post"

    // 判断是否是特殊分享链接
    fun isPostShareLink(content: String): Boolean {
        return content.startsWith("$SCHEME://$HOST$PATH_POST")
    }

    // 生成链接字符串 (帖子页调用)
    fun generatePostLink(postId: Int, title: String, coverUrl: String?): String {
        val safeTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
        val safeCover = if (coverUrl != null) URLEncoder.encode(coverUrl, StandardCharsets.UTF_8.toString()) else ""
        return "$SCHEME://$HOST$PATH_POST?id=$postId&title=$safeTitle&cover=$safeCover"
    }

    // 解析链接数据 (在聊天气泡调用)
    fun parsePostLink(content: String): PostShareData? {
        return try {
            val uri = Uri.parse(content)
            val id = uri.getQueryParameter("id")?.toIntOrNull() ?: return null
            val title = uri.getQueryParameter("title") ?: "Unknown Model"
            val cover = uri.getQueryParameter("cover")

            PostShareData(id, title, cover)
        } catch (e: Exception) {
            null
        }
    }
}

data class PostShareData(
    val postId: Int,
    val title: String,
    val coverUrl: String?
)