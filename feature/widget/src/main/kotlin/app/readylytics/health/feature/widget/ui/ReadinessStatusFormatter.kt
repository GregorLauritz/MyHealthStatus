package app.readylytics.health.feature.widget.ui

import android.content.Context
import androidx.annotation.StringRes
import app.readylytics.health.feature.widget.R
import java.util.Locale

object ReadinessStatusFormatter {
    @StringRes
    fun resolveStringRes(category: String?): Int? =
        when (category?.lowercase(Locale.ROOT)) {
            "peak" -> R.string.widget_status_peak
            "maintain" -> R.string.widget_status_maintain
            "caution" -> R.string.widget_status_caution
            "high fatigue" -> R.string.widget_status_high_fatigue
            "optimal" -> R.string.widget_status_optimal
            "good" -> R.string.widget_status_good
            "fair" -> R.string.widget_status_fair
            "low" -> R.string.widget_status_low
            else -> null
        }

    fun format(
        context: Context,
        category: String?,
    ): String? {
        val resId = resolveStringRes(category) ?: return category
        return context.getString(resId)
    }
}
