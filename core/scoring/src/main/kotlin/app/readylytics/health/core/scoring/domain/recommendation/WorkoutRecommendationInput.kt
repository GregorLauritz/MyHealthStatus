package app.readylytics.health.core.scoring.domain.recommendation

import app.readylytics.health.core.model.domain.model.RecoveryFlag

/**
 * Fully-resolved inputs for [ComputeWorkoutRecommendationUseCase].
 *
 * Every value here is already computed by the scoring pipeline for the day in question; the
 * evaluator performs no reads, clock access, or Health Connect/Room lookups of its own. A `null`
 * or non-finite number always means "unknown", never "zero" — the evaluator must not coerce a
 * missing value into a value that happens to read as within-range.
 */
data class WorkoutRecommendationInput(
    val hasSleep: Boolean,
    val nightlyHrv: Float?,
    val isCalibrating: Boolean,
    val hasCircadianBaseline: Boolean,
    val zLnHrv: Float?,
    val lowHrvBound: Float?,
    val highHrvBound: Float?,
    val sleepScore: Float?,
    val residualFatigue: Float?,
    val fatigueGain: Float,
    val recoveryFlags: Set<RecoveryFlag>,
)
