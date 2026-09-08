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
import androidx.glance.layout.padding
import androidx.glance.layout.width
import app.readylytics.health.feature.widget.ui.WidgetLayoutSpec

@Composable
@GlanceComposable
fun SubtleDivider(
    modifier: GlanceModifier = GlanceModifier,
    isVertical: Boolean = true,
    verticalInset: androidx.compose.ui.unit.Dp = if (isVertical) WidgetLayoutSpec.dividerInset else 0.dp,
) {
    Spacer(
        modifier =
            if (isVertical) {
                GlanceModifier
                    .width(WidgetLayoutSpec.dividerThickness)
                    .fillMaxHeight()
                    .padding(vertical = verticalInset)
                    .then(modifier)
                    .background(GlanceTheme.colors.outline)
            } else {
                GlanceModifier
                    .height(WidgetLayoutSpec.dividerThickness)
                    .fillMaxWidth()
                    .then(modifier)
                    .background(GlanceTheme.colors.outline)
            },
    )
}
