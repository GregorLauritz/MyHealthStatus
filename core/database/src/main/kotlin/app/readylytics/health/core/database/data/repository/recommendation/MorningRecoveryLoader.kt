package app.readylytics.health.core.database.data.repository.recommendation

import app.readylytics.health.core.database.data.repository.ResidualFatigueComputer
import app.readylytics.health.core.database.data.repository.ScoringDayContext
import app.readylytics.health.core.model.domain.model.DailySummary
import app.readylytics.health.core.model.domain.model.SleepSession
import app.readylytics.health.core.model.domain.model.getOrElse
import app.readylytics.health.core.model.domain.preferences.PhysiologyProfile
import app.readylytics.health.core.model.domain.repository.SleepSessionData
import app.readylytics.health.core.model.domain.repository.SleepSessionRepository
import app.readylytics.health.core.model.domain.scoring.ScoringConstants
import app.readylytics.health.core.scoring.domain.recommendation.WorkoutRecommendationInput
import app.readylytics.health.core.scoring.domain.scoring.CircadianWakeBaseline
import app.readylytics.health.core.scoring.domain.scoring.ComputeSleepMetricsUseCase
import app.readylytics.health.core.scoring.domain.scoring.ScoringConfigFactory
import app.readylytics.health.core.scoring.domain.scoring.SleepMetricsRequest
import app.readylytics.health.core.scoring.domain.scoring.components.EmergencyFlagThresholds
import app.readylytics.health.core.scoring.domain.scoring.sleep.CurrentNightHrvResolver
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Sleep history the circadian baseline needs, expressed in milliseconds. */
internal const val CIRCADIAN_HISTORY_WINDOW_MS =
    ScoringConstants.CIRCADIAN_CONSISTENCY_WINDOW_DAYS.toLong() * 24L * 60L * 60L * 1000L

private const val MILLIS_PER_DAY = 86_400_000L

/** Domain view of a stored sleep row, for the scoring pass that only speaks [SleepSession]. */
internal fun SleepSessionData.toDomainSession(): SleepSession =
    SleepSession(
        id = id,
        startTime = startTime,
        endTime = endTime,
        durationMinutes = durationMinutes,
        efficiency = efficiency,
        deepSleepMinutes = deepSleepMinutes,
        remSleepMinutes = remSleepMinutes,
        lightSleepMinutes = lightSleepMinutes,
        awakeMinutes = awakeMinutes,
        sleepScore = sleepScore,
        startZoneOffsetSeconds = startZoneOffsetSeconds,
        endZoneOffsetSeconds = endZoneOffsetSeconds,
        deviceName = deviceName,
    )

/**
 * Resolves every recovery input the workout-recommendation evaluator needs, as of the moment the
 * user woke up.
 *
 * This is the impure edge: it reads Room and re-runs the existing sleep-scoring pass rather than
 * copying the completed day's stored `zLnHrv`, sleep score, and flags, because those are computed
 * at next-day midnight and a nap recorded later the same day can still move them. The re-run is the
 * *same* [ComputeSleepMetricsUseCase] with the same formulas — only its request bounds differ (see
 * [SleepMetricsRequest]).
 */
@Singleton
class MorningRecoveryLoader
    @Inject
    constructor(
        private val sleepSessionRepository: SleepSessionRepository,
        private val computeSleepMetricsUseCase: ComputeSleepMetricsUseCase,
        private val hrvResolver: CurrentNightHrvResolver,
        private val residualFatigueComputer: ResidualFatigueComputer,
        private val scoringConfigFactory: ScoringConfigFactory,
    ) {
        /**
         * [sessions] is the caller's already-loaded sleep history covering at least
         * [CIRCADIAN_HISTORY_WINDOW_MS] before the day; it is re-derived from Room when absent.
         */
        suspend fun load(
            context: ScoringDayContext,
            session: SleepSession,
            sessions: List<SleepSessionData>? = null,
        ): WorkoutRecommendationInput {
            val prefs = context.prefs
            val wakeTimeMs = session.endTime
            val history =
                sessions ?: sleepSessionRepository.getSince(context.nextDayMidnightMs - CIRCADIAN_HISTORY_WINDOW_MS)

            // The habitual wake time must come from days that are already over, so the day being
            // scored cannot define its own "usual".
            val priorHistory = history.filter { it.endTime < context.dayMidnightMs }
            val hasCircadianBaseline =
                CircadianWakeBaseline.resolve(priorHistory, prefs.consistencyBaselineDays, context.zoneId) != null

            val morningSummary = computeMorningSleepMetrics(context, session, history)
            val hrv = hrvResolver.resolve(session, setOf(session.id))
            val thresholds = emergencyThresholds(context)

            return WorkoutRecommendationInput(
                hasSleep = true,
                nightlyHrv = hrv.mean,
                isCalibrating = morningSummary.isCalibrating,
                hasCircadianBaseline = hasCircadianBaseline,
                zLnHrv = morningSummary.zLnHrv,
                lowHrvBound = thresholds.illnessZHrvThreshold,
                highHrvBound = thresholds.strongRecoveryZHrvThreshold,
                sleepScore = morningSummary.sleepScore,
                residualFatigue = residualFatigueComputer.computeAt(wakeTimeMs, prefs),
                fatigueGain = prefs.residualFatigueGain,
                recoveryFlags = morningSummary.recoveryFlags,
            )
        }

        private suspend fun computeMorningSleepMetrics(
            context: ScoringDayContext,
            session: SleepSession,
            history: List<SleepSessionData>,
        ): DailySummary {
            val wakeTimeMs = session.endTime
            val baseSummary = context.dailySummary ?: DailySummary(date = context.targetDate)
            val request =
                SleepMetricsRequest(
                    session = session,
                    dayMidnight = Instant.ofEpochMilli(context.dayMidnightMs),
                    targetDate = context.targetDate,
                    prefs = context.prefs,
                    summary = baseSummary,
                    loadScore = baseSummary.loadScoreWorkoutOnly ?: 0f,
                    loadScoreEverydayHr = baseSummary.loadScoreEverydayHr,
                    zoneId = context.zoneId,
                    rhrBaselineValue = context.initialBaselines.rhrBaselineValue,
                    dayEndMs = wakeTimeMs,
                    currentSessionIds = setOf(session.id),
                    prefetchedSessions =
                        history.filter { it.endTime <= wakeTimeMs }.map { it.toDomainSession() },
                )
            // A failed pass is an operational failure, not "no recovery data": surfacing it lets the
            // caller retry instead of persisting a fabricated unavailable state.
            return computeSleepMetricsUseCase(request).getOrElse { failure ->
                error("Morning sleep metrics failed for ${context.targetDate}: ${failure.code} ${failure.reason}")
            }
        }

        /**
         * HRV deviation bounds for the day, taken from the profile the day was *scored* with. A user
         * who switches profile later must not retroactively change an already-frozen day's bounds.
         */
        private fun emergencyThresholds(context: ScoringDayContext): EmergencyFlagThresholds {
            val frozenProfile =
                context.dailySummary?.snapshotProfile?.let { name ->
                    runCatching { enumValueOf<PhysiologyProfile>(name.uppercase()) }.getOrNull()
                }
            if (frozenProfile == null || frozenProfile == context.prefs.physiologyProfile) {
                return context.scoringConfig.emergencyFlags
            }
            return scoringConfigFactory
                .build(
                    userPreferences = context.prefs.copy(physiologyProfile = frozenProfile),
                    installDate = LocalDate.ofEpochDay(context.prefs.installDate / MILLIS_PER_DAY),
                    currentDate = context.targetDate,
                ).emergencyFlags
        }
    }
