package app.readylytics.health.core.model.domain.sync

import java.time.LocalDate

/**
 * R2-CACHE-001: bounds how far forward a rollup or retention deletion's date range must be
 * recomputed to cover every scoring lookback that could read the changed data.
 *
 * `daily_summaries` is derived from raw health data via walk-forward scoring lookbacks (acute/
 * chronic TRIMP, baselines, HRV sigma, circadian regularity, ...). When a tier rollup
 * (`DataRollupManager`, in the data layer) or a retention deletion (`RetentionCleanup`, in the
 * data layer) mutates raw data underneath an already-computed day, every later day whose lookback
 * window could have read that raw data is stale and must be recomputed. [affectedRange] widens the
 * touched date range forward by the longest such lookback so callers can enqueue a single bounded
 * recompute-only resync that covers every dependent day, capped at today (recomputing the future
 * is meaningless).
 */
object ScoreInvalidation {
    /**
     * Longest scoring lookback that reads historical data, in days — see
     * `ScoreInvalidationTest`'s depth-guard, which fails the build if a new lookback constant
     * exceeds this.
     */
    const val MAX_DEPENDENT_WINDOW_DAYS = 84L

    /** A closed date range `[start, endInclusive]`, plain data class per this codebase's
     * [app.readylytics.health.core.model.domain.util.RetentionBounds.HistoricalWindow] precedent
     * (no `LocalDate.rangeTo` operator extension exists here). */
    data class AffectedRange(val start: LocalDate, val endInclusive: LocalDate)

    /**
     * Widens [changed] forward by [MAX_DEPENDENT_WINDOW_DAYS] so every scoring lookback that could
     * have read the changed data is covered, capped so the result never extends past [today].
     */
    fun affectedRange(
        changed: AffectedRange,
        today: LocalDate,
    ): AffectedRange {
        val widenedEnd = changed.endInclusive.plusDays(MAX_DEPENDENT_WINDOW_DAYS)
        val end = if (widenedEnd.isAfter(today)) today else widenedEnd
        val boundedEnd = if (end.isBefore(changed.start)) changed.start else end
        return AffectedRange(changed.start, boundedEnd)
    }

    /**
     * Merges multiple [AffectedRange] instances into their minimal bounding range, or returns null if
     * all inputs are null or empty.
     */
    fun merge(ranges: Iterable<AffectedRange?>): AffectedRange? {
        val nonNull = ranges.filterNotNull()
        if (nonNull.isEmpty()) return null
        val start = nonNull.minOf { it.start }
        val endInclusive = nonNull.maxOf { it.endInclusive }
        return AffectedRange(start, endInclusive)
    }

    /** Vararg overload of [merge]. */
    fun merge(vararg ranges: AffectedRange?): AffectedRange? = merge(ranges.asIterable())

    /**
     * Task 5: recommendation examples ([app.readylytics.health.core.scoring.domain.recommendation.SelectWorkoutRecommendationExamples]
     * via `WorkoutExampleLoader`/`MorningRecommendationAssembler`, `core:database`) draw candidate
     * workouts from a rolling `EXAMPLE_WINDOW_DAYS` (30-day) lookback ending at each day's wake
     * time. Correcting or deleting a workout can therefore change which examples are eligible for
     * every day up to [EXAMPLE_SELECTION_LOOKBACK_DAYS] after it -- a *distinct* dependency from
     * the general scoring-formula lookback [affectedRange] widens for, modeled separately here so a
     * future change to [MAX_DEPENDENT_WINDOW_DAYS] can never silently under-cover it.
     */
    const val EXAMPLE_SELECTION_LOOKBACK_DAYS = 30L

    /**
     * Bounds the recompute range for a workout correction/deletion on [correctionDate] to the days
     * whose recommendation examples could have referenced it: `[correctionDate, correctionDate +
     * EXAMPLE_SELECTION_LOOKBACK_DAYS]`, intersected with `[retentionStart, today]` so the result
     * never reaches into the future (nothing to recompute there) or before what the app still
     * retains (nothing there to repair). Returns null when that intersection is empty (e.g. a
     * correction older than the retention window).
     *
     * Purely additive: this must never be used in place of a wider range already computed for
     * another reason (e.g. [affectedRange]) -- callers holding both should [merge] them, never pick
     * one over the other, so neither dependency's horizon is ever silently shrunk.
     */
    fun exampleFanOutRange(
        correctionDate: LocalDate,
        today: LocalDate,
        retentionStart: LocalDate,
    ): AffectedRange? {
        val naiveEnd = correctionDate.plusDays(EXAMPLE_SELECTION_LOOKBACK_DAYS)
        val start = maxOf(correctionDate, retentionStart)
        val end = minOf(naiveEnd, today)
        return if (end.isBefore(start)) null else AffectedRange(start, end)
    }
}
