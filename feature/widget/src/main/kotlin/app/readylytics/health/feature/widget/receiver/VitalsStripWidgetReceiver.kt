package app.readylytics.health.feature.widget.receiver

import androidx.glance.appwidget.GlanceAppWidget
import app.readylytics.health.feature.widget.ui.VitalsStripWidget

class VitalsStripWidgetReceiver : BaseGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = VitalsStripWidget()
}
