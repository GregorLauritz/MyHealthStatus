package app.readylytics.health.core.database.data.mapper

import app.readylytics.health.core.database.data.repository.recommendation.EXAMPLE_STATES
import app.readylytics.health.core.database.data.repository.recommendation.RULE_VERSION
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationSnapshot
import app.readylytics.health.core.scoring.domain.recommendation.SelectWorkoutRecommendationExamples
import kotlinx.serialization.json.Json

/**
 * Encodes/decodes [WorkoutRecommendationSnapshot] to and from the `workoutRecommendationJson` TEXT
 * column on `daily_summaries`.
 *
 * [decode] treats every unrecognized or internally-inconsistent payload as absent (`null`) rather
 * than attempting to coerce it into a decision -- a corrupt or future-rule-version row must present
 * as "no recommendation stored", never as a guessed state such as `HARDER`. Validation is checked
 * against the assembler's own `RULE_VERSION`/`EXAMPLE_STATES` and the example selector's own
 * `MAX_EXAMPLES`, not duplicated copies -- a future change to any of those is automatically honored
 * here instead of silently causing every snapshot to fail decode.
 */
object WorkoutRecommendationCodec {
    // ignoreUnknownKeys: a future app version may add fields this build has never heard of, and a
    // record written by that version must still decode here instead of throwing. encodeDefaults:
    // keeps `ruleVersion` (and any other defaulted field) explicit in the stored payload so a
    // reader never has to fall back to a Kotlin-side default it cannot see in the raw JSON.
    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    fun encode(snapshot: WorkoutRecommendationSnapshot): String = json.encodeToString(snapshot)

    fun decode(raw: String?): WorkoutRecommendationSnapshot? =
        raw
            ?.takeIf { it.isNotBlank() }
            ?.let { payload -> decodeOrNull(payload) }
            ?.takeIf { it.isValid() }

    private fun decodeOrNull(payload: String): WorkoutRecommendationSnapshot? =
        runCatching { json.decodeFromString<WorkoutRecommendationSnapshot>(payload) }.getOrNull()

    private fun WorkoutRecommendationSnapshot.isValid(): Boolean {
        // A named source session and its wake time are set together or not at all.
        val hasConsistentWakeAnchor = (wakeSessionId == null) == (wakeTimeMs == null)
        val hasValidExampleCount = examples.size <= SelectWorkoutRecommendationExamples.MAX_EXAMPLES
        // Only EASY/HARDER decisions ever ship examples; any other state carrying examples is a
        // corrupt or hand-edited payload, not a legitimate variant of that state.
        val examplesMatchState = examples.isEmpty() || decision.state in EXAMPLE_STATES
        // The producer keeps at most one example per exercise type (see
        // SelectWorkoutRecommendationExamples' dedup step); duplicate exercise types are not a shape
        // the assembler can ever produce.
        val examplesAreDistinctByType = examples.distinctBy { it.exerciseType }.size == examples.size
        return ruleVersion == RULE_VERSION &&
            hasConsistentWakeAnchor &&
            hasValidExampleCount &&
            examplesMatchState &&
            examplesAreDistinctByType
    }
}
