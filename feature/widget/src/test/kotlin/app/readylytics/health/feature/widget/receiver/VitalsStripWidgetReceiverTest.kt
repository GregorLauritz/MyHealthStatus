package app.readylytics.health.feature.widget.receiver

import app.readylytics.health.feature.widget.data.WidgetSnapshotDefinition
import app.readylytics.health.feature.widget.ui.VitalsStripWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VitalsStripWidgetReceiverTest {
    @Test
    fun glanceAppWidget_isInstanceOfVitalsStripWidget() {
        val receiver = VitalsStripWidgetReceiver()
        assertTrue(receiver.glanceAppWidget is VitalsStripWidget)
        assertEquals(WidgetSnapshotDefinition, receiver.glanceAppWidget.stateDefinition)
    }
}
