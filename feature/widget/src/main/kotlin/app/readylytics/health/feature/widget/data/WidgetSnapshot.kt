package app.readylytics.health.feature.widget.data

import kotlinx.serialization.Serializable

@Serializable
data class WidgetSnapshot(
    val lastUpdatedEpochMs: Long = 0L,
    val hasData: Boolean = false,
    val readinessScore: Int? = null,
    val readinessCategory: String? = null,
    val isCalibrating: Boolean = false,
    val calibrationDays: Int = 0,
    val sleepScore: Int? = null,
    val sleepDurationFormatted: String? = null,
    val deepSleepPercent: Int? = null,
    val remSleepPercent: Int? = null,
    val strainScore: Float? = null,
    val strainTargetFormatted: String? = null,
    val stepCountFormatted: String? = null,
    val restingHeartRate: Int? = null,
    val rhrDelta: Int? = null,
    @Deprecated("Use rhrDelta with WidgetDeltaFormatter for i18n support")
    val rhrDeltaFormatted: String? = null,
    val nocturnalHrv: Int? = null,
    val hrvDelta: Int? = null,
    @Deprecated("Use hrvDelta with WidgetDeltaFormatter for i18n support")
    val hrvDeltaFormatted: String? = null,
    val avgSpo2Formatted: String? = null,
    val skinTempDeltaFormatted: String? = null,
    @Deprecated("Use WidgetDeltaFormatter.formatSecondaryRecoveryMetric for i18n support")
    val secondaryRecoveryMetricFormatted: String? = null,
) {
    companion object {
        val EMPTY = WidgetSnapshot()
    }
}
