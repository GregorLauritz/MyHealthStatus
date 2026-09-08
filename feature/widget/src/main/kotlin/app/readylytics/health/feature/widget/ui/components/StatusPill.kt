package app.readylytics.health.feature.widget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

@Composable
@GlanceComposable
fun StatusPill(
    text: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    Box(
        modifier =
            GlanceModifier
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(12.dp)
                .padding(horizontal = 8.dp, vertical = 3.dp)
                .then(modifier),
        contentAlignment = Alignment.Center,
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
