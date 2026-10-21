package dev.aaa1115910.bv.tv.screens.season

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.entity.video.season.PgcSeason
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.carddata.SeasonCardData
import dev.aaa1115910.bv.tv.component.BvCardDefaults
import dev.aaa1115910.bv.tv.component.videocard.SeasonCard
import dev.aaa1115910.bv.ui.theme.BVTheme
import dev.aaa1115910.bv.ui.theme.BvColors
import dev.aaa1115910.bv.util.ImageSize
import dev.aaa1115910.bv.util.formatHourMinSec
import dev.aaa1115910.bv.util.ifElse
import dev.aaa1115910.bv.util.resizedImageUrl
import dev.aaa1115910.bv.viewmodel.season.episodeRanges

private val EpisodeCardWidth = 168.dp
private val RowTitleHeight = 28.dp

/** 分行顶部到卡片顶部的距离；纵向滚动时把聚焦的分行卡片对齐到这个位置下方。 */
val SeasonRowCardTop = RowTitleHeight + 10.dp

/**
 * 剧集分行：16:9 缩略图，图上左下角为“第N话”、右下角为时长，图下为标题。
 * 最后观看的剧集有强调色描边和进度条。剧集多于 20 集时，标题旁显示分段选择（1–20、21–40…）。
 *
 * @param featureFilm 是否为正片，正片在图上显示“第N话”
 * @param rowFocusRequester 用于“选集”按钮把焦点移到本行，恢复到上次聚焦的剧集
 */
@Composable
fun SeasonEpisodeRow(
    modifier: Modifier = Modifier,
    title: String,
    episodes: List<Episode>,
    featureFilm: Boolean,
    lastPlayedId: Int = 0,
    lastPlayedTime: Int = 0,
    rowFocusRequester: FocusRequester = remember { FocusRequester() },
    onClick: (avid: Long, cid: Long, epid: Int, episodeTitle: String, startTime: Int) -> Unit
) {
    val ranges = remember(episodes.size) { episodeRanges(episodes.size) }
    val lastPlayedIndex = remember(episodes, lastPlayedId) { episodes.indexOfFirst { it.id == lastPlayedId } }
    // 默认选中包含最后观看剧集的分段；从播放页返回后进度变化时不自动切换，避免重建分行导致焦点丢失
    var selectedRange by remember(ranges) {
        mutableIntStateOf(ranges.indexOfFirst { lastPlayedIndex + 1 in it }.coerceAtLeast(0))
    }
    val range = ranges.getOrNull(selectedRange)
    val visibleEpisodes = remember(episodes, range) {
        if (range == null) episodes else episodes.subList(range.first - 1, range.last)
    }
    val indexOffset = range?.first?.minus(1) ?: 0

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .height(RowTitleHeight)
                .padding(start = SeasonContentStartPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.padding(end = 14.dp),
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BvColors.TextPrimary
            )
            if (ranges.isNotEmpty()) {
                // 分段很多时（例如上千集）可横向滚动
                LazyRow(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(end = SeasonContentStartPadding),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(items = ranges) { index, r ->
                        RangeChip(
                            text = "${r.first}–${r.last}",
                            selected = index == selectedRange,
                            onClick = { selectedRange = index }
                        )
                    }
                }
            }
        }

        // 切换分段时重建分行，滚动位置与焦点恢复都从新分段开始
        key(selectedRange) {
            val lastPlayedInSlice = (lastPlayedIndex - indexOffset).takeIf { it in visibleEpisodes.indices }
            val entryIndex = lastPlayedInSlice ?: 0
            val entryFocusRequester = remember { FocusRequester() }
            val rowState = rememberLazyListState(
                initialFirstVisibleItemIndex = (entryIndex - 1).coerceAtLeast(0)
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(rowFocusRequester)
                    .focusRestorer(entryFocusRequester),
                state = rowState,
                contentPadding = PaddingValues(
                    start = SeasonContentStartPadding,
                    end = SeasonContentStartPadding,
                    top = 10.dp,
                    bottom = 12.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                itemsIndexed(items = visibleEpisodes) { index, episode ->
                    val isLastPlayed = index == lastPlayedInSlice
                    SeasonEpisodeCard(
                        modifier = Modifier.ifElse(
                            index == entryIndex,
                            Modifier.focusRequester(entryFocusRequester)
                        ),
                        episode = episode,
                        badge = if (featureFilm) episodeBadge(episode) else null,
                        isLastPlayed = isLastPlayed,
                        progress = if (isLastPlayed) watchProgress(lastPlayedTime, episode.duration) else null,
                        onClick = {
                            onClick(
                                episode.aid,
                                episode.cid,
                                episode.id,
                                episode.playerTitle(),
                                if (episode.id == lastPlayedId) lastPlayedTime else 0
                            )
                        }
                    )
                }
            }
        }
    }
}

/** 正片在图上显示“第N话”；标题不是数字（例如 SP）时原样显示。 */
private fun episodeBadge(episode: Episode): String? {
    val number = episode.title.trim().toIntOrNull()
    return if (number != null) "第${number}话" else episode.title.takeIf { it.isNotBlank() }
}

/** 播放器里的分集标题：“第N话 标题”；标题不是数字时用原标题代替“第N话”。 */
internal fun Episode.playerTitle(): String =
    listOfNotNull(episodeBadge(this), longTitle.takeIf { it.isNotBlank() }).joinToString(" ")

/** [lastTime] 单位为秒，[durationMs] 单位为毫秒；lastTime < 0 表示已看完。 */
private fun watchProgress(lastTime: Int, durationMs: Int): Float? {
    if (durationMs <= 0) return null
    if (lastTime < 0) return 1f
    return (lastTime * 1000f / durationMs).coerceIn(0f, 1f)
}

@Composable
private fun RangeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) BvColors.TextPrimary else BvColors.ChipFill,
            contentColor = if (selected) BvColors.Background else BvColors.TextSecondary,
            focusedContainerColor = if (selected) BvColors.TextPrimary else Color.White.copy(alpha = 0.3f),
            focusedContentColor = if (selected) BvColors.Background else BvColors.TextPrimary,
            pressedContainerColor = BvColors.TextPrimary,
            pressedContentColor = BvColors.Background
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = BvCardDefaults.FocusedScale),
        border = ClickableSurfaceDefaults.border(
            // 选中的分段本身是白底，聚焦时用强调色描边区分
            focusedBorder = Border(
                border = if (selected) BorderStroke(2.dp, BvColors.Accent) else BorderStroke(1.5.dp, Color.White),
                shape = shape
            )
        )
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
            text = text,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
private fun SeasonEpisodeCard(
    modifier: Modifier = Modifier,
    episode: Episode,
    badge: String?,
    isLastPlayed: Boolean,
    progress: Float?,
    onClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    Column(modifier = modifier.width(EpisodeCardWidth)) {
        Card(
            onClick = onClick,
            colors = CardDefaults.colors(
                containerColor = EpisodeThumbPlaceholder,
                focusedContainerColor = EpisodeThumbPlaceholder,
                pressedContainerColor = EpisodeThumbPlaceholder
            ),
            shape = CardDefaults.shape(shape = shape),
            scale = BvCardDefaults.scale(),
            border = if (isLastPlayed) {
                CardDefaults.border(
                    border = Border(BorderStroke(1.5.dp, BvColors.Accent.copy(alpha = 0.55f)), shape = shape),
                    focusedBorder = BvCardDefaults.focusedBorder()
                )
            } else {
                BvCardDefaults.border()
            },
            glow = BvCardDefaults.glow()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                if (episode.cover.isNotBlank()) {
                    AsyncImage(
                        modifier = Modifier.fillMaxSize(),
                        model = episode.cover.resizedImageUrl(ImageSize.SmallVideoCardCover),
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }
                val labelBottom = if (progress != null) 7.dp else 5.dp
                if (badge != null) {
                    ThumbLabel(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 5.dp, bottom = labelBottom),
                        text = badge,
                        bold = true
                    )
                }
                if (episode.duration > 0) {
                    ThumbLabel(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 5.dp, bottom = labelBottom),
                        text = episode.duration.toLong().formatHourMinSec()
                    )
                }
                if (progress != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress)
                                .background(BvColors.Accent)
                        )
                    }
                }
            }
        }
        Text(
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
            text = episode.longTitle.ifBlank { episode.title },
            style = MaterialTheme.typography.bodySmall,
            color = BvColors.TextPrimary.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val EpisodeThumbPlaceholder = Color(0xFF222222)

@Composable
private fun ThumbLabel(
    modifier: Modifier = Modifier,
    text: String,
    bold: Boolean = false
) {
    Text(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        color = Color.White,
        maxLines = 1
    )
}

/** 同系列其他季：海报分行（系列共 N 部）。 */
@Composable
fun SeasonPosterRow(
    modifier: Modifier = Modifier,
    seasons: List<PgcSeason>,
    currentSeasonId: Int,
    onClickSeason: (Int) -> Unit
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .height(RowTitleHeight)
                .padding(start = SeasonContentStartPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(R.string.season_count_tip, seasons.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BvColors.TextPrimary
            )
        }
        val entryFocusRequester = remember { FocusRequester() }
        val currentIndex = seasons.indexOfFirst { it.seasonId == currentSeasonId }
        val entryIndex = currentIndex.coerceAtLeast(0)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .focusRestorer(entryFocusRequester),
            state = rememberLazyListState(initialFirstVisibleItemIndex = (entryIndex - 1).coerceAtLeast(0)),
            contentPadding = PaddingValues(
                start = SeasonContentStartPadding,
                end = SeasonContentStartPadding,
                top = 10.dp,
                bottom = 12.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(items = seasons) { index, season ->
                val data = remember(season, currentSeasonId) {
                    SeasonCardData(
                        seasonId = season.seasonId,
                        title = season.shortTitle.ifBlank { season.title ?: "" },
                        subTitle = if (season.seasonId == currentSeasonId) "当前" else null,
                        cover = season.cover.resizedImageUrl(ImageSize.SeasonCoverThumbnail)
                    )
                }
                SeasonCard(
                    modifier = Modifier.ifElse(
                        index == entryIndex,
                        Modifier.focusRequester(entryFocusRequester)
                    ),
                    data = data,
                    coverHeight = 150.dp,
                    onClick = { onClickSeason(season.seasonId) }
                )
            }
        }
    }
}

@Preview(device = "id:tv_1080p")
@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SeasonEpisodeRowPreview() {
    val episodes = (1..30).map { i ->
        Episode(
            id = i,
            aid = 0,
            bvid = "",
            cid = 0,
            epid = 1000 + i,
            title = "$i",
            longTitle = "这可能是我这辈子距离梅西最近的一次",
            cover = "",
            duration = 1385000,
            dimension = null,
            pages = emptyList()
        )
    }
    BVTheme {
        Box(modifier = Modifier.background(BvColors.Background)) {
            SeasonEpisodeRow(
                title = "正片",
                episodes = episodes,
                featureFilm = true,
                lastPlayedId = 3,
                lastPlayedTime = 600,
                onClick = { _, _, _, _, _ -> }
            )
        }
    }
}

@Preview(device = "id:tv_1080p")
@Preview(device = "id:tv_1080p", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SeasonPosterRowPreview() {
    val seasons = listOf(
        PgcSeason(
            seasonId = 25210,
            title = "命运之夜  06版",
            shortTitle = "FATE TV",
            cover = "http://i0.hdslb.com/bfs/bangumi/1113d844ad3a9b42af576d80142146cbecc1b7ff.jpg",
            horizontalCover = "http://i0.hdslb.com/bfs/bangumi/e3993240914c3d881d97e4527a52efa2a9dcdeaf.jpg"
        ),
        PgcSeason(
            seasonId = 29006,
            title = "Fate/stay night UNLIMITED BLADE WORKS",
            shortTitle = "UBW 剧场版",
            cover = "http://i0.hdslb.com/bfs/bangumi/image/b7ee578ff3c258f173587db3f687fa2d56e3b8c1.jpg",
            horizontalCover = "http://i0.hdslb.com/bfs/archive/ae6fcc22f6c627a899bfe2d736765cc83cd4e827.png"
        ),
        PgcSeason(
            seasonId = 1586,
            title = "Fate/stay night [Unlimited Blade Works] 第一季",
            shortTitle = "UBW第一季",
            cover = "http://i0.hdslb.com/bfs/bangumi/image/e67e09c9e48a32371a81100e0f65a61b18aabb24.png",
            horizontalCover = "http://i0.hdslb.com/bfs/bangumi/25e9da6dd71e4aaa23a7dc04b6f97a94ea1ddd9d.jpg"
        )
    )
    BVTheme {
        Box(modifier = Modifier.background(BvColors.Background)) {
            SeasonPosterRow(
                seasons = seasons,
                currentSeasonId = 25210,
                onClickSeason = {}
            )
        }
    }
}
