package app.readylytics.health.core.model.domain.repository

import app.readylytics.health.core.model.domain.model.DailySummary
import kotlinx.coroutines.flow.Flow

interface DailySummaryRepository {
    fun observeLatest(): Flow<DailySummary?>

    fun observeSince(fromMs: Long): Flow<List<DailySummary>>

    fun observeByDate(dateMidnightMs: Long): Flow<DailySummary?>

    suspend fun getByDate(dateMidnightMs: Long): DailySummary?

    suspend fun getSince(fromMs: Long): List<DailySummary>

    /**
     * Summaries whose local-day midnight falls in `[fromMs, toMs]`, oldest first.
     *
     * The bounded counterpart of [getSince], for callers that need a fixed window rather than
     * "everything from here on". [getSince] reads preferences and maps every row through
     * `DailySummaryMapper` (JSON decode included) up to the newest row in the table, so replaying N
     * historical days with it costs O(N^2) row materializations instead of O(N).
     */
    suspend fun getInRange(
        fromMs: Long,
        toMs: Long,
    ): List<DailySummary>

    fun observeFirstSessionEndingInRange(
        fromMs: Long,
        toMs: Long,
    ): Flow<SleepSessionData?>
}
