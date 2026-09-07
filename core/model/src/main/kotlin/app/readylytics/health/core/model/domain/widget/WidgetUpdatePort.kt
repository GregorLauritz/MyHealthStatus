package app.readylytics.health.core.model.domain.widget

interface WidgetUpdatePort {
    suspend fun updateAllWidgets()
}
