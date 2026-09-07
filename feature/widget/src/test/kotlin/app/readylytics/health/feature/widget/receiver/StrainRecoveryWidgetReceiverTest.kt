package app.readylytics.health.feature.widget.receiver

import app.readylytics.health.feature.widget.data.WidgetSnapshotDefinition
import app.readylytics.health.feature.widget.ui.StrainRecoveryWidget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrainRecoveryWidgetReceiverTest {
    @Test
    fun glanceAppWidget_isInstanceOfStrainRecoveryWidget() {
        val receiver = StrainRecoveryWidgetReceiver()
        assertTrue(receiver.glanceAppWidget is StrainRecoveryWidget)
        assertEquals(WidgetSnapshotDefinition, receiver.glanceAppWidget.stateDefinition)
    }
}
