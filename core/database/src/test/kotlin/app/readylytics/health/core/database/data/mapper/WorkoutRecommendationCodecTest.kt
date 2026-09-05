package app.readylytics.health.core.database.data.mapper

import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationDecision
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationExample
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationReason
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationSnapshot
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationState
import app.readylytics.health.core.model.domain.scoring.WorkoutLoadLevel
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Round-trip and rejection coverage for [WorkoutRecommendationCodec]. The codec is the only place
 * that turns a [WorkoutRecommendationSnapshot] into the `workoutRecommendationJson` TEXT column and
 * back, so every shape the assembler can ever produce -- and every shape a hostile/legacy payload
 * could contain -- needs to be provable here rather than only exercised transitively through Room.
 */
class WorkoutRecommendationCodecTest {
    @Test
    fun roundTripsMissingHrv() {
        val value =
            WorkoutRecommendationSnapshot(
                wakeSessionId = "sleep-1",
                wakeTimeMs = 1000,
                decision = WorkoutRecommendationDecision(WorkoutRecommendationState.NO_HRV),
            )
        assertEquals(value, WorkoutRecommendationCodec.decode(WorkoutRecommendationCodec.encode(value)))
    }

    @Test
    fun roundTripsNoSleepStateWithNullSessionAndWakeTime() {
        val value =
            WorkoutRecommendationSnapshot(
                wakeSessionId = null,
                wakeTimeMs = null,
                decision = WorkoutRecommendationDecision(WorkoutRecommendationState.NO_SLEEP),
            )
        assertEquals(value, WorkoutRecommendationCodec.decode(WorkoutRecommendationCodec.encode(value)))
    }

    @Test
    fun roundTripsEmptyExamples() {
        val value =
            WorkoutRecommendationSnapshot(
                wakeSessionId = "sleep-1",
                wakeTimeMs = 1000,
                decision =
                    WorkoutRecommendationDecision(
                        WorkoutRecommendationState.REST,
                        listOf(WorkoutRecommendationReason.HRV_LOW),
                    ),
                examples = emptyList(),
            )
        assertEquals(value, WorkoutRecommendationCodec.decode(WorkoutRecommendationCodec.encode(value)))
    }

    @Test
    fun roundTripsFullExampleSetPreservingOrder() {
        val value =
            WorkoutRecommendationSnapshot(
                wakeSessionId = "sleep-1",
                wakeTimeMs = 1000,
                decision =
                    WorkoutRecommendationDecision(
                        WorkoutRecommendationState.HARDER,
                        listOf(WorkoutRecommendationReason.WITHIN_USUAL_RANGE),
                    ),
                examples =
                    listOf(
                        example("w1", WorkoutLoadLevel.HARD, exerciseType = "Running"),
                        example("w2", WorkoutLoadLevel.MODERATE, exerciseType = "Cycling"),
                        example("w3", WorkoutLoadLevel.HARD, exerciseType = "Rowing"),
                    ),
            )
        val decoded = WorkoutRecommendationCodec.decode(WorkoutRecommendationCodec.encode(value))
        assertEquals(value, decoded)
        assertEquals(listOf("w1", "w2", "w3"), decoded?.examples?.map { it.workoutId })
    }

    @Test
    fun decodeReturnsNullWhenExamplesShareAnExerciseType() {
        // Invalid invariant: the producer (SelectWorkoutRecommendationExamples) keeps at most one
        // example per exercise type. Two "Running" examples is not a shape it can ever produce.
        val value =
            WorkoutRecommendationSnapshot(
                wakeSessionId = "sleep-1",
                wakeTimeMs = 1000,
                decision = WorkoutRecommendationDecision(WorkoutRecommendationState.HARDER),
                examples =
                    listOf(
                        example("w1", WorkoutLoadLevel.HARD, exerciseType = "Running"),
                        example("w2", WorkoutLoadLevel.HARD, exerciseType = "Running"),
                    ),
            )
        assertNull(WorkoutRecommendationCodec.decode(WorkoutRecommendationCodec.encode(value)))
    }

    @Test
    fun decodeReturnsNullForNullInput() {
        assertNull(WorkoutRecommendationCodec.decode(null))
    }

    @Test
    fun decodeReturnsNullForBlankInput() {
        assertNull(WorkoutRecommendationCodec.decode(""))
    }

    @Test
    fun decodeReturnsNullForMalformedJson() {
        assertNull(WorkoutRecommendationCodec.decode("{not-json"))
    }

    @Test
    fun decodeReturnsNullForUnknownRuleVersionRatherThanCoercing() {
        val futureVersionJson =
            """{"ruleVersion":99,"wakeSessionId":"s1","wakeTimeMs":1000,
                "decision":{"state":"HARDER","reasons":[]},"examples":[]}"""
        assertNull(WorkoutRecommendationCodec.decode(futureVersionJson))
    }

    @Test
    fun decodeTolerantOfUnknownKeysInDecisionPayload() {
        val withExtraKey =
            """{"ruleVersion":1,"wakeSessionId":"s1","wakeTimeMs":1000,
                "decision":{"state":"REST","reasons":["HRV_LOW"],"futureField":"x"},
                "examples":[],"anotherFutureField":42}"""
        val decoded = WorkoutRecommendationCodec.decode(withExtraKey)
        assertEquals(WorkoutRecommendationState.REST, decoded?.decision?.state)
    }

    @Test
    fun decodeReturnsNullWhenWakeSessionPresentButWakeTimeMissing() {
        // Invalid invariant: a named source session with no wake time is not a shape the assembler
        // can ever produce; treat it as absent rather than guess a wake time.
        val inconsistent =
            """{"ruleVersion":1,"wakeSessionId":"s1","wakeTimeMs":null,
                "decision":{"state":"REST","reasons":[]},"examples":[]}"""
        assertNull(WorkoutRecommendationCodec.decode(inconsistent))
    }

    @Test
    fun decodeReturnsNullWhenUnavailableStateCarriesExamples() {
        // Invalid invariant: only EASY/HARDER ever ship examples. A payload claiming otherwise is
        // corrupt, not a HARDER recommendation in disguise -- must not be coerced.
        val inconsistent =
            """{"ruleVersion":1,"wakeSessionId":"s1","wakeTimeMs":1000,"decision":{"state":"NO_HRV","reasons":[]},
                "examples":[{"workoutId":"w1","exerciseType":"Running","startTimeMs":0,"endTimeMs":1,
                "durationMinutes":30,"averageHr":null,"finalLoad":"MODERATE"}]}"""
        assertNull(WorkoutRecommendationCodec.decode(inconsistent))
    }

    @Test
    fun decodeReturnsNullWhenMoreThanThreeExamples() {
        val tooManyExamples =
            (1..4).joinToString(",") {
                """{"workoutId":"w$it","exerciseType":"Running","startTimeMs":0,"endTimeMs":1,
                    "durationMinutes":30,"averageHr":null,"finalLoad":"MODERATE"}"""
            }
        val inconsistent =
            """{"ruleVersion":1,"wakeSessionId":"s1","wakeTimeMs":1000,
                "decision":{"state":"HARDER","reasons":[]},"examples":[$tooManyExamples]}"""
        assertNull(WorkoutRecommendationCodec.decode(inconsistent))
    }

    @Test
    fun encodeIsDeterministicForIdenticalInput() {
        val value =
            WorkoutRecommendationSnapshot(
                wakeSessionId = "sleep-1",
                wakeTimeMs = 1000,
                decision =
                    WorkoutRecommendationDecision(
                        WorkoutRecommendationState.EASY,
                        listOf(WorkoutRecommendationReason.SLEEP_LOW),
                    ),
                examples = listOf(example("w1", WorkoutLoadLevel.LIGHT)),
            )
        assertEquals(WorkoutRecommendationCodec.encode(value), WorkoutRecommendationCodec.encode(value))
    }

    private fun example(id: String, load: WorkoutLoadLevel, exerciseType: String = "Running") =
        WorkoutRecommendationExample(
            workoutId = id,
            exerciseType = exerciseType,
            startTimeMs = 0L,
            endTimeMs = 1L,
            durationMinutes = 30,
            averageHr = 140f,
            finalLoad = load,
        )
}
