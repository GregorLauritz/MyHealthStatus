package app.readylytics.health.feature.widget.navigation

import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WidgetDeepLinkHandlerTest {
    @Test
    fun createTabIntent_setsCorrectActionFlagsAndExtra() {
        val context = mockk<Context>()
        every { context.packageName } returns "app.readylytics.health"

        val intent = WidgetDeepLinkHandler.createTabIntent(context, WidgetDeepLinkHandler.TAB_DASHBOARD)

        assertEquals("app.readylytics.health", intent.`package`)
        assertEquals("dashboard", intent.getStringExtra(WidgetDeepLinkHandler.EXTRA_TARGET_TAB))
        val flags = intent.flags
        assertTrue((flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
        assertTrue((flags and Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0)
    }

    @Test
    fun createTabIntent_sleepTab() {
        val context = mockk<Context>()
        every { context.packageName } returns "app.readylytics.health"

        val intent = WidgetDeepLinkHandler.createTabIntent(context, WidgetDeepLinkHandler.TAB_SLEEP)

        assertEquals("app.readylytics.health", intent.`package`)
        assertEquals("sleep", intent.getStringExtra(WidgetDeepLinkHandler.EXTRA_TARGET_TAB))
    }
}
