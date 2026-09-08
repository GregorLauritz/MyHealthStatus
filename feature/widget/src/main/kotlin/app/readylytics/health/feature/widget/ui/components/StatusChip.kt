package app.readylytics.health.feature.widget.ui.components

import androidx.compose.runtime.Composable
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier

@Composable
@GlanceComposable
fun StatusChip(
    text: String,
    modifier: GlanceModifier = GlanceModifier,
) {
    StatusPill(text = text, modifier = modifier)
}
