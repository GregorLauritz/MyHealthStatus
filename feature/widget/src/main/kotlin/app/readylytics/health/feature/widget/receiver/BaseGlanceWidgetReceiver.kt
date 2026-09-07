package app.readylytics.health.feature.widget.receiver

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import app.readylytics.health.core.model.domain.util.logE
import app.readylytics.health.core.model.domain.widget.WidgetUpdatePort
import app.readylytics.health.feature.widget.di.WidgetReceiverEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

abstract class BaseGlanceWidgetReceiver(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : GlanceAppWidgetReceiver() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        triggerWidgetUpdate(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        triggerWidgetUpdate(context)
    }

    internal open fun resolveWidgetUpdatePort(context: Context): WidgetUpdatePort {
        val entryPoint =
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                WidgetReceiverEntryPoint::class.java,
            )
        return entryPoint.widgetUpdatePort()
    }

    internal fun triggerWidgetUpdate(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(ioDispatcher).launch {
            try {
                resolveWidgetUpdatePort(context).updateAllWidgets()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logE(TAG, e) { "Failed to trigger widget update from receiver" }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BaseGlanceWidgetReceiver"
    }
}
