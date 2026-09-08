package app.readylytics.health.feature.widget.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.readylytics.health.feature.widget.data.WidgetSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetDeltaFormatterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun formatDelta_null_returnsNull() {
        assertNull(WidgetDeltaFormatter.formatDelta(context, null))
        assertNull(WidgetDeltaFormatter.formatDelta(context, null, includeVsBaseline = true))
    }

    @Test
    fun formatDelta_positive_formatsUpArrow() {
        assertEquals("↑ 5", WidgetDeltaFormatter.formatDelta(context, 5))
        assertEquals("↑ 5 vs baseline", WidgetDeltaFormatter.formatDelta(context, 5, includeVsBaseline = true))
    }

    @Test
    fun formatDelta_negative_formatsDownArrowWithAbsoluteValue() {
        assertEquals("↓ 2", WidgetDeltaFormatter.formatDelta(context, -2))
        assertEquals("↓ 2 vs baseline", WidgetDeltaFormatter.formatDelta(context, -2, includeVsBaseline = true))
    }

    @Test
    fun formatDelta_zero_formatsZeroWithoutSign() {
        assertEquals("0", WidgetDeltaFormatter.formatDelta(context, 0))
        assertEquals("0 vs baseline", WidgetDeltaFormatter.formatDelta(context, 0, includeVsBaseline = true))
    }

    @Test
    fun formatRhrDelta_usesVsBaselineFormat() {
        val snapshot = WidgetSnapshot(rhrDelta = -1)
        assertEquals("↓ 1 vs baseline", WidgetDeltaFormatter.formatRhrDelta(context, snapshot))
    }

    @Test
    fun formatRhrDelta_fallsBackToLegacyFormattedWhenDeltaNull() {
        val snapshot = WidgetSnapshot(rhrDelta = null, rhrDeltaFormatted = "legacy delta")
        assertEquals("legacy delta", WidgetDeltaFormatter.formatRhrDelta(context, snapshot))
    }

    @Test
    fun formatHrvDelta_usesCompactFormat() {
        val snapshot = WidgetSnapshot(hrvDelta = 3)
        assertEquals("↑ 3", WidgetDeltaFormatter.formatHrvDelta(context, snapshot))
    }

    @Test
    fun formatHrvDelta_fallsBackToLegacyFormattedWhenDeltaNull() {
        val snapshot = WidgetSnapshot(hrvDelta = null, hrvDeltaFormatted = "legacy hrv")
        assertEquals("legacy hrv", WidgetDeltaFormatter.formatHrvDelta(context, snapshot))
    }

    @Test
    fun formatSecondaryRecoveryMetric_prioritizesHrvWhenAvailable() {
        val snapshot =
            WidgetSnapshot(
                nocturnalHrv = 45,
                hrvDelta = 3,
                restingHeartRate = 46,
                rhrDelta = -1,
            )
        assertEquals("HRV: 45 ms (↑ 3)", WidgetDeltaFormatter.formatSecondaryRecoveryMetric(context, snapshot))
    }

    @Test
    fun formatSecondaryRecoveryMetric_fallsBackToRhrWhenHrvUnavailable() {
        val snapshot =
            WidgetSnapshot(
                nocturnalHrv = null,
                hrvDelta = null,
                restingHeartRate = 46,
                rhrDelta = -1,
            )
        assertEquals("RHR: 46 bpm (↓ 1)", WidgetDeltaFormatter.formatSecondaryRecoveryMetric(context, snapshot))
    }

    @Test
    fun formatSecondaryRecoveryMetric_handlesZeroDelta() {
        val snapshot =
            WidgetSnapshot(
                nocturnalHrv = 55,
                hrvDelta = 0,
            )
        assertEquals("HRV: 55 ms (0)", WidgetDeltaFormatter.formatSecondaryRecoveryMetric(context, snapshot))
    }

    @Test
    fun formatSecondaryRecoveryMetric_fallsBackToLegacyFormatted() {
        val snapshot =
            WidgetSnapshot(
                nocturnalHrv = null,
                hrvDelta = null,
                restingHeartRate = null,
                rhrDelta = null,
                secondaryRecoveryMetricFormatted = "Legacy Secondary",
            )
        assertEquals("Legacy Secondary", WidgetDeltaFormatter.formatSecondaryRecoveryMetric(context, snapshot))
    }

    @Test
    fun formatSecondaryRecoveryMetric_returnsNullWhenNoMetricsAvailable() {
        val snapshot = WidgetSnapshot.EMPTY
        assertNull(WidgetDeltaFormatter.formatSecondaryRecoveryMetric(context, snapshot))
    }
}
