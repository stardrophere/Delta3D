package com.example.delta3d.utils

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {
    /**
     * 将 ISO 时间字符串 (e.g., 2025-12-19T13:37:40.157589)
     * 转换为东八区 (GMT+8) 的 "yyyy-MM-dd HH:mm" 格式
     */
    fun formatToEastEight(isoString: String?): String {
        if (isoString.isNullOrBlank()) return "Unknown Date"
        return try {
            // 解析 ISO 格式
            val inputDate = LocalDateTime.parse(isoString)
            val zonedDateTime = inputDate.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(ZoneId.of("Asia/Shanghai")) // 切换到东八区

            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.getDefault())
            zonedDateTime.format(formatter)
        } catch (e: Exception) {
            isoString
        }
    }
}