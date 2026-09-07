package app.readylytics.health.core.scoring.domain.recommendation

import app.readylytics.health.core.model.domain.model.MetricStatus
import app.readylytics.health.core.model.domain.model.RecoveryFlag
import app.readylytics.health.core.model.domain.model.scoreStatus
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationDecision
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationReason
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationState
import app.readylytics.health.core.model.domain.scoring.ResidualFatigueThresholds

/**
 * Pure decision rules for the HRV-guided daily workout recommendation.
 *
 * [compute] performs no reads, clock access, Health Connect/Room lookups, or string
 * localization — every input is already resolved by the caller. The evaluation is two-phase:
 *
 * 1. **Availability.** In a fixed order (no sleep, no usable nightly HRV, still calibrating,
 *    missing circadian baseline, unusable HRV z-score/bounds), determine whether a recommendation
 *    can be formed at all. The first failing check wins; later checks are not evaluated.
 * 2. **Reasons.** Once available, collect every limiting factor (illness, HRV out of range, low or
 *    missing sleep score, high or missing fatigue) in a stable order. Illness forces [WorkoutRecommendationState.REST]
 *    but does not suppress the other reasons — all limiting reasons are preserved on the decision.
 *    Any other non-empty reason list yields [WorkoutRecommendationState.EASY]; an empty list yields
 *    [WorkoutRecommendationState.HARDER] with [WorkoutRecommendationReason.WITHIN_USUAL_RANGE] as
 *    the sole reason.
 */
class ComputeWorkoutRecommendationUseCase {
    fun compute(input: WorkoutRecommendationInput): WorkoutRecommendationDecision {
        unavailableState(input)?.let { state ->
            return WorkoutRecommendationDecision(state = state)
        }

        val limitingReasons = collectLimitingReasons(input)
        val state =
            when {
                WorkoutRecommendationReason.POSSIBLE_ILLNESS in limitingReasons -> WorkoutRecommendationState.REST
                limitingReasons.isNotEmpty() -> WorkoutRecommendationState.EASY
                else -> WorkoutRecommendationState.HARDER
            }
        val reasons =
            limitingReasons.ifEmpty { listOf(WorkoutRecommendationReason.WITHIN_USUAL_RANGE) }
        return WorkoutRecommendationDecision(state = state, reasons = reasons)
    }

    private fun unavailableState(input: WorkoutRecommendationInput): WorkoutRecommendationState? =
        when {
            !input.hasSleep -> WorkoutRecommendationState.NO_SLEEP
            !input.nightlyHrv.isUsableNightlyHrv() -> WorkoutRecommendationState.NO_HRV
            input.isCalibrating -> WorkoutRecommendationState.CALIBRATING
            !input.hasCircadianBaseline -> WorkoutRecommendationState.NO_CIRCADIAN_BASELINE
            !input.hasUsableHrvBaseline() -> WorkoutRecommendationState.NO_HRV_BASELINE
            else -> null
        }

    /** A nightly HRV reading is only usable when it is a positive, finite measurement. */
    private fun Float?.isUsableNightlyHrv(): Boolean = this != null && isFinite() && this > 0f

    /**
     * The HRV z-score and its bounds must all be finite, and the bounds must be strictly ordered,
     * for a low/high comparison to be meaningful. A missing z-score is never coerced to zero
     * deviation.
     */
    private fun WorkoutRecommendationInput.hasUsableHrvBaseline(): Boolean {
        val z = zLnHrv
        val low = lowHrvBound
        val high = highHrvBound
        return z != null && z.isFinite() &&
            low != null && low.isFinite() &&
            high != null && high.isFinite() &&
            low < high
    }

    private fun collectLimitingReasons(input: WorkoutRecommendationInput): List<WorkoutRecommendationReason> {
        val reasons = mutableListOf<WorkoutRecommendationReason>()

        if (RecoveryFlag.ILLNESS_ONSET in input.recoveryFlags) {
            reasons += WorkoutRecommendationReason.POSSIBLE_ILLNESS
        }

        // Availability already guaranteed these are finite and strictly ordered.
        val zLnHrv = checkNotNull(input.zLnHrv)
        val lowHrvBound = checkNotNull(input.lowHrvBound)
        val highHrvBound = checkNotNull(input.highHrvBound)
        when {
            zLnHrv < lowHrvBound -> reasons += WorkoutRecommendationReason.HRV_LOW
            zLnHrv > highHrvBound -> reasons += WorkoutRecommendationReason.HRV_HIGH
        }

        when (input.sleepScore.scoreStatus()) {
            MetricStatus.CALIBRATING -> reasons += WorkoutRecommendationReason.SLEEP_SCORE_MISSING
            MetricStatus.POOR, MetricStatus.WARNING -> reasons += WorkoutRecommendationReason.SLEEP_LOW
            MetricStatus.NEUTRAL, MetricStatus.OPTIMAL, MetricStatus.NO_DATA -> Unit
        }

        when (ResidualFatigueThresholds.classify(input.residualFatigue, input.fatigueGain)) {
            MetricStatus.NO_DATA -> reasons += WorkoutRecommendationReason.FATIGUE_MISSING
            MetricStatus.WARNING -> reasons += WorkoutRecommendationReason.FATIGUE_HIGH
            MetricStatus.OPTIMAL, MetricStatus.NEUTRAL, MetricStatus.POOR, MetricStatus.CALIBRATING -> Unit
        }

        return reasons
    }
}
