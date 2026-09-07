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
import app.readylytics.health.feature.widget.ui.components.StatusChip
import app.readylytics.health.feature.widget.ui.theme.WidgetGlanceTheme
import app.readylytics.health.feature.widget.ui.theme.surfaceContainer
import app.readylytics.health.feature.widget.ui.theme.surfaceContainerLow
import java.util.Locale

class StrainRecoveryWidget : GlanceAppWidget() {
    override val stateDefinition = WidgetSnapshotDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        provideContent {
            WidgetGlanceTheme {
                val snapshot = currentState<WidgetSnapshot>()
                StrainRecoveryContent(context, snapshot)
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun StrainRecoveryContent(
        context: Context,
        snapshot: WidgetSnapshot,
    ) {
        Row(
            modifier =
                GlanceModifier
                    .fillMaxSize()
                    .background(GlanceTheme.colors.surfaceContainerLow)
                    .cornerRadius(16.dp)
                    .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ReadinessColumn(
                context = context,
                snapshot = snapshot,
                modifier = GlanceModifier.defaultWeight(),
            )

            Spacer(modifier = GlanceModifier.width(6.dp))

            SleepColumn(
                context = context,
                snapshot = snapshot,
                modifier = GlanceModifier.defaultWeight(),
            )

            Spacer(modifier = GlanceModifier.width(6.dp))

            StrainColumn(
                context = context,
                snapshot = snapshot,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }

    @Composable
    @GlanceComposable
    private fun ReadinessColumn(
        context: Context,
        snapshot: WidgetSnapshot,
        modifier: GlanceModifier = GlanceModifier,
    ) {
        val dashboardIntent =
            WidgetDeepLinkHandler.createTabIntent(
                context = context,
                targetTab = WidgetDeepLinkHandler.TAB_DASHBOARD,
            )

        Column(
            modifier =
                modifier
                    .fillMaxHeight()
                    .background(GlanceTheme.colors.surfaceContainer)
                    .cornerRadius(12.dp)
                    .padding(8.dp)
                    .clickable(actionStartActivity(dashboardIntent)),
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
            val readinessScoreText =
                snapshot.readinessScore?.toString()
                    ?: context.getString(R.string.widget_placeholder_value)
            Text(
                text = readinessScoreText,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            ReadinessStatus(context, snapshot)
        }
    }

    @Composable
    @GlanceComposable
    private fun ReadinessStatus(
        context: Context,
        snapshot: WidgetSnapshot,
    ) {
        when {
            snapshot.isCalibrating ->
                StatusChip(
                    text =
                        context.getString(
                            R.string.widget_calibrating_format,
                            snapshot.calibrationDays,
                        ),
                )
            snapshot.readinessCategory != null -> StatusChip(text = snapshot.readinessCategory)
        }
    }

    @Composable
    @GlanceComposable
    private fun SleepColumn(
        context: Context,
        snapshot: WidgetSnapshot,
        modifier: GlanceModifier = GlanceModifier,
    ) {
        val sleepIntent =
            WidgetDeepLinkHandler.createTabIntent(
                context = context,
                targetTab = WidgetDeepLinkHandler.TAB_SLEEP,
            )

        Column(
            modifier =
                modifier
                    .fillMaxHeight()
                    .background(GlanceTheme.colors.surfaceContainer)
                    .cornerRadius(12.dp)
                    .padding(8.dp)
                    .clickable(actionStartActivity(sleepIntent)),
        ) {
            Text(
                text = context.getString(R.string.widget_sleep_title),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
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
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            SleepDetails(context, snapshot)
        }
    }

    @Composable
    @GlanceComposable
    private fun SleepDetails(
        context: Context,
        snapshot: WidgetSnapshot,
    ) {
        Text(
            text =
                snapshot.sleepDurationFormatted
                    ?: context.getString(R.string.widget_awaiting_sync),
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                ),
        )
        if (snapshot.deepSleepPercent != null && snapshot.remSleepPercent != null) {
            Text(
                text =
                    context.getString(
                        R.string.widget_deep_rem_format,
                        snapshot.deepSleepPercent,
                        snapshot.remSleepPercent,
                    ),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.outline,
                        fontSize = 10.sp,
                    ),
            )
        }
    }

    @Composable
    @GlanceComposable
    private fun StrainColumn(
        context: Context,
        snapshot: WidgetSnapshot,
        modifier: GlanceModifier = GlanceModifier,
    ) {
        val workoutsIntent =
            WidgetDeepLinkHandler.createTabIntent(
                context = context,
                targetTab = WidgetDeepLinkHandler.TAB_WORKOUTS,
            )

        Column(
            modifier =
                modifier
                    .fillMaxHeight()
                    .background(GlanceTheme.colors.surfaceContainer)
                    .cornerRadius(12.dp)
                    .padding(8.dp)
                    .clickable(actionStartActivity(workoutsIntent)),
        ) {
            Text(
                text = context.getString(R.string.widget_strain_title),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            val strainScoreText =
                snapshot.strainScore?.let { String.format(Locale.US, "%.1f", it) }
                    ?: context.getString(R.string.widget_placeholder_value)
            Text(
                text = strainScoreText,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.tertiary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            if (snapshot.stepCountFormatted != null) {
                Text(
                    text =
                        context.getString(
                            R.string.widget_steps_format,
                            snapshot.stepCountFormatted,
                        ),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 12.sp,
                        ),
                )
            }
        }
    }
}
