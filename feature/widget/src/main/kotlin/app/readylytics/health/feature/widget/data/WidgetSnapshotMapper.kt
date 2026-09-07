package app.readylytics.health.feature.widget.data

import app.readylytics.health.core.model.domain.model.DailyMetricsMapper
import app.readylytics.health.core.model.domain.model.DailySummary
import app.readylytics.health.core.model.domain.preferences.UserPreferences
import app.readylytics.health.core.model.domain.scoring.LoadSourceMode
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

object WidgetSnapshotMapper {
    private const val CALIBRATION_TOTAL_DAYS = 7
    private const val READINESS_PEAK_THRESHOLD = 85
    private const val READINESS_MAINTAIN_THRESHOLD = 60
    private const val READINESS_CAUTION_THRESHOLD = 30
    private const val READINESS_MAX_SCORE = 100
    private const val MINUTES_PER_HOUR = 60
    private const val DEFAULT_STRAIN_TARGET = "10 - 14"

    fun map(
        summary: DailySummary?,
        prefs: UserPreferences,
        date: LocalDate,
    ): WidgetSnapshot {
        if (summary == null || summary.date != date) return WidgetSnapshot.EMPTY

        val numberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())
        val isCalibrating = summary.isCalibrating
        val calibrationDays = (summary.baselineObservationCount ?: 0).coerceIn(0, CALIBRATION_TOTAL_DAYS)

        val (readinessScore, readinessCategory) = resolveReadiness(summary, prefs, isCalibrating)
        val sleepDurationFormatted = formatSleepDuration(summary.sleepDurationMinutes)
        val strainScore = resolveStrainScore(summary, prefs)

        val rhrBaseline = DailyMetricsMapper.rhrBaselineRounded(summary, prefs)
        val rhrDeltaFormatted =
            computeDeltaString(
                current = summary.restingHeartRate,
                baseline = rhrBaseline,
            )

        val hrvBaseline = DailyMetricsMapper.hrvBaselineRounded(summary, prefs)
        val hrvDeltaFormatted =
            computeDeltaString(
                current = summary.nocturnalHrv,
                baseline = hrvBaseline,
            )

        return WidgetSnapshot(
            lastUpdatedEpochMs = System.currentTimeMillis(),
            hasData = true,
            readinessScore = readinessScore,
            readinessCategory = readinessCategory,
            isCalibrating = isCalibrating,
            calibrationDays = calibrationDays,
            sleepScore = summary.sleepScore?.roundToInt(),
            sleepDurationFormatted = sleepDurationFormatted,
            deepSleepPercent = summary.deepSleepPercent?.roundToInt(),
            remSleepPercent = summary.remSleepPercent?.roundToInt(),
            strainScore = strainScore,
            strainTargetFormatted = DEFAULT_STRAIN_TARGET,
            stepCountFormatted = summary.stepCount?.let { numberFormat.format(it) },
            restingHeartRate = summary.restingHeartRate,
            rhrDeltaFormatted = rhrDeltaFormatted,
            nocturnalHrv = summary.nocturnalHrv,
            hrvDeltaFormatted = hrvDeltaFormatted,
            avgSpo2Formatted = summary.avgSleepingSpo2?.let { "${it.roundToInt()}%" },
            skinTempDeltaFormatted = null,
        )
    }

    private fun resolveReadiness(
        summary: DailySummary,
        prefs: UserPreferences,
        isCalibrating: Boolean,
    ): Pair<Int?, String?> {
        val rawReadiness =
            when (prefs.rasSourceMode) {
                LoadSourceMode.WORKOUT_ONLY -> summary.readinessWorkoutOnly
                LoadSourceMode.EVERYDAY_HEART_RATE -> summary.readinessEverydayHr ?: summary.readinessWorkoutOnly
            }
        val readinessScore =
            if (!isCalibrating && rawReadiness != null) {
                rawReadiness.roundToInt().coerceIn(0, READINESS_MAX_SCORE)
            } else {
                null
            }
        val readinessCategory =
            when {
                isCalibrating -> null
                readinessScore == null -> null
                readinessScore >= READINESS_PEAK_THRESHOLD -> "Peak"
                readinessScore >= READINESS_MAINTAIN_THRESHOLD -> "Maintain"
                readinessScore >= READINESS_CAUTION_THRESHOLD -> "Caution"
                else -> "High Fatigue"
            }
        return Pair(readinessScore, readinessCategory)
    }

    private fun formatSleepDuration(sleepDurationMinutes: Int?): String? {
        if (sleepDurationMinutes == null || sleepDurationMinutes <= 0) return null
        val hours = sleepDurationMinutes / MINUTES_PER_HOUR
        val minutes = sleepDurationMinutes % MINUTES_PER_HOUR
        return "${hours}h ${minutes}m"
    }

    private fun resolveStrainScore(
        summary: DailySummary,
        prefs: UserPreferences,
    ): Float? =
        when (prefs.rasSourceMode) {
            LoadSourceMode.WORKOUT_ONLY -> summary.trimpWorkoutOnly
            LoadSourceMode.EVERYDAY_HEART_RATE -> summary.trimpEverydayHr ?: summary.trimpWorkoutOnly
        }

    private fun computeDeltaString(
        current: Int?,
        baseline: Int?,
    ): String? {
        if (current == null || baseline == null) return null
        val diff = current - baseline
        return if (diff > 0) "+$diff" else "$diff"
    }
}
