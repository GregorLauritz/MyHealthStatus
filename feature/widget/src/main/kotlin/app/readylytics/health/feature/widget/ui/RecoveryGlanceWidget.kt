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
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
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
        val layout = WidgetLayoutSpec.recovery(LocalSize.current)

        WidgetSurface(
            modifier =
                GlanceModifier.padding(
                    horizontal =
                        if (layout.useCompactColumns) {
                            WidgetLayoutSpec.compactStripHorizontalPadding
                        } else {
                            WidgetLayoutSpec.standardOuterHorizontalPadding
                        },
                    vertical =
                        if (layout.useCompactColumns) {
                            WidgetLayoutSpec.compactStripVerticalPadding
                        } else {
                            WidgetLayoutSpec.standardOuterVerticalPadding
                        },
                ),
        ) {
            if (layout.useCompactColumns) {
                CompactRecoveryContent(
                    context = context,
                    snapshot = snapshot,
                    dashboardIntent = dashboardIntent,
                    sleepIntent = sleepIntent,
                )
            } else {
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                ) {
                    ReadinessSection(
                        context = context,
                        snapshot = snapshot,
                        layout = layout,
                        onClick = actionStartActivity(dashboardIntent),
                    )

                    SubtleDivider(
                        isVertical = false,
                        modifier = GlanceModifier.padding(vertical = WidgetLayoutSpec.sectionSpacing),
                    )

                    SleepSection(
                        context = context,
                        snapshot = snapshot,
                        layout = layout,
                        onClick = actionStartActivity(sleepIntent),
                    )
                }
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun ReadinessSection(
        context: Context,
        snapshot: WidgetSnapshot,
        layout: WidgetLayoutSpec.Recovery,
        onClick: Action,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth().clickable(onClick),
        ) {
            ReadinessHeaderRow(context, snapshot, layout)

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
                maxLines = 1,
            )

            val secondaryRecoveryMetric =
                if (layout.showHrvSupport) {
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
                    maxLines = 1,
                )
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun ReadinessHeaderRow(
        context: Context,
        snapshot: WidgetSnapshot,
        layout: WidgetLayoutSpec.Recovery,
    ) {
        if (layout.reflowStatusChip) {
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                RecoveryHeaderContent(
                    iconResId = R.drawable.ic_widget_sparkle,
                    title = context.getString(R.string.widget_readiness_title),
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                ReadinessStatusBadge(
                    context = context,
                    snapshot = snapshot,
                    modifier =
                        GlanceModifier.padding(
                            start =
                                WidgetLayoutSpec.iconBadgeSize + WidgetLayoutSpec.iconToLabelSpacing,
                        ),
                    compact = true,
                )
            }
        } else {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RecoveryHeaderContent(
                    iconResId = R.drawable.ic_widget_sparkle,
                    title = context.getString(R.string.widget_readiness_title),
                )
                Spacer(modifier = GlanceModifier.defaultWeight())
                if (layout.showStatusChip) {
                    ReadinessStatusBadge(context, snapshot)
                }
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun RecoveryHeaderContent(
        iconResId: Int,
        title: String,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(iconResId = iconResId, contentDescription = title)
            Text(
                text = title,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                modifier = GlanceModifier.padding(start = WidgetLayoutSpec.iconToLabelSpacing),
                maxLines = 1,
            )
        }
    }

    @Composable
    @GlanceComposable
    private fun SleepSection(
        context: Context,
        snapshot: WidgetSnapshot,
        layout: WidgetLayoutSpec.Recovery,
        onClick: Action,
    ) {
        Column(
            modifier = GlanceModifier.fillMaxWidth().clickable(onClick),
        ) {
            SleepHeaderRow(context)

            Spacer(modifier = GlanceModifier.height(4.dp))

            SleepMetricsRow(context, snapshot, layout)
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
            )
            Text(
                text = context.getString(R.string.widget_sleep_title),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                modifier = GlanceModifier.padding(start = WidgetLayoutSpec.iconToLabelSpacing),
                maxLines = 1,
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
        layout: WidgetLayoutSpec.Recovery,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val sleepScoreText =
                snapshot.sleepScore?.toString()
                    ?: context.getString(R.string.widget_placeholder_value)
            Box(
                modifier = GlanceModifier.width(WidgetLayoutSpec.metricScoreColumnWidth),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = sleepScoreText,
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    maxLines = 1,
                )
            }

            Spacer(modifier = GlanceModifier.width(WidgetLayoutSpec.metricDividerGap))
            SubtleDivider(
                isVertical = true,
                modifier = GlanceModifier.height(WidgetLayoutSpec.metricDividerHeight),
                verticalInset = 0.dp,
            )
            Spacer(modifier = GlanceModifier.width(WidgetLayoutSpec.metricDividerGap))

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
                    maxLines = 1,
                )
                if (layout.showTotalSleep) {
                    Text(
                        text = context.getString(R.string.widget_total_sleep_title),
                        style =
                            TextStyle(
                                color = GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 10.sp,
                            ),
                        maxLines = 1,
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
        modifier: GlanceModifier = GlanceModifier,
        compact: Boolean = false,
    ) {
        when {
            snapshot.isCalibrating ->
                StatusPill(
                    text = context.getString(R.string.widget_calibrating_format, snapshot.calibrationDays),
                    modifier = modifier,
                    compact = compact,
                )
            snapshot.readinessCategory != null -> {
                ReadinessStatusFormatter.format(context, snapshot.readinessCategory)?.let { categoryLabel ->
                    StatusPill(text = categoryLabel, modifier = modifier, compact = compact)
                }
            }
        }
    }
}
