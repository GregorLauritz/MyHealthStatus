package app.readylytics.health.ui.navigation

import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import app.readylytics.health.R
import app.readylytics.health.feature.widget.navigation.WidgetDeepLinkHandler
import kotlinx.serialization.Serializable
import app.readylytics.health.core.ui.R as CoreUiR

sealed interface TabDestination {
    val labelRes: Int
    val icon: ImageVector
    val selectedIcon: ImageVector

    @Serializable
    data object Dashboard : TabDestination {
        override val labelRes = R.string.tab_dashboard
        override val icon = Icons.Outlined.Dashboard
        override val selectedIcon = Icons.Filled.Dashboard
    }

    @Serializable
    data object Sleep : TabDestination {
        override val labelRes = R.string.tab_sleep
        override val icon = Icons.Outlined.Bedtime
        override val selectedIcon = Icons.Filled.Bedtime
    }

    @Serializable
    data object Vitals : TabDestination {
        override val labelRes = CoreUiR.string.tab_vitals
        override val icon = Icons.Outlined.MonitorHeart
        override val selectedIcon = Icons.Filled.MonitorHeart
    }

    @Serializable
    data object Workouts : TabDestination {
        override val labelRes = R.string.tab_workouts
        override val icon = Icons.Outlined.FitnessCenter
        override val selectedIcon = Icons.Filled.FitnessCenter
    }

    @Serializable
    data object Settings : TabDestination {
        override val labelRes = R.string.tab_settings
        override val icon = Icons.Outlined.Settings
        override val selectedIcon = Icons.Filled.Settings
    }

    companion object {
        val all = listOf(Dashboard, Sleep, Vitals, Workouts, Settings)

        fun fromTabName(name: String?): TabDestination? =
            when (name) {
                WidgetDeepLinkHandler.TAB_DASHBOARD -> Dashboard
                WidgetDeepLinkHandler.TAB_SLEEP -> Sleep
                WidgetDeepLinkHandler.TAB_VITALS -> Vitals
                WidgetDeepLinkHandler.TAB_WORKOUTS -> Workouts
                else -> null
            }

        fun fromIntent(intent: Intent?): TabDestination? =
            fromTabName(intent?.getStringExtra(WidgetDeepLinkHandler.EXTRA_TARGET_TAB))
    }
}
