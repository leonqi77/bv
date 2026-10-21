package dev.aaa1115910.biliapi.entity.video

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DimensionTest {
    @Test
    fun `quality label uses the long side`() {
        assertEquals("4K", Dimension(3840, 2160).qualityLabel)
        assertEquals("4K", Dimension(2160, 3840).qualityLabel)
        assertEquals("4K", Dimension(3840, 1600).qualityLabel)
        assertEquals("2K", Dimension(2560, 1440).qualityLabel)
        assertEquals("1080P", Dimension(1920, 1080).qualityLabel)
        assertEquals("1080P", Dimension(1080, 1920).qualityLabel)
        assertEquals("720P", Dimension(1280, 720).qualityLabel)
        assertEquals("480P", Dimension(854, 480).qualityLabel)
        assertEquals("360P", Dimension(640, 360).qualityLabel)
        assertEquals("", Dimension(0, 0).qualityLabel)
    }

    @Test
    fun `is 4K`() {
        assertTrue(Dimension(3840, 2160).is4K)
        assertTrue(Dimension(2160, 3840).is4K)
        assertFalse(Dimension(2560, 1440).is4K)
    }
}
