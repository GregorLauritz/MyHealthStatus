package app.readylytics.health.core.scoring.domain.recommendation

import app.readylytics.health.core.model.domain.model.RecoveryFlag
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationReason
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComputeWorkoutRecommendationUseCaseTest {
    private val useCase = ComputeWorkoutRecommendationUseCase()

    /** A fully-available, mid-range input: no illness, HRV/sleep/fatigue all within usual range. */
    private fun baseline(
        hasSleep: Boolean = true,
        nightlyHrv: Float? = 45f,
        isCalibrating: Boolean = false,
        hasCircadianBaseline: Boolean = true,
        zLnHrv: Float? = 0f,
        lowHrvBound: Float? = -1.5f,
        highHrvBound: Float? = 1.5f,
        sleepScore: Float? = 80f,
        residualFatigue: Float? = 20f,
        fatigueGain: Float = 1f,
        recoveryFlags: Set<RecoveryFlag> = emptySet(),
    ) = WorkoutRecommendationInput(
        hasSleep = hasSleep,
        nightlyHrv = nightlyHrv,
        isCalibrating = isCalibrating,
        hasCircadianBaseline = hasCircadianBaseline,
        zLnHrv = zLnHrv,
        lowHrvBound = lowHrvBound,
        highHrvBound = highHrvBound,
        sleepScore = sleepScore,
        residualFatigue = residualFatigue,
        fatigueGain = fatigueGain,
        recoveryFlags = recoveryFlags,
    )

    @Test fun missingHrvPrecedesIllness() {
        val input = WorkoutRecommendationInput(
            true, null, false, true, null, -1.5f, 1.5f,
            80f, 20f, 1f, setOf(RecoveryFlag.ILLNESS_ONSET),
        )
        assertEquals(
            WorkoutRecommendationState.NO_HRV,
            ComputeWorkoutRecommendationUseCase().compute(input).state,
        )
    }

    // --- Availability: no sleep -------------------------------------------------------------

    @Test fun `no sleep session yields NO_SLEEP regardless of other inputs`() {
        val input = baseline(hasSleep = false, nightlyHrv = null, isCalibrating = true, hasCircadianBaseline = false)
        val decision = useCase.compute(input)
        assertEquals(WorkoutRecommendationState.NO_SLEEP, decision.state)
        assertTrue(decision.reasons.isEmpty())
    }

    // --- Availability: nightly HRV must be positive and finite -------------------------------

    @Test fun `nightly HRV null, zero, negative, or non-finite yields NO_HRV`() {
        listOf(null, 0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY).forEach { hrv ->
            val decision = useCase.compute(baseline(nightlyHrv = hrv))
            assertEquals("nightlyHrv=$hrv", WorkoutRecommendationState.NO_HRV, decision.state)
        }
    }

    @Test fun `no sleep takes precedence over missing HRV`() {
        val input = baseline(hasSleep = false, nightlyHrv = null)
        assertEquals(WorkoutRecommendationState.NO_SLEEP, useCase.compute(input).state)
    }

    // --- Availability: calibration ------------------------------------------------------------

    @Test fun `calibrating flag yields CALIBRATING even with otherwise-usable inputs`() {
        val input = baseline(isCalibrating = true, hasCircadianBaseline = false, zLnHrv = null)
        assertEquals(WorkoutRecommendationState.CALIBRATING, useCase.compute(input).state)
    }

    @Test fun `missing HRV takes precedence over calibrating`() {
        val input = baseline(nightlyHrv = null, isCalibrating = true)
        assertEquals(WorkoutRecommendationState.NO_HRV, useCase.compute(input).state)
    }

    // --- Availability: circadian baseline -----------------------------------------------------

    @Test fun `missing circadian baseline yields NO_CIRCADIAN_BASELINE`() {
        val input = baseline(hasCircadianBaseline = false, zLnHrv = null)
        assertEquals(WorkoutRecommendationState.NO_CIRCADIAN_BASELINE, useCase.compute(input).state)
    }

    @Test fun `calibrating takes precedence over missing circadian baseline`() {
        val input = baseline(isCalibrating = true, hasCircadianBaseline = false)
        assertEquals(WorkoutRecommendationState.CALIBRATING, useCase.compute(input).state)
    }

    // --- Availability: HRV z-score / bounds must be finite and ordered -------------------------

    @Test fun `missing or non-finite z-score or bounds yields NO_HRV_BASELINE`() {
        val invalidCombos =
            listOf(
                baseline(zLnHrv = null),
                baseline(zLnHrv = Float.NaN),
                baseline(lowHrvBound = null),
                baseline(lowHrvBound = Float.NEGATIVE_INFINITY),
                baseline(highHrvBound = null),
                baseline(highHrvBound = Float.POSITIVE_INFINITY),
                // Bounds not strictly ordered.
                baseline(lowHrvBound = 1.5f, highHrvBound = 1.5f),
                baseline(lowHrvBound = 2f, highHrvBound = 1f),
            )
        invalidCombos.forEach { input ->
            assertEquals("input=$input", WorkoutRecommendationState.NO_HRV_BASELINE, useCase.compute(input).state)
        }
    }

    @Test fun `missing circadian baseline takes precedence over invalid HRV baseline`() {
        val input = baseline(hasCircadianBaseline = false, zLnHrv = null)
        assertEquals(WorkoutRecommendationState.NO_CIRCADIAN_BASELINE, useCase.compute(input).state)
    }

    // --- Available: HRV bounds, at and immediately outside --------------------------------------

    @Test fun `z-score at the low bound is within usual range, just below triggers HRV_LOW`() {
        val atBound = useCase.compute(baseline(zLnHrv = -1.5f, lowHrvBound = -1.5f, highHrvBound = 1.5f))
        assertEquals(WorkoutRecommendationState.HARDER, atBound.state)
        assertEquals(listOf(WorkoutRecommendationReason.WITHIN_USUAL_RANGE), atBound.reasons)

        val belowBound = useCase.compute(baseline(zLnHrv = -1.50001f, lowHrvBound = -1.5f, highHrvBound = 1.5f))
        assertEquals(WorkoutRecommendationState.EASY, belowBound.state)
        assertEquals(listOf(WorkoutRecommendationReason.HRV_LOW), belowBound.reasons)
    }

    @Test fun `z-score at the high bound is within usual range, just above triggers HRV_HIGH`() {
        val atBound = useCase.compute(baseline(zLnHrv = 1.5f, lowHrvBound = -1.5f, highHrvBound = 1.5f))
        assertEquals(WorkoutRecommendationState.HARDER, atBound.state)
        assertEquals(listOf(WorkoutRecommendationReason.WITHIN_USUAL_RANGE), atBound.reasons)

        val aboveBound = useCase.compute(baseline(zLnHrv = 1.50001f, lowHrvBound = -1.5f, highHrvBound = 1.5f))
        assertEquals(WorkoutRecommendationState.EASY, aboveBound.state)
        assertEquals(listOf(WorkoutRecommendationReason.HRV_HIGH), aboveBound.reasons)
    }

    // --- Available: sleep score, using the shared scoreStatus() warning/poor categories ---------

    @Test fun `sleep score 59point999 is SLEEP_LOW, 60 is within usual range`() {
        val low = useCase.compute(baseline(sleepScore = 59.999f))
        assertEquals(WorkoutRecommendationState.EASY, low.state)
        assertEquals(listOf(WorkoutRecommendationReason.SLEEP_LOW), low.reasons)

        val ok = useCase.compute(baseline(sleepScore = 60f))
        assertEquals(WorkoutRecommendationState.HARDER, ok.state)
        assertEquals(listOf(WorkoutRecommendationReason.WITHIN_USUAL_RANGE), ok.reasons)
    }

    @Test fun `poor sleep score is also SLEEP_LOW`() {
        val decision = useCase.compute(baseline(sleepScore = 10f))
        assertEquals(WorkoutRecommendationState.EASY, decision.state)
        assertEquals(listOf(WorkoutRecommendationReason.SLEEP_LOW), decision.reasons)
    }

    @Test fun `missing or non-finite sleep score is SLEEP_SCORE_MISSING`() {
        listOf(null, Float.NaN, Float.POSITIVE_INFINITY).forEach { score ->
            val decision = useCase.compute(baseline(sleepScore = score))
            assertEquals("sleepScore=$score", WorkoutRecommendationState.EASY, decision.state)
            assertEquals(listOf(WorkoutRecommendationReason.SLEEP_SCORE_MISSING), decision.reasons)
        }
    }

    // --- Available: fatigue, gain-scaled 70/70.001 boundary at multiple gains --------------------

    @Test fun `fatigue at 70 times gain is within usual range, just above triggers FATIGUE_HIGH`() {
        listOf(0.1f, 1f, 5f).forEach { gain ->
            val atThreshold = useCase.compute(baseline(residualFatigue = 70f * gain, fatigueGain = gain))
            assertEquals("gain=$gain", WorkoutRecommendationState.HARDER, atThreshold.state)
            assertEquals("gain=$gain", listOf(WorkoutRecommendationReason.WITHIN_USUAL_RANGE), atThreshold.reasons)

            val aboveThreshold = useCase.compute(baseline(residualFatigue = 70.001f * gain, fatigueGain = gain))
            assertEquals("gain=$gain", WorkoutRecommendationState.EASY, aboveThreshold.state)
            assertEquals("gain=$gain", listOf(WorkoutRecommendationReason.FATIGUE_HIGH), aboveThreshold.reasons)
        }
    }

    @Test fun `missing, non-finite, or negative fatigue is FATIGUE_MISSING`() {
        listOf(null, Float.NaN, Float.POSITIVE_INFINITY, -1f).forEach { fatigue ->
            val decision = useCase.compute(baseline(residualFatigue = fatigue))
            assertEquals("residualFatigue=$fatigue", WorkoutRecommendationState.EASY, decision.state)
            assertEquals(listOf(WorkoutRecommendationReason.FATIGUE_MISSING), decision.reasons)
        }
    }

    @Test fun `non-finite or non-positive fatigue gain is also FATIGUE_MISSING`() {
        listOf(Float.NaN, 0f, -1f).forEach { gain ->
            val decision = useCase.compute(baseline(fatigueGain = gain))
            assertEquals("fatigueGain=$gain", WorkoutRecommendationState.EASY, decision.state)
            assertEquals(listOf(WorkoutRecommendationReason.FATIGUE_MISSING), decision.reasons)
        }
    }

    // --- Illness ----------------------------------------------------------------------------

    @Test fun `illness onset alone forces REST`() {
        val decision = useCase.compute(baseline(recoveryFlags = setOf(RecoveryFlag.ILLNESS_ONSET)))
        assertEquals(WorkoutRecommendationState.REST, decision.state)
        assertEquals(listOf(WorkoutRecommendationReason.POSSIBLE_ILLNESS), decision.reasons)
    }

    @Test fun `illness onset still forces REST but preserves the other limiting reasons`() {
        val decision =
            useCase.compute(
                baseline(
                    zLnHrv = -2f,
                    lowHrvBound = -1.5f,
                    highHrvBound = 1.5f,
                    sleepScore = 30f,
                    recoveryFlags = setOf(RecoveryFlag.ILLNESS_ONSET),
                ),
            )
        assertEquals(WorkoutRecommendationState.REST, decision.state)
        assertEquals(
            listOf(
                WorkoutRecommendationReason.POSSIBLE_ILLNESS,
                WorkoutRecommendationReason.HRV_LOW,
                WorkoutRecommendationReason.SLEEP_LOW,
            ),
            decision.reasons,
        )
    }

    @Test fun `other, non-illness recovery flags do not affect the decision`() {
        val decision =
            useCase.compute(
                baseline(
                    recoveryFlags =
                        setOf(
                            RecoveryFlag.STRONG_RECOVERY_SIGNAL,
                            RecoveryFlag.REST_DAY_SUCCESS,
                            RecoveryFlag.NADIR_DELAYED,
                        ),
                ),
            )
        assertEquals(WorkoutRecommendationState.HARDER, decision.state)
        assertEquals(listOf(WorkoutRecommendationReason.WITHIN_USUAL_RANGE), decision.reasons)
    }

    // --- Multiple reasons without illness -----------------------------------------------------

    @Test fun `multiple limiting reasons without illness yield EASY with all reasons in stable order`() {
        val decision =
            useCase.compute(
                baseline(
                    zLnHrv = 2f,
                    lowHrvBound = -1.5f,
                    highHrvBound = 1.5f,
                    sleepScore = null,
                    residualFatigue = 500f,
                ),
            )
        assertEquals(WorkoutRecommendationState.EASY, decision.state)
        assertEquals(
            listOf(
                WorkoutRecommendationReason.HRV_HIGH,
                WorkoutRecommendationReason.SLEEP_SCORE_MISSING,
                WorkoutRecommendationReason.FATIGUE_HIGH,
            ),
            decision.reasons,
        )
    }

    // --- Fully within usual range --------------------------------------------------------------

    @Test fun `all signals within usual range with no flags yields HARDER`() {
        val decision = useCase.compute(baseline())
        assertEquals(WorkoutRecommendationState.HARDER, decision.state)
        assertEquals(listOf(WorkoutRecommendationReason.WITHIN_USUAL_RANGE), decision.reasons)
    }

    // --- Seven-day gate: exercised purely through the supplied calibration flag ----------------

    // The evaluator has no clock, date, or day-counter input of its own — `isCalibrating` is the
    // only lever, and it is expected to already encode the seven-day gate computed upstream.
    @Test fun `seven-day calibration gate is honored exclusively via the supplied isCalibrating flag`() {
        val stillCalibrating = useCase.compute(baseline(isCalibrating = true))
        assertEquals(WorkoutRecommendationState.CALIBRATING, stillCalibrating.state)

        val pastCalibration = useCase.compute(baseline(isCalibrating = false))
        assertEquals(WorkoutRecommendationState.HARDER, pastCalibration.state)
    }
}
