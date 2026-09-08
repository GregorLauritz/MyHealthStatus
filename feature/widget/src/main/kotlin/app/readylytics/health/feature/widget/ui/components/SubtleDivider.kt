package app.readylytics.health.feature.widget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.background
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width

@Composable
@GlanceComposable
fun SubtleDivider(
    modifier: GlanceModifier = GlanceModifier,
    isVertical: Boolean = true,
) {
    Spacer(
        modifier =
            if (isVertical) {
                GlanceModifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(GlanceTheme.colors.outline)
                    .then(modifier)
            } else {
                GlanceModifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(GlanceTheme.colors.outline)
                    .then(modifier)
            },
    )
}
