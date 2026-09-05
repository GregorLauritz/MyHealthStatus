package app.readylytics.health.core.scoring.domain.recommendation

import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationExample
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationState
import app.readylytics.health.core.model.domain.scoring.WorkoutLoadLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectWorkoutRecommendationExamplesTest {
    private val selector = SelectWorkoutRecommendationExamples()

    private val windowStart = 0L
    private val windowEnd = TimeUnit.DAYS_30_MS

    /** A single eligible, EASY-eligible template: 30 minutes of LIGHT running, well inside the window. */
    private fun template(
        workoutId: String = "w1",
        exerciseType: String = "RUNNING",
        startTimeMs: Long = 1_000L,
        endTimeMs: Long = 1_000L + TimeUnit.MINUTES_30_MS,
        durationMinutes: Int = 30,
        averageHr: Float? = 130f,
        finalLoad: WorkoutLoadLevel = WorkoutLoadLevel.LIGHT,
    ) = WorkoutRecommendationExample(
        workoutId = workoutId,
        exerciseType = exerciseType,
        startTimeMs = startTimeMs,
        endTimeMs = endTimeMs,
        durationMinutes = durationMinutes,
        averageHr = averageHr,
        finalLoad = finalLoad,
    )

    private object TimeUnit {
        const val MINUTES_30_MS = 30 * 60_000L
        const val DAYS_30_MS = 30L * 24 * 60 * 60 * 1000L
    }

    // --- Given test from brief ----------------------------------------------------------------

    @Test fun selectsNewestPerType() {
        val old = WorkoutRecommendationExample("old", "RUNNING", 100, 200, 45, 130f, WorkoutLoadLevel.LIGHT)
        val recent = old.copy(workoutId = "new", startTimeMs = 300, endTimeMs = 400)
        val actual = selector.select(
            WorkoutRecommendationState.EASY, listOf(old, recent), 0, 500,
        )
        assertEquals(listOf("new"), actual.map { it.workoutId })
    }

    // --- Duration boundary: strictly > 15 minutes ----------------------------------------------

    @Test fun `15 minute workout is excluded`() {
        val fifteen = template(durationMinutes = 15)
        val actual = selector.select(WorkoutRecommendationState.EASY, listOf(fifteen), windowStart, windowEnd)
        assertTrue(actual.isEmpty())
    }

    @Test fun `16 minute workout is included`() {
        val sixteen = template(durationMinutes = 16)
        val actual = selector.select(WorkoutRecommendationState.EASY, listOf(sixteen), windowStart, windowEnd)
        assertEquals(listOf("w1"), actual.map { it.workoutId })
    }

    // --- 30-day cutoff / window bounds ----------------------------------------------------------

    @Test fun `workout starting exactly at window start is included`() {
        val atStart = template(startTimeMs = windowStart, endTimeMs = windowStart + TimeUnit.MINUTES_30_MS)
        val actual = selector.select(WorkoutRecommendationState.EASY, listOf(atStart), windowStart, windowEnd)
        assertEquals(listOf("w1"), actual.map { it.workoutId })
    }

    @Test fun `workout starting before window start is excluded`() {
        val beforeStart = template(startTimeMs = windowStart - 1, endTimeMs = windowStart - 1 + TimeUnit.MINUTES_30_MS)
        val actual = selector.select(WorkoutRecommendationState.EASY, listOf(beforeStart), windowStart, windowEnd)
        assertTrue(actual.isEmpty())
    }

    @Test fun `workout ending exactly at window end is included`() {
        val atEnd = template(startTimeMs = windowEnd - TimeUnit.MINUTES_30_MS, endTimeMs = windowEnd)
        val actual = selector.select(WorkoutRecommendationState.EASY, listOf(atEnd), windowStart, windowEnd)
        assertEquals(listOf("w1"), actual.map { it.workoutId })
    }

    @Test fun `workout ending after window end is excluded (30-day cutoff)`() {
        val afterEnd = template(startTimeMs = windowEnd - TimeUnit.MINUTES_30_MS, endTimeMs = windowEnd + 1)
        val actual = selector.select(WorkoutRecommendationState.EASY, listOf(afterEnd), windowStart, windowEnd)
        assertTrue(actual.isEmpty())
    }

    // --- Future / in-progress sessions -----------------------------------------------------------

    @Test fun `future session entirely beyond window end is excluded`() {
        val future = template(startTimeMs = windowEnd + 10, endTimeMs = windowEnd + 10 + TimeUnit.MINUTES_30_MS)
        val actual = selector.select(WorkoutRecommendationState.EASY, listOf(future), windowStart, windowEnd)
        assertTrue(actual.isEmpty())
    }

    @Test fun `in-progress session with end not after start is excluded`() {
        val zeroDuration = template(startTimeMs = 1_000L, endTimeMs = 1_000L)
        val negativeDuration = template(workoutId = "w2", startTimeMs = 2_000L, endTimeMs = 1_500L)
        val actual = selector.select(
            WorkoutRecommendationState.EASY,
            listOf(zeroDuration, negativeDuration),
            windowStart,
            windowEnd,
        )
        assertTrue(actual.isEmpty())
    }

    // --- More than three types available: cap at 3, keep newest per type -----------------------

    @Test fun `caps at three examples even with more distinct types available`() {
        val types = listOf("RUNNING", "CYCLING", "SWIMMING", "ROWING")
        val candidates = types.mapIndexed { index, type ->
            val start = (index + 1) * 1_000L
            val end = start + TimeUnit.MINUTES_30_MS
            template(workoutId = type, exerciseType = type, startTimeMs = start, endTimeMs = end)
        }
        val actual = selector.select(WorkoutRecommendationState.EASY, candidates, windowStart, windowEnd)
        assertEquals(3, actual.size)
        // Newest-first: ROWING (index 3), SWIMMING (index 2), CYCLING (index 1); RUNNING dropped.
        assertEquals(listOf("ROWING", "SWIMMING", "CYCLING"), actual.map { it.workoutId })
    }

    // --- One / zero matches -----------------------------------------------------------------------

    @Test fun `single eligible candidate yields one example`() {
        val actual = selector.select(WorkoutRecommendationState.EASY, listOf(template()), windowStart, windowEnd)
        assertEquals(listOf("w1"), actual.map { it.workoutId })
    }

    @Test fun `no eligible candidates yields empty list`() {
        val wrongLoad = template(finalLoad = WorkoutLoadLevel.HARD)
        val actual = selector.select(WorkoutRecommendationState.EASY, listOf(wrongLoad), windowStart, windowEnd)
        assertTrue(actual.isEmpty())
    }

    @Test fun `empty candidate list yields empty list`() {
        val actual = selector.select(WorkoutRecommendationState.EASY, emptyList(), windowStart, windowEnd)
        assertTrue(actual.isEmpty())
    }

    // --- Rest and every unavailable state: never yields examples ---------------------------------

    @Test fun `REST and every unavailable state yield no examples regardless of candidates`() {
        val candidates = listOf(
            template(finalLoad = WorkoutLoadLevel.VERY_LIGHT),
            template(workoutId = "w2", exerciseType = "CYCLING", finalLoad = WorkoutLoadLevel.LIGHT),
            template(workoutId = "w3", exerciseType = "SWIMMING", finalLoad = WorkoutLoadLevel.MODERATE),
            template(workoutId = "w4", exerciseType = "ROWING", finalLoad = WorkoutLoadLevel.HARD),
            template(workoutId = "w5", exerciseType = "HIKING", finalLoad = WorkoutLoadLevel.VERY_HARD),
        )
        val noExampleStates = listOf(
            WorkoutRecommendationState.REST,
            WorkoutRecommendationState.NO_SLEEP,
            WorkoutRecommendationState.NO_HRV,
            WorkoutRecommendationState.CALIBRATING,
            WorkoutRecommendationState.NO_CIRCADIAN_BASELINE,
            WorkoutRecommendationState.NO_HRV_BASELINE,
        )
        noExampleStates.forEach { state ->
            val actual = selector.select(state, candidates, windowStart, windowEnd)
            assertTrue("state=$state should yield no examples, got $actual", actual.isEmpty())
        }
    }

    // --- Permutation independence: shuffled input yields identical output ------------------------

    @Test fun `result is identical regardless of input order (tie ordering is deterministic)`() {
        // Two candidates with the SAME endTimeMs and startTimeMs (a true tie), broken only by workoutId.
        val tieEnd = 5_000L + TimeUnit.MINUTES_30_MS
        val tieA = template(workoutId = "b-workout", exerciseType = "RUNNING", startTimeMs = 5_000L, endTimeMs = tieEnd)
        val tieB = template(workoutId = "a-workout", exerciseType = "RUNNING", startTimeMs = 5_000L, endTimeMs = tieEnd)
        val otherEnd = 2_000L + TimeUnit.MINUTES_30_MS
        val other =
            template(workoutId = "cycling-1", exerciseType = "CYCLING", startTimeMs = 2_000L, endTimeMs = otherEnd)
        val candidates = listOf(tieA, tieB, other)

        val permutations = listOf(
            candidates,
            listOf(other, tieA, tieB),
            listOf(tieB, other, tieA),
            listOf(tieB, tieA, other),
            candidates.shuffled(java.util.Random(42)),
            candidates.shuffled(java.util.Random(7)),
        )

        val results = permutations.map { permutation ->
            selector.select(WorkoutRecommendationState.EASY, permutation, windowStart, windowEnd).map { it.workoutId }
        }
        val expected = listOf("a-workout", "cycling-1")
        results.forEach { assertEquals(expected, it) }
    }

    // --- Nonblank type required ----------------------------------------------------------------

    @Test fun `blank exercise type is excluded`() {
        val blank = template(exerciseType = "   ")
        val actual = selector.select(WorkoutRecommendationState.EASY, listOf(blank), windowStart, windowEnd)
        assertTrue(actual.isEmpty())
    }

    // --- HARDER state: promotion to Hard load still qualifies -------------------------------------

    @Test fun `promotion to Hard load still qualifies for harder guidance`() {
        val hard = template(finalLoad = WorkoutLoadLevel.HARD)
        val actual = selector.select(WorkoutRecommendationState.HARDER, listOf(hard), windowStart, windowEnd)
        assertEquals(listOf("w1"), actual.map { it.workoutId })
    }

    @Test fun `HARDER draws from Moderate, Hard, and Very Hard but not Light`() {
        val moderate = template(workoutId = "m", exerciseType = "RUNNING", finalLoad = WorkoutLoadLevel.MODERATE)
        val hard = template(workoutId = "h", exerciseType = "CYCLING", finalLoad = WorkoutLoadLevel.HARD)
        val veryHard = template(workoutId = "vh", exerciseType = "SWIMMING", finalLoad = WorkoutLoadLevel.VERY_HARD)
        val light = template(workoutId = "l", exerciseType = "ROWING", finalLoad = WorkoutLoadLevel.LIGHT)
        val actual = selector.select(
            WorkoutRecommendationState.HARDER,
            listOf(moderate, hard, veryHard, light),
            windowStart,
            windowEnd,
        )
        assertEquals(setOf("m", "h", "vh"), actual.map { it.workoutId }.toSet())
        assertTrue("l" !in actual.map { it.workoutId })
    }

    @Test fun `EASY draws from Very Light and Light but not Moderate`() {
        val veryLight = template(workoutId = "vl", exerciseType = "RUNNING", finalLoad = WorkoutLoadLevel.VERY_LIGHT)
        val light = template(workoutId = "l", exerciseType = "CYCLING", finalLoad = WorkoutLoadLevel.LIGHT)
        val moderate = template(workoutId = "m", exerciseType = "SWIMMING", finalLoad = WorkoutLoadLevel.MODERATE)
        val actual = selector.select(
            WorkoutRecommendationState.EASY,
            listOf(veryLight, light, moderate),
            windowStart,
            windowEnd,
        )
        assertEquals(setOf("vl", "l"), actual.map { it.workoutId }.toSet())
        assertTrue("m" !in actual.map { it.workoutId })
    }
}
