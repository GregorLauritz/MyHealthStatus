package app.readylytics.health.feature.widget.data

import app.readylytics.health.core.model.data.preferences.UserPreferences
import app.readylytics.health.core.model.domain.model.DailySummary
import app.readylytics.health.core.model.domain.preferences.UserPreferencesReader
import app.readylytics.health.core.model.domain.repository.DailySummaryRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

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
}
