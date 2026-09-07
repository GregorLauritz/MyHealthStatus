package app.readylytics.health.feature.widget.receiver

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import app.readylytics.health.feature.widget.ui.RecoveryGlanceWidget

class RecoveryGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecoveryGlanceWidget()
}
