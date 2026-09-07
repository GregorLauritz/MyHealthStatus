package app.readylytics.health.feature.widget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.readylytics.health.feature.widget.ui.theme.surfaceContainer

@Composable
@GlanceComposable
fun MetricPill(
    label: String,
    value: String,
    modifier: GlanceModifier = GlanceModifier,
    subtitle: String? = null,
) {
    Column(
        modifier =
            modifier
                .background(GlanceTheme.colors.surfaceContainer)
                .cornerRadius(12.dp)
                .padding(8.dp),
    ) {
        Text(
            text = label,
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
        )
        Text(
            text = value,
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                ),
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                style =
                    TextStyle(
                        color = GlanceTheme.colors.outline,
                        fontSize = 10.sp,
                    ),
            )
        }
    }
}
