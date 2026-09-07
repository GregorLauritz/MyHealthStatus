package app.readylytics.health.feature.widget.navigation

import android.content.ComponentName
import android.content.Context
import android.content.Intent

object WidgetDeepLinkHandler {
    const val EXTRA_TARGET_TAB = "app.readylytics.health.TARGET_TAB"

    const val TAB_DASHBOARD = "dashboard"
    const val TAB_SLEEP = "sleep"
    const val TAB_VITALS = "vitals"
    const val TAB_WORKOUTS = "workouts"

    private const val MAIN_ACTIVITY_CLASS = "app.readylytics.health.MainActivity"

    fun createTabIntent(
        context: Context,
        targetTab: String,
    ): Intent =
        Intent().apply {
            component = ComponentName(context.packageName, MAIN_ACTIVITY_CLASS)
            `package` = context.packageName
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TARGET_TAB, targetTab)
        }
}
