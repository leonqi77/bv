package dev.aaa1115910.bv.player.tv.controller

import dev.aaa1115910.bv.player.entity.VideoPlayerVideoInfoData

enum class ControlsFocus { Hidden, ProgressBar, Buttons }

enum class RemoteKey { Ok, Left, Right, Up, Down, Back, Menu }

data class ControlsState(
    val focus: ControlsFocus = ControlsFocus.Hidden,
    val buttonIndex: Int = 0
)

sealed interface ControlsAction {
    data object PassThrough : ControlsAction
    data object None : ControlsAction
    data object TogglePlay : ControlsAction
    data object SeekBack : ControlsAction
    data object SeekForward : ControlsAction
    data object OpenPlaylist : ControlsAction
    data object OpenMenu : ControlsAction
    data class Activate(val index: Int) : ControlsAction
}

private val Hidden = ControlsState(ControlsFocus.Hidden)
private val ProgressBar = ControlsState(ControlsFocus.ProgressBar)

/** Reduces a remote key press to the next controls-layer state and the action to perform. */
fun ControlsState.onKey(key: RemoteKey, buttonCount: Int): Pair<ControlsState, ControlsAction> =
    when (focus) {
        ControlsFocus.Hidden -> when (key) {
            RemoteKey.Down -> ProgressBar to ControlsAction.None
            else -> this to ControlsAction.PassThrough
        }

        ControlsFocus.ProgressBar -> when (key) {
            RemoteKey.Left -> this to ControlsAction.SeekBack
            RemoteKey.Right -> this to ControlsAction.SeekForward
            RemoteKey.Ok -> this to ControlsAction.TogglePlay
            RemoteKey.Up -> Hidden to ControlsAction.OpenPlaylist
            RemoteKey.Down -> ControlsState(ControlsFocus.Buttons, 0) to ControlsAction.None
            RemoteKey.Back -> Hidden to ControlsAction.None
            RemoteKey.Menu -> Hidden to ControlsAction.OpenMenu
        }

        ControlsFocus.Buttons -> when (key) {
            RemoteKey.Left -> copy(buttonIndex = (buttonIndex - 1).coerceAtLeast(0)) to ControlsAction.None
            RemoteKey.Right -> copy(
                buttonIndex = (buttonIndex + 1).coerceAtMost((buttonCount - 1).coerceAtLeast(0))
            ) to ControlsAction.None
            RemoteKey.Ok -> this to ControlsAction.Activate(buttonIndex)
            RemoteKey.Up -> ProgressBar to ControlsAction.None
            RemoteKey.Back -> Hidden to ControlsAction.None
            RemoteKey.Down -> this to ControlsAction.None
            RemoteKey.Menu -> Hidden to ControlsAction.OpenMenu
        }
    }

/** Pausing while the layer is hidden reveals the progress bar. */
fun ControlsState.onPaused(): ControlsState =
    if (focus == ControlsFocus.Hidden) ProgressBar else this

data class PlayerHeader(val small: String, val smallFace: String, val title: String)

fun playerHeader(info: VideoPlayerVideoInfoData, fromSeason: Boolean): PlayerHeader = when {
    fromSeason -> PlayerHeader(info.title, "", info.partTitle)
    info.upName.isNotEmpty() -> PlayerHeader(info.upName, info.upFace, info.title)
    else -> PlayerHeader("", "", info.title)
}
