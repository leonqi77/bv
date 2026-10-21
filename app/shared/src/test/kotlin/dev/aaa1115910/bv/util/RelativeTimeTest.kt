package dev.aaa1115910.bv.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class RelativeTimeTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val nowSec = ZonedDateTime.of(2026, 10, 21, 12, 0, 0, 0, zone).toEpochSecond()
    private val nowMillis = nowSec * 1000

    private fun fmt(sec: Long) = formatRelativeTime(sec, nowMillis, TimeZone.getTimeZone(zone))
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

    @Test
    fun boundaries() {
        assertEquals("刚刚", fmt(nowSec - 59))
        assertEquals("1分钟前", fmt(nowSec - 60))
        assertEquals("59分钟前", fmt(nowSec - 3599))
        assertEquals("1小时前", fmt(nowSec - 3600))
        assertEquals("6天前", fmt(nowSec - 7 * 86400 + 1))
        assertEquals("10-14", fmt(nowSec - 7 * 86400))
        assertEquals("刚刚", fmt(nowSec + 3600))
    }

    @Test
    fun yearBoundary() {
        val now = ZonedDateTime.of(2026, 1, 10, 12, 0, 0, 0, zone)
        val f = { s: Long -> formatRelativeTime(s, now.toEpochSecond() * 1000, TimeZone.getTimeZone(zone)) }
        assertEquals("01-01", f(day(2026, 1, 1)))
        assertEquals("2025-12-31", f(day(2025, 12, 31)))
    }
}
