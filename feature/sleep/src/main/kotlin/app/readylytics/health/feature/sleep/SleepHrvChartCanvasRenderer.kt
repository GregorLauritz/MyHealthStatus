package app.readylytics.health.feature.sleep

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import app.readylytics.health.core.model.domain.repository.HrvRecordData
import java.time.Instant

// Canvas draw passes for the nightly HRV chart, structured like SleepHrChartCanvasRenderer.kt
// (grid/axes, HRV line, avg-HRV reference line, selection highlight).

internal fun DrawScope.renderSleepHrvCanvas(
    data: SleepHrvDerivedData,
    style: SleepHrvChartStyle,
    selectedSample: HrvRecordData?,
    pulse: SleepHrPulseAnimation,
    leftLabelWidthPx: Float,
    avgHrv: Float?,
    zoomedX: (Long) -> Float,
) {
    val plotTop = 0f
    val bottomLabelHeight = SLEEP_HRV_BOTTOM_LABEL_HEIGHT.toPx()
    val plotRect = Rect(leftLabelWidthPx, plotTop, size.width, size.height - bottomLabelHeight)
    val plotH = plotRect.bottom - plotRect.top

    fun msToY(ms: Float): Float = plotRect.top + (1f - (ms - data.yMin) / (data.yMax - data.yMin).toFloat()) * plotH

    drawSleepHrvGridAndAxes(plotRect, style, data.yLabels, data.labelTimestamps, ::msToY, zoomedX)
    drawSleepHrvLine(plotRect, data.segments, style.lineColor, ::msToY, zoomedX)
    if (avgHrv != null) {
        drawSleepHrvAvgLine(plotRect, avgHrv, style.avgColor, ::msToY)
    }
    drawSleepHrvSelection(plotRect, selectedSample, style.lineColor, pulse, ::msToY, zoomedX)
}

private fun DrawScope.drawSleepHrvGridAndAxes(
    plotRect: Rect,
    style: SleepHrvChartStyle,
    yLabels: List<Int>,
    labelTimestamps: List<Long>,
    msToY: (Float) -> Float,
    zoomedX: (Long) -> Float,
) {
    val gridLineColor = style.axisLineColor.copy(alpha = 0.4f)
    val strokePx = 1.dp.toPx()

    for (ms in yLabels) {
        val y = msToY(ms.toFloat())
        if (y < plotRect.bottom && y > plotRect.top) {
            drawLine(gridLineColor, Offset(plotRect.left, y), Offset(plotRect.right, y), strokePx)
        }
    }

    for (ts in labelTimestamps) {
        val x = zoomedX(ts)
        if (x in plotRect.left..plotRect.right) {
            drawLine(gridLineColor, Offset(x, plotRect.top), Offset(x, plotRect.bottom), strokePx)
        }
    }

    drawLine(
        style.axisLineColor,
        Offset(plotRect.left, plotRect.bottom),
        Offset(plotRect.right, plotRect.bottom),
        1.dp.toPx(),
    )

    drawSleepHrvYAxisLabels(plotRect, style, yLabels, msToY)
    drawSleepHrvXAxisLabels(plotRect, style, labelTimestamps, zoomedX)
}

private fun DrawScope.drawSleepHrvYAxisLabels(
    plotRect: Rect,
    style: SleepHrvChartStyle,
    yLabels: List<Int>,
    msToY: (Float) -> Float,
) {
    for (ms in yLabels) {
        val y = msToY(ms.toFloat())
        if (y < plotRect.bottom - 4.dp.toPx() && y > plotRect.top + 4.dp.toPx()) {
            val measured = style.textMeasurer.measure(ms.toString(), style.labelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(plotRect.left - measured.size.width - 4.dp.toPx(), y - measured.size.height / 2f),
            )
        }
    }

    val msUnitMeasured = style.textMeasurer.measure(style.msUnitLabel, style.axisTitleStyle)
    val msUnitPivot = Offset(x = 10.dp.toPx(), y = (plotRect.top + plotRect.bottom) / 2f)
    rotate(degrees = -90f, pivot = msUnitPivot) {
        drawText(
            textLayoutResult = msUnitMeasured,
            topLeft =
                Offset(
                    msUnitPivot.x - msUnitMeasured.size.width / 2f,
                    msUnitPivot.y - msUnitMeasured.size.height / 2f,
                ),
        )
    }
}

private fun DrawScope.drawSleepHrvXAxisLabels(
    plotRect: Rect,
    style: SleepHrvChartStyle,
    labelTimestamps: List<Long>,
    zoomedX: (Long) -> Float,
) {
    for (ts in labelTimestamps) {
        val x = zoomedX(ts)
        if (x in plotRect.left..plotRect.right) {
            val label = style.timeFormatter.format(Instant.ofEpochMilli(ts))
            val measured = style.textMeasurer.measure(label, style.labelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft =
                    Offset(
                        (x - measured.size.width / 2f).coerceIn(plotRect.left, plotRect.right - measured.size.width),
                        plotRect.bottom + 2.dp.toPx(),
                    ),
            )
        }
    }
}

private fun DrawScope.drawSleepHrvLine(
    plotRect: Rect,
    segments: List<List<HrvRecordData>>,
    lineColor: Color,
    msToY: (Float) -> Float,
    zoomedX: (Long) -> Float,
) {
    clipRect(left = plotRect.left, top = plotRect.top, right = plotRect.right, bottom = plotRect.bottom) {
        for (segment in segments) {
            if (segment.size == 1) {
                val x = zoomedX(segment[0].timestampMs)
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x, msToY(segment[0].rmssdMs)),
                )
            } else {
                val path = Path()
                segment.forEachIndexed { i, sample ->
                    val x = zoomedX(sample.timestampMs)
                    val y = msToY(sample.rmssdMs)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

private fun DrawScope.drawSleepHrvAvgLine(
    plotRect: Rect,
    avgHrv: Float,
    avgColor: Color,
    msToY: (Float) -> Float,
) {
    val y = msToY(avgHrv)
    if (y !in plotRect.top..plotRect.bottom) return

    drawLine(
        color = avgColor.copy(alpha = 0.7f),
        start = Offset(plotRect.left, y),
        end = Offset(plotRect.right, y),
        strokeWidth = 1.5.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
    )
}

private fun DrawScope.drawSleepHrvSelection(
    plotRect: Rect,
    selectedSample: HrvRecordData?,
    lineColor: Color,
    pulse: SleepHrPulseAnimation,
    msToY: (Float) -> Float,
    zoomedX: (Long) -> Float,
) {
    val selected = selectedSample ?: return
    val selectedX = zoomedX(selected.timestampMs)
    val selectedY = msToY(selected.rmssdMs)
    if (selectedX !in plotRect.left..plotRect.right) return

    drawLine(
        color = lineColor.copy(alpha = 0.4f),
        start = Offset(selectedX, plotRect.top),
        end = Offset(selectedX, plotRect.bottom),
        strokeWidth = 1.5.dp.toPx(),
    )
    drawCircle(
        color = lineColor.copy(alpha = pulse.alpha.value),
        radius = 8.dp.toPx() * pulse.radiusCoeff.value,
        center = Offset(selectedX, selectedY),
    )
    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(selectedX, selectedY))
    drawCircle(
        color = Color.White,
        radius = 1.5.dp.toPx(),
        center = Offset(selectedX, selectedY),
    )
}
