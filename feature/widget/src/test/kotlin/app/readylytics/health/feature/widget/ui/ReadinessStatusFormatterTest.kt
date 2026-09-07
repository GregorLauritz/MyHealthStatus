package app.readylytics.health.feature.widget.ui

import android.content.Context
import app.readylytics.health.feature.widget.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReadinessStatusFormatterTest {
    @Test
    fun resolveStringRes_mapsKnownCategoriesCorrectly() {
        assertEquals(R.string.widget_status_peak, ReadinessStatusFormatter.resolveStringRes("Peak"))
        assertEquals(R.string.widget_status_peak, ReadinessStatusFormatter.resolveStringRes("peak"))
        assertEquals(R.string.widget_status_maintain, ReadinessStatusFormatter.resolveStringRes("Maintain"))
        assertEquals(R.string.widget_status_caution, ReadinessStatusFormatter.resolveStringRes("Caution"))
        assertEquals(R.string.widget_status_high_fatigue, ReadinessStatusFormatter.resolveStringRes("High Fatigue"))
        assertEquals(R.string.widget_status_optimal, ReadinessStatusFormatter.resolveStringRes("Optimal"))
        assertEquals(R.string.widget_status_good, ReadinessStatusFormatter.resolveStringRes("Good"))
        assertEquals(R.string.widget_status_fair, ReadinessStatusFormatter.resolveStringRes("Fair"))
        assertEquals(R.string.widget_status_low, ReadinessStatusFormatter.resolveStringRes("Low"))
    }

    @Test
    fun resolveStringRes_returnsNullForUnknownOrNull() {
        assertNull(ReadinessStatusFormatter.resolveStringRes("Unknown"))
        assertNull(ReadinessStatusFormatter.resolveStringRes(null))
    }

    @Test
    fun format_resolvesContextStringForKnownCategory() {
        val context = mockk<Context>()
        every { context.getString(R.string.widget_status_optimal) } returns "Optimal"

        val formatted = ReadinessStatusFormatter.format(context, "Optimal")
        assertEquals("Optimal", formatted)
    }

    @Test
    fun format_returnsRawStringForUnknownCategory() {
        val context = mockk<Context>()
        val formatted = ReadinessStatusFormatter.format(context, "CustomCategory")
        assertEquals("CustomCategory", formatted)
    }

    @Test
    fun format_returnsNullForNullCategory() {
        val context = mockk<Context>()
        val formatted = ReadinessStatusFormatter.format(context, null)
        assertNull(formatted)
    }
}
