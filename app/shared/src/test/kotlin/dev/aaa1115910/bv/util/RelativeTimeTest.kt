package dev.aaa1115910.bv.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val nowSec = ZonedDateTime.of(2026, 10, 21, 12, 0, 0, 0, zone).toEpochSecond()
    private val nowMillis = nowSec * 1000

    private fun fmt(sec: Long) = formatRelativeTime(sec, nowMillis, zone)
    private fun day(y: Int, m: Int, d: Int) =
        LocalDate.of(y, m, d).atStartOfDay(zone).toEpochSecond()

    @Test
    fun relative() {
        assertEquals("刚刚", fmt(nowSec - 30))
        assertEquals("5分钟前", fmt(nowSec - 5 * 60))
        assertEquals("7小时前", fmt(nowSec - 7 * 3600))
        assertEquals("2天前", fmt(nowSec - 2 * 86400))
    }

    @Test
    fun absolute() {
        assertEquals("10-01", fmt(day(2026, 10, 1)))
        assertEquals("2025-12-31", fmt(day(2025, 12, 31)))
    }

    @Test
    fun invalid() {
        assertEquals("", fmt(0))
        assertEquals("", fmt(-5))
    }
}
