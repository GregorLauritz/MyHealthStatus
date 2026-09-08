package app.readylytics.health.feature.widget.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
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
import androidx.glance.layout.fillMaxHeight
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
import app.readylytics.health.feature.widget.ui.components.IconBadge
import app.readylytics.health.feature.widget.ui.components.SubtleDivider
import app.readylytics.health.feature.widget.ui.components.WidgetSurface
import app.readylytics.health.feature.widget.ui.theme.WidgetGlanceTheme

class VitalsStripWidget : GlanceAppWidget() {
    override val stateDefinition = WidgetSnapshotDefinition

    override val sizeMode =
        SizeMode.Responsive(
            setOf(
                DpSize(200.dp, 40.dp), // Compact
                DpSize(260.dp, 60.dp), // Standard
            ),
        )

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
        val isCompact = LocalSize.current.height < 60.dp
        val layout =
            WidgetLayoutSpec.vitals(
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
                    horizontal =
                        if (isCompact) {
                            WidgetLayoutSpec.compactStripHorizontalPadding
                        } else {
                            WidgetLayoutSpec.standardOuterHorizontalPadding
                        },
                    vertical =
                        if (isCompact) {
                            WidgetLayoutSpec.compactStripVerticalPadding
                        } else {
                            WidgetLayoutSpec.standardOuterVerticalPadding
                        },
                ),
        ) {
            VitalsRow(
                context = context,
                snapshot = snapshot,
                isCompact = isCompact,
                showDelta = layout.showDelta,
                vitalsAction = actionStartActivity(vitalsIntent),
                workoutsAction = actionStartActivity(workoutsIntent),
            )
        }
    }

    @Composable
    @GlanceComposable
    private fun VitalsRow(
        context: Context,
        snapshot: WidgetSnapshot,
        isCompact: Boolean,
        showDelta: Boolean,
        vitalsAction: Action,
        workoutsAction: Action,
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1: RHR
            VitalsCell(
                iconRes = R.drawable.ic_widget_heart,
                label = context.getString(R.string.widget_rhr_title),
                value =
                    snapshot.restingHeartRate?.let { context.getString(R.string.widget_bpm_format, it) }
                        ?: context.getString(R.string.widget_placeholder_value),
                delta = if (showDelta) WidgetDeltaFormatter.formatRhrDelta(context, snapshot) else null,
                compact = isCompact,
                onClick = vitalsAction,
                modifier = GlanceModifier.defaultWeight(),
            )

            SubtleDivider(isVertical = true)

            // 2: HRV
            VitalsCell(
                iconRes = R.drawable.ic_widget_hrv,
                label = context.getString(R.string.widget_hrv_title),
                value =
                    snapshot.nocturnalHrv?.let { context.getString(R.string.widget_ms_format, it) }
                        ?: context.getString(R.string.widget_placeholder_value),
                delta = if (showDelta) WidgetDeltaFormatter.formatHrvDelta(context, snapshot) else null,
                compact = isCompact,
                onClick = vitalsAction,
                modifier = GlanceModifier.defaultWeight(),
            )

            SubtleDivider(isVertical = true)

            // 3: SpO2
            VitalsCell(
                iconRes = R.drawable.ic_widget_water_drop,
                label = context.getString(R.string.widget_spo2_title),
                value = snapshot.avgSpo2Formatted ?: context.getString(R.string.widget_placeholder_value),
                delta = null,
                compact = isCompact,
                onClick = vitalsAction,
                modifier = GlanceModifier.defaultWeight(),
            )

            SubtleDivider(isVertical = true)

            // 4: Steps
            VitalsCell(
                iconRes = R.drawable.ic_widget_steps,
                label = context.getString(R.string.widget_steps_title),
                value = snapshot.stepCountFormatted ?: context.getString(R.string.widget_placeholder_value),
                delta = null,
                compact = isCompact,
                onClick = workoutsAction,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }

    @Composable
    @GlanceComposable
    private fun VitalsCell(
        iconRes: Int,
        label: String,
        value: String,
        delta: String?,
        compact: Boolean,
        onClick: Action,
        modifier: GlanceModifier = GlanceModifier,
    ) {
        Column(
            modifier =
                modifier
                    .fillMaxHeight()
                    .padding(horizontal = WidgetLayoutSpec.compactCellHorizontalPadding)
                    .clickable(onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Top,
        ) {
            IconBadge(iconResId = iconRes, contentDescription = label)
            Spacer(modifier = GlanceModifier.height(WidgetLayoutSpec.iconToLabelSpacing))
            Text(
                text = label,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                maxLines = 1,
            )
            Text(
                text = value,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = if (compact) 12.sp else 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                maxLines = 1,
            )
            if (delta != null) {
                Text(
                    text = delta,
                    style =
                        TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 9.sp,
                        ),
                    maxLines = 1,
                )
            }
        }
    }
}
