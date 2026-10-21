package dev.aaa1115910.bv.player.tv.controller

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import dev.aaa1115910.biliapi.entity.video.Subtitle
import dev.aaa1115910.bv.player.entity.Audio
import dev.aaa1115910.bv.player.entity.DanmakuType
import dev.aaa1115910.bv.player.entity.PlayMode
import dev.aaa1115910.bv.player.entity.Resolution
import dev.aaa1115910.bv.player.entity.VideoAspectRatio
import dev.aaa1115910.bv.player.entity.VideoCodec
import dev.aaa1115910.bv.player.entity.VideoPlayerMenuNavItem
import dev.aaa1115910.bv.player.entity.VideoPlayerPictureMenuItem
import dev.aaa1115910.bv.player.tv.PlayerColors
import dev.aaa1115910.bv.player.tv.controller.playermenu.MenuActions
import dev.aaa1115910.bv.player.tv.controller.playermenu.MenuPanelState
import dev.aaa1115910.bv.player.tv.controller.playermenu.SettingsPanel

/**
 * The settings side panel. A targeted open ([initialPictureItem], or [initialNav] for a group) shows that
 * item's options directly; Back then returns to the list (see [MenuPanelState.back]).
 */
@Composable
fun MenuController(
    modifier: Modifier = Modifier,
    show: Boolean,
    state: MenuPanelState = remember { MenuPanelState() },
    initialNav: VideoPlayerMenuNavItem? = null,
    initialPictureItem: VideoPlayerPictureMenuItem? = null,
    onResolutionChange: (Resolution) -> Unit = {},
    onCodecChange: (VideoCodec) -> Unit = {},
    onAspectRatioChange: (VideoAspectRatio) -> Unit,
    onPlaySpeedChange: (Float) -> Unit = {},
    onAudioChange: (Audio) -> Unit,
    onDanmakuSwitchChange: (List<DanmakuType>) -> Unit,
    onDanmakuSizeChange: (Float) -> Unit,
    onDanmakuOpacityChange: (Float) -> Unit,
    onDanmakuAreaChange: (Float) -> Unit,
    onDanmakuMaskChange: (Boolean) -> Unit = {},
    onSubtitleChange: (Subtitle) -> Unit,
    onSubtitleSizeChange: (TextUnit) -> Unit,
    onSubtitleBackgroundOpacityChange: (Float) -> Unit,
    onSubtitleBottomPadding: (Dp) -> Unit,
    onPlayModeChange: (PlayMode) -> Unit
) {
    val actions = remember(
        onResolutionChange, onCodecChange, onAspectRatioChange, onPlaySpeedChange, onAudioChange,
        onDanmakuSwitchChange, onDanmakuSizeChange, onDanmakuOpacityChange, onDanmakuAreaChange,
        onDanmakuMaskChange, onSubtitleChange, onSubtitleSizeChange, onSubtitleBackgroundOpacityChange,
        onSubtitleBottomPadding, onPlayModeChange
    ) {
        MenuActions(
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

    // Reset to the requested page each time the panel opens, before its content is composed.
    remember(show, initialNav, initialPictureItem) {
        if (show) state.open(initialNav, initialPictureItem)
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visible = show,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it }
        ) {
            SettingsPanel(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.31f)
                    .background(PlayerColors.ScrimStrong)
                    .padding(horizontal = 19.dp, vertical = 30.dp),
                state = state,
                actions = actions
            )
        }
    }
}
