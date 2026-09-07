package app.readylytics.health.feature.widget.data

import app.readylytics.health.core.model.domain.model.DailySummary
import app.readylytics.health.core.model.domain.preferences.UserPreferences
import app.readylytics.health.core.model.domain.scoring.LoadSourceMode
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

object WidgetSnapshotMapper {
    private const val CALIBRATION_TOTAL_DAYS = 7
    private const val READINESS_OPTIMAL_THRESHOLD = 80
    private const val READINESS_GOOD_THRESHOLD = 60
    private const val READINESS_FAIR_THRESHOLD = 40
    private const val READINESS_MAX_SCORE = 100
    private const val MINUTES_PER_HOUR = 60
    private const val BASELINE_BODY_TEMP_CELSIUS = 36.5f
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

        val rhrDeltaFormatted =
            computeDeltaString(
                current = summary.restingHeartRate?.toDouble(),
                baseline = summary.rhrBpm?.toDouble(),
            )

        val hrvDeltaFormatted =
            computeDeltaString(
                current = summary.nocturnalHrv?.toDouble(),
                baseline = summary.hrvMuMssd?.toDouble(),
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
            skinTempDeltaFormatted =
                summary.avgSleepingBodyTemp?.let {
                    String.format(Locale.US, "%+.1f°C", it - BASELINE_BODY_TEMP_CELSIUS)
                },
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
                readinessScore >= READINESS_OPTIMAL_THRESHOLD -> "Optimal"
                readinessScore >= READINESS_GOOD_THRESHOLD -> "Good"
                readinessScore >= READINESS_FAIR_THRESHOLD -> "Fair"
                else -> "Low"
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
        current: Double?,
        baseline: Double?,
    ): String? {
        if (current == null || baseline == null) return null
        val diff = (current - baseline).roundToInt()
        return if (diff > 0) "+$diff" else "$diff"
    }
}
