package dev.aaa1115910.bv.entity.carddata

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class VideoCardDataTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val nowSec = ZonedDateTime.of(2026, 10, 21, 12, 0, 0, 0, zone).toEpochSecond()

    private fun card(upName: String, play: Int?, pubTime: Int?) = VideoCardData(
        avid = 1, title = "t", cover = "", upName = upName, play = play, pubTime = pubTime
    )

    private fun VideoCardData.meta() = metaLine(nowSec * 1000, zone)

    @Test
    fun metaLineJoinsAll() {
        assertEquals(
            "影视飓风 · 103万播放 · 7小时前",
            card("影视飓风", 1035185, (nowSec - 7 * 3600).toInt()).meta()
        )
    }

    @Test
    fun metaLineSkipsBlanks() {
        assertEquals("影视飓风", card("影视飓风", null, null).meta())
        assertEquals("103万播放", card("", 1035185, 0).meta())
    }
}
