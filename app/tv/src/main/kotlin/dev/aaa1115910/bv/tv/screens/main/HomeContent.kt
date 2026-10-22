package dev.aaa1115910.bv.tv.screens.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.bv.entity.carddata.HomeFeaturedItem
import dev.aaa1115910.bv.entity.carddata.VideoCardData
import dev.aaa1115910.bv.entity.carddata.takeIfKnownCount
import dev.aaa1115910.bv.entity.carddata.toPubTimeOrNull
import dev.aaa1115910.bv.entity.proxy.ProxyArea
import dev.aaa1115910.bv.tv.activities.video.VideoInfoActivity
import dev.aaa1115910.bv.tv.screens.main.home.HomeFeaturedHeader
import dev.aaa1115910.bv.tv.screens.main.home.HomeFeaturedHeaderHeight
import dev.aaa1115910.bv.tv.screens.main.home.HomeVideoRow
import dev.aaa1115910.bv.tv.screens.main.home.HomeVideoRowCardTop
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.viewmodel.UserViewModel
import dev.aaa1115910.bv.viewmodel.home.DynamicViewModel
import dev.aaa1115910.bv.viewmodel.home.HomeRow
import dev.aaa1115910.bv.viewmodel.home.HomeRowLoader
import dev.aaa1115910.bv.viewmodel.home.PopularViewModel
import dev.aaa1115910.bv.viewmodel.home.RecommendViewModel
import dev.aaa1115910.bv.viewmodel.home.homeRows
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/** 焦点停留多久后才切换头部内容。 */
private const val HeaderSwitchDelayMs = 300L

/**
 * 首页：顶部是当前聚焦视频的大图头部，下方是 为你推荐 / 关注的 UP 主 / 热门 分行。
 *
 * @param navFocusRequester 侧边栏向右移动焦点时用来进入首页内容
 * @param reloadKey 在首页时再次选择侧边栏的 首页 会增加，用来刷新 为你推荐
 * @param onFocusToNav 在顶部分行按返回键时把焦点交还给侧边栏
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeContent(
    modifier: Modifier = Modifier,
    navFocusRequester: FocusRequester,
    reloadKey: Int = 0,
    onFocusToNav: () -> Unit = {},
    recommendViewModel: RecommendViewModel = koinViewModel(),
    popularViewModel: PopularViewModel = koinViewModel(),
    dynamicViewModel: DynamicViewModel = koinViewModel(),
    userViewModel: UserViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val logger = KotlinLogging.logger("HomeContent")

    val columnState = rememberLazyListState()
    val rowStates = remember { HomeRow.entries.associateWith { LazyListState() } }
    val rowFirstCards = remember { HomeRow.entries.associateWith { FocusRequester() } }
    val rowLoader = remember(scope) { HomeRowLoader(scope) }
    val headerState = remember { FeaturedHeaderState() }
    // 只在回调中读写，避免焦点移动引起整页重组
    val focusedRowIndex = remember { mutableIntStateOf(0) }
    var contentHasFocus by remember { mutableStateOf(false) }
    var everFocused by remember { mutableStateOf(false) }
    val startedEmpty = remember { recommendViewModel.recommendVideoList.isEmpty() }
    var handledReloadKey by remember { mutableIntStateOf(reloadKey) }
    val loginEffectFirstRun = remember { booleanArrayOf(true) }

    val recommendCards by remember {
        derivedStateOf { recommendViewModel.recommendVideoList.map { it.toVideoCardData() } }
    }
    val followingCards by remember {
        derivedStateOf { dynamicViewModel.dynamicVideoList.map { it.toVideoCardData() } }
    }
    val popularCards by remember {
        derivedStateOf { popularViewModel.popularVideoList.map { it.toVideoCardData() } }
    }
    val rows = homeRows(
        isLogin = userViewModel.isLogin,
        followingCount = dynamicViewModel.dynamicVideoList.size
    )

    val loadRow: suspend (HomeRow) -> Unit = { row ->
        when (row) {
            HomeRow.Recommend -> recommendViewModel.loadMore()
            HomeRow.Following -> dynamicViewModel.loadMoreVideo()
            HomeRow.Popular -> popularViewModel.loadMore()
        }
    }
    val loadMore: (HomeRow) -> Job = { row -> rowLoader.loadMore(row, Dispatchers.IO) { loadRow(row) } }

    //启动时刷新数据：先加载推荐，完成后再加载热门和动态
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            userViewModel.updateUserInfo()
        }
        val recommendJob = loadMore(HomeRow.Recommend)
        // 推荐加载结束（或被刷新取消）后再加载热门和动态
        scope.launch {
            recommendJob.join()
            loadMore(HomeRow.Popular)
            loadMore(HomeRow.Following)
        }
    }

    //监听登录变化
    LaunchedEffect(userViewModel.isLogin) {
        if (userViewModel.isLogin) {
            //login
            userViewModel.updateUserInfo()
            // 启动后才登录时补充加载关注动态
            if (!loginEffectFirstRun[0] && dynamicViewModel.dynamicVideoList.isEmpty()) {
                loadMore(HomeRow.Following)
            }
        } else {
            //logout
            userViewModel.clearUserInfo()
        }
        loginEffectFirstRun[0] = false
    }

    // 首次启动时推荐加载完成后，焦点落在第一张卡片上
    LaunchedEffect(recommendCards.isNotEmpty()) {
        if (startedEmpty && !everFocused && recommendCards.isNotEmpty()) {
            focusFirstCard(rowFirstCards.getValue(HomeRow.Recommend))
        }
    }

    // 在首页时再次选择侧边栏的 首页：刷新推荐并回到顶部
    LaunchedEffect(reloadKey) {
        if (reloadKey == handledReloadKey) return@LaunchedEffect
        handledReloadKey = reloadKey
        logger.fInfo { "reload recommend data" }
        headerState.reset()
        columnState.scrollToItem(0)
        rowStates.getValue(HomeRow.Recommend).scrollToItem(0)
        // 先取消并等待进行中的推荐加载，避免旧请求的结果落进清空后的列表
        rowLoader.reload(
            row = HomeRow.Recommend,
            context = Dispatchers.IO,
            reset = { recommendViewModel.clearData() }
        ) { loadRow(HomeRow.Recommend) }
    }

    BackHandler(contentHasFocus) {
        if (focusedRowIndex.intValue > 0) {
            logger.fInfo { "onBack: focus first card of top row" }
            scope.launch {
                columnState.scrollToItem(0)
                rowStates.getValue(HomeRow.Recommend).scrollToItem(0)
                focusFirstCard(rowFirstCards.getValue(HomeRow.Recommend))
            }
        } else {
            logger.fInfo { "onBack: focus side menu" }
            onFocusToNav()
        }
    }

    // 纵向：把聚焦卡片所在分行对齐到头部下方；横向分行沿用默认行为
    val defaultBringIntoViewSpec = LocalBringIntoViewSpec.current
    val cardTopPx = with(LocalDensity.current) { HomeVideoRowCardTop.toPx() }
    val rowAlignSpec = remember(cardTopPx) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float
            ): Float = offset - cardTopPx
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .onFocusChanged {
                contentHasFocus = it.hasFocus
                if (it.hasFocus) everFocused = true
            }
    ) {
        FeaturedHeaderHost(
            state = headerState,
            recommendList = recommendViewModel.recommendVideoList
        )

        CompositionLocalProvider(LocalBringIntoViewSpec provides rowAlignSpec) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = HomeFeaturedHeaderHeight - 8.dp)
                    .focusRequester(navFocusRequester)
                    .focusRestorer(rowFirstCards.getValue(HomeRow.Recommend)),
                state = columnState,
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(items = rows, key = { it }) { row ->
                    val rowIndex = rows.indexOf(row)
                    CompositionLocalProvider(LocalBringIntoViewSpec provides defaultBringIntoViewSpec) {
                        when (row) {
                            HomeRow.Recommend -> HomeVideoRow(
                                title = "为你推荐",
                                items = recommendCards,
                                loading = recommendViewModel.loading,
                                listState = rowStates.getValue(row),
                                firstCardFocusRequester = rowFirstCards.getValue(row),
                                onItemFocused = { index ->
                                    focusedRowIndex.intValue = rowIndex
                                    recommendViewModel.recommendVideoList.getOrNull(index)?.let {
                                        headerState.pending = HomeFeaturedItem.fromUgcItem(it)
                                    }
                                },
                                onItemClick = { index ->
                                    recommendViewModel.recommendVideoList.getOrNull(index)
                                        ?.let { VideoInfoActivity.actionStart(context, it.aid) }
                                },
                                onNearEnd = { loadMore(row) }
                            )

                            HomeRow.Following -> HomeVideoRow(
                                title = "关注的 UP 主",
                                items = followingCards,
                                listState = rowStates.getValue(row),
                                firstCardFocusRequester = rowFirstCards.getValue(row),
                                onItemFocused = { index ->
                                    focusedRowIndex.intValue = rowIndex
                                    dynamicViewModel.dynamicVideoList.getOrNull(index)?.let {
                                        headerState.pending = HomeFeaturedItem.fromDynamicVideo(it)
                                    }
                                },
                                onItemClick = { index ->
                                    dynamicViewModel.dynamicVideoList.getOrNull(index)?.let {
                                        VideoInfoActivity.actionStart(
                                            context = context,
                                            aid = it.aid,
                                            proxyArea = ProxyArea.checkProxyArea(it.title)
                                        )
                                    }
                                },
                                onNearEnd = { loadMore(row) }
                            )

                            HomeRow.Popular -> HomeVideoRow(
                                title = "热门",
                                items = popularCards,
                                loading = popularViewModel.loading,
                                listState = rowStates.getValue(row),
                                firstCardFocusRequester = rowFirstCards.getValue(row),
                                onItemFocused = { index ->
                                    focusedRowIndex.intValue = rowIndex
                                    popularViewModel.popularVideoList.getOrNull(index)?.let {
                                        headerState.pending = HomeFeaturedItem.fromUgcItem(it)
                                    }
                                },
                                onItemClick = { index ->
                                    popularViewModel.popularVideoList.getOrNull(index)
                                        ?.let { VideoInfoActivity.actionStart(context, it.aid) }
                                },
                                onNearEnd = { loadMore(row) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Stable
private class FeaturedHeaderState {
    /** 最近聚焦的视频，焦点停留 [HeaderSwitchDelayMs] 后才会显示 */
    var pending by mutableStateOf<HomeFeaturedItem?>(null)
    var shown by mutableStateOf<HomeFeaturedItem?>(null)

    fun reset() {
        pending = null
        shown = null
    }
}

/** 单独的作用域，焦点移动只重组头部，不重组分行。 */
@Composable
private fun FeaturedHeaderHost(
    state: FeaturedHeaderState,
    recommendList: List<UgcItem>
) {
    val firstRecommend by remember(recommendList) {
        derivedStateOf { recommendList.firstOrNull()?.let { HomeFeaturedItem.fromUgcItem(it) } }
    }
    val pending = state.pending
    LaunchedEffect(pending) {
        if (pending == null) return@LaunchedEffect
        delay(HeaderSwitchDelayMs)
        state.shown = pending
    }
    HomeFeaturedHeader(item = state.shown ?: firstRecommend)
}

/** 等待分行完成布局后再把焦点移到第一张卡片，最多重试几帧。 */
private suspend fun focusFirstCard(focusRequester: FocusRequester) {
    repeat(5) {
        withFrameNanos { }
        if (runCatching { focusRequester.requestFocus() }.isSuccess) return
    }
}

private fun UgcItem.toVideoCardData() = VideoCardData(
    avid = aid,
    title = title,
    cover = cover,
    play = play.takeIfKnownCount(),
    danmaku = danmaku.takeIfKnownCount(),
    upName = author,
    time = duration * 1000L,
    is4K = dimension?.is4K == true,
    pubTime = pubDate.toPubTimeOrNull(),
    upFace = authorFace
)

private fun DynamicVideo.toVideoCardData() = VideoCardData(
    avid = aid,
    title = title,
    cover = cover,
    play = play.takeIfKnownCount(),
    danmaku = danmaku.takeIfKnownCount(),
    upName = author,
    time = duration * 1000L,
    pubTime = time.toPubTimeOrNull(),
    upFace = avatar
)
