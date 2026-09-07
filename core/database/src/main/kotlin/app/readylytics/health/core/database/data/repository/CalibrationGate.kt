package app.readylytics.health.core.database.data.repository

import app.readylytics.health.core.model.domain.model.SleepSession
import app.readylytics.health.core.model.domain.scoring.ScoringConstants
import app.readylytics.health.core.scoring.domain.scoring.BaselineComputer
import javax.inject.Inject

class CalibrationGate
    @Inject
    constructor(
        private val baselineComputer: BaselineComputer,
    ) {
        /**
         * [toMs] narrows the window the live valid-night count is taken over; it defaults to the
         * day's own end. The morning workout-recommendation path passes the wake time so a nap
         * recorded later that day cannot tip a day into "calibrated".
         *
         * A frozen day short-circuits to calibrated in both cases: the freeze stamp is only written
         * once a day has already passed this gate, so re-deriving it could only ever disagree with
         * what the user was actually scored against.
         */
        suspend fun isCalibrated(
            context: ScoringDayContext,
            prefetchedSessions: List<SleepSession>?,
            hasSession: Boolean,
            toMs: Long? = null,
        ): Boolean =
            context.dailySummary?.baselineCalculatedAtDate != null ||
                baselineComputer
                    .computeHrvWindowsBetween(
                        fromMs = context.dayMidnightMs,
                        toMs = toMs ?: context.nextDayMidnightMs,
                        zoneId = context.zoneId,
                        sleepDayPolicy = context.sleepDayPolicy,
                        prefetchedSessions = prefetchedSessions,
                    )?.validHistoricalDayCount
                    ?.plus(if (hasSession) 1 else 0)
                    ?.let { it >= ScoringConstants.MIN_SESSIONS_FOR_CALIBRATION }
                ?: false
    }
