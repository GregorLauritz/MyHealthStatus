package app.readylytics.health.core.model.domain.scoring

import app.readylytics.health.core.model.domain.model.MetricStatus

/**
 * Shared gain-scaled classification for a Residual Fatigue value.
 *
 * Residual Fatigue is `gain * sum(TRIMP) * decay`, and `gain` is user-settable over 0.1..5.0, so
 * fixed cut-points would read Optimal with a pinned-to-zero gauge at low gain and Warning with a
 * saturated gauge at high gain. The 30/70 cut-points below are expressed at gain 1.0 and scaled by
 * the caller's resolved gain before comparison.
 *
 * This is the single source of truth for the classification: both the Residual Fatigue dashboard
 * card ([app.readylytics.health.feature.dashboard.usecase.ResidualFatiguePresentationFactory]) and
 * the workout-recommendation evaluator call [classify] rather than duplicating the comparison.
 */
object ResidualFatigueThresholds {
    private const val OPTIMAL_BELOW = 30f
    private const val NEUTRAL_THROUGH = 70f

    /**
     * Classifies [value] against thresholds scaled by [gain].
     *
     * A null, non-finite, or negative [value] is unknown, not zero, so it returns [MetricStatus.NO_DATA]
     * rather than being coerced into [MetricStatus.OPTIMAL]. Likewise a non-finite or non-positive
     * [gain] cannot scale the thresholds meaningfully and also returns [MetricStatus.NO_DATA].
     */
    fun classify(
        value: Float?,
        gain: Float,
    ): MetricStatus =
        when {
            value == null || !value.isFinite() || value < 0f -> MetricStatus.NO_DATA
            !gain.isFinite() || gain <= 0f -> MetricStatus.NO_DATA
            value < OPTIMAL_BELOW * gain -> MetricStatus.OPTIMAL
            value <= NEUTRAL_THROUGH * gain -> MetricStatus.NEUTRAL
            else -> MetricStatus.WARNING
        }
}
