package app.readylytics.health.feature.widget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.readylytics.health.feature.widget.R
import app.readylytics.health.feature.widget.data.WidgetSnapshot
import app.readylytics.health.feature.widget.data.WidgetSnapshotDefinition
import app.readylytics.health.feature.widget.navigation.WidgetDeepLinkHandler
import app.readylytics.health.feature.widget.ui.components.StatusChip
import app.readylytics.health.feature.widget.ui.theme.WidgetGlanceTheme
import app.readylytics.health.feature.widget.ui.theme.surfaceContainerLow

class RecoveryGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = WidgetSnapshotDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            WidgetGlanceTheme {
                val snapshot = currentState<WidgetSnapshot>()
                RecoveryGlanceContent(context, snapshot)
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun RecoveryGlanceContent(
        context: Context,
        snapshot: WidgetSnapshot,
    ) {
        val launchIntent = WidgetDeepLinkHandler.createTabIntent(context, WidgetDeepLinkHandler.TAB_DASHBOARD)

        Column(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surfaceContainerLow)
                    .cornerRadius(16.dp)
                    .padding(12.dp)
                    .clickable(actionStartActivity(launchIntent)),
            verticalAlignment = Alignment.Top,
        ) {
            RecoveryHeaderRow(context, snapshot)

            Spacer(modifier = GlanceModifier.height(4.dp))

            val readinessText =
                snapshot.readinessScore?.toString()
                    ?: context.getString(R.string.widget_placeholder_value)

            Text(
                text = readinessText,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            SleepSummaryRow(context, snapshot)
        }
    }

    @Composable
    @GlanceComposable
    private fun RecoveryHeaderRow(
        context: Context,
        snapshot: WidgetSnapshot,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = context.getString(R.string.widget_readiness_title),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            when {
                snapshot.isCalibrating ->
                    StatusChip(
                        text = context.getString(R.string.widget_calibrating_format, snapshot.calibrationDays),
                    )
                snapshot.readinessCategory != null -> {
                    ReadinessStatusFormatter.format(context, snapshot.readinessCategory)?.let { categoryLabel ->
                        StatusChip(text = categoryLabel)
                    }
                }
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun SleepSummaryRow(
        context: Context,
        snapshot: WidgetSnapshot,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = context.getString(R.string.widget_sleep_title),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                )
                val sleepScoreText =
                    snapshot.sleepScore?.toString()
                        ?: context.getString(R.string.widget_placeholder_value)
                Text(
                    text = sleepScoreText,
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                )
            }

            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text =
                        snapshot.sleepDurationFormatted
                            ?: context.getString(R.string.widget_awaiting_sync),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                        ),
                )
            }
        }
    }
}
