package app.readylytics.health.feature.widget.data

import app.readylytics.health.core.model.domain.model.DailySummary
import app.readylytics.health.core.model.domain.preferences.UserPreferences
import app.readylytics.health.core.model.domain.scoring.LoadSourceMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlin.math.ln

class WidgetSnapshotMapperTest {
    private val testDate = LocalDate.of(2026, 9, 7)

    @Test
    fun nullSummary_returnsEmptySnapshot() {
        val snapshot =
            WidgetSnapshotMapper.map(
                summary = null,
                prefs = UserPreferences(),
                date = testDate,
            )
        assertFalse(snapshot.hasData)
        assertNull(snapshot.readinessScore)
        assertNull(snapshot.sleepScore)
        assertNull(snapshot.sleepDurationFormatted)
        assertNull(snapshot.strainScore)
    }

    @Test
    fun summaryFromDifferentDate_returnsEmptySnapshot() {
        val summary =
            DailySummary(
                date = testDate.minusDays(1),
                sleepScore = 85f,
            )
        val snapshot =
            WidgetSnapshotMapper.map(
                summary = summary,
                prefs = UserPreferences(),
                date = testDate,
            )
        assertFalse(snapshot.hasData)
        assertNull(snapshot.readinessScore)
        assertNull(snapshot.sleepScore)
    }

    @Test
    fun calibratingSummary_formatsCalibratingStatus() {
        val summary =
            DailySummary(
                date = testDate,
                isCalibrating = true,
                baselineObservationCount = 4,
                sleepScore = 82f,
                sleepDurationMinutes = 450,
            )
        val snapshot =
            WidgetSnapshotMapper.map(
                summary = summary,
                prefs = UserPreferences(),
                date = testDate,
            )
        assertTrue(snapshot.hasData)
        assertTrue(snapshot.isCalibrating)
        assertEquals(4, snapshot.calibrationDays)
        assertNull(snapshot.readinessScore)
        assertNull(snapshot.readinessCategory)
        assertEquals(82, snapshot.sleepScore)
        assertEquals("7h 30m", snapshot.sleepDurationFormatted)
    }

    @Test
    fun populatedSummary_formatsReadinessAndDeltas() {
        val summary =
            DailySummary(
                date = testDate,
                isCalibrating = false,
                readinessWorkoutOnly = 88f,
                readinessEverydayHr = 79f,
                sleepScore = 85f,
                sleepDurationMinutes = 462,
                deepSleepPercent = 19.4f,
                remSleepPercent = 21.8f,
                trimpWorkoutOnly = 14.2f,
                trimpEverydayHr = 18.5f,
                stepCount = 10450,
                restingHeartRate = 56,
                rhrBpm = 58f,
                baselineCalculatedAtDate = testDate,
                nocturnalHrv = 65,
                hrvMuMssd = ln(60.0).toFloat(),
                avgSleepingSpo2 = 97.4f,
                avgSleepingBodyTemp = 36.6f,
            )
        val workoutOnlySnapshot =
            WidgetSnapshotMapper.map(
                summary = summary,
                prefs = UserPreferences(rasSourceMode = LoadSourceMode.WORKOUT_ONLY),
                date = testDate,
            )
        assertTrue(workoutOnlySnapshot.hasData)
        assertFalse(workoutOnlySnapshot.isCalibrating)
        assertEquals(88, workoutOnlySnapshot.readinessScore)
        assertEquals("Peak", workoutOnlySnapshot.readinessCategory)
        assertEquals(85, workoutOnlySnapshot.sleepScore)
        assertEquals("7h 42m", workoutOnlySnapshot.sleepDurationFormatted)
        assertEquals(19, workoutOnlySnapshot.deepSleepPercent)
        assertEquals(22, workoutOnlySnapshot.remSleepPercent)
        assertEquals(14.2f, workoutOnlySnapshot.strainScore)
        assertEquals("-2", workoutOnlySnapshot.rhrDeltaFormatted)
        assertEquals("+5", workoutOnlySnapshot.hrvDeltaFormatted)
        val expectedStepCount = NumberFormat.getIntegerInstance(Locale.getDefault()).format(10450)
        assertEquals(expectedStepCount, workoutOnlySnapshot.stepCountFormatted)
        assertEquals("97%", workoutOnlySnapshot.avgSpo2Formatted)
        assertNull(workoutOnlySnapshot.skinTempDeltaFormatted)
    }

    @Test
    fun everydayHrMode_selectsEverydayHrReadinessAndTrimp() {
        val summary =
            DailySummary(
                date = testDate,
                isCalibrating = false,
                readinessWorkoutOnly = 88f,
                readinessEverydayHr = 79f,
                trimpWorkoutOnly = 14.2f,
                trimpEverydayHr = 18.5f,
            )
        val everydaySnapshot =
            WidgetSnapshotMapper.map(
                summary = summary,
                prefs = UserPreferences(rasSourceMode = LoadSourceMode.EVERYDAY_HEART_RATE),
                date = testDate,
            )
        assertEquals(79, everydaySnapshot.readinessScore)
        assertEquals("Maintain", everydaySnapshot.readinessCategory)
        assertEquals(18.5f, everydaySnapshot.strainScore)
    }

    @Test
    fun everydayHrMode_fallsBackToWorkoutOnlyWhenEverydayHrNull() {
        val summary =
            DailySummary(
                date = testDate,
                isCalibrating = false,
                readinessWorkoutOnly = 84f,
                readinessEverydayHr = null,
                trimpWorkoutOnly = 12.3f,
                trimpEverydayHr = null,
            )
        val snapshot =
            WidgetSnapshotMapper.map(
                summary = summary,
                prefs = UserPreferences(rasSourceMode = LoadSourceMode.EVERYDAY_HEART_RATE),
                date = testDate,
            )
        assertEquals(84, snapshot.readinessScore)
        assertEquals("Maintain", snapshot.readinessCategory)
        assertEquals(12.3f, snapshot.strainScore)
    }

    @Test
    fun readinessCategory_mapsScoreTiersCorrectly() {
        val cases =
            listOf(
                90f to "Peak",
                85f to "Peak",
                84f to "Maintain",
                60f to "Maintain",
                59f to "Caution",
                30f to "Caution",
                29f to "High Fatigue",
                0f to "High Fatigue",
            )
        for ((score, expectedCategory) in cases) {
            val summary =
                DailySummary(
                    date = testDate,
                    isCalibrating = false,
                    readinessWorkoutOnly = score,
                )
            val snapshot =
                WidgetSnapshotMapper.map(
                    summary = summary,
                    prefs = UserPreferences(rasSourceMode = LoadSourceMode.WORKOUT_ONLY),
                    date = testDate,
                )
            assertEquals("Score $score should map to $expectedCategory", expectedCategory, snapshot.readinessCategory)
        }
    }

    @Test
    fun calibrationDays_clampsBetweenZeroAndSeven() {
        val summaryClampedHigh =
            DailySummary(
                date = testDate,
                isCalibrating = true,
                baselineObservationCount = 12,
            )
        val snapshotHigh =
            WidgetSnapshotMapper.map(
                summary = summaryClampedHigh,
                prefs = UserPreferences(),
                date = testDate,
            )
        assertEquals(7, snapshotHigh.calibrationDays)

        val summaryClampedLow =
            DailySummary(
                date = testDate,
                isCalibrating = true,
                baselineObservationCount = -3,
            )
        val snapshotLow =
            WidgetSnapshotMapper.map(
                summary = summaryClampedLow,
                prefs = UserPreferences(),
                date = testDate,
            )
        assertEquals(0, snapshotLow.calibrationDays)
    }

    @Test
    fun sleepDuration_nullOrZero_formatsNull() {
        val summaryZero =
            DailySummary(
                date = testDate,
                sleepDurationMinutes = 0,
            )
        val snapshotZero =
            WidgetSnapshotMapper.map(
                summary = summaryZero,
                prefs = UserPreferences(),
                date = testDate,
            )
        assertNull(snapshotZero.sleepDurationFormatted)

        val summaryNull =
            DailySummary(
                date = testDate,
                sleepDurationMinutes = null,
            )
        val snapshotNull =
            WidgetSnapshotMapper.map(
                summary = summaryNull,
                prefs = UserPreferences(),
                date = testDate,
            )
        assertNull(snapshotNull.sleepDurationFormatted)
    }

    @Test
    fun deltas_zeroDifference_formatsWithoutSign() {
        val summary =
            DailySummary(
                date = testDate,
                restingHeartRate = 60,
                rhrBpm = 60f,
                baselineCalculatedAtDate = testDate,
                nocturnalHrv = 55,
                hrvMuMssd = ln(55.0).toFloat(),
            )
        val snapshot =
            WidgetSnapshotMapper.map(
                summary = summary,
                prefs = UserPreferences(),
                date = testDate,
            )
        assertEquals("0", snapshot.rhrDeltaFormatted)
        assertEquals("0", snapshot.hrvDeltaFormatted)
    }

    @Test
    fun deltas_nullCurrentOrBaseline_formatsNull() {
        val summary =
            DailySummary(
                date = testDate,
                restingHeartRate = null,
                rhrBpm = 60f,
                baselineCalculatedAtDate = testDate,
                nocturnalHrv = 55,
                hrvMuMssd = null,
            )
        val snapshot =
            WidgetSnapshotMapper.map(
                summary = summary,
                prefs = UserPreferences(),
                date = testDate,
            )
        assertNull(snapshot.rhrDeltaFormatted)
        assertNull(snapshot.hrvDeltaFormatted)
    }

    @Test
    fun deltas_userScenario_calculatesLinearHrvDeltaAndIgnoresTemp() {
        // User scenario: nocturnalHrv = 45, baseline hrv = 42 (hrvMuMssd = ln(42)), rhr = 46, baseline rhr = 47
        val summary =
            DailySummary(
                date = testDate,
                readinessWorkoutOnly = 82f,
                restingHeartRate = 46,
                rhrBpm = 47f,
                baselineCalculatedAtDate = testDate,
                nocturnalHrv = 45,
                hrvMuMssd = ln(42.0).toFloat(),
                avgSleepingSpo2 = 99.2f,
                avgSleepingBodyTemp = 34.2f,
            )
        val snapshot =
            WidgetSnapshotMapper.map(
                summary = summary,
                prefs = UserPreferences(),
                date = testDate,
            )
        assertEquals(82, snapshot.readinessScore)
        assertEquals("Maintain", snapshot.readinessCategory)
        assertEquals("-1", snapshot.rhrDeltaFormatted)
        assertEquals("+3", snapshot.hrvDeltaFormatted)
        assertEquals("99%", snapshot.avgSpo2Formatted)
        assertNull(snapshot.skinTempDeltaFormatted)
    }
}
