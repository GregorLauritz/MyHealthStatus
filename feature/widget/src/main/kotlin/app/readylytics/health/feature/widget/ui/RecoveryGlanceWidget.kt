package app.readylytics.health.feature.widget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.readylytics.health.feature.widget.R
import app.readylytics.health.feature.widget.data.WidgetSnapshot
import app.readylytics.health.feature.widget.data.WidgetSnapshotDefinition
import app.readylytics.health.feature.widget.navigation.WidgetDeepLinkHandler
import app.readylytics.health.feature.widget.ui.components.IconBadge
import app.readylytics.health.feature.widget.ui.components.StatusPill
import app.readylytics.health.feature.widget.ui.components.SubtleDivider
import app.readylytics.health.feature.widget.ui.components.WidgetSurface
import app.readylytics.health.feature.widget.ui.theme.WidgetGlanceTheme

class RecoveryGlanceWidget : GlanceAppWidget() {
    override val stateDefinition = WidgetSnapshotDefinition

    override val sizeMode =
        SizeMode.Responsive(
            setOf(
                DpSize(110.dp, 110.dp), // Compact
                DpSize(140.dp, 140.dp), // Standard
            ),
        )

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
        val dashboardIntent = WidgetDeepLinkHandler.createTabIntent(context, WidgetDeepLinkHandler.TAB_DASHBOARD)
        val sleepIntent = WidgetDeepLinkHandler.createTabIntent(context, WidgetDeepLinkHandler.TAB_SLEEP)
        val isCompact = LocalSize.current.height < 140.dp

        WidgetSurface(modifier = GlanceModifier.padding(12.dp)) {
            Column(verticalAlignment = Alignment.Top) {
                ReadinessSection(
                    context = context,
                    snapshot = snapshot,
                    isCompact = isCompact,
                    onClick = actionStartActivity(dashboardIntent),
                )

                SubtleDivider(isVertical = false, modifier = GlanceModifier.padding(vertical = 8.dp))

                SleepSection(
                    context = context,
                    snapshot = snapshot,
                    isCompact = isCompact,
                    onClick = actionStartActivity(sleepIntent),
                )
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun ReadinessSection(
        context: Context,
        snapshot: WidgetSnapshot,
        isCompact: Boolean,
        onClick: Action,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth().clickable(onClick),
        ) {
            ReadinessHeaderRow(context, snapshot)

            Spacer(modifier = GlanceModifier.height(4.dp))

            val readinessScoreText =
                snapshot.readinessScore?.toString()
                    ?: context.getString(R.string.widget_placeholder_value)
            Text(
                text = readinessScoreText,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )

            val secondaryRecoveryMetric =
                if (!isCompact) {
                    WidgetDeltaFormatter.formatSecondaryRecoveryMetric(context, snapshot)
                } else {
                    null
                }
            if (secondaryRecoveryMetric != null) {
                Text(
                    text = secondaryRecoveryMetric,
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 11.sp,
                        ),
                )
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun ReadinessHeaderRow(
        context: Context,
        snapshot: WidgetSnapshot,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                iconResId = R.drawable.ic_widget_sparkle,
                contentDescription = context.getString(R.string.widget_readiness_title),
                size = 24.dp,
                iconSize = 14.dp,
            )
            Text(
                text = context.getString(R.string.widget_readiness_title),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                modifier = GlanceModifier.padding(start = 6.dp),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            ReadinessStatusBadge(context, snapshot)
        }
    }

    @Composable
    @GlanceComposable
    private fun SleepSection(
        context: Context,
        snapshot: WidgetSnapshot,
        isCompact: Boolean,
        onClick: Action,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth().clickable(onClick),
        ) {
            SleepHeaderRow(context)

            Spacer(modifier = GlanceModifier.height(4.dp))

            SleepMetricsRow(context, snapshot, isCompact)
        }
    }

    @Composable
    @GlanceComposable
    private fun SleepHeaderRow(context: Context) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(
                iconResId = R.drawable.ic_widget_moon,
                contentDescription = context.getString(R.string.widget_sleep_title),
                size = 24.dp,
                iconSize = 14.dp,
            )
            Text(
                text = context.getString(R.string.widget_sleep_title),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                modifier = GlanceModifier.padding(start = 6.dp),
            )
            Spacer(modifier = GlanceModifier.defaultWeight())
            Image(
                provider = ImageProvider(R.drawable.ic_widget_chevron_right),
                contentDescription = null,
                modifier = GlanceModifier.size(16.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            )
        }
    }

    @Composable
    @GlanceComposable
    private fun SleepMetricsRow(
        context: Context,
        snapshot: WidgetSnapshot,
        isCompact: Boolean,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val sleepScoreText =
                snapshot.sleepScore?.toString()
                    ?: context.getString(R.string.widget_placeholder_value)
            Text(
                text = sleepScoreText,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )

            SubtleDivider(
                isVertical = true,
                modifier = GlanceModifier.height(28.dp).padding(horizontal = 8.dp),
            )

            Column {
                Text(
                    text =
                        snapshot.sleepDurationFormatted
                            ?: context.getString(R.string.widget_awaiting_sync),
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                )
                if (!isCompact) {
                    Text(
                        text = context.getString(R.string.widget_total_sleep_title),
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 10.sp,
                            ),
                    )
                }
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun ReadinessStatusBadge(
        context: Context,
        snapshot: WidgetSnapshot,
    ) {
        when {
            snapshot.isCalibrating ->
                StatusPill(
                    text = context.getString(R.string.widget_calibrating_format, snapshot.calibrationDays),
                )
            snapshot.readinessCategory != null -> {
                ReadinessStatusFormatter.format(context, snapshot.readinessCategory)?.let { categoryLabel ->
                    StatusPill(text = categoryLabel)
                }
            }
        }
    }
}
