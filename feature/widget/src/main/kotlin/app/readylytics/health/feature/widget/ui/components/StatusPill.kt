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
    compact: Boolean = false,
) {
    val horizontalPadding = if (compact) 6.dp else 8.dp
    val textSize = if (compact) 10.sp else 11.sp
    Box(
        modifier =
            GlanceModifier
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(12.dp)
                .padding(horizontal = horizontalPadding, vertical = 3.dp)
                .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style =
                TextStyle(
                    color = GlanceTheme.colors.onSecondaryContainer,
                    fontSize = textSize,
                    fontWeight = FontWeight.Medium,
                ),
            maxLines = 1,
        )
    }
}
