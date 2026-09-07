package app.readylytics.health.feature.widget.ui.theme

import androidx.compose.runtime.Composable
import androidx.glance.GlanceComposable
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProviders
import androidx.glance.unit.ColorProvider

val ColorProviders.surfaceContainer: ColorProvider
    get() = surfaceVariant

val ColorProviders.surfaceContainerLow: ColorProvider
    get() = widgetBackground

@Composable
@GlanceComposable
fun WidgetGlanceTheme(content: @Composable () -> Unit) {
    GlanceTheme(content = content)
}
