package app.readylytics.health.feature.sleep

/**
 * Pure geometry: given each label's already-computed left edge and width (both in px, already
 * clamped to whatever bounds the caller draws within), returns the indices to keep so that no two
 * accepted labels' bounding boxes sit closer than [spacingPx]. Mirrors [resolveNonOverlappingLabels]'s
 * greedy strategy (keep first + last, then fill in the middle) but operates directly on rendered
 * bounds instead of re-deriving them from timestamps -- needed by SleepHrChart/SleepHrvChart, whose
 * label x-positions already reflect pinch-zoom/pan and can't be recomputed from a plain time fraction.
 */
internal fun resolveNonOverlappingLabelsByBounds(
    lefts: List<Float>,
    widthsPx: List<Int>,
    spacingPx: Float,
): List<Int> {
    if (lefts.isEmpty()) return emptyList()

    data class LabelBounds(val index: Int, val left: Float, val right: Float)

    fun overlaps(
        a: LabelBounds,
        b: LabelBounds,
    ) = a.left < b.right + spacingPx && b.left < a.right + spacingPx

    val allBounds = lefts.mapIndexed { i, left -> LabelBounds(i, left, left + widthsPx.getOrElse(i) { 0 }) }

    val accepted = mutableListOf<LabelBounds>()
    accepted.add(allBounds.first())

    if (allBounds.size > 1) {
        val last = allBounds.last()
        if (!overlaps(last, accepted.first())) {
            accepted.add(last)
        }
    }

    for (i in 1 until allBounds.size - 1) {
        val candidate = allBounds[i]
        if (accepted.none { overlaps(candidate, it) }) {
            accepted.add(candidate)
        }
    }

    return accepted.map { it.index }.sorted()
}
