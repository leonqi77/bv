package dev.aaa1115910.bv.entity.carddata

import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.bv.util.formatRelativeTime
import java.util.TimeZone

/**
 * 首页大图头部展示的视频，只使用列表中已加载的数据。
 *
 * @property play 播放量，未知为 null
 * @property pubTime 发布时间（秒），未知为 0
 * @property durationSec 时长（秒），未知为 0
 */
data class HomeFeaturedItem(
    val aid: Long,
    val title: String,
    val cover: String,
    val upName: String,
    val upFace: String,
    val play: Int?,
    val pubTime: Long,
    val durationSec: Int,
    val is4K: Boolean,
    val description: String = ""
) {
    companion object {
        fun fromUgcItem(item: UgcItem) = HomeFeaturedItem(
            aid = item.aid,
            title = item.title,
            cover = item.cover,
            upName = item.author,
            upFace = item.authorFace,
            play = item.play.takeIfKnownCount(),
            pubTime = item.pubDate,
            durationSec = item.duration,
            is4K = item.dimension?.is4K == true
        )

        fun fromDynamicVideo(video: DynamicVideo) = HomeFeaturedItem(
            aid = video.aid,
            title = video.title,
            cover = video.cover,
            upName = video.author,
            upFace = video.avatar,
            play = video.play.takeIfKnownCount(),
            pubTime = video.time,
            durationSec = video.duration,
            is4K = false
        )
    }
}

/** 头部信息行：播放量 · 发布时间 · 时长，空项跳过；4K 由头部单独以强调色绘制。 */
fun HomeFeaturedItem.statsLine(
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    val playText = play?.let { "${it.formatCount()}播放" } ?: ""
    val timeText = formatRelativeTime(pubTime, nowMillis, timeZone)
    val durationText = if (durationSec > 0) formatDuration(durationSec) else ""
    return listOf(playText, timeText, durationText).filter { it.isNotBlank() }.joinToString(" · ")
}

/** m:ss / mm:ss / h:mm:ss */
private fun formatDuration(totalSec: Int): String {
    val hours = totalSec / 3600
    val minutes = totalSec % 3600 / 60
    val seconds = totalSec % 60
    val mmss = "$minutes:${seconds.toString().padStart(2, '0')}"
    return if (hours > 0) "$hours:${mmss.padStart(5, '0')}" else mmss
}
