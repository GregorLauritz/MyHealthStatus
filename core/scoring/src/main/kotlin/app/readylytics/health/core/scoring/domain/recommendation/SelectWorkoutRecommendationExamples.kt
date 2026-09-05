package app.readylytics.health.core.scoring.domain.recommendation

import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationExample
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationState
import app.readylytics.health.core.model.domain.scoring.WorkoutLoadLevel

/**
 * Pure selection of up to three past workouts to show as concrete examples of what today's HRV
 * guidance looks like in practice.
 *
 * [select] performs no reads, clock access, Health Connect/Room lookups, or string localization:
 * `fromMs`/`throughMs` bound an already-resolved window (e.g. a 30-day retrospective ending at this
 * morning's anchor), and the caller is responsible for constructing that window. Given that window
 * and a set of already-classified candidate workouts:
 *
 * 1. **Eligibility.** A candidate is only considered if its `exerciseType` is non-blank, its
 *    `durationMinutes` is strictly greater than 15, it starts at or after `fromMs`, it ends at or
 *    before `throughMs` (excludes in-progress/future sessions), and its end is strictly after its
 *    start (excludes zero/negative-duration records).
 * 2. **Load match.** [WorkoutRecommendationState.EASY] only draws from [WorkoutLoadLevel.VERY_LIGHT]/
 *    [WorkoutLoadLevel.LIGHT] history; [WorkoutRecommendationState.HARDER] only draws from
 *    [WorkoutLoadLevel.MODERATE]/[WorkoutLoadLevel.HARD]/[WorkoutLoadLevel.VERY_HARD]. Every other
 *    state (REST and the unavailable states) has no allowed load levels, so it always yields no
 *    examples.
 * 3. **Dedup + ordering.** Candidates are sorted by end time (newest first), then start time
 *    (newest first), then `workoutId` (ascending) as a final deterministic tiebreaker so the result
 *    never depends on input order. Exactly one example is kept per distinct `exerciseType` — the
 *    first (i.e. newest) survivor of the sort — and at most three examples are returned.
 */
class SelectWorkoutRecommendationExamples {
    fun select(
        state: WorkoutRecommendationState,
        candidates: List<WorkoutRecommendationExample>,
        fromMs: Long,
        throughMs: Long,
    ): List<WorkoutRecommendationExample> {
        val allowed = allowedLoadLevels(state)
        if (allowed.isEmpty()) return emptyList()

        return candidates.asSequence()
            .filter { it.durationMinutes > MIN_ELIGIBLE_DURATION_MINUTES && it.exerciseType.isNotBlank() }
            .filter { it.startTimeMs >= fromMs && it.endTimeMs <= throughMs && it.endTimeMs > it.startTimeMs }
            .filter { it.finalLoad in allowed }
            .sortedWith(
                compareByDescending<WorkoutRecommendationExample> { it.endTimeMs }
                    .thenByDescending { it.startTimeMs }
                    .thenBy { it.workoutId },
            )
            .distinctBy { it.exerciseType }
            .take(MAX_EXAMPLES)
            .toList()
    }

    private fun allowedLoadLevels(state: WorkoutRecommendationState): Set<WorkoutLoadLevel> =
        when (state) {
            WorkoutRecommendationState.EASY -> setOf(WorkoutLoadLevel.VERY_LIGHT, WorkoutLoadLevel.LIGHT)
            WorkoutRecommendationState.HARDER ->
                setOf(WorkoutLoadLevel.MODERATE, WorkoutLoadLevel.HARD, WorkoutLoadLevel.VERY_HARD)
            WorkoutRecommendationState.REST,
            WorkoutRecommendationState.NO_SLEEP,
            WorkoutRecommendationState.NO_HRV,
            WorkoutRecommendationState.CALIBRATING,
            WorkoutRecommendationState.NO_CIRCADIAN_BASELINE,
            WorkoutRecommendationState.NO_HRV_BASELINE,
            -> emptySet()
        }

    companion object {
        private const val MIN_ELIGIBLE_DURATION_MINUTES = 15

        // Not private: `WorkoutRecommendationCodec` (core/database) validates a decoded snapshot's
        // example count against this same constant on read-back, rather than a hand-duplicated copy
        // that could silently drift out of sync with a future change here.
        const val MAX_EXAMPLES = 3
    }
}
