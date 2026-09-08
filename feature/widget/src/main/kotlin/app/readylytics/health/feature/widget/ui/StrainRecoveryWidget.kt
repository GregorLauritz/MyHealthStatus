package app.readylytics.health.feature.widget.ui

import android.content.Context
import androidx.annotation.DrawableRes
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
import androidx.glance.layout.fillMaxHeight
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
import java.util.Locale

class StrainRecoveryWidget : GlanceAppWidget() {
    override val stateDefinition = WidgetSnapshotDefinition

    override val sizeMode =
        SizeMode.Responsive(
            setOf(
                DpSize(250.dp, 110.dp), // Compact
                DpSize(280.dp, 120.dp), // Standard
            ),
        )

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
        val layout =
            WidgetLayoutSpec.strain(
                widthDp =
                    LocalSize.current.width.value
                        .toInt(),
                heightDp =
                    LocalSize.current.height.value
                        .toInt(),
            )

        WidgetSurface(
            modifier =
                GlanceModifier.padding(
                    horizontal = WidgetLayoutSpec.standardOuterHorizontalPadding,
                    vertical = WidgetLayoutSpec.standardOuterVerticalPadding,
                ),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReadinessColumn(
                    context = context,
                    snapshot = snapshot,
                    layout = layout,
                    modifier = GlanceModifier.defaultWeight(),
                )

                SubtleDivider(isVertical = true)

                SleepColumn(
                    context = context,
                    snapshot = snapshot,
                    layout = layout,
                    modifier = GlanceModifier.defaultWeight(),
                )

                SubtleDivider(isVertical = true)

                StrainColumn(
                    context = context,
                    snapshot = snapshot,
                    layout = layout,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun ColumnHeader(
        @DrawableRes iconResId: Int,
        title: String,
        stack: Boolean,
    ) {
        if (stack) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconBadge(iconResId = iconResId, contentDescription = title)
                Spacer(modifier = GlanceModifier.height(WidgetLayoutSpec.iconToLabelSpacing))
                HeaderText(title)
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(iconResId = iconResId, contentDescription = title)
                Spacer(modifier = GlanceModifier.size(WidgetLayoutSpec.iconToLabelSpacing))
                HeaderText(title)
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun HeaderText(title: String) {
        Text(
            text = title,
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            maxLines = 1,
        )
    }

    @Composable
    @GlanceComposable
    private fun ReadinessColumn(
        context: Context,
        snapshot: WidgetSnapshot,
        layout: WidgetLayoutSpec.Strain,
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
                    .padding(horizontal = WidgetLayoutSpec.compactCellHorizontalPadding)
                    .clickable(actionStartActivity(dashboardIntent)),
        ) {
            ColumnHeader(
                iconResId = R.drawable.ic_widget_sparkle,
                title = context.getString(R.string.widget_readiness_title),
                stack = layout.stackHeader,
            )

            val readinessScoreText =
                snapshot.readinessScore?.toString()
                    ?: context.getString(R.string.widget_placeholder_value)
            Text(
                text = readinessScoreText,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.primary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )

            Spacer(modifier = GlanceModifier.defaultWeight())
            ReadinessStatus(context, snapshot, compact = layout.stackHeader)
        }
    }

    @Composable
    @GlanceComposable
    private fun ReadinessStatus(
        context: Context,
        snapshot: WidgetSnapshot,
        compact: Boolean,
    ) {
        when {
            snapshot.isCalibrating ->
                StatusPill(
                    text = context.getString(R.string.widget_calibrating_format, snapshot.calibrationDays),
                    compact = compact,
                )
            snapshot.readinessCategory != null -> {
                ReadinessStatusFormatter.format(context, snapshot.readinessCategory)?.let { categoryLabel ->
                    StatusPill(text = categoryLabel, compact = compact)
                }
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun SleepColumn(
        context: Context,
        snapshot: WidgetSnapshot,
        layout: WidgetLayoutSpec.Strain,
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
                    .padding(horizontal = WidgetLayoutSpec.compactCellHorizontalPadding)
                    .clickable(actionStartActivity(sleepIntent)),
        ) {
            ColumnHeader(
                iconResId = R.drawable.ic_widget_moon,
                title = context.getString(R.string.widget_sleep_title),
                stack = layout.stackHeader,
            )

            val sleepScoreText =
                snapshot.sleepScore?.toString()
                    ?: context.getString(R.string.widget_placeholder_value)
            Text(
                text = sleepScoreText,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                    ),
            )

            Spacer(modifier = GlanceModifier.defaultWeight())
            SleepDetails(context, snapshot, layout)
        }
    }

    @Composable
    @GlanceComposable
    private fun SleepDetails(
        context: Context,
        snapshot: WidgetSnapshot,
        layout: WidgetLayoutSpec.Strain,
    ) {
        Text(
            text =
                snapshot.sleepDurationFormatted
                    ?: context.getString(R.string.widget_awaiting_sync),
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                ),
        )
        if (layout.showSleepBreakdown && snapshot.deepSleepPercent != null && snapshot.remSleepPercent != null) {
            Text(
                text =
                    context.getString(
                        R.string.widget_deep_rem_format,
                        snapshot.deepSleepPercent,
                        snapshot.remSleepPercent,
                    ),
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
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
        layout: WidgetLayoutSpec.Strain,
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
                    .padding(horizontal = WidgetLayoutSpec.compactCellHorizontalPadding)
                    .clickable(actionStartActivity(workoutsIntent)),
        ) {
            ColumnHeader(
                iconResId = R.drawable.ic_widget_bolt,
                title = context.getString(R.string.widget_strain_title),
                stack = layout.stackHeader,
            )

            val strainScoreText =
                snapshot.strainScore?.let { String.format(Locale.US, "%.1f", it) }
                    ?: context.getString(R.string.widget_placeholder_value)
            Text(
                text = strainScoreText,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.tertiary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                maxLines = 1,
            )

            Spacer(modifier = GlanceModifier.defaultWeight())
            if (snapshot.stepCountFormatted != null) {
                StrainSteps(context, snapshot.stepCountFormatted)
            }
        }
    }

    @Composable
    @GlanceComposable
    private fun StrainSteps(
        context: Context,
        stepCountFormatted: String,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_steps),
                contentDescription = null,
                modifier = GlanceModifier.size(12.dp),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurfaceVariant),
            )
            Spacer(modifier = GlanceModifier.size(4.dp))
            Text(
                text =
                    context.getString(
                        R.string.widget_steps_format,
                        stepCountFormatted,
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
