package dev.aaa1115910.bv.player.tv.controller

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.player.AbstractVideoPlayer
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerClockData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerDebugInfoData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerHistoryData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerSeekData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerStateData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.entity.LocalVideoPlayerVideoShotData
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.VideoListItem
import dev.aaa1115910.bv.player.entity.VideoListItemData
import dev.aaa1115910.bv.player.entity.VideoListPart
import dev.aaa1115910.bv.player.entity.VideoPlayerConfigData
import dev.aaa1115910.bv.player.entity.VideoPlayerMenuNavItem
import dev.aaa1115910.bv.player.entity.VideoPlayerPictureMenuItem
import dev.aaa1115910.bv.player.seekbar.SeekMoveState
import dev.aaa1115910.bv.player.shared.BuildConfig
import dev.aaa1115910.bv.player.shared.R
import dev.aaa1115910.bv.util.fInfo
import dev.aaa1115910.bv.util.toast
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun VideoPlayerController(
    modifier: Modifier = Modifier,
    videoPlayer: AbstractVideoPlayer,

    //player events
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onExit: () -> Unit,
    onGoTime: (time: Long) -> Unit,
    onBackToHistory: () -> Unit,
    onPlayNewVideo: (VideoListItem) -> Unit,

    //menu events
    onResolutionChange: (Resolution) -> Unit,
    onCodecChange: (VideoCodec) -> Unit,
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit,
    onAudioChange: (Audio) -> Unit,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit,
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit,

    onRequestFocus: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val videoPlayerSeekData = LocalVideoPlayerSeekData.current
    val videoPlayerHistoryData = LocalVideoPlayerHistoryData.current
    val videoPlayerStateData = LocalVideoPlayerStateData.current
    val videoPlayerDebugInfoData = LocalVideoPlayerDebugInfoData.current
    val videoPlayerConfigData = LocalVideoPlayerConfigData.current
    val videoPlayerVideoInfoData = LocalVideoPlayerVideoInfoData.current
    val videoPlayerClockData = LocalVideoPlayerClockData.current
    val videoPlayerVideoShotData = LocalVideoPlayerVideoShotData.current
    val logger = KotlinLogging.logger {}

    var showListController by remember { mutableStateOf(false) }
    var showMenuController by remember { mutableStateOf(false) }
    var showSeekController by remember { mutableStateOf(false) }
    val showClickableControllers by remember { derivedStateOf { showListController || showMenuController } }

    var lastPressBack by remember { mutableLongStateOf(0L) }
    var hasFocus by remember { mutableStateOf(false) }

    var goTime by remember { mutableLongStateOf(0L) }
    var seekChangeCount by remember { mutableIntStateOf(0) }
    var lastSeekChangeTime by remember { mutableLongStateOf(0L) }
    var moveState by remember { mutableStateOf(SeekMoveState.Idle) }

    var controls by remember { mutableStateOf(ControlsState()) }
    var lastKeyAt by remember { mutableLongStateOf(0L) }
    var menuNav by remember { mutableStateOf<VideoPlayerMenuNavItem?>(null) }
    var menuPictureItem by remember { mutableStateOf<VideoPlayerPictureMenuItem?>(null) }

    val playlist = videoPlayerConfigData.availableVideoList.filterIsInstance<VideoListItemData>()
    val currentIndex = playlist.indexOfFirst { it.cid == videoPlayerConfigData.currentVideoCid }
    val previousVideo = if (currentIndex > 0) playlist[currentIndex - 1] else null
    val nextVideo = if (currentIndex >= 0) playlist.getOrNull(currentIndex + 1) else null

    // The danmaku button switches between none and the last non-empty selection.
    val enabledDanmaku = videoPlayerConfigData.currentDanmakuEnabledList.toList()
    var lastEnabledDanmaku by remember { mutableStateOf(enabledDanmaku) }
    LaunchedEffect(enabledDanmaku) {
        if (enabledDanmaku.isNotEmpty()) lastEnabledDanmaku = enabledDanmaku
    }

    // Same condition as the pause indicator, so buffering does not count as a pause.
    val paused = with(videoPlayerStateData) { !isPlaying && !isBuffering && !isError }
    var wasPaused by remember { mutableStateOf(true) }
    LaunchedEffect(paused) {
        if (paused && !wasPaused && !showClickableControllers) controls = controls.onPaused()
        wasPaused = paused
    }
    LaunchedEffect(showClickableControllers) {
        if (showClickableControllers) controls = ControlsState()
    }
    LaunchedEffect(showMenuController) {
        if (!showMenuController) {
            menuNav = null
            menuPictureItem = null
        }
    }
    LaunchedEffect(controls.focus, videoPlayerStateData.isPlaying, lastKeyAt, showSeekController) {
        if (controls.focus != ControlsFocus.Hidden && videoPlayerStateData.isPlaying && !showSeekController) {
            delay(5000)
            controls = ControlsState()
        }
    }

    val openSeekController = {
        if (!showSeekController) goTime = videoPlayerSeekData.position
        showSeekController = true
    }

    val calCoefficient = {
        if (System.currentTimeMillis() - lastSeekChangeTime < 200) {
            seekChangeCount++
            seekChangeCount / 5
        } else {
            seekChangeCount = 0
            0
        }
    }

    val onTimeForward = {
        val targetTime = goTime + (10000 + calCoefficient() * 5000)
        goTime =
            if (targetTime > videoPlayerSeekData.duration) videoPlayerSeekData.duration else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
        moveState = SeekMoveState.Forward
        logger.info { "onTimeForward: [current=${videoPlayer.currentPosition}, goTime=$goTime]" }
    }
    val onTimeBack = {
        val targetTime = goTime - (5000 + calCoefficient() * 5000)
        goTime = if (targetTime < 0) 0 else targetTime
        lastSeekChangeTime = System.currentTimeMillis()
        moveState = SeekMoveState.Backward
        logger.info { "onTimeBack: [current=${videoPlayer.currentPosition}, goTime=$goTime]" }
    }

    val togglePlay = { if (videoPlayer.isPlaying) onPause() else onPlay() }
    val openMenu = { nav: VideoPlayerMenuNavItem?, pictureItem: VideoPlayerPictureMenuItem? ->
        menuNav = nav
        menuPictureItem = pictureItem
        showMenuController = true
        onRequestFocus()
    }
    val activateButton = { id: ControlButtonId ->
        logger.info { "Controls layer: activate $id" }
        when (id) {
            ControlButtonId.PlayPause -> togglePlay()
            ControlButtonId.Previous -> previousVideo?.let(onPlayNewVideo)
            ControlButtonId.Next -> nextVideo?.let(onPlayNewVideo)
            ControlButtonId.Resolution ->
                openMenu(VideoPlayerMenuNavItem.Picture, VideoPlayerPictureMenuItem.Resolution)

            ControlButtonId.Speed ->
                openMenu(VideoPlayerMenuNavItem.Picture, VideoPlayerPictureMenuItem.PlaySpeed)

            ControlButtonId.Danmaku -> onDanmakuSwitchChange(
                if (enabledDanmaku.isNotEmpty()) emptyList()
                else lastEnabledDanmaku.ifEmpty { DanmakuType.entries }
            )

            ControlButtonId.Subtitle -> openMenu(VideoPlayerMenuNavItem.ClosedCaption, null)
            ControlButtonId.Episodes -> showListController = true
            ControlButtonId.Settings -> openMenu(null, null)
        }
    }
    val runControlsAction = { action: ControlsAction ->
        when (action) {
            ControlsAction.PassThrough, ControlsAction.None -> {}
            ControlsAction.TogglePlay -> togglePlay()
            ControlsAction.SeekBack -> {
                openSeekController()
                onTimeBack()
            }

            ControlsAction.SeekForward -> {
                openSeekController()
                onTimeForward()
            }

            ControlsAction.OpenPlaylist -> showListController = true
            ControlsAction.OpenMenu -> openMenu(null, null)
            is ControlsAction.Activate ->
                ControlButtonId.entries.getOrNull(action.index)?.let(activateButton)
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black)
            .onFocusChanged { hasFocus = it.hasFocus }
            .focusable()
            //.ifElse(hasFocus, Modifier.border(2.dp, Color.Yellow))
            .onPreviewKeyEvent {

                if (showClickableControllers) {
                    if (listOf(Key.Back, Key.Menu).contains(it.key)) {
                        if (it.type == KeyEventType.KeyUp) {
                            logger.fInfo { "[${it.key}] hide all controllers" }
                            showMenuController = false
                            showListController = false
                            showSeekController = false
                        }
                        onRequestFocus()
                        return@onPreviewKeyEvent true
                    }
                    return@onPreviewKeyEvent false
                }

                if (showSeekController) {
                    if (listOf(
                            Key.Back,
                            Key.Menu,
                            Key.DirectionDown,
                            Key.DirectionUp
                        ).contains(it.key)
                    ) {
                        if (it.type != KeyEventType.KeyDown) showSeekController = false
                        onRequestFocus()
                        return@onPreviewKeyEvent true
                    }
                }

                // Controls layer first. OK during a seek still confirms it in the branch below.
                if (controls.focus != ControlsFocus.Hidden) lastKeyAt = System.currentTimeMillis()
                val remoteKey = it.key.toRemoteKey()
                if (remoteKey != null && !(showSeekController && remoteKey == RemoteKey.Ok)) {
                    // ←/→ act on key down so holding them repeats; everything else acts on key up.
                    val actsOnDown = remoteKey == RemoteKey.Left || remoteKey == RemoteKey.Right
                    val acting = it.type == if (actsOnDown) KeyEventType.KeyDown else KeyEventType.KeyUp
                    if (acting) {
                        val (next, action) = controls.onKey(remoteKey, ControlButtonId.entries.size)
                        if (action != ControlsAction.PassThrough) {
                            logger.info { "[${it.key}] controls layer: ${controls.focus} -> ${next.focus}, $action" }
                            controls = next
                            runControlsAction(action)
                            return@onPreviewKeyEvent true
                        }
                    } else if (controls.focus != ControlsFocus.Hidden || remoteKey == RemoteKey.Down) {
                        return@onPreviewKeyEvent true
                    }
                }

                when (it.key) {
                    Key.DirectionCenter, Key.Enter, Key.Spacebar -> {
                        @Suppress("KotlinConstantConditions")
                        if (!showClickableControllers && videoPlayerStateData.showBackToHistory) {
                            if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                            onBackToHistory()
                            return@onPreviewKeyEvent true
                        }

                        if (showSeekController) {
                            if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                            onGoTime(goTime)
                            if (!videoPlayer.isPlaying) onPlay()
                            moveState = SeekMoveState.Idle
                            showSeekController = false
                            return@onPreviewKeyEvent true
                        }

                        if (it.nativeKeyEvent.isLongPress) {
                            logger.fInfo { "[${it.key}] long press" }
                            showMenuController = true
                            return@onPreviewKeyEvent true
                        }

                        logger.fInfo { "[${it.key}] short press" }
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        if (videoPlayer.isPlaying) onPause() else onPlay()
                        return@onPreviewKeyEvent false
                    }

                    // KEYCODE_CENTER_LONG
                    // 一切设备上长按 DirectionCenter 键会是这个按键事件
                    Key(763) -> {
                        showMenuController = true
                        return@onPreviewKeyEvent true
                    }

                    Key.DirectionUp -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        showListController = true
                        return@onPreviewKeyEvent true
                    }

                    Key.Menu -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        showMenuController = !showMenuController
                        onRequestFocus()
                        return@onPreviewKeyEvent true
                    }

                    Key.Back -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }

                        if (!videoPlayer.isPlaying) {
                            logger.fInfo { "Exiting video player" }
                            onExit()
                            return@onPreviewKeyEvent true
                        }

                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastPressBack < 1000 * 3) {
                            logger.fInfo { "Exiting video player" }
                            onExit()
                        } else {
                            lastPressBack = currentTime
                            R.string.video_player_press_back_again_to_exit.toast(context)
                        }
                        return@onPreviewKeyEvent true
                    }

                    Key.MediaPlayPause -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        if (videoPlayer.isPlaying) onPause() else onPlay()
                        return@onPreviewKeyEvent true
                    }

                    Key.MediaPlay -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        if (!videoPlayer.isPlaying) onPlay()
                        return@onPreviewKeyEvent true
                    }

                    Key.MediaPause -> {
                        if (it.type == KeyEventType.KeyDown) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        if (videoPlayer.isPlaying) onPause()
                        return@onPreviewKeyEvent true
                    }

                    Key.MediaFastForward -> {
                        if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        openSeekController()
                        onTimeForward()
                    }

                    Key.MediaRewind -> {
                        if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        openSeekController()
                        onTimeBack()
                    }

                    Key.DirectionLeft -> {
                        if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        openSeekController()
                        onTimeBack()
                    }

                    Key.DirectionRight -> {
                        if (it.type == KeyEventType.KeyUp) return@onPreviewKeyEvent true
                        logger.info { "[${it.key} press]" }
                        openSeekController()
                        onTimeForward()
                    }
                }

                false
            }
    ) {
        content()
        if (BuildConfig.DEBUG) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Text(
                    modifier = Modifier.padding(8.dp),
                    text = videoPlayerDebugInfoData.debugInfo
                )
            }
        }
        BottomSubtitle()
        SkipTips()
        PlayStateTips(showPauseIcon = controls.focus == ControlsFocus.Hidden)
        ControlsLayer(
            state = controls,
            header = playerHeader(videoPlayerVideoInfoData, videoPlayerVideoInfoData.fromSeason),
            positionMs = if (showSeekController) goTime else videoPlayerSeekData.position,
            durationMs = videoPlayerSeekData.duration,
            bufferedMs = videoPlayerSeekData.duration * videoPlayerSeekData.bufferedPercentage / 100,
            isPlaying = videoPlayerStateData.isPlaying,
            buttonValues = controlButtonValues(context, videoPlayerConfigData, enabledDanmaku.isNotEmpty()),
            clock = "%02d:%02d".format(videoPlayerClockData.hour, videoPlayerClockData.minute),
            followed = videoPlayerVideoInfoData.upFollowed && !videoPlayerVideoInfoData.fromSeason,
            activeButtons = buildSet {
                add(ControlButtonId.Resolution)
                if (enabledDanmaku.isNotEmpty()) add(ControlButtonId.Danmaku)
                if (videoPlayerConfigData.subtitleOn) add(ControlButtonId.Subtitle)
            },
            disabledButtons = buildSet {
                if (previousVideo == null) add(ControlButtonId.Previous)
                if (nextVideo == null) add(ControlButtonId.Next)
            },
            seekPreview = videoPlayerVideoShotData.videoShot.takeIf { showSeekController }
        )
        SeekController(
            // While the layer is up, its own progress bar shows the seek target instead.
            show = showSeekController && controls.focus == ControlsFocus.Hidden,
            goTime = goTime,
            moveState = moveState
        )
        VideoListController(
            show = showListController,
            onPlayNewVideo = onPlayNewVideo
        )
        MenuController(
            show = showMenuController,
            initialNav = menuNav,
            initialPictureItem = menuPictureItem,
            onResolutionChange = onResolutionChange,
            onCodecChange = onCodecChange,
            onAspectRatioChange = onAspectRatioChange,
            onPlaySpeedChange = onPlaySpeedChange,
            onAudioChange = onAudioChange,
            onDanmakuSwitchChange = onDanmakuSwitchChange,
            onDanmakuSizeChange = onDanmakuSizeChange,
            onDanmakuOpacityChange = onDanmakuOpacityChange,
            onDanmakuAreaChange = onDanmakuAreaChange,
            onDanmakuMaskChange = onDanmakuMaskChange,
            onSubtitleChange = onSubtitleChange,
            onSubtitleSizeChange = onSubtitleSizeChange,
            onSubtitleBackgroundOpacityChange = onSubtitleBackgroundOpacityChange,
            onSubtitleBottomPadding = onSubtitleBottomPadding,
            onPlayModeChange = onPlayModeChange
        )
    }
}

private fun Key.toRemoteKey(): RemoteKey? = when (this) {
    Key.DirectionCenter, Key.Enter, Key.Spacebar -> RemoteKey.Ok
    Key.DirectionLeft -> RemoteKey.Left
    Key.DirectionRight -> RemoteKey.Right
    Key.DirectionUp -> RemoteKey.Up
    Key.DirectionDown -> RemoteKey.Down
    Key.Back -> RemoteKey.Back
    Key.Menu -> RemoteKey.Menu
    else -> null
}

private val VideoPlayerConfigData.subtitleOn: Boolean
    get() = currentSubtitleId != -1L && currentSubtitleId != 0L

private fun controlButtonValues(
    context: Context,
    config: VideoPlayerConfigData,
    danmakuOn: Boolean
): Map<ControlButtonId, String> {
    val current = config.availableVideoList
        .filterIsInstance<VideoListItemData>()
        .firstOrNull { it.cid == config.currentVideoCid }
    val episode = when (current) {
        null -> ""
        is VideoListPart -> "P${current.index + 1}"
        else -> "EP${current.index + 1}"
    }
    return mapOf(
        ControlButtonId.Resolution to config.currentResolution.getShortDisplayName(context),
        ControlButtonId.Speed to "${(config.currentVideoSpeed * 100).roundToInt() / 100f}×",
        ControlButtonId.Danmaku to if (danmakuOn) "开" else "关",
        ControlButtonId.Subtitle to if (config.subtitleOn) "开" else "关",
        ControlButtonId.Episodes to episode,
    )
}
