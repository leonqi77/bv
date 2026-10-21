package dev.aaa1115910.bv.player.tv.controller

import dev.aaa1115910.bv.player.entity.VideoPlayerVideoInfoData
import dev.aaa1115910.bv.player.tv.controller.ControlsAction.Activate
import dev.aaa1115910.bv.player.tv.controller.ControlsAction.None
import dev.aaa1115910.bv.player.tv.controller.ControlsAction.OpenMenu
import dev.aaa1115910.bv.player.tv.controller.ControlsAction.OpenPlaylist
import dev.aaa1115910.bv.player.tv.controller.ControlsAction.PassThrough
import dev.aaa1115910.bv.player.tv.controller.ControlsAction.SeekBack
import dev.aaa1115910.bv.player.tv.controller.ControlsAction.SeekForward
import dev.aaa1115910.bv.player.tv.controller.ControlsAction.TogglePlay
import dev.aaa1115910.bv.player.tv.controller.RemoteKey.Back
import dev.aaa1115910.bv.player.tv.controller.RemoteKey.Down
import dev.aaa1115910.bv.player.tv.controller.RemoteKey.Left
import dev.aaa1115910.bv.player.tv.controller.RemoteKey.Menu
import dev.aaa1115910.bv.player.tv.controller.RemoteKey.Ok
import dev.aaa1115910.bv.player.tv.controller.RemoteKey.Right
import dev.aaa1115910.bv.player.tv.controller.RemoteKey.Up
import kotlin.test.Test
import kotlin.test.assertEquals

class ControlsLayerStateTest {
    private val H = ControlsState(ControlsFocus.Hidden)
    private val P = ControlsState(ControlsFocus.ProgressBar)
    private fun B(i: Int) = ControlsState(ControlsFocus.Buttons, i)

    private fun info(
        title: String = "Title",
        partTitle: String = "PartTitle",
        upName: String = "",
        upFace: String = "",
    ) = VideoPlayerVideoInfoData(
        title = title, partTitle = partTitle, upName = upName, upFace = upFace
    )

    @Test
    fun hiddenPassesThroughExceptDown() {
        for (k in listOf(Ok, Left, Right, Up, Back, Menu)) assertEquals(H to PassThrough, H.onKey(k, 9))
        assertEquals(P to None, H.onKey(Down, 9))
    }

    @Test
    fun progressBar() {
        assertEquals(P to SeekBack, P.onKey(Left, 9))
        assertEquals(P to SeekForward, P.onKey(Right, 9))
        assertEquals(P to TogglePlay, P.onKey(Ok, 9))
        assertEquals(H to OpenPlaylist, P.onKey(Up, 9))
        assertEquals(B(0) to None, P.onKey(Down, 9))
        assertEquals(H to None, P.onKey(Back, 9))
        assertEquals(H to OpenMenu, P.onKey(Menu, 9))
    }

    @Test
    fun buttons() {
        assertEquals(B(1) to None, B(0).onKey(Right, 9))
        assertEquals(B(0) to None, B(0).onKey(Left, 9))
        assertEquals(B(8) to None, B(8).onKey(Right, 9))
        assertEquals(B(4) to Activate(4), B(4).onKey(Ok, 9))
        assertEquals(P to None, B(3).onKey(Up, 9))
        assertEquals(H to None, B(3).onKey(Back, 9))
        assertEquals(B(3) to None, B(3).onKey(Down, 9))
    }

    @Test
    fun onPaused() {
        assertEquals(P, H.onPaused())
        assertEquals(B(2), B(2).onPaused())
    }

    @Test
    fun header() {
        assertEquals(
            PlayerHeader("影视飓风", "f", "标题"),
            playerHeader(info(upName = "影视飓风", upFace = "f", title = "标题"), false)
        )
        assertEquals(
            PlayerHeader("全职猎人", "", "第3话 自尊心×荒海×决斗"),
            playerHeader(info(title = "全职猎人", partTitle = "第3话 自尊心×荒海×决斗"), true)
        )
        assertEquals(PlayerHeader("", "", "标题"), playerHeader(info(title = "标题"), false))
    }
}
