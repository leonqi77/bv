package dev.aaa1115910.bv.viewmodel.season

import dev.aaa1115910.biliapi.entity.video.season.Episode
import dev.aaa1115910.biliapi.entity.video.season.SeasonDetail
import dev.aaa1115910.biliapi.entity.video.season.Section
import kotlin.test.Test
import kotlin.test.assertEquals

class SeasonPageTest {
    private fun ep(id: Int, title: String, durationMs: Int = 1_385_000, cover: String = "ep${id}cover") = Episode(
        id = id, aid = id.toLong(), bvid = "bv$id", cid = id.toLong(), epid = id,
        title = title, longTitle = "", cover = cover, duration = durationMs,
        dimension = null, pages = emptyList()
    )

    private fun detail(
        eps: List<Episode> = emptyList(),
        sections: List<Section> = emptyList(),
        wideCover: String = "",
        cover: String = "poster",
        rating: Float? = null,
        areas: List<String> = emptyList(),
        publishDate: String = "",
        styles: List<String> = emptyList(),
        newEpDesc: String = "已完结, 全92话"
    ) = SeasonDetail(
        title = "t", styles = styles, cover = cover, description = "", subType = 1, seasonId = 1,
        userStatus = SeasonDetail.UserStatus(follow = false, pay = false),
        publish = SeasonDetail.Publish(isPublished = true, publishDate = publishDate),
        newEpDesc = newEpDesc, episodes = eps, sections = sections,
        rating = rating, areas = areas, wideCover = wideCover
    )

    private fun progress(lastEpId: Int, lastTime: Int) =
        SeasonDetail.UserStatus.Progress(lastEpId = lastEpId, lastEpIndex = "", lastTime = lastTime)

    private val eps = (1..5).map { ep(it, it.toString()) }

    @Test
    fun resumeShowsRemainingMinutes() {
        assertEquals(
            PlayButtonState("继续播放 第3话 · 剩 12 分钟", true, eps[2].id),
            playButtonState(detail(eps = eps), progress(eps[2].id, 665), 0)
        )
    }

    @Test
    fun remainingMinutesRoundUp() {
        assertEquals(
            PlayButtonState("继续播放 第3话 · 剩 13 分钟", true, eps[2].id),
            playButtonState(detail(eps = eps), progress(eps[2].id, 660), 0)
        )
    }

    @Test
    fun remainingMinutesAtLeastOne() {
        assertEquals(
            "继续播放 第3话 · 剩 1 分钟",
            playButtonState(detail(eps = eps), progress(eps[2].id, 5000), 0).text
        )
    }

    @Test
    fun playFirstWithoutProgress() {
        assertEquals(PlayButtonState("播放 第1话", true, eps[0].id), playButtonState(detail(eps = eps), null, 0))
    }

    @Test
    fun playFirstWhenProgressEpisodeMissing() {
        assertEquals(
            PlayButtonState("播放 第1话", true, eps[0].id),
            playButtonState(detail(eps = eps), progress(999, 10), 3)
        )
    }

    @Test
    fun allHiddenDisabled() {
        assertEquals(
            PlayButtonState("正片均需大会员或付费观看", false, null),
            playButtonState(detail(eps = emptyList()), null, 200)
        )
    }

    @Test
    fun allHiddenDisabledEvenWithSections() {
        val clip = ep(88, "出自65话")
        assertEquals(
            PlayButtonState("正片均需大会员或付费观看", false, null),
            playButtonState(detail(sections = listOf(Section(1, "经典回顾", listOf(clip)))), progress(clip.id, 10), 200)
        )
    }

    @Test
    fun resumeFromSectionEpisode() {
        val pv = ep(77, "PV1")
        assertEquals(
            PlayButtonState("继续播放 PV1 · 剩 12 分钟", true, pv.id),
            playButtonState(detail(eps = eps, sections = listOf(Section(1, "s", listOf(pv)))), progress(pv.id, 665), 0)
        )
    }

    @Test
    fun noFeatureFilm() {
        assertEquals(
            PlayButtonState("还没有正片", false, null),
            playButtonState(detail(eps = emptyList()), null, 0)
        )
    }

    @Test
    fun pvTitleUsedAsIs() {
        val pv = ep(77, "PV1")
        assertEquals(PlayButtonState("播放 PV1", true, pv.id), playButtonState(detail(eps = listOf(pv)), null, 0))
    }

    @Test
    fun ranges() {
        assertEquals(emptyList(), episodeRanges(20))
        assertEquals(listOf(1..20, 21..40, 41..60, 61..80, 81..92), episodeRanges(92))
    }

    @Test
    fun backdropOrder() {
        assertEquals(SeasonBackdrop.Image("wide"), seasonBackdrop(detail(wideCover = "wide")))
        assertEquals(SeasonBackdrop.Image("ep1cover"), seasonBackdrop(detail(eps = listOf(ep(1, "1")))))
        assertEquals(
            SeasonBackdrop.Image("seccover"),
            seasonBackdrop(detail(sections = listOf(Section(1, "s", listOf(ep(9, "PV1", cover = "seccover"))))))
        )
        assertEquals(SeasonBackdrop.BlurredPoster("poster"), seasonBackdrop(detail(cover = "poster")))
    }

    @Test
    fun factsFull() {
        assertEquals(
            SeasonFacts("★ 9.8", listOf("1999", "日本", "已完结, 全92话"), listOf("奇幻", "热血")),
            seasonFacts(
                detail(rating = 9.8f, areas = listOf("日本"), publishDate = "1999-10-16", styles = listOf("奇幻", "热血"))
            )
        )
    }

    @Test
    fun factsMissing() {
        assertEquals(SeasonFacts(null, listOf("已完结, 全92话"), emptyList()), seasonFacts(detail()))
    }
}
