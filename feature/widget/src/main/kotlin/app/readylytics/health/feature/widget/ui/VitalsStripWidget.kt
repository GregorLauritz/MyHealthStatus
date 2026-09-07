package app.readylytics.health.feature.widget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.readylytics.health.feature.widget.R
import app.readylytics.health.feature.widget.data.WidgetSnapshot
import app.readylytics.health.feature.widget.data.WidgetSnapshotDefinition
import app.readylytics.health.feature.widget.navigation.WidgetDeepLinkHandler
import app.readylytics.health.feature.widget.ui.theme.WidgetGlanceTheme
import app.readylytics.health.feature.widget.ui.theme.surfaceContainer
import app.readylytics.health.feature.widget.ui.theme.surfaceContainerLow

class VitalsStripWidget : GlanceAppWidget() {
    override val stateDefinition = WidgetSnapshotDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            WidgetGlanceTheme {
                val snapshot = currentState<WidgetSnapshot>()
                VitalsStripContent(context, snapshot)
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun VitalsStripContent(
        context: Context,
        snapshot: WidgetSnapshot,
    ) {
        val vitalsIntent =
            WidgetDeepLinkHandler.createTabIntent(
                context = context,
                targetTab = WidgetDeepLinkHandler.TAB_VITALS,
            )
        val workoutsIntent =
            WidgetDeepLinkHandler.createTabIntent(
                context = context,
                targetTab = WidgetDeepLinkHandler.TAB_WORKOUTS,
            )

        Row(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surfaceContainerLow)
                    .cornerRadius(16.dp)
                    .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Cell 1: RHR
            VitalsCell(
                label = context.getString(R.string.widget_rhr_title),
                value =
                    snapshot.restingHeartRate?.let { context.getString(R.string.widget_bpm_format, it) }
                        ?: context.getString(R.string.widget_placeholder_value),
                delta = snapshot.rhrDeltaFormatted,
                onClick = actionStartActivity(vitalsIntent),
                modifier = GlanceModifier.defaultWeight(),
            )

            Spacer(modifier = GlanceModifier.width(4.dp))

            // Cell 2: HRV
            VitalsCell(
                label = context.getString(R.string.widget_hrv_title),
                value =
                    snapshot.nocturnalHrv?.let { context.getString(R.string.widget_ms_format, it) }
                        ?: context.getString(R.string.widget_placeholder_value),
                delta = snapshot.hrvDeltaFormatted,
                onClick = actionStartActivity(vitalsIntent),
                modifier = GlanceModifier.defaultWeight(),
            )

            Spacer(modifier = GlanceModifier.width(4.dp))

            // Cell 3: SpO2
            VitalsCell(
                label = context.getString(R.string.widget_spo2_title),
                value = snapshot.avgSpo2Formatted ?: context.getString(R.string.widget_placeholder_value),
                delta = snapshot.skinTempDeltaFormatted,
                onClick = actionStartActivity(vitalsIntent),
                modifier = GlanceModifier.defaultWeight(),
            )

            Spacer(modifier = GlanceModifier.width(4.dp))

            // Cell 4: Steps
            VitalsCell(
                label = context.getString(R.string.widget_steps_title),
                value = snapshot.stepCountFormatted ?: context.getString(R.string.widget_placeholder_value),
                delta = null,
                onClick = actionStartActivity(workoutsIntent),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }

    @Composable
    @GlanceComposable
    private fun VitalsCell(
        label: String,
        value: String,
        delta: String?,
        onClick: Action,
        modifier: GlanceModifier = GlanceModifier,
    ) {
        Column(
            modifier =
                modifier
                    .fillMaxHeight()
                    .background(GlanceTheme.colors.surfaceContainer)
                    .cornerRadius(10.dp)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .clickable(onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            Text(
                text = value,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            if (delta != null) {
                Text(
                    text = delta,
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.outline,
                            fontSize = 9.sp,
                        ),
                )
            }
        }
    }
}
