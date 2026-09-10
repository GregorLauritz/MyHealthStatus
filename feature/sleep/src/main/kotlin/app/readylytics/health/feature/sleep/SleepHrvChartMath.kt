package app.readylytics.health.feature.sleep

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.unit.IntOffset
import app.readylytics.health.core.model.domain.repository.HrvRecordData
import app.readylytics.health.core.ui.components.DataPointTooltipData
import app.readylytics.health.core.ui.components.DayTimelineScale
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

// Pure (non-Composable) plot math for the nightly HRV chart, mirroring SleepHrChartMath.kt's
// tap-resolution/tooltip helpers for HrvRecordData. sleepHrZoomedX/sleepHrZoomPan are geometry-only
// (no HR-specific types) so this chart reuses them directly instead of duplicating.

internal fun findTappedSleepHrvSample(
    tapOffset: Offset,
    tappedUnscaledX: Float,
    plotRect: Rect,
    plotW: Float,
    sessionStartMs: Long,
    scale: DayTimelineScale,
    sortedSamples: List<HrvRecordData>,
): HrvRecordData? {
    if (!plotRect.contains(tapOffset)) return null
    val tapFrac = ((tappedUnscaledX - plotRect.left) / plotW).coerceIn(0f, 1f)
    val tapMs = sessionStartMs + (tapFrac * scale.durationMs).toLong()
    return sortedSamples.minByOrNull { abs(it.timestampMs - tapMs) }
}

/** Resolves the tapped gesture coordinate to the nearest sample, using the PointerInputScope's own size/density. */
internal fun PointerInputScope.resolveTappedSleepHrvSample(
    tapOffset: Offset,
    tappedUnscaledX: Float,
    leftLabelWidthPx: Float,
    plotW: Float,
    sessionStartMs: Long,
    scale: DayTimelineScale,
    sortedSamples: List<HrvRecordData>,
): HrvRecordData? {
    val bottomLabelHeightPx = SLEEP_HRV_BOTTOM_LABEL_HEIGHT.toPx()
    val topLabelPaddingPx = SLEEP_HRV_TOP_LABEL_PADDING.toPx()
    val plotRect =
        Rect(leftLabelWidthPx, topLabelPaddingPx, size.width.toFloat(), size.height.toFloat() - bottomLabelHeightPx)
    return findTappedSleepHrvSample(tapOffset, tappedUnscaledX, plotRect, plotW, sessionStartMs, scale, sortedSamples)
}

internal fun computeSleepHrvTooltip(
    selectedSample: HrvRecordData?,
    yMin: Int,
    yMax: Int,
    zoomedX: (Long) -> Float,
    plotTop: Float,
    plotBottom: Float,
    timeFormatter: DateTimeFormatter,
    msTemplate: String,
): DataPointTooltipData? {
    val sample = selectedSample ?: return null
    val plotH = plotBottom - plotTop

    val sampleX = zoomedX(sample.timestampMs)
    val sampleY = plotTop + (1f - (sample.rmssdMs - yMin) / (yMax - yMin).toFloat()) * plotH
    val timeStr = timeFormatter.format(Instant.ofEpochMilli(sample.timestampMs))

    return DataPointTooltipData(
        valueText = String.format(Locale.getDefault(), msTemplate, sample.rmssdMs.roundToInt()),
        dateText = timeStr,
        offset = IntOffset(sampleX.roundToInt(), sampleY.roundToInt()),
    )
}
