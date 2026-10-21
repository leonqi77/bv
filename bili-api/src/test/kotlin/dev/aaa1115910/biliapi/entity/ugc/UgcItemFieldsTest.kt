package dev.aaa1115910.biliapi.entity.ugc

import dev.aaa1115910.biliapi.http.BiliHttpApi
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

internal class UgcItemFieldsTest {
    @Test
    fun `fromVideoInfo carries avatar and publish date`() = runBlocking {
        val item = UgcItem.fromVideoInfo(BiliHttpApi.getPopularVideoData().data!!.list.first())
        assertTrue(item.authorFace.startsWith("http"))
        assertTrue(item.pubDate > 0)
    }
}
