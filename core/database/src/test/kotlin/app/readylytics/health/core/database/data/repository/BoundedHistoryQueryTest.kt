package app.readylytics.health.core.database.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import app.readylytics.health.core.database.data.local.HealthDatabase
import app.readylytics.health.core.databaseschema.data.local.entity.DailySummaryEntity
import app.readylytics.health.core.databaseschema.data.local.entity.SleepSessionEntity
import app.readylytics.health.core.model.data.preferences.UserPreferences
import app.readylytics.health.core.model.domain.model.DailySummary
import app.readylytics.health.core.model.domain.preferences.SettingsRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneId
import kotlin.test.assertEquals

/**
 * Boundary coverage for the bounded history reads the morning-recommendation assembly uses.
 *
 * Both exist so a day being replayed during a historical backfill reads only its own window
 * instead of everything through today (see `SleepSessionRepository.getInRange` /
 * `DailySummaryRepository.getInRange`). The window is only useful if it is exact at both ends,
 * so every case here sits directly on a boundary.
 */
@RunWith(AndroidJUnit4::class)
class BoundedHistoryQueryTest {
    private lateinit var db: HealthDatabase
    private lateinit var sleepRepository: SleepSessionRepositoryImpl
    private lateinit var summaryRepository: DailySummaryRepositoryImpl

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db =
            Room
                .inMemoryDatabaseBuilder(context, HealthDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        val settingsRepository =
            mockk<SettingsRepository>().also {
                every { it.userPreferences } returns flowOf(UserPreferences(scoringZoneId = "UTC"))
            }
        sleepRepository = SleepSessionRepositoryImpl(db.sleepSessionDao(), db.sleepStageDao())
        summaryRepository =
            DailySummaryRepositoryImpl(db.dailySummaryDao(), db.sleepSessionDao(), settingsRepository)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `sleep getInRange keeps sessions on both bounds and drops the ones just outside`() =
        runTest {
            db.sleepSessionDao().upsertAll(
                listOf(
                    session("starts_one_ms_early", startTime = FROM_MS - 1, endTime = FROM_MS + HOUR_MS),
                    session("starts_on_lower_bound", startTime = FROM_MS, endTime = FROM_MS + HOUR_MS),
                    session("inside", startTime = FROM_MS + DAY_MS, endTime = FROM_MS + DAY_MS + HOUR_MS),
                    session("ends_on_upper_bound", startTime = TO_MS - HOUR_MS, endTime = TO_MS),
                    session("ends_one_ms_late", startTime = TO_MS - HOUR_MS, endTime = TO_MS + 1),
                ),
            )

            val ids = sleepRepository.getInRange(FROM_MS, TO_MS).map { it.id }

            assertEquals(listOf("starts_on_lower_bound", "inside", "ends_on_upper_bound"), ids)
        }

    @Test
    fun `sleep getInRange is empty when the window is inverted`() =
        runTest {
            db.sleepSessionDao().upsertAll(
                listOf(session("inside", startTime = FROM_MS, endTime = FROM_MS + HOUR_MS)),
            )

            assertEquals(emptyList(), sleepRepository.getInRange(TO_MS, FROM_MS).map { it.id })
        }

    @Test
    fun `summary getInRange keeps days on both bounds and drops the ones just outside`() =
        runTest {
            listOf(FROM_MS - DAY_MS, FROM_MS, FROM_MS + DAY_MS, TO_MS, TO_MS + DAY_MS).forEach {
                db.dailySummaryDao().upsert(DailySummaryEntity(dateMidnightMs = it))
            }

            val dates = summaryRepository.getInRange(FROM_MS, TO_MS).map { it.dateMidnightMs() }

            assertEquals(listOf(FROM_MS, FROM_MS + DAY_MS, TO_MS), dates)
        }

    @Test
    fun `summary getInRange is empty when the window is inverted`() =
        runTest {
            db.dailySummaryDao().upsert(DailySummaryEntity(dateMidnightMs = FROM_MS))

            assertEquals(emptyList(), summaryRepository.getInRange(TO_MS, FROM_MS))
        }

    private fun DailySummary.dateMidnightMs(): Long =
        date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

    private fun session(
        id: String,
        startTime: Long,
        endTime: Long,
    ): SleepSessionEntity =
        SleepSessionEntity(
            id = id,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = ((endTime - startTime) / 60_000L).toInt().coerceAtLeast(1),
            efficiency = 90f,
            deepSleepMinutes = 60,
            remSleepMinutes = 60,
            lightSleepMinutes = 180,
            awakeMinutes = 20,
            deviceName = "Pixel",
        )

    private companion object {
        const val HOUR_MS = 3_600_000L
        const val DAY_MS = 86_400_000L

        /** Arbitrary but aligned to a UTC midnight, so summary rows are valid day keys. */
        const val FROM_MS = 1_700_006_400_000L
        const val TO_MS = FROM_MS + 5 * DAY_MS
    }
}
