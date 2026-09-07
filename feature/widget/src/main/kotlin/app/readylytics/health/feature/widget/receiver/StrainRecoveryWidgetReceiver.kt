package app.readylytics.health.feature.widget.receiver

import androidx.glance.appwidget.GlanceAppWidget
import app.readylytics.health.feature.widget.ui.StrainRecoveryWidget

class StrainRecoveryWidgetReceiver : BaseGlanceWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StrainRecoveryWidget()
}
