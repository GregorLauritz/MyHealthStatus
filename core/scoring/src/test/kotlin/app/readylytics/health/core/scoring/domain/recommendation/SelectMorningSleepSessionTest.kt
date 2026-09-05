package app.readylytics.health.core.scoring.domain.recommendation

import app.readylytics.health.core.model.domain.model.SleepSession
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val HOUR_MS = 3_600_000L

class SelectMorningSleepSessionTest {
    private val selector = SelectMorningSleepSession()

    private fun session(
        id: String,
        endMs: Long,
        durationHours: Long = 8,
    ): SleepSession =
        SleepSession(
            id = id,
            startTime = endMs - durationHours * HOUR_MS,
            endTime = endMs,
            durationMinutes = (durationHours * 60).toInt(),
            efficiency = 0.9f,
            deepSleepMinutes = 90,
            remSleepMinutes = 90,
            lightSleepMinutes = 280,
            awakeMinutes = 20,
        )

    private fun endOf(
        date: LocalDate,
        zone: ZoneId,
        hour: Int,
        minute: Int = 0,
    ): Long = date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun choosesClosestRecordedWake() {
        val date = LocalDate.of(2026, 9, 5)
        val zone = ZoneId.of("UTC")
        val end = date.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
        val main = session("main", end)
        val later = main.copy(id = "later", startTime = end + HOUR_MS, endTime = end + 7 * HOUR_MS)
        assertEquals("main", selector.select(listOf(later, main), date, zone, 420)?.id)
    }

    @Test
    fun `returns null when no session ends on the target date`() {
        val date = LocalDate.of(2026, 9, 5)
        val zone = ZoneId.of("UTC")
        val yesterday = session("yesterday", endOf(date.minusDays(1), zone, 7))
        val tomorrow = session("tomorrow", endOf(date.plusDays(1), zone, 7))
        assertNull(selector.select(listOf(yesterday, tomorrow), date, zone, 420))
    }

    @Test
    fun `returns null for an empty candidate list`() {
        assertNull(selector.select(emptyList(), LocalDate.of(2026, 9, 5), ZoneId.of("UTC"), 420))
    }

    @Test
    fun `prefers the later segment of a split night when it is closer to the usual wake`() {
        val date = LocalDate.of(2026, 9, 5)
        val zone = ZoneId.of("UTC")
        val firstSegment = session("split-a", endOf(date, zone, 3), durationHours = 4)
        val secondSegment = session("split-b", endOf(date, zone, 7), durationHours = 3)
        assertEquals(
            "split-b",
            selector.select(listOf(firstSegment, secondSegment), date, zone, 420)?.id,
        )
    }

    @Test
    fun `measures distance across the midnight boundary rather than linearly`() {
        val date = LocalDate.of(2026, 9, 5)
        val zone = ZoneId.of("UTC")
        // Usual wake 00:15. A 23:45 wake is 30 clock-minutes away across midnight; an 03:00 wake is
        // 165 minutes away. Linear distance would pick 03:00.
        val justBeforeMidnight = session("late", endOf(date, zone, 23, 45))
        val earlyMorning = session("early", endOf(date, zone, 3))
        assertEquals(
            "late",
            selector.select(listOf(earlyMorning, justBeforeMidnight), date, zone, 15)?.id,
        )
    }

    @Test
    fun `breaks equal distances by earliest end time`() {
        val date = LocalDate.of(2026, 9, 5)
        val zone = ZoneId.of("UTC")
        val before = session("before", endOf(date, zone, 6))
        val after = session("after", endOf(date, zone, 8))
        assertEquals("before", selector.select(listOf(after, before), date, zone, 420)?.id)
        assertEquals("before", selector.select(listOf(before, after), date, zone, 420)?.id)
    }

    @Test
    fun `breaks identical end times by stable id`() {
        val date = LocalDate.of(2026, 9, 5)
        val zone = ZoneId.of("UTC")
        val end = endOf(date, zone, 7)
        val alpha = session("aaa", end)
        val beta = session("bbb", end)
        assertEquals("aaa", selector.select(listOf(beta, alpha), date, zone, 420)?.id)
        assertEquals("aaa", selector.select(listOf(alpha, beta), date, zone, 420)?.id)
    }

    @Test
    fun `resolves the local date in the scoring zone, not UTC`() {
        val zone = ZoneId.of("Pacific/Auckland")
        val date = LocalDate.of(2026, 9, 5)
        val local = session("local", endOf(date, zone, 7))
        // 22:00 UTC the previous day is 10:00 local on the target date in Auckland (UTC+12).
        assertEquals("local", selector.select(listOf(local), date, zone, 420)?.id)
        assertNull(selector.select(listOf(local), date, ZoneId.of("UTC"), 420))
    }

    @Test
    fun `handles the spring-forward DST transition without skipping the morning session`() {
        val zone = ZoneId.of("Europe/Berlin")
        // 2026-03-29 loses 02:00-03:00 local time in Berlin.
        val date = LocalDate.of(2026, 3, 29)
        val morning = session("dst-morning", endOf(date, zone, 7))
        val nap = session("dst-nap", endOf(date, zone, 15), durationHours = 1)
        assertEquals("dst-morning", selector.select(listOf(nap, morning), date, zone, 420)?.id)
    }

    @Test
    fun `handles the autumn fall-back DST transition without skipping the morning session`() {
        val zone = ZoneId.of("Europe/Berlin")
        // 2026-10-25 repeats 02:00-03:00 local time in Berlin.
        val date = LocalDate.of(2026, 10, 25)
        val morning = session("dst-morning", endOf(date, zone, 7))
        val nap = session("dst-nap", endOf(date, zone, 15), durationHours = 1)
        assertEquals("dst-morning", selector.select(listOf(nap, morning), date, zone, 420)?.id)
    }

    @Test
    fun `normalizes an out-of-range usual wake time onto the clock`() {
        val date = LocalDate.of(2026, 9, 5)
        val zone = ZoneId.of("UTC")
        val morning = session("morning", endOf(date, zone, 7))
        val nap = session("nap", endOf(date, zone, 14), durationHours = 1)
        // 1860 is 07:00 in the circadian baseline's "past midnight" normalized space.
        assertEquals("morning", selector.select(listOf(nap, morning), date, zone, 1860)?.id)
    }
}
