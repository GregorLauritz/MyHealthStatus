package app.readylytics.health.feature.widget.receiver

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import app.readylytics.health.core.model.domain.widget.WidgetUpdatePort
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BaseGlanceWidgetReceiverTest {
    private val widgetUpdatePort = mockk<WidgetUpdatePort>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)

    private class TestGlanceWidgetReceiver(
        private val port: WidgetUpdatePort,
    ) : BaseGlanceWidgetReceiver(ioDispatcher = Dispatchers.Unconfined) {
        override val glanceAppWidget: GlanceAppWidget = mockk(relaxed = true)

        override fun resolveWidgetUpdatePort(context: Context): WidgetUpdatePort = port
    }

    @Test
    fun onUpdate_triggersWidgetUpdate() {
        val receiver = spyk(TestGlanceWidgetReceiver(widgetUpdatePort))
        every { receiver.goAsync() } returns mockk(relaxed = true)

        receiver.onUpdate(context, mockk<AppWidgetManager>(relaxed = true), intArrayOf(1))

        coVerify(exactly = 1) { widgetUpdatePort.updateAllWidgets() }
    }

    @Test
    fun onEnabled_triggersWidgetUpdate() {
        val receiver = spyk(TestGlanceWidgetReceiver(widgetUpdatePort))
        every { receiver.goAsync() } returns mockk(relaxed = true)

        receiver.onEnabled(context)

        coVerify(exactly = 1) { widgetUpdatePort.updateAllWidgets() }
    }
}
