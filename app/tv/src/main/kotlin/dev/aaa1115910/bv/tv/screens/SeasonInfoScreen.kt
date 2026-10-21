package dev.aaa1115910.bv.tv.screens

import android.app.Activity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.bv.R
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.player.entity.VideoListPgcEpisode
import dev.aaa1115910.bv.repository.VideoInfoRepository
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.screens.season.SeasonBackdropImage
import dev.aaa1115910.bv.tv.screens.season.SeasonEpisodeRow
import dev.aaa1115910.bv.tv.screens.season.SeasonHero
import dev.aaa1115910.bv.tv.screens.season.SeasonPosterRow
import dev.aaa1115910.bv.tv.screens.season.SeasonRowCardTop
import dev.aaa1115910.bv.tv.util.launchPlayerActivity
import dev.aaa1115910.bv.ui.theme.BvColors
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.requestFocus
import dev.aaa1115910.bv.util.toast
import dev.aaa1115910.bv.viewmodel.SeasonViewModel
import dev.aaa1115910.bv.viewmodel.season.playButtonState
import dev.aaa1115910.bv.viewmodel.season.seasonBackdrop
import dev.aaa1115910.bv.viewmodel.season.seasonFacts
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun SeasonInfoScreen(
    modifier: Modifier = Modifier,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    videoInfoRepository: VideoInfoRepository = koinInject(),
    seasonViewModel: SeasonViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val intent = (context as Activity).intent
    val logger = KotlinLogging.logger { }

    var paused by remember { mutableStateOf(false) }
    val defaultFocusRequester = remember { FocusRequester() }

    val onClickVideo: (avid: Long, cid: Long, epid: Int, episodeTitle: String, startTime: Int) -> Unit =
        { avid, cid, epid, episodeTitle, startTime ->
            logger.debug { "onClickVideo: [avid=$avid, cid=$cid, epid=$epid, episodeTitle=$episodeTitle, startTime=$startTime]" }
            if (cid != 0L) {
                launchPlayerActivity(
                    context = context,
                    avid = avid,
                    cid = cid,
                    title = seasonViewModel.seasonData!!.title,
                    partTitle = episodeTitle,
                    played = startTime * 1000,
                    fromSeason = true,
                    subType = seasonViewModel.seasonData?.subType,
                    epid = epid,
                    seasonId = seasonViewModel.seasonData?.seasonId,
                    proxyArea = seasonViewModel.proxyArea,
                    playerIconIdle = seasonViewModel.seasonData?.playerIcon?.idle ?: "",
                    playerIconMoving = seasonViewModel.seasonData?.playerIcon?.moving ?: ""
                )
            } else {
                //如果 cid==0，就需要跳转回 VideoInfoActivity 去获取 cid 再跳转播放器
                VideoInfoActivity.actionStart(
                    context = context,
                    aid = avid,
                    fromSeason = true
                )
            }
        }

    val onClickFollow: (Boolean) -> Unit = { isFollowing ->
        scope.launch(Dispatchers.IO) {
            if (isFollowing) seasonViewModel.unFollowSeason() else seasonViewModel.followSeason()
        }
    }

    LaunchedEffect(Unit) {
        val epId = intent.getIntExtra("epid", 0)
        val seasonId = intent.getIntExtra("seasonid", 0)
        val proxyAreaIndex = intent.getIntExtra("proxy_area", 0)
        val proxyArea = ProxyArea.entries[proxyAreaIndex]
        logger.fInfo { "Read extras from content: [epId=$epId, seasonId=$seasonId, proxyArea=$proxyArea]" }

        seasonViewModel.epId = epId
        seasonViewModel.seasonId = seasonId
        seasonViewModel.proxyArea = proxyArea

        if (seasonViewModel.epId != null || seasonViewModel.seasonId != null) {
            scope.launch(Dispatchers.IO) {
                seasonViewModel.updateSeasonData()
            }
        } else {
            context.finish()
        }
    }

    LaunchedEffect(seasonViewModel.seasonData) {
        seasonViewModel.seasonData?.let {
            logger.fInfo { "season data change: ${seasonViewModel.seasonData}" }
            seasonViewModel.lastPlayProgress = it.userStatus.progress
            //请求默认焦点到播放按钮上
            defaultFocusRequester.requestFocus(scope)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) {
                paused = true
            } else if (event == Lifecycle.Event.ON_RESUME) {
                // 如果 pause==true 那可能是从播放页返回回来的，此时更新历史记录
                if (paused) {
                    scope.launch(Dispatchers.IO) {
                        seasonViewModel.updateLastPlayProgress()
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (seasonViewModel.seasonData == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BvColors.Background)
        ) {
            if (seasonViewModel.tip == "Loading") {
                LoadingIndicator(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.Center),
                )
            } else {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = seasonViewModel.tip,
                    color = BvColors.TextPrimary
                )
            }
        }
    } else {
        val seasonData = seasonViewModel.seasonData!!
        val backdrop = remember(seasonData) { seasonBackdrop(seasonData) }
        val facts = remember(seasonData) {
            val facts = seasonFacts(seasonData)
            // 未开播时补充开播时间（原播放按钮上显示的内容）
            val publishDate = seasonData.publish.publishDate
            if (!seasonData.publish.isPublished && publishDate.isNotBlank() && publishDate !in facts.items) {
                facts.copy(items = facts.items + publishDate)
            } else facts
        }
        val playState = remember(
            seasonData,
            seasonViewModel.lastPlayProgress,
            seasonViewModel.hiddenPaidEpisodeCount
        ) {
            playButtonState(
                detail = seasonData,
                progress = seasonViewModel.lastPlayProgress,
                hiddenPaidEpisodeCount = seasonViewModel.hiddenPaidEpisodeCount
            )
        }
        val featureFilmTitle = stringResource(R.string.season_feature_film)
        val rows = remember(seasonData, featureFilmTitle) {
            buildList {
                if (seasonData.episodes.isNotEmpty()) {
                    add(EpisodeRowData(featureFilmTitle, seasonData.episodes, featureFilm = true))
                }
                seasonData.sections.forEach { add(EpisodeRowData(it.title, it.episodes, featureFilm = false)) }
            }
        }
        val showSeasonRow = seasonData.seasons.any { it.seasonId != seasonData.seasonId }

        val onPlay: () -> Unit = onPlay@{
            logger.fInfo { "Click play button: $playState" }
            val playEpid = playState.epid
            if (!playState.enabled || playEpid == null) return@onPlay

            //懒得去改播放器那边来支持epid，就直接在这边查找cid了；先找正片，再找相关视频
            var episodeList: List<Episode> = emptyList()
            var playEpisode: Episode? = seasonData.episodes.firstOrNull { it.id == playEpid }
            if (playEpisode != null) {
                episodeList = seasonData.episodes
            } else {
                for (section in seasonData.sections) {
                    playEpisode = section.episodes.firstOrNull { it.id == playEpid }
                    if (playEpisode != null) {
                        episodeList = section.episodes
                        break
                    }
                }
            }
            if (playEpisode == null) {
                logger.fInfo { "Can't find cid" }
                "无法判断最后播放的剧集".toast(context)
                return@onPlay
            }

            val progress = seasonViewModel.lastPlayProgress?.takeIf { it.lastEpId == playEpisode.id }
            logger.fInfo { "Play aid: ${playEpisode.aid}, cid: ${playEpisode.cid}, resume: $progress" }
            onClickVideo(
                playEpisode.aid,
                playEpisode.cid,
                playEpisode.id,
                progress?.lastEpIndex ?: playEpisode.longTitle,
                progress?.lastTime ?: 0
            )
            videoInfoRepository.videoList.clear()
            videoInfoRepository.videoList.addAll(episodeList.toVideoList(seasonData.seasonId))
        }

        val listState = rememberLazyListState()
        val episodesRowFocusRequester = remember { FocusRequester() }
        // 焦点在头部或第一行剧集时，页面保持在顶部；焦点在下方分行时，把分行卡片对齐到屏幕上方
        var focusInTopArea by remember { mutableStateOf(true) }
        val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
        val rowCardTopPx = with(LocalDensity.current) { (24.dp + SeasonRowCardTop).toPx() }
        val columnBringIntoViewSpec = remember(listState, rowCardTopPx) {
            object : BringIntoViewSpec {
                override fun calculateScrollDistance(
                    offset: Float,
                    size: Float,
                    containerSize: Float
                ): Float = when {
                    !focusInTopArea -> offset - rowCardTopPx
                    listState.firstVisibleItemIndex == 0 -> -listState.firstVisibleItemScrollOffset.toFloat()
                    // 头部已滚出屏幕：先往回滚，头部出现后再精确回到顶部
                    else -> -containerSize
                }
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BvColors.Background)
        ) {
            SeasonBackdropImage(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .graphicsLayer {
                        // 背景图跟随头部滚动，只改图层位移，不触发重组
                        translationY = if (listState.firstVisibleItemIndex == 0) {
                            -listState.firstVisibleItemScrollOffset.toFloat()
                        } else {
                            -size.height
                        }
                    },
                backdrop = backdrop
            )

            CompositionLocalProvider(LocalBringIntoViewSpec provides columnBringIntoViewSpec) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(bottom = 48.dp)
                ) {
                    item(key = "hero") {
                        CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                            SeasonHero(
                                modifier = Modifier.onFocusChanged {
                                    if (it.hasFocus) focusInTopArea = true
                                },
                                title = seasonData.title,
                                facts = facts,
                                description = seasonData.description,
                                playState = playState,
                                following = seasonViewModel.isFollowing,
                                showEpisodesButton = seasonData.episodes.isNotEmpty(),
                                playFocusRequester = defaultFocusRequester,
                                onPlay = onPlay,
                                onClickFollow = onClickFollow,
                                onClickEpisodes = {
                                    runCatching { episodesRowFocusRequester.requestFocus() }
                                }
                            )
                        }
                    }
                    rows.forEachIndexed { index, row ->
                        item(key = "row-$index") {
                            val ownRowFocusRequester = remember { FocusRequester() }
                            CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                                SeasonEpisodeRow(
                                    modifier = Modifier.onFocusChanged {
                                        if (it.hasFocus) focusInTopArea = index == 0
                                    },
                                    title = row.title,
                                    episodes = row.episodes,
                                    featureFilm = row.featureFilm,
                                    lastPlayedId = seasonViewModel.lastPlayProgress?.lastEpId ?: 0,
                                    lastPlayedTime = seasonViewModel.lastPlayProgress?.lastTime ?: 0,
                                    rowFocusRequester = if (row.featureFilm) episodesRowFocusRequester else ownRowFocusRequester,
                                    onClick = { avid, cid, epid, episodeTitle, startTime ->
                                        onClickVideo(avid, cid, epid, episodeTitle, startTime)
                                        videoInfoRepository.videoList.clear()
                                        videoInfoRepository.videoList.addAll(row.episodes.toVideoList(seasonData.seasonId))
                                    }
                                )
                            }
                        }
                    }
                    if (showSeasonRow) {
                        item(key = "seasons") {
                            CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                                SeasonPosterRow(
                                    modifier = Modifier.onFocusChanged {
                                        if (it.hasFocus) focusInTopArea = false
                                    },
                                    seasons = seasonData.seasons,
                                    currentSeasonId = seasonData.seasonId,
                                    onClickSeason = { sid ->
                                        if (seasonData.seasonId != sid) {
                                            seasonViewModel.seasonData = null
                                            seasonViewModel.seasonId = sid
                                            seasonViewModel.epId = null
                                            scope.launch(Dispatchers.IO) { seasonViewModel.updateSeasonData() }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class EpisodeRowData(
    val title: String,
    val episodes: List<Episode>,
    val featureFilm: Boolean
)

private fun List<Episode>.toVideoList(seasonId: Int?) = mapIndexed { index, episode ->
    VideoListPgcEpisode(
        aid = episode.aid,
        cid = episode.cid,
        epid = episode.id,
        seasonId = seasonId,
        title = runCatching {
            "第 ${episode.title.toInt()} 集"
        }.getOrDefault(episode.title) + " " + episode.longTitle,
        index = index,
    )
}
