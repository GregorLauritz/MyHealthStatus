package app.readylytics.health.feature.widget.ui

import android.content.Context
import app.readylytics.health.feature.widget.R
import app.readylytics.health.feature.widget.data.WidgetSnapshot
import kotlin.math.abs

object WidgetDeltaFormatter {
    fun formatDelta(
        context: Context,
        delta: Int?,
        includeVsBaseline: Boolean = false,
    ): String? {
        if (delta == null) return null
        val arrowText =
            when {
                delta > 0 -> context.getString(R.string.widget_delta_up_format, delta.toString())
                delta < 0 -> context.getString(R.string.widget_delta_down_format, abs(delta).toString())
                else -> "0"
            }
        return if (includeVsBaseline) {
            context.getString(R.string.widget_delta_vs_baseline_format, arrowText)
        } else {
            arrowText
        }
    }

    fun formatRhrDelta(
        context: Context,
        snapshot: WidgetSnapshot,
    ): String? =
        formatDelta(context, snapshot.rhrDelta, includeVsBaseline = true)
            ?: snapshot.rhrDeltaFormatted

    fun formatHrvDelta(
        context: Context,
        snapshot: WidgetSnapshot,
    ): String? =
        formatDelta(context, snapshot.hrvDelta, includeVsBaseline = false)
            ?: snapshot.hrvDeltaFormatted

    fun formatSecondaryRecoveryMetric(
        context: Context,
        snapshot: WidgetSnapshot,
    ): String? =
        formatSecondaryRecoveryMetric(
            context = context,
            nocturnalHrv = snapshot.nocturnalHrv,
            hrvDelta = snapshot.hrvDelta,
            restingHeartRate = snapshot.restingHeartRate,
            rhrDelta = snapshot.rhrDelta,
        ) ?: snapshot.secondaryRecoveryMetricFormatted

    fun formatSecondaryRecoveryMetric(
        context: Context,
        nocturnalHrv: Int?,
        hrvDelta: Int?,
        restingHeartRate: Int?,
        rhrDelta: Int?,
    ): String? {
        val hrvDeltaFormatted = formatDelta(context, hrvDelta, includeVsBaseline = false)
        val rhrDeltaFormatted = formatDelta(context, rhrDelta, includeVsBaseline = false)
        return when {
            nocturnalHrv != null && hrvDeltaFormatted != null ->
                context.getString(R.string.widget_secondary_hrv_format, nocturnalHrv, hrvDeltaFormatted)
            restingHeartRate != null && rhrDeltaFormatted != null ->
                context.getString(R.string.widget_secondary_rhr_format, restingHeartRate, rhrDeltaFormatted)
            else -> null
        }
    }
}
