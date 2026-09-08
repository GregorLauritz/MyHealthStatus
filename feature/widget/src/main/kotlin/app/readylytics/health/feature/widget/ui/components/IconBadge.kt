package app.readylytics.health.feature.widget.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.size

@Composable
@GlanceComposable
fun IconBadge(
    @DrawableRes iconResId: Int,
    contentDescription: String?,
    modifier: GlanceModifier = GlanceModifier,
    size: Dp = 28.dp,
    iconSize: Dp = 16.dp,
) {
    Box(
        modifier =
            GlanceModifier
                .size(size)
                .background(GlanceTheme.colors.secondaryContainer)
                .cornerRadius(size / 2)
                .then(modifier),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            provider = ImageProvider(iconResId),
            contentDescription = contentDescription,
            modifier = GlanceModifier.size(iconSize),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer),
        )
    }
}
