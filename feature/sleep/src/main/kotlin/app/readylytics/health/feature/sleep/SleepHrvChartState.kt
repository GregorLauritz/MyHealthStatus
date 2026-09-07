package app.readylytics.health.feature.sleep

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import app.readylytics.health.core.model.domain.repository.HrvRecordData
import app.readylytics.health.core.model.domain.repository.SleepSessionData
import app.readylytics.health.core.ui.components.DayTimelineScale
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import app.readylytics.health.core.ui.R as CoreUiR

// State holders and their @Composable factories for the nightly HRV chart, structured like
// SleepHrChartState.kt. SleepHrPulseAnimation/rememberSleepHrPulseAnimation and getLabelTimestamps
// are geometry/animation-only helpers already shared with the HR chart, so they're reused as-is.

internal data class SleepHrvChartStyle(
    val lineColor: Color,
    val axisLineColor: Color,
    val avgColor: Color,
    val textMeasurer: TextMeasurer,
    val labelStyle: TextStyle,
    val axisTitleStyle: TextStyle,
    val timeFormatter: DateTimeFormatter,
    val msUnitLabel: String,
)

@Composable
internal fun rememberSleepHrvChartStyle(timeFormatter: DateTimeFormatter): SleepHrvChartStyle {
    val axisTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    return SleepHrvChartStyle(
        lineColor = MaterialTheme.colorScheme.primary,
        axisLineColor = MaterialTheme.colorScheme.outlineVariant,
        avgColor = MaterialTheme.colorScheme.tertiary,
        textMeasurer = rememberTextMeasurer(),
        labelStyle = TextStyle(color = axisTextColor, fontSize = 10.sp),
        axisTitleStyle = TextStyle(color = axisTextColor, fontSize = 12.sp),
        timeFormatter = timeFormatter,
        msUnitLabel = stringResource(CoreUiR.string.unit_ms),
    )
}

internal data class SleepHrvDerivedData(
    val sortedSamples: List<HrvRecordData>,
    val segments: List<List<HrvRecordData>>,
    val yMin: Int,
    val yMax: Int,
    val labelTimestamps: List<Long>,
    val yLabels: List<Int>,
)

@Composable
internal fun rememberSleepHrvDerivedData(
    session: SleepSessionData,
    samples: List<HrvRecordData>,
    avgHrv: Float?,
): SleepHrvDerivedData {
    val sortedSamples = remember(samples) { samples.sortedBy { it.timestampMs } }
    val bounds =
        remember(sortedSamples, avgHrv) {
            val values = sortedSamples.map { it.rmssdMs } + listOfNotNull(avgHrv)
            values.minOf { it }.roundToInt() to values.maxOf { it }.roundToInt()
        }
    val yMin = remember(bounds) { (bounds.first - 10).coerceAtLeast(0) }
    val yMax = remember(bounds, yMin) { (bounds.second + 10).coerceAtLeast(yMin + 20) }
    val segments =
        remember(sortedSamples) { SleepHrvChartHelper.splitIntoSegments(sortedSamples, SLEEP_HRV_GAP_THRESHOLD_MS) }
    val labelTimestamps =
        remember(session.startTime, session.endTime) { getLabelTimestamps(session.startTime, session.endTime) }
    val yLabels =
        remember(yMin, yMax) {
            (0 until SLEEP_HRV_Y_TICK_COUNT).map { i -> yMin + (yMax - yMin) * i / (SLEEP_HRV_Y_TICK_COUNT - 1) }
        }
    return SleepHrvDerivedData(sortedSamples, segments, yMin, yMax, labelTimestamps, yLabels)
}

internal class SleepHrvInteractionState(
    val scaleX: MutableFloatState,
    val offsetX: MutableFloatState,
    val selectedSample: MutableState<HrvRecordData?>,
)

@Composable
internal fun rememberSleepHrvInteractionState(
    session: SleepSessionData,
    sortedSamples: List<HrvRecordData>,
): SleepHrvInteractionState {
    val scaleX = remember { mutableFloatStateOf(1f) }
    val offsetX = remember { mutableFloatStateOf(0f) }
    val selectedSample = remember { mutableStateOf<HrvRecordData?>(null) }

    LaunchedEffect(session.id, sortedSamples) {
        if (selectedSample.value != null &&
            sortedSamples.none { it.timestampMs == selectedSample.value?.timestampMs }
        ) {
            selectedSample.value = null
        }
    }

    LaunchedEffect(session.id) {
        scaleX.floatValue = 1f
        offsetX.floatValue = 0f
    }

    return remember(scaleX, offsetX, selectedSample) { SleepHrvInteractionState(scaleX, offsetX, selectedSample) }
}

internal class SleepHrvChartState(
    val session: SleepSessionData,
    val data: SleepHrvDerivedData,
    val scale: DayTimelineScale,
    val interaction: SleepHrvInteractionState,
    val pulse: SleepHrPulseAnimation,
    val style: SleepHrvChartStyle,
    val avgHrv: Float?,
)

internal data class SleepHrvAccessibility(
    val chartSummary: String,
    val selectedValueDescription: String,
    val customActions: List<CustomAccessibilityAction>,
)

@Composable
internal fun rememberSleepHrvAccessibility(
    sortedSamples: List<HrvRecordData>,
    selectedSampleState: MutableState<HrvRecordData?>,
    timeFormatter: DateTimeFormatter,
): SleepHrvAccessibility {
    var selectedSample by selectedSampleState
    val prevActionLabel = stringResource(CoreUiR.string.action_previous_point)
    val nextActionLabel = stringResource(CoreUiR.string.action_next_point)
    val clearActionLabel = stringResource(CoreUiR.string.action_clear_selection)

    val customActionsList =
        remember(selectedSample, sortedSamples) {
            val list = mutableListOf<CustomAccessibilityAction>()
            if (sortedSamples.isNotEmpty()) {
                list.add(
                    CustomAccessibilityAction(prevActionLabel) {
                        val currentIndex =
                            sortedSamples.indexOfFirst { it.timestampMs == selectedSample?.timestampMs }
                        selectedSample =
                            if (currentIndex > 0) sortedSamples[currentIndex - 1] else sortedSamples.last()
                        true
                    },
                )
                list.add(
                    CustomAccessibilityAction(nextActionLabel) {
                        val currentIndex =
                            sortedSamples.indexOfFirst { it.timestampMs == selectedSample?.timestampMs }
                        selectedSample =
                            if (currentIndex != -1 && currentIndex < sortedSamples.lastIndex) {
                                sortedSamples[currentIndex + 1]
                            } else {
                                sortedSamples.first()
                            }
                        true
                    },
                )
            }
            if (selectedSample != null) {
                list.add(
                    CustomAccessibilityAction(clearActionLabel) {
                        selectedSample = null
                        true
                    },
                )
            }
            list
        }

    val chartSummary = stringResource(R.string.chart_accessibility_sleep_hrv_summary)
    val selectedValueDescription =
        selectedSample?.let { sample ->
            val timeStr = timeFormatter.format(Instant.ofEpochMilli(sample.timestampMs))
            stringResource(R.string.chart_accessibility_selected_sleep_hrv, sample.rmssdMs.roundToInt(), timeStr)
        } ?: stringResource(CoreUiR.string.chart_accessibility_no_selection)

    return SleepHrvAccessibility(chartSummary, selectedValueDescription, customActionsList)
}
