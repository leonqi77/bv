package dev.aaa1115910.biliapi.repositories

import dev.aaa1115910.biliapi.entity.ApiType
import dev.aaa1115910.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 不需要登录的 Web 接口回归测试，直接请求真实接口
 *
 * Bilibili 会对 `/x/web-interface/view` 和 `/x/web-interface/view/detail` 返回
 * HTTP 412 `{"code":-412,"message":"request was banned"}`，导致所有视频都无法打开
 */
class VideoDetailWebApiTest {
    // aid 超出 Int 范围
    private val aid = 117400196356132L

    private val authRepository = AuthRepository()
    private val videoDetailRepository = VideoDetailRepository(
        authRepository = authRepository,
        channelRepository = ChannelRepository(),
        favoriteRepository = FavoriteRepository(authRepository)
    )

    @Test
    fun `get video info is not banned`() = runBlocking {
        val response = BiliHttpApi.getVideoInfo(av = aid)
        assertEquals(0, response.code, response.message)
        assertEquals(aid, response.data!!.aid)
    }

    @Test
    fun `get video detail with web api`() = runBlocking {
        val videoDetail = videoDetailRepository.getVideoDetail(aid = aid, preferApiType = ApiType.Web)
        assertEquals(aid, videoDetail.aid)
        assertTrue(videoDetail.pages.isNotEmpty())
        assertTrue(videoDetail.relatedVideos.isNotEmpty())
        assertTrue(videoDetail.tags.isNotEmpty())
    }
}
