package app.readylytics.health.feature.widget.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class WidgetSnapshotSerializerTest {
    @Test
    fun defaultValue_hasDataIsFalse() {
        val defaultSnapshot = WidgetSnapshotSerializer.defaultValue
        assertFalse(defaultSnapshot.hasData)
        assertEquals(0L, defaultSnapshot.lastUpdatedEpochMs)
    }

    @Test
    fun roundTripSerialization_preservesFields() =
        runTest {
            val snapshot =
                WidgetSnapshot(
                    lastUpdatedEpochMs = 1757239200000L,
                    hasData = true,
                    readinessScore = 84,
                    readinessCategory = "Optimal",
                    isCalibrating = false,
                    calibrationDays = 7,
                    sleepScore = 88,
                    sleepDurationFormatted = "7h 42m",
                    deepSleepPercent = 18,
                    remSleepPercent = 22,
                    strainScore = 12.4f,
                    strainTargetFormatted = "10 - 14",
                    stepCountFormatted = "8,420",
                    restingHeartRate = 54,
                    rhrDeltaFormatted = "-2 vs base",
                    nocturnalHrv = 68,
                    hrvDeltaFormatted = "+5 vs base",
                    avgSpo2Formatted = "97%",
                    skinTempDeltaFormatted = "+0.2°C",
                )

            val output = ByteArrayOutputStream()
            WidgetSnapshotSerializer.writeTo(snapshot, output)

            val input = ByteArrayInputStream(output.toByteArray())
            val restored = WidgetSnapshotSerializer.readFrom(input)

            assertEquals(snapshot, restored)
            assertTrue(restored.hasData)
            assertEquals(84, restored.readinessScore)
            assertEquals("7h 42m", restored.sleepDurationFormatted)
        }

    @Test
    fun corruptedInput_returnsDefaultSnapshot() =
        runTest {
            val corruptInput = ByteArrayInputStream("{ not-valid-json ]".toByteArray())
            val result = WidgetSnapshotSerializer.readFrom(corruptInput)
            assertEquals(WidgetSnapshotSerializer.defaultValue, result)
        }

    @Test
    fun emptyInput_returnsDefaultSnapshot() =
        runTest {
            val emptyInput = ByteArrayInputStream(ByteArray(0))
            val result = WidgetSnapshotSerializer.readFrom(emptyInput)
            assertEquals(WidgetSnapshotSerializer.defaultValue, result)
        }

    @Test
    fun blankInput_returnsDefaultSnapshot() =
        runTest {
            val blankInput = ByteArrayInputStream("   \n\t  ".toByteArray())
            val result = WidgetSnapshotSerializer.readFrom(blankInput)
            assertEquals(WidgetSnapshotSerializer.defaultValue, result)
        }
}
