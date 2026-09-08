package app.readylytics.health.feature.widget.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.readylytics.health.feature.widget.R
import app.readylytics.health.feature.widget.data.WidgetSnapshot
import app.readylytics.health.feature.widget.ui.components.IconBadge
import app.readylytics.health.feature.widget.ui.components.SubtleDivider

@Composable
@GlanceComposable
internal fun CompactRecoveryContent(
    context: Context,
    snapshot: WidgetSnapshot,
    dashboardIntent: Intent,
    sleepIntent: Intent,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactRecoveryMetric(
            iconResId = R.drawable.ic_widget_sparkle,
            title = context.getString(R.string.widget_readiness_title),
            value = snapshot.readinessScore?.toString() ?: context.getString(R.string.widget_placeholder_value),
            onClick = actionStartActivity(dashboardIntent),
            modifier = GlanceModifier.defaultWeight(),
        )
        SubtleDivider(isVertical = true)
        CompactRecoveryMetric(
            iconResId = R.drawable.ic_widget_moon,
            title = context.getString(R.string.widget_sleep_title),
            value = snapshot.sleepScore?.toString() ?: context.getString(R.string.widget_placeholder_value),
            supportingText = snapshot.sleepDurationFormatted ?: context.getString(R.string.widget_awaiting_sync),
            onClick = actionStartActivity(sleepIntent),
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}

@Composable
@GlanceComposable
private fun CompactRecoveryMetric(
    iconResId: Int,
    title: String,
    value: String,
    supportingText: String? = null,
    onClick: Action,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(
        modifier = modifier.clickable(onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(iconResId = iconResId, contentDescription = title)
        Spacer(modifier = GlanceModifier.height(WidgetLayoutSpec.iconToLabelSpacing))
        Text(
            text = title,
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
                    color = GlanceTheme.colors.primary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                ),
            maxLines = 1,
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                maxLines = 1,
            )
        }
    }
}
