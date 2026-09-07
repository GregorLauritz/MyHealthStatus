package app.readylytics.health.feature.widget.di

import app.readylytics.health.core.model.domain.widget.WidgetUpdatePort
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetReceiverEntryPoint {
    fun widgetUpdatePort(): WidgetUpdatePort
}
