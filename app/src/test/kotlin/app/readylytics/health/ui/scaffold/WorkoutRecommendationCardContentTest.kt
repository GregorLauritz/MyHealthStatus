package app.readylytics.health.ui.scaffold

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.readylytics.health.R
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationDecision
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationExample
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationReason
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationSnapshot
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationState
import app.readylytics.health.core.model.domain.scoring.WorkoutLoadLevel
import app.readylytics.health.core.model.domain.workouts.detail.WorkoutLayoutTypeMapper
import app.readylytics.health.feature.workouts.displayNameResId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for the pure state -> presentation mapping in WorkoutRecommendationCardContent.kt.
 *
 * [buildWorkoutRecommendationPresentation] takes a plain [Context] instead of using
 * `stringResource`, so it is testable with plain Robolectric/AndroidJUnit4 — no Compose test
 * infrastructure is required (the app module does not otherwise depend on it).
 */
@RunWith(AndroidJUnit4::class)
class WorkoutRecommendationCardContentTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun example(
        workoutId: String = "run-42",
        exerciseType: String = "RUNNING",
        durationMinutes: Int = 45,
        averageHr: Float? = null,
        finalLoad: WorkoutLoadLevel = WorkoutLoadLevel.LIGHT,
    ) = WorkoutRecommendationExample(
        workoutId = workoutId,
        exerciseType = exerciseType,
        startTimeMs = 0L,
        endTimeMs = durationMinutes * 60_000L,
        durationMinutes = durationMinutes,
        averageHr = averageHr,
        finalLoad = finalLoad,
    )

    @Test
    fun nullSnapshotRendersNotCalculatedYetCopy() {
        val presentation = buildWorkoutRecommendationPresentation(null, context)

        assertEquals(context.getString(R.string.workout_recommendation_category_not_calculated), presentation.category)
        assertEquals(
            context.getString(R.string.workout_recommendation_explanation_not_calculated),
            presentation.explanation,
        )
        assertTrue(presentation.examples.isEmpty())
    }

    @Test
    fun calibratingSnapshotIsDistinctFromNullSnapshot() {
        val nullCase = buildWorkoutRecommendationPresentation(null, context)
        val calibratingSnapshot =
            WorkoutRecommendationSnapshot(
                wakeSessionId = "sleep-1",
                wakeTimeMs = 1_000L,
                decision = WorkoutRecommendationDecision(state = WorkoutRecommendationState.CALIBRATING),
            )
        val calibrating = buildWorkoutRecommendationPresentation(calibratingSnapshot, context)

        // The critical regression check: a never-computed/rejected row (null) must never render
        // the same copy as an evaluator-run CALIBRATING decision.
        assertNotEquals(nullCase.category, calibrating.category)
        assertNotEquals(nullCase.explanation, calibrating.explanation)
        assertEquals(context.getString(R.string.workout_recommendation_category_calibrating), calibrating.category)
    }

    @Test
    fun everyUnavailableStateHasDistinctCategoryAndExplanationText() {
        val unavailableStates =
            listOf(
                WorkoutRecommendationState.NO_SLEEP,
                WorkoutRecommendationState.NO_HRV,
                WorkoutRecommendationState.CALIBRATING,
                WorkoutRecommendationState.NO_CIRCADIAN_BASELINE,
                WorkoutRecommendationState.NO_HRV_BASELINE,
            )
        val presentations =
            unavailableStates.map { state ->
                buildWorkoutRecommendationPresentation(
                    WorkoutRecommendationSnapshot(
                        wakeSessionId = null,
                        wakeTimeMs = null,
                        decision = WorkoutRecommendationDecision(state = state),
                    ),
                    context,
                )
            }

        assertEquals(unavailableStates.size, presentations.map { it.category }.distinct().size)
        assertEquals(unavailableStates.size, presentations.map { it.explanation }.distinct().size)
        presentations.forEach { assertTrue(it.examples.isEmpty()) }
    }

    @Test
    fun restStateHasNoExamplesAndRestCategory() {
        val snapshot =
            WorkoutRecommendationSnapshot(
                wakeSessionId = "sleep-1",
                wakeTimeMs = 1_000L,
                decision =
                    WorkoutRecommendationDecision(
                        state = WorkoutRecommendationState.REST,
                        reasons = listOf(WorkoutRecommendationReason.HRV_LOW),
                    ),
            )

        val presentation = buildWorkoutRecommendationPresentation(snapshot, context)

        assertEquals(context.getString(R.string.workout_recommendation_category_rest), presentation.category)
        assertTrue(presentation.examples.isEmpty())
    }

    @Test
    fun easyStateWithThreeExamplesMapsEachExampleAndReusesSharedTypeLabel() {
        val snapshot =
            WorkoutRecommendationSnapshot(
                wakeSessionId = "sleep-1",
                wakeTimeMs = 1_000L,
                decision =
                    WorkoutRecommendationDecision(
                        state = WorkoutRecommendationState.EASY,
                        reasons = listOf(WorkoutRecommendationReason.HRV_LOW),
                    ),
                examples =
                    listOf(
                        example(workoutId = "run-1", exerciseType = "RUNNING"),
                        example(workoutId = "ride-1", exerciseType = "BIKING"),
                        example(workoutId = "swim-1", exerciseType = "SWIMMING_OPEN_WATER"),
                    ),
            )

        val presentation = buildWorkoutRecommendationPresentation(snapshot, context)

        assertEquals(3, presentation.examples.size)
        assertEquals(listOf("run-1", "ride-1", "swim-1"), presentation.examples.map { it.workoutId })

        val expectedRunLabel = context.getString(WorkoutLayoutTypeMapper.fromExerciseType("RUNNING").displayNameResId)
        assertEquals(expectedRunLabel, presentation.examples[0].typeLabel)
    }

    @Test
    fun averageHrIsOmittedWhenNullAndIncludedWhenPresent() {
        val snapshotWithoutHr =
            WorkoutRecommendationSnapshot(
                wakeSessionId = null,
                wakeTimeMs = null,
                decision = WorkoutRecommendationDecision(state = WorkoutRecommendationState.EASY),
                examples = listOf(example(durationMinutes = 45, averageHr = null)),
            )
        val withoutHr = buildWorkoutRecommendationPresentation(snapshotWithoutHr, context)
        assertEquals(
            context.getString(R.string.workout_recommendation_duration_format, 45),
            withoutHr.examples.single().recordedSessionDescription,
        )

        val snapshotWithHr =
            WorkoutRecommendationSnapshot(
                wakeSessionId = null,
                wakeTimeMs = null,
                decision = WorkoutRecommendationDecision(state = WorkoutRecommendationState.EASY),
                examples = listOf(example(durationMinutes = 45, averageHr = 120.4f)),
            )
        val withHr = buildWorkoutRecommendationPresentation(snapshotWithHr, context)
        assertEquals(
            context.getString(R.string.workout_recommendation_duration_with_hr_format, 45, 120),
            withHr.examples.single().recordedSessionDescription,
        )
    }

    @Test
    fun multipleReasonsAreAllReflectedInTheExplanation() {
        val snapshot =
            WorkoutRecommendationSnapshot(
                wakeSessionId = null,
                wakeTimeMs = null,
                decision =
                    WorkoutRecommendationDecision(
                        state = WorkoutRecommendationState.REST,
                        reasons =
                            listOf(
                                WorkoutRecommendationReason.HRV_LOW,
                                WorkoutRecommendationReason.SLEEP_LOW,
                            ),
                    ),
            )

        val presentation = buildWorkoutRecommendationPresentation(snapshot, context)

        assertTrue(presentation.explanation.contains(context.getString(R.string.workout_recommendation_reason_hrv_low)))
        assertTrue(
            presentation.explanation.contains(context.getString(R.string.workout_recommendation_reason_sleep_low)),
        )
    }

    @Test
    fun mappingIsIndependentOfWhichDayIsBeingViewed() {
        // The mapper takes no date/clock input at all — it is a pure function of the stored
        // snapshot for whichever day's DailySummary the caller passes in. Viewing a past day on
        // the dashboard therefore renders that day's own recommendation, never "today"'s.
        val snapshot =
            WorkoutRecommendationSnapshot(
                wakeSessionId = "sleep-past",
                wakeTimeMs = 1_000L,
                decision =
                    WorkoutRecommendationDecision(
                        state = WorkoutRecommendationState.HARDER,
                        reasons = listOf(WorkoutRecommendationReason.WITHIN_USUAL_RANGE),
                    ),
            )

        val first = buildWorkoutRecommendationPresentation(snapshot, context)
        val second = buildWorkoutRecommendationPresentation(snapshot, context)

        assertEquals(first, second)
        assertEquals(context.getString(R.string.workout_recommendation_category_harder), first.category)
    }
}
