package dev.aaa1115910.bv.entity.carddata

import dev.aaa1115910.biliapi.entity.ugc.UgcItem
import dev.aaa1115910.biliapi.entity.user.DynamicVideo
import dev.aaa1115910.biliapi.entity.video.Dimension
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HomeFeaturedItemTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val tz = TimeZone.getTimeZone(zone)
    private val nowSec = ZonedDateTime.of(2026, 10, 21, 12, 0, 0, 0, zone).toEpochSecond()
    private val now = nowSec * 1000

    private fun item(play: Int?, pubTime: Long, durationSec: Int, is4K: Boolean = false) =
        HomeFeaturedItem(
            aid = 1, title = "t", cover = "c", upName = "u", upFace = "",
            play = play, pubTime = pubTime, durationSec = durationSec, is4K = is4K
        )

    @Test
    fun statsLineJoinsPlayTimeAndDuration() {
        assertEquals(
            "103万播放 · 7小时前 · 24:52",
            item(play = 1035185, pubTime = nowSec - 7 * 3600, durationSec = 1492).statsLine(now, tz)
        )
    }

    @Test
    fun statsLineSkipsUnknownParts() {
        assertEquals("24:52", item(play = null, pubTime = 0, durationSec = 1492).statsLine(now, tz))
        assertEquals("", item(play = null, pubTime = 0, durationSec = 0).statsLine(now, tz))
        assertEquals("520播放", item(play = 520, pubTime = 0, durationSec = 0).statsLine(now, tz))
    }

    @Test
    fun statsLineExcludes4K() {
        assertEquals("24:52", item(play = null, pubTime = 0, durationSec = 1492, is4K = true).statsLine(now, tz))
    }

    @Test
    fun durationFormats() {
        assertEquals("1:28:44", item(play = null, pubTime = 0, durationSec = 5324).statsLine(now, tz))
        assertEquals("1:14", item(play = null, pubTime = 0, durationSec = 74).statsLine(now, tz))
        assertEquals("0:05", item(play = null, pubTime = 0, durationSec = 5).statsLine(now, tz))
        assertEquals("1:00:00", item(play = null, pubTime = 0, durationSec = 3600).statsLine(now, tz))
    }

    @Test
    fun fromUgcItemMapsFields() {
        val ugc = UgcItem(
            aid = 42, title = "标题", cover = "cover", author = "影视飓风",
            play = 1035185, danmaku = 10, duration = 1492,
            dimension = Dimension(3840, 2160), authorFace = "face", pubDate = 1700000000L
        )
        val item = HomeFeaturedItem.fromUgcItem(ugc)
        assertEquals(42, item.aid)
        assertEquals("标题", item.title)
        assertEquals("cover", item.cover)
        assertEquals("影视飓风", item.upName)
        assertEquals("face", item.upFace)
        assertEquals(1035185, item.play)
        assertEquals(1700000000L, item.pubTime)
        assertEquals(1492, item.durationSec)
        assertTrue(item.is4K)
        assertEquals(null, item.description)
    }

    @Test
    fun fromUgcItemMapsDescription() {
        val base = UgcItem(
            aid = 1, title = "t", cover = "c", author = "a", play = 1, danmaku = 1, duration = 1
        )
        assertEquals("简介", HomeFeaturedItem.fromUgcItem(base.copy(desc = "  简介 ")).description)
        assertEquals(null, HomeFeaturedItem.fromUgcItem(base.copy(desc = "   ")).description)
    }

    @Test
    fun fromUgcItemMapsUnknownPlayToNull() {
        val ugc = UgcItem(
            aid = 1, title = "t", cover = "c", author = "a", play = -1, danmaku = -1, duration = 0
        )
        val item = HomeFeaturedItem.fromUgcItem(ugc)
        assertEquals(null, item.play)
        assertFalse(item.is4K)
        assertEquals(0L, item.pubTime)
    }

    @Test
    fun fromDynamicVideoMapsFields() {
        val video = DynamicVideo(
            aid = 7, cid = 0, title = "动态", cover = "dc", author = "UP",
            duration = 600, play = -1, danmaku = 0, avatar = "av", time = 1700000000L
        )
        val item = HomeFeaturedItem.fromDynamicVideo(video)
        assertEquals(7, item.aid)
        assertEquals("动态", item.title)
        assertEquals("dc", item.cover)
        assertEquals("UP", item.upName)
        assertEquals("av", item.upFace)
        assertEquals(null, item.play)
        assertEquals(1700000000L, item.pubTime)
        assertEquals(600, item.durationSec)
        assertFalse(item.is4K)
    }
}
