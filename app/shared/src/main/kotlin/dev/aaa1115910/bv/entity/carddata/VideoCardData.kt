package dev.aaa1115910.bv.entity.carddata

import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.formatRelativeTime
import java.time.ZoneId

data class VideoCardData(
    val avid: Long,
    val title: String,
    val cover: String,
    val upName: String,
    val reason: String = "",
    val play: Int? = null,
    var playString: String = "",
    val danmaku: Int? = null,
    var danmakuString: String = "",
    val time: Long? = null,
    var timeString: String = "",
    val jumpToSeason: Boolean = false,
    val epId: Int? = null,
    val pubTime: Int? = null,
    var pubTimeString: String = "",
    val is4K: Boolean = false,
    val upFace: String = "",
) {
    init {
        play?.let {
            playString = if (it >= 10000) "${it / 10000}万" else "$it"
        }
        danmaku?.let {
            danmakuString = if (it >= 10000) "${it / 10000}万" else "$it"
        }
        time?.let {
            timeString = if (it > 0) it.formatHourMinSec() else ""
        }
        pubTime?.let {
            pubTimeString = formatRelativeTime(it.toLong())
        }
    }
}

/** 卡片信息行：UP 主 · 播放量 · 发布时间，空项会被跳过。 */
fun VideoCardData.metaLine(
    nowMillis: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): String {
    val timeText = pubTime?.let { formatRelativeTime(it.toLong(), nowMillis, zone) } ?: ""
    val playText = if (playString.isNotBlank()) "${playString}播放" else ""
    return listOf(upName, playText, timeText).filter { it.isNotBlank() }.joinToString(" · ")
}
