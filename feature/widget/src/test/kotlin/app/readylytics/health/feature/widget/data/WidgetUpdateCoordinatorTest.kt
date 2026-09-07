package app.readylytics.health.feature.widget.data

import app.readylytics.health.core.model.data.preferences.UserPreferences
import app.readylytics.health.core.model.domain.model.DailySummary
import app.readylytics.health.core.model.domain.preferences.UserPreferencesReader
import app.readylytics.health.core.model.domain.repository.DailySummaryRepository
import app.readylytics.health.core.model.domain.util.toMidnightEpochMilli
import app.readylytics.health.feature.widget.ui.RecoveryGlanceWidget
import app.readylytics.health.feature.widget.ui.StrainRecoveryWidget
import app.readylytics.health.feature.widget.ui.VitalsStripWidget
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class WidgetUpdateCoordinatorTest {
    @Test
    fun buildSnapshot_returnsMappedData() =
        runTest {
            val dailySummaryRepo = mockk<DailySummaryRepository>()
            val prefsReader = mockk<UserPreferencesReader>()

            val today = LocalDate.now()
            val summary = DailySummary(date = today, sleepScore = 80f)
            val prefs = UserPreferences()

            coEvery { dailySummaryRepo.getByDate(any()) } returns summary
            coEvery { prefsReader.userPreferences } returns flowOf(prefs)

            val coordinator =
                WidgetUpdateCoordinator(
                    context = mockk(relaxed = true),
                    dailySummaryRepository = dailySummaryRepo,
                    preferencesReader = prefsReader,
                )

            val snapshot = coordinator.buildLatestSnapshot(today)
            assertNotNull(snapshot)
            assertEquals(80, snapshot.sleepScore)
        }

    @Test
    fun buildSnapshot_defaultDateUsesScoringZone() =
        runTest {
            val dailySummaryRepo = mockk<DailySummaryRepository>()
            val prefsReader = mockk<UserPreferencesReader>()

            val tokyoZone = ZoneId.of("Asia/Tokyo")
            val prefs = UserPreferences(scoringZoneId = tokyoZone.id)
            val expectedDate = LocalDate.now(tokyoZone)
            val expectedMidnightMs = expectedDate.toMidnightEpochMilli(tokyoZone)
            val summary = DailySummary(date = expectedDate, sleepScore = 92f)

            coEvery { dailySummaryRepo.getByDate(expectedMidnightMs) } returns summary
            coEvery { prefsReader.userPreferences } returns flowOf(prefs)

            val coordinator =
                WidgetUpdateCoordinator(
                    context = mockk(relaxed = true),
                    dailySummaryRepository = dailySummaryRepo,
                    preferencesReader = prefsReader,
                )

            val snapshot = coordinator.buildLatestSnapshot()
            assertNotNull(snapshot)
            assertEquals(92, snapshot.sleepScore)
            coVerify(exactly = 1) { dailySummaryRepo.getByDate(expectedMidnightMs) }
        }

    @Test
    fun buildSnapshot_whenSummaryIsNull_returnsEmptySnapshot() =
        runTest {
            val dailySummaryRepo = mockk<DailySummaryRepository>()
            val prefsReader = mockk<UserPreferencesReader>()

            val today = LocalDate.now()
            val prefs = UserPreferences()

            coEvery { dailySummaryRepo.getByDate(any()) } returns null
            coEvery { prefsReader.userPreferences } returns flowOf(prefs)

            val coordinator =
                WidgetUpdateCoordinator(
                    context = mockk(relaxed = true),
                    dailySummaryRepository = dailySummaryRepo,
                    preferencesReader = prefsReader,
                )

            val snapshot = coordinator.buildLatestSnapshot(today)
            assertNotNull(snapshot)
            assertEquals(WidgetSnapshot.EMPTY, snapshot)
            assertNull(snapshot.sleepScore)
        }

    @Test
    fun updateAllWidgets_handlesExceptionGracefully() =
        runTest {
            val dailySummaryRepo = mockk<DailySummaryRepository>()
            val prefsReader = mockk<UserPreferencesReader>()

            coEvery { dailySummaryRepo.getByDate(any()) } throws RuntimeException("Storage failure")
            coEvery { prefsReader.userPreferences } returns flowOf(UserPreferences())

            val coordinator =
                WidgetUpdateCoordinator(
                    context = mockk(relaxed = true),
                    dailySummaryRepository = dailySummaryRepo,
                    preferencesReader = prefsReader,
                )

            // Should not throw
            coordinator.updateAllWidgets()
        }

    @Test(expected = CancellationException::class)
    fun updateAllWidgets_rethrowsCancellationException() =
        runTest {
            val dailySummaryRepo = mockk<DailySummaryRepository>()
            val prefsReader = mockk<UserPreferencesReader>()

            coEvery { dailySummaryRepo.getByDate(any()) } throws CancellationException("Cancelled")
            coEvery { prefsReader.userPreferences } returns flowOf(UserPreferences())

            val coordinator =
                WidgetUpdateCoordinator(
                    context = mockk(relaxed = true),
                    dailySummaryRepository = dailySummaryRepo,
                    preferencesReader = prefsReader,
                )

            coordinator.updateAllWidgets()
        }

    @Test
    fun updateAllWidgets_whenOneWidgetGroupThrows_continuesUpdatingOtherGroups() =
        runTest {
            val dailySummaryRepo = mockk<DailySummaryRepository>()
            val prefsReader = mockk<UserPreferencesReader>()

            val today = LocalDate.now()
            val summary = DailySummary(date = today, sleepScore = 80f)
            val prefs = UserPreferences()

            coEvery { dailySummaryRepo.getByDate(any()) } returns summary
            coEvery { prefsReader.userPreferences } returns flowOf(prefs)

            val coordinator =
                spyk(
                    WidgetUpdateCoordinator(
                        context = mockk(relaxed = true),
                        dailySummaryRepository = dailySummaryRepo,
                        preferencesReader = prefsReader,
                    ),
                )

            coEvery { coordinator.persistSnapshot(any()) } returns Unit

            coEvery {
                coordinator.updateWidgetGroup(any<RecoveryGlanceWidget>(), any(), any())
            } throws RuntimeException("Glance render failed")

            coEvery {
                coordinator.updateWidgetGroup(any<StrainRecoveryWidget>(), any(), any())
            } returns 1

            coEvery {
                coordinator.updateWidgetGroup(any<VitalsStripWidget>(), any(), any())
            } returns 1

            // Should not throw, and remaining groups should still be updated
            coordinator.updateAllWidgets()

            coVerify(exactly = 1) { coordinator.persistSnapshot(any()) }
            coVerify(exactly = 1) {
                coordinator.updateWidgetGroup(any<RecoveryGlanceWidget>(), any(), any())
            }
            coVerify(exactly = 1) {
                coordinator.updateWidgetGroup(any<StrainRecoveryWidget>(), any(), any())
            }
            coVerify(exactly = 1) {
                coordinator.updateWidgetGroup(any<VitalsStripWidget>(), any(), any())
            }
        }

    @Test(expected = CancellationException::class)
    fun updateAllWidgets_whenWidgetGroupThrowsCancellationException_rethrows() =
        runTest {
            val dailySummaryRepo = mockk<DailySummaryRepository>()
            val prefsReader = mockk<UserPreferencesReader>()

            val today = LocalDate.now()
            val summary = DailySummary(date = today, sleepScore = 80f)
            val prefs = UserPreferences()

            coEvery { dailySummaryRepo.getByDate(any()) } returns summary
            coEvery { prefsReader.userPreferences } returns flowOf(prefs)

            val coordinator =
                spyk(
                    WidgetUpdateCoordinator(
                        context = mockk(relaxed = true),
                        dailySummaryRepository = dailySummaryRepo,
                        preferencesReader = prefsReader,
                    ),
                )

            coEvery { coordinator.persistSnapshot(any()) } returns Unit

            coEvery {
                coordinator.updateWidgetGroup(any<RecoveryGlanceWidget>(), any(), any())
            } throws CancellationException("Widget update cancelled")

            coordinator.updateAllWidgets()
        }

    @Test
    fun updateAllWidgets_alwaysPersistsSnapshotEvenWhenNoWidgetsPresent() =
        runTest {
            val dailySummaryRepo = mockk<DailySummaryRepository>()
            val prefsReader = mockk<UserPreferencesReader>()

            val today = LocalDate.now()
            val summary = DailySummary(date = today, sleepScore = 85f)
            val prefs = UserPreferences()

            coEvery { dailySummaryRepo.getByDate(any()) } returns summary
            coEvery { prefsReader.userPreferences } returns flowOf(prefs)

            val coordinator =
                spyk(
                    WidgetUpdateCoordinator(
                        context = mockk(relaxed = true),
                        dailySummaryRepository = dailySummaryRepo,
                        preferencesReader = prefsReader,
                    ),
                )

            coEvery { coordinator.persistSnapshot(any()) } returns Unit
            coEvery { coordinator.updateWidgetGroup(any(), any(), any()) } returns 0

            coordinator.updateAllWidgets()

            coVerify(exactly = 1) { coordinator.persistSnapshot(match { it.sleepScore == 85 }) }
            coVerify(exactly = 3) { coordinator.updateWidgetGroup(any(), any(), any()) }
        }
}
