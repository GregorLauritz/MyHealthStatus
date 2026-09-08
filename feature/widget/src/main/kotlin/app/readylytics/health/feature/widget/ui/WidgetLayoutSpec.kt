package app.readylytics.health.feature.widget.ui

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

object WidgetLayoutSpec {
    val standardOuterHorizontalPadding = 12.dp
    val standardOuterVerticalPadding = 8.dp
    val compactStripHorizontalPadding = 8.dp
    val compactStripVerticalPadding = 4.dp
    val iconBadgeSize = 24.dp
    val iconSize = 14.dp
    val iconToLabelSpacing = 4.dp
    val compactCellHorizontalPadding = 2.dp
    val dividerThickness = 1.dp
    val dividerInset = 4.dp
    val sectionSpacing = 8.dp
    val compactSectionSpacing = 4.dp
    val metricScoreColumnWidth = 32.dp
    val metricDividerGap = 8.dp
    val metricDividerHeight = 24.dp

    data class Vitals(
        val fillAvailableWidth: Boolean,
        val keepPrimaryValueOnOneLine: Boolean,
        val showDelta: Boolean,
    )

    data class Recovery(
        val useCompactColumns: Boolean,
        val showStatusChip: Boolean,
        val reflowStatusChip: Boolean,
        val showHrvSupport: Boolean,
        val showTotalSleep: Boolean,
    )

    data class Strain(
        val fillAvailableWidth: Boolean,
        val stackHeader: Boolean,
        val showSleepBreakdown: Boolean,
    )

    fun vitals(
        widthDp: Int,
        heightDp: Int,
    ): Vitals =
        Vitals(
            fillAvailableWidth = true,
            keepPrimaryValueOnOneLine = widthDp >= 200,
            showDelta = heightDp >= 60,
        )

    fun recovery(
        widthDp: Int,
        heightDp: Int,
    ): Recovery =
        Recovery(
            useCompactColumns = widthDp < 140 || heightDp < 140,
            showStatusChip = widthDp >= 140 && heightDp >= 140,
            reflowStatusChip = widthDp < 140 && heightDp >= 140,
            showHrvSupport = heightDp >= 140,
            showTotalSleep = heightDp >= 140,
        )

    fun recovery(size: DpSize): Recovery =
        recovery(
            widthDp = size.width.value.toInt(),
            heightDp = size.height.value.toInt(),
        )

    fun strain(
        widthDp: Int,
        heightDp: Int,
    ): Strain =
        Strain(
            fillAvailableWidth = true,
            stackHeader = widthDp < 280 || heightDp < 120,
            showSleepBreakdown = widthDp >= 280 && heightDp >= 120,
        )
}
