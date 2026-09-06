package app.readylytics.health.core.database.data.repository.recommendation

import app.readylytics.health.core.model.domain.preferences.UserPreferences
import app.readylytics.health.core.model.domain.repository.DailySummaryRepository
import app.readylytics.health.core.model.domain.repository.WorkoutData
import app.readylytics.health.core.model.domain.repository.WorkoutRepository
import app.readylytics.health.core.model.domain.scoring.WorkoutIntensityLevel
import app.readylytics.health.core.model.domain.scoring.WorkoutLoadLevel
import app.readylytics.health.core.scoring.domain.scoring.GetWorkoutDisplayMetricsUseCase
import app.readylytics.health.core.scoring.domain.scoring.WorkoutDisplayMetrics
import app.readylytics.health.core.scoring.domain.scoring.WorkoutLoadClassification
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkoutExampleLoaderTest {
    private val workouts = mockk<WorkoutRepository>()
    private val summaries = mockk<DailySummaryRepository>()
    private val metrics = mockk<GetWorkoutDisplayMetricsUseCase>()
    private val loader = WorkoutExampleLoader(workouts, summaries, metrics)
    private val prefs = UserPreferences(scoringZoneId = "UTC")
    private val workout =
        WorkoutData(
            id = "stable-id",
            startTime = 1_000L,
            endTime = 1_801_000L,
            exerciseType = "Running",
            durationMinutes = 30,
            zone1Minutes = 0f,
            zone2Minutes = 0f,
            zone3Minutes = 0f,
            zone4Minutes = 0f,
            zone5Minutes = 0f,
            trimp = 20f,
            avgHr = 110f,
        )

    @Test
    fun `same ID workout correction refreshes classification`() =
        runTest {
            stubWorkout(workout)
            coEvery { metrics.execute(any(), any(), any(), any()) } answers {
                metricsFor(if (firstArg<WorkoutData>().avgHr < 150f) WorkoutLoadLevel.LIGHT else WorkoutLoadLevel.HARD)
            }
            assertEquals(WorkoutLoadLevel.LIGHT, load().single().finalLoad)

            stubWorkout(workout.copy(avgHr = 170f))
            val corrected = load().single()
            assertEquals(workout.id, corrected.workoutId)
            assertEquals(WorkoutLoadLevel.HARD, corrected.finalLoad)
            assertEquals(170f, corrected.averageHr)
        }

    @Test
    fun `classification input corrections refresh unchanged workout and preferences`() =
        runTest {
            stubWorkout(workout)
            // HR samples and the daily RHR baseline are read inside the metrics use case;
            // neither is part of WorkoutData or UserPreferences.
            var currentLevel = WorkoutLoadLevel.LIGHT
            coEvery { metrics.execute(any(), any(), any(), any()) } answers { metricsFor(currentLevel) }
            assertEquals(WorkoutLoadLevel.LIGHT, load().single().finalLoad)

            currentLevel = WorkoutLoadLevel.HARD
            assertEquals(WorkoutLoadLevel.HARD, load().single().finalLoad)
        }

    @Test
    fun `previously missing classification is retried after data arrives`() =
        runTest {
            stubWorkout(workout)
            coEvery { metrics.execute(any(), any(), any(), any()) } returns metricsFor(null)
            assertTrue(load().isEmpty())

            coEvery { metrics.execute(any(), any(), any(), any()) } returns metricsFor(WorkoutLoadLevel.LIGHT)
            assertEquals(WorkoutLoadLevel.LIGHT, load().single().finalLoad)
        }

    private suspend fun load() = loader.load(workout.startTime, workout.endTime, prefs)

    private fun stubWorkout(row: WorkoutData) {
        coEvery { workouts.getInRange(any(), any()) } returns listOf(row)
        coEvery { summaries.getInRange(any(), any()) } returns emptyList()
    }

    private fun metricsFor(level: WorkoutLoadLevel?) =
        WorkoutDisplayMetrics(
            preciseTrimp = 20f,
            computedTrimp = 20,
            trimpDisplay = "20",
            gainedStrain = 0.1f,
            gainedStrainDisplay = "0.1",
            classification =
                level?.let {
                    WorkoutLoadClassification(
                        totalTrimp = 20.0,
                        trimpPerMinute = 0.67,
                        baseLoad = it,
                        intensity = WorkoutIntensityLevel.MODERATE,
                        finalLoad = it,
                        wasPromoted = false,
                    )
                },
        )
}
