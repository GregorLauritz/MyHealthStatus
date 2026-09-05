package app.readylytics.health.core.model.domain.recommendation

import kotlinx.serialization.Serializable

/**
 * Terminal states an HRV-guided workout recommendation can resolve to.
 *
 * [REST], [EASY], and [HARDER] are the three available guidance outcomes. The remaining values are
 * "unavailable" states: the evaluator could not form a guidance opinion because a required input
 * was missing, still calibrating, or otherwise unusable. Callers should treat every non-REST/EASY/
 * HARDER value as "no recommendation today" rather than attempt to infer a training intensity from
 * it.
 */
@Serializable
enum class WorkoutRecommendationState {
    REST,
    EASY,
    HARDER,
    NO_SLEEP,
    NO_HRV,
    CALIBRATING,
    NO_CIRCADIAN_BASELINE,
    NO_HRV_BASELINE,
}

/**
 * Individual factors that can limit today's recommended training intensity, or explain why none
 * were limiting.
 *
 * [WITHIN_USUAL_RANGE] is not a limiting factor: it is the sole reason attached to a [HARDER]
 * decision, explaining that every checked signal was inside its usual range.
 */
@Serializable
enum class WorkoutRecommendationReason {
    POSSIBLE_ILLNESS,
    HRV_LOW,
    HRV_HIGH,
    SLEEP_LOW,
    FATIGUE_HIGH,
    SLEEP_SCORE_MISSING,
    FATIGUE_MISSING,
    WITHIN_USUAL_RANGE,
}

/**
 * Output of [app.readylytics.health.core.scoring.domain.recommendation.ComputeWorkoutRecommendationUseCase].
 *
 * [reasons] is empty only for the unavailable [WorkoutRecommendationState] values; every REST,
 * EASY, or HARDER decision carries at least one reason (limiting factors, or [WorkoutRecommendationReason.WITHIN_USUAL_RANGE]
 * when there are none).
 */
@Serializable
data class WorkoutRecommendationDecision(
    val state: WorkoutRecommendationState,
    val reasons: List<WorkoutRecommendationReason> = emptyList(),
)
