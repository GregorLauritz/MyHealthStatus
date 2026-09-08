package app.readylytics.health.feature.widget.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetLayoutSpecTest {
    @Test
    fun vitalsLayout_preservesPrimaryValuesBeforeOptionalDeltas() {
        val compact = WidgetLayoutSpec.vitals(widthDp = 200, heightDp = 40)
        val standard = WidgetLayoutSpec.vitals(widthDp = 260, heightDp = 60)

        assertTrue(compact.fillAvailableWidth)
        assertTrue(compact.keepPrimaryValueOnOneLine)
        assertFalse(compact.showDelta)
        assertTrue(standard.showDelta)
    }

    @Test
    fun recoveryLayout_reflowsSecondaryContentAtCompactSize() {
        val compact = WidgetLayoutSpec.recovery(widthDp = 110, heightDp = 110)
        val standard = WidgetLayoutSpec.recovery(widthDp = 140, heightDp = 140)

        assertTrue(compact.useCompactColumns)
        assertFalse(compact.showStatusChip)
        assertFalse(compact.showHrvSupport)
        assertFalse(compact.showTotalSleep)
        assertTrue(standard.showHrvSupport)
        assertTrue(standard.showTotalSleep)
    }

    @Test
    fun strainLayout_keepsPrimaryColumnsAndDropsSupportingBreakdownFirst() {
        val compact = WidgetLayoutSpec.strain(widthDp = 250, heightDp = 110)
        val standard = WidgetLayoutSpec.strain(widthDp = 280, heightDp = 120)

        assertTrue(compact.fillAvailableWidth)
        assertTrue(compact.stackHeader)
        assertFalse(compact.showSleepBreakdown)
        assertTrue(standard.showSleepBreakdown)
    }
}
