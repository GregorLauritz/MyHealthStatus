package app.readylytics.health.core.model.domain.recommendation

import app.readylytics.health.core.model.domain.scoring.WorkoutLoadLevel
import kotlinx.serialization.Serializable

/**
 * A single past workout offered as a concrete example of what "easy" or "harder" guidance looks
 * like for this user.
 *
 * [finalLoad] is the workout's final history load classification (the same
 * [WorkoutLoadLevel] that `GetWorkoutDisplayMetricsUseCase` exposes as `classification.finalLoad`),
 * not a re-derived score. This class only stores the shape; selection logic lives in
 * [app.readylytics.health.core.scoring.domain.recommendation.SelectWorkoutRecommendationExamples].
 */
@Serializable
data class WorkoutRecommendationExample(
    val workoutId: String,
    val exerciseType: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMinutes: Int,
    val averageHr: Float?,
    val finalLoad: WorkoutLoadLevel,
)
