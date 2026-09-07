package app.readylytics.health.feature.widget.receiver

import androidx.glance.appwidget.GlanceAppWidget
import app.readylytics.health.feature.widget.ui.RecoveryGlanceWidget

class RecoveryGlanceWidgetReceiver : BaseGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RecoveryGlanceWidget()
}
