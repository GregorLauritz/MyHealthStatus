package app.readylytics.health.core.model.domain.recommendation

import kotlinx.serialization.Serializable

/**
 * The complete, reproducible morning workout guidance for one local day.
 *
 * Everything here is anchored to the moment the user woke up ([wakeTimeMs], the end of the sleep
 * record identified by [wakeSessionId]); nothing recorded later that day contributes. Recomputing a
 * day from the same stored rows therefore yields an identical snapshot, whether it is computed once
 * on the morning itself or replayed during a historical resync.
 *
 * [wakeSessionId] and [wakeTimeMs] are null only when no sleep record ended on the day, in which
 * case [decision] carries an unavailable state. [examples] is empty for every state other than
 * `EASY` and `HARDER`.
 *
 * [ruleVersion] identifies the decision rules that produced [decision], so a stored snapshot can be
 * recognized as stale after the rules change.
 */
@Serializable
data class WorkoutRecommendationSnapshot(
    val ruleVersion: Int = 1,
    val wakeSessionId: String?,
    val wakeTimeMs: Long?,
    val decision: WorkoutRecommendationDecision,
    val examples: List<WorkoutRecommendationExample> = emptyList(),
)
