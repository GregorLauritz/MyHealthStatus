package app.readylytics.health.feature.widget.data

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import app.readylytics.health.core.model.data.preferences.scoringZone
import app.readylytics.health.core.model.domain.preferences.UserPreferencesReader
import app.readylytics.health.core.model.domain.repository.DailySummaryRepository
import app.readylytics.health.core.model.domain.util.logE
import app.readylytics.health.core.model.domain.util.logI
import app.readylytics.health.core.model.domain.util.toMidnightEpochMilli
import app.readylytics.health.core.model.domain.widget.WidgetUpdatePort
import app.readylytics.health.feature.widget.ui.RecoveryGlanceWidget
import app.readylytics.health.feature.widget.ui.StrainRecoveryWidget
import app.readylytics.health.feature.widget.ui.VitalsStripWidget
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetUpdateCoordinator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dailySummaryRepository: DailySummaryRepository,
        private val preferencesReader: UserPreferencesReader,
    ) : WidgetUpdatePort {
        suspend fun buildLatestSnapshot(date: LocalDate = LocalDate.now()): WidgetSnapshot {
            val prefs = preferencesReader.userPreferences.first()
            val dateMidnightMs = date.toMidnightEpochMilli(prefs.scoringZone())
            val summary = dailySummaryRepository.getByDate(dateMidnightMs)
            return WidgetSnapshotMapper.map(summary, prefs, date)
        }

        override suspend fun updateAllWidgets() {
            try {
                val snapshot = buildLatestSnapshot()
                val manager = GlanceAppWidgetManager(context)

                val updatedCount =
                    updateWidgetGroup(manager, RecoveryGlanceWidget(), RecoveryGlanceWidget::class.java, snapshot) +
                        updateWidgetGroup(manager, StrainRecoveryWidget(), StrainRecoveryWidget::class.java, snapshot) +
                        updateWidgetGroup(manager, VitalsStripWidget(), VitalsStripWidget::class.java, snapshot)

                logI(TAG) { "Pushed WidgetSnapshot to $updatedCount widgets" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logE(TAG, e) { "Failed to update Glance widgets" }
            }
        }

        private suspend fun updateWidgetGroup(
            manager: GlanceAppWidgetManager,
            widget: GlanceAppWidget,
            widgetClass: Class<out GlanceAppWidget>,
            snapshot: WidgetSnapshot,
        ): Int {
            val ids = manager.getGlanceIds(widgetClass)
            ids.forEach { id ->
                updateAppWidgetState(context, WidgetSnapshotDefinition, id) { snapshot }
                widget.update(context, id)
            }
            return ids.size
        }

        companion object {
            private const val TAG = "WidgetUpdateCoordinator"
        }
    }
