package app.readylytics.health.feature.sleep

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepChartLabelLayoutTest {
    @Test
    fun resolveNonOverlappingLabelsByBounds_allLabelsFit_returnsAllIndices() {
        val result =
            resolveNonOverlappingLabelsByBounds(
                lefts = listOf(0f, 125f, 250f),
                widthsPx = listOf(50, 50, 50),
                spacingPx = 10f,
            )

        assertEquals(listOf(0, 1, 2), result)
    }

    @Test
    fun resolveNonOverlappingLabelsByBounds_middleOverlaps_skipsMiddle() {
        val result =
            resolveNonOverlappingLabelsByBounds(
                lefts = listOf(0f, 30f, 60f),
                widthsPx = listOf(50, 50, 50),
                spacingPx = 10f,
            )

        assertEquals(listOf(0, 2), result)
    }

    @Test
    fun resolveNonOverlappingLabelsByBounds_tailLabelCollidesWithPriorTick_dropsPriorTick() {
        // Regression for the sleep HR/HRV chart overlap: a session-end label ("6:02 AM") landing
        // close enough to the prior hour tick ("5:00 AM") that their clamped bounds collide. The
        // session-end label (always the last entry) must win -- the colliding hour tick is dropped
        // instead of being drawn on top of it.
        val result =
            resolveNonOverlappingLabelsByBounds(
                lefts = listOf(0f, 90f, 180f, 200f),
                widthsPx = listOf(40, 40, 40, 40),
                spacingPx = 8f,
            )

        assertEquals(listOf(0, 1, 3), result)
    }

    @Test
    fun resolveNonOverlappingLabelsByBounds_empty_returnsEmpty() {
        val result = resolveNonOverlappingLabelsByBounds(lefts = emptyList(), widthsPx = emptyList(), spacingPx = 8f)

        assertEquals(emptyList<Int>(), result)
    }
}
