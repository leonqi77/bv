package dev.aaa1115910.bv.entity.carddata

import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.formatRelativeTime
import java.text.SimpleDateFormat
import java.util.TimeZone

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
        play?.let { playString = it.formatCount() }
        danmaku?.let { danmakuString = it.formatCount() }
        time?.let {
            timeString = if (it > 0) it.formatHourMinSec() else ""
        }
        pubTime?.let {
            pubTimeString =
                if (it > 0) SimpleDateFormat("yyyy-MM-dd").format(java.util.Date(it * 1000L)) else ""
        }
    }
}

/** 卡片信息行：UP 主 · 播放量 · 发布时间，空项会被跳过。 */
fun VideoCardData.metaLine(
    nowMillis: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): String {
    val timeText = pubTime?.let { formatRelativeTime(it.toLong(), nowMillis, timeZone) } ?: ""
    val playText = if (playString.isNotBlank()) "${playString}播放" else ""
    return listOf(upName, playText, timeText).filter { it.isNotBlank() }.joinToString(" · ")
}

/** 卡片计数文字：满 1 万显示为 N万。 */
fun Int.formatCount(): String = if (this >= 10000) "${this / 10000}万" else "$this"

/** 未知计数（接口返回 -1）转为 null，避免卡片显示 "-1播放"。 */
fun Int.takeIfKnownCount(): Int? = takeIf { it >= 0 }

/** 发布时间戳（秒）转为 [VideoCardData.pubTime]，未知（<=0）返回 null。 */
fun Long.toPubTimeOrNull(): Int? = takeIf { it > 0 }?.toInt()

/** [Int] 版本，用于接口直接返回 Int 秒的场景。 */
fun Int.toPubTimeOrNull(): Int? = toLong().toPubTimeOrNull()
