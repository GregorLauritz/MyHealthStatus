package app.readylytics.health.ui.navigation

import android.content.Context
import android.content.Intent
import app.readylytics.health.feature.widget.navigation.WidgetDeepLinkHandler
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TabDestinationDeepLinkTest {
    @Test
    fun fromTabName_resolvesValidTabs() {
        assertEquals(TabDestination.Dashboard, TabDestination.fromTabName(WidgetDeepLinkHandler.TAB_DASHBOARD))
        assertEquals(TabDestination.Sleep, TabDestination.fromTabName(WidgetDeepLinkHandler.TAB_SLEEP))
        assertEquals(TabDestination.Vitals, TabDestination.fromTabName(WidgetDeepLinkHandler.TAB_VITALS))
        assertEquals(TabDestination.Workouts, TabDestination.fromTabName(WidgetDeepLinkHandler.TAB_WORKOUTS))
    }

    @Test
    fun fromTabName_returnsNullForInvalidOrUnsupportedTabs() {
        assertNull(TabDestination.fromTabName("settings"))
        assertNull(TabDestination.fromTabName("insights"))
        assertNull(TabDestination.fromTabName("about"))
        assertNull(TabDestination.fromTabName(""))
        assertNull(TabDestination.fromTabName("   "))
        assertNull(TabDestination.fromTabName("unknown_tab"))
        assertNull(TabDestination.fromTabName(null))
    }

    @Test
    fun fromIntent_resolvesTargetTabExtra() {
        val intent = Intent().putExtra(WidgetDeepLinkHandler.EXTRA_TARGET_TAB, WidgetDeepLinkHandler.TAB_SLEEP)
        assertEquals(TabDestination.Sleep, TabDestination.fromIntent(intent))
    }

    @Test
    fun fromIntent_returnsNullWhenExtraMissingOrNullIntent() {
        assertNull(TabDestination.fromIntent(null))
        assertNull(TabDestination.fromIntent(Intent()))
        val intentWithInvalidExtra = Intent().putExtra(WidgetDeepLinkHandler.EXTRA_TARGET_TAB, "non_existent")
        assertNull(TabDestination.fromIntent(intentWithInvalidExtra))
    }

    @Test
    fun fromIntent_roundTripWithWidgetDeepLinkHandler() {
        val context = mockk<Context>()
        every { context.packageName } returns "app.readylytics.health"

        val dashboardIntent = WidgetDeepLinkHandler.createTabIntent(context, WidgetDeepLinkHandler.TAB_DASHBOARD)
        assertEquals(TabDestination.Dashboard, TabDestination.fromIntent(dashboardIntent))

        val sleepIntent = WidgetDeepLinkHandler.createTabIntent(context, WidgetDeepLinkHandler.TAB_SLEEP)
        assertEquals(TabDestination.Sleep, TabDestination.fromIntent(sleepIntent))

        val vitalsIntent = WidgetDeepLinkHandler.createTabIntent(context, WidgetDeepLinkHandler.TAB_VITALS)
        assertEquals(TabDestination.Vitals, TabDestination.fromIntent(vitalsIntent))

        val workoutsIntent = WidgetDeepLinkHandler.createTabIntent(context, WidgetDeepLinkHandler.TAB_WORKOUTS)
        assertEquals(TabDestination.Workouts, TabDestination.fromIntent(workoutsIntent))
    }
}
