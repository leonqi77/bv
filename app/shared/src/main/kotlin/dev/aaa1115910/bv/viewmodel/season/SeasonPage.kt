package dev.aaa1115910.bv.viewmodel.season

import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.entity.video.season.SeasonDetail
import kotlin.math.ceil
import kotlin.math.max

sealed interface SeasonBackdrop {
    data class Image(val url: String) : SeasonBackdrop
    data class BlurredPoster(val url: String) : SeasonBackdrop
}

/** 背景图优先级：横版背景图 > 第一集封面（正片，其次相关视频）> 模糊海报 */
fun seasonBackdrop(detail: SeasonDetail): SeasonBackdrop {
    if (detail.wideCover.isNotBlank()) return SeasonBackdrop.Image(detail.wideCover)
    val epCover = (detail.episodes.asSequence() + detail.sections.asSequence().flatMap { it.episodes })
        .map { it.cover }
        .firstOrNull { it.isNotBlank() }
    return if (epCover != null) SeasonBackdrop.Image(epCover) else SeasonBackdrop.BlurredPoster(detail.cover)
}

data class PlayButtonState(val text: String, val enabled: Boolean, val epid: Int?)

private fun episodeLabel(episode: Episode): String {
    val number = episode.title.trim().toIntOrNull()
    return if (number != null) "第${number}话" else episode.title
}

/**
 * [Episode.duration] 单位为毫秒，[SeasonDetail.UserStatus.Progress.lastTime] 单位为秒。
 * 被隐藏的会员/付费剧集已从 [SeasonDetail.episodes] 中过滤，故全部隐藏时 episodes 为空。
 */
fun playButtonState(
    detail: SeasonDetail,
    progress: SeasonDetail.UserStatus.Progress?,
    hiddenPaidEpisodeCount: Int
): PlayButtonState {
    // 正片全部因付费被隐藏时，即使还有相关视频（PV、经典回顾等）也不从相关视频开始播放
    if (detail.episodes.isEmpty() && hiddenPaidEpisodeCount > 0) {
        return PlayButtonState("正片均需大会员或付费观看", false, null)
    }
    val first = detail.episodes.firstOrNull()
        ?: detail.sections.firstOrNull { it.episodes.isNotEmpty() }?.episodes?.first()
        ?: return PlayButtonState("还没有正片", false, null) // 同 season_no_feature_film 字符串资源
    // 最后观看剧集所在的列表：正片，或其所属的相关视频分区
    val resumedList = progress?.let { p ->
        if (detail.episodes.any { it.id == p.lastEpId }) detail.episodes
        else detail.sections.firstOrNull { s -> s.episodes.any { it.id == p.lastEpId } }?.episodes
    }
    val resumed = resumedList?.firstOrNull { it.id == progress.lastEpId }
    if (resumed != null && progress != null && progress.lastTime < 0) {
        // lastTime < 0 表示已看完：播放同一列表中的下一集；已是最后一集则回到第一集（优先正片）
        val index = resumedList.indexOf(resumed)
        val next = resumedList.getOrNull(index + 1)
            ?: detail.episodes.firstOrNull()
            ?: resumedList.first()
        return PlayButtonState("播放 ${episodeLabel(next)}", true, next.id)
    }
    if (resumed != null && progress != null) {
        val remainingSec = resumed.duration / 1000.0 - progress.lastTime
        val minutes = max(1, ceil(remainingSec / 60.0).toInt())
        return PlayButtonState("继续播放 ${episodeLabel(resumed)} · 剩 $minutes 分钟", true, resumed.id)
    }
    return PlayButtonState("播放 ${episodeLabel(first)}", true, first.id)
}

fun episodeRanges(count: Int, size: Int = 20): List<IntRange> {
    if (count <= size) return emptyList()
    return (1..count step size).map { start -> start..minOf(start + size - 1, count) }
}

data class SeasonFacts(val rating: String?, val items: List<String>, val tags: List<String>)

fun seasonFacts(detail: SeasonDetail): SeasonFacts {
    val year = detail.publish.publishDate.trim().take(4).takeIf { y -> y.length == 4 && y.all { it.isDigit() } }
    val items = buildList {
        year?.let { add(it) }
        addAll(detail.areas.filter { it.isNotBlank() })
        if (detail.newEpDesc.isNotBlank()) add(detail.newEpDesc)
    }
    return SeasonFacts(
        rating = detail.rating?.let { "★ %.1f".format(java.util.Locale.US, it) },
        items = items,
        tags = detail.styles
    )
}
