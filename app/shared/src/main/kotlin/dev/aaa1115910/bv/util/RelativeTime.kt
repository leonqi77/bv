package dev.aaa1115910.bv.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val sameYearFormatter = DateTimeFormatter.ofPattern("MM-dd")
private val otherYearFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

/**
 * 将秒级时间戳格式化为相对时间：刚刚 / N分钟前 / N小时前 / N天前（7 天内），
 * 更早显示 MM-dd（当年）或 yyyy-MM-dd（其他年份）。无效时间返回空字符串。
 */
fun formatRelativeTime(
    epochSeconds: Long,
    nowMillis: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    if (epochSeconds <= 0) return ""
    val diff = nowMillis / 1000 - epochSeconds
    return when {
        diff < 60 -> "刚刚"
        diff < 3600 -> "${diff / 60}分钟前"
        diff < 86400 -> "${diff / 3600}小时前"
        diff < 7 * 86400 -> "${diff / 86400}天前"
        else -> {
            val date = Instant.ofEpochSecond(epochSeconds).atZone(zone)
            val nowYear = Instant.ofEpochMilli(nowMillis).atZone(zone).year
            if (date.year == nowYear) sameYearFormatter.format(date)
            else otherYearFormatter.format(date)
        }
    }
}
