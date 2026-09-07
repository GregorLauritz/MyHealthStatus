package app.readylytics.health.core.database.domain.scoring.golden

import app.readylytics.health.core.databaseschema.data.local.entity.DailySummaryEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

/**
 * Serialization shared by the two scoring golden suites.
 *
 * The golden fixtures exist to lock the *scoring* output of the walk-forward/day pipeline. Fields
 * that are not scoring results are excluded here so an unrelated column cannot mask, or be mistaken
 * for, a real score regression.
 *
 * [EXCLUDED_FIELDS] currently holds `workoutRecommendationJson`: the morning recommendation is
 * assembled from a wake-time-bounded snapshot that both suites feed with relaxed mocks, so every row
 * would serialize the same degenerate "no sleep record" payload — ~1.8 MB of constant noise across
 * the 730-day walk-forward fixture that verifies nothing. The recommendation path has its own
 * dedicated coverage (`MorningRecommendationAssemblerTest`, `WorkoutRecommendationCodecTest`,
 * `ScoringRepositoryRecommendationTest`).
 *
 * Only the golden *comparison* is narrowed; the production entity and
 * [app.readylytics.health.core.database.data.mapper.DailySummaryMapper] still carry the column.
 */
internal object GoldenEntityJson {
    private val EXCLUDED_FIELDS = setOf("workoutRecommendationJson")

    private val json = Json { prettyPrint = true }

    fun encode(entity: DailySummaryEntity): String =
        json.encodeToString(
            JsonObject.serializer(),
            json.encodeToJsonElement(DailySummaryEntity.serializer(), entity).jsonObject.withoutExcluded(),
        )

    fun encode(entities: List<DailySummaryEntity>): String =
        json.encodeToString(
            JsonArray.serializer(),
            JsonArray(
                json
                    .encodeToJsonElement(ListSerializer(DailySummaryEntity.serializer()), entities)
                    .jsonArray
                    .map { it.jsonObject.withoutExcluded() },
            ),
        )

    private fun JsonObject.withoutExcluded(): JsonObject =
        JsonObject(filterKeys { it !in EXCLUDED_FIELDS })
}
