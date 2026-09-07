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
open class WidgetUpdateCoordinator
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val dailySummaryRepository: DailySummaryRepository,
        private val preferencesReader: UserPreferencesReader,
    ) : WidgetUpdatePort {
        suspend fun buildLatestSnapshot(date: LocalDate? = null): WidgetSnapshot {
            val prefs = preferencesReader.userPreferences.first()
            val targetDate = date ?: LocalDate.now(prefs.scoringZone())
            val dateMidnightMs = targetDate.toMidnightEpochMilli(prefs.scoringZone())
            val summary = dailySummaryRepository.getByDate(dateMidnightMs)
            return WidgetSnapshotMapper.map(summary, prefs, targetDate)
        }

        override suspend fun updateAllWidgets() {
            try {
                val snapshot = buildLatestSnapshot()
                persistSnapshot(snapshot)
                val manager = createWidgetManager()

                val updatedCount =
                    safeUpdateWidgetGroup(manager, RecoveryGlanceWidget(), RecoveryGlanceWidget::class.java, snapshot) +
                        safeUpdateWidgetGroup(
                            manager,
                            StrainRecoveryWidget(),
                            StrainRecoveryWidget::class.java,
                            snapshot,
                        ) +
                        safeUpdateWidgetGroup(manager, VitalsStripWidget(), VitalsStripWidget::class.java, snapshot)

                logI(TAG) { "Pushed WidgetSnapshot to $updatedCount widgets" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logE(TAG, e) { "Failed to update Glance widgets" }
            }
        }

        internal open suspend fun persistSnapshot(snapshot: WidgetSnapshot) {
            WidgetSnapshotDefinition.getDataStore(context, "").updateData { snapshot }
        }

        internal open fun createWidgetManager(): GlanceAppWidgetManager = GlanceAppWidgetManager(context)

        private suspend fun safeUpdateWidgetGroup(
            manager: GlanceAppWidgetManager,
            widget: GlanceAppWidget,
            widgetClass: Class<out GlanceAppWidget>,
            snapshot: WidgetSnapshot,
        ): Int =
            runCatching {
                updateWidgetGroup(manager, widget, widgetClass, snapshot)
            }.onFailure { e ->
                if (e is CancellationException) throw e
                logE(TAG, e) { "Failed to update widget group ${widgetClass.simpleName}" }
            }.getOrDefault(0)

        internal open suspend fun updateWidgetGroup(
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
