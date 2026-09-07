package app.readylytics.health.feature.widget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

@Composable
@GlanceComposable
fun StatusChip(
    text: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        modifier =
            modifier
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(8.dp)
                .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                ),
        )
    }
}
