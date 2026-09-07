package app.readylytics.health.core.scoring.domain.recommendation

import app.readylytics.health.core.model.domain.model.SleepSession
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.abs

/**
 * Picks the sleep record that represents "this morning" for the workout recommendation.
 *
 * Pure: it performs no reads and no clock access. [sessions] is whatever the caller already loaded
 * — source filtering (which app/device a record came from) is applied in Room before this point and
 * is deliberately not re-applied here.
 *
 * The candidate set is every session whose *end* falls on [date] in [zone]. Among those, the winner
 * is the one whose wake time is closest to [usualWakeMinutes] measured around the clock face, so a
 * 23:45 wake is 30 minutes from a 00:15 habit rather than 1,410. Ties break by earliest end time,
 * then by stable id, so the result never depends on the input ordering.
 *
 * A later nap therefore cannot displace the morning record just by being more recent, and it can
 * only win at all if it lands closer to the habitual wake time than the night's own end does.
 */
class SelectMorningSleepSession {
    fun select(
        sessions: List<SleepSession>,
        date: LocalDate,
        zone: ZoneId,
        usualWakeMinutes: Int,
    ): SleepSession? {
        val anchorMinutes = Math.floorMod(usualWakeMinutes, MINUTES_PER_DAY)
        return sessions
            .filter { Instant.ofEpochMilli(it.endTime).atZone(zone).toLocalDate() == date }
            .minWithOrNull(
                compareBy(
                    { clockDistanceMinutes(it.endTime, zone, anchorMinutes) },
                    { it.endTime },
                    { it.id },
                ),
            )
    }

    private fun clockDistanceMinutes(
        endTimeMs: Long,
        zone: ZoneId,
        anchorMinutes: Int,
    ): Int {
        val wakeMinutes =
            Instant.ofEpochMilli(endTimeMs).atZone(zone).let { it.hour * 60 + it.minute }
        val direct = abs(wakeMinutes - anchorMinutes)
        return minOf(direct, MINUTES_PER_DAY - direct)
    }

    private companion object {
        const val MINUTES_PER_DAY = 1440
    }
}
