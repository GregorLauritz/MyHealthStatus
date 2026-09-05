package app.readylytics.health.core.database.data.repository

import app.readylytics.health.core.database.data.repository.recommendation.MorningRecommendationAssembler
import app.readylytics.health.core.model.domain.repository.DailySummaryRepository
import app.readylytics.health.core.model.domain.repository.SleepSessionRepository
import app.readylytics.health.core.model.domain.repository.WorkoutRepository
import app.readylytics.health.core.scoring.domain.scoring.ComputeSleepMetricsUseCase
import app.readylytics.health.core.scoring.domain.scoring.GetWorkoutDisplayMetricsUseCase
import app.readylytics.health.core.scoring.domain.scoring.sleep.CurrentNightHrvResolver
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Hilt-injectable collaborators [MorningRecommendationAssembler] needs beyond what
 * [ScoringRepositoryImpl] already builds for the rest of the daily pipeline -- residual fatigue,
 * calibration, and baseline computation are reused directly from the repository's own instances
 * (see `ScoringRepositoryImpl.morningRecommendationAssembler`) rather than duplicated here.
 *
 * Grouped into a parameter object for the same reason as [ScoringDataLoaders]/[ScoringDayUseCases]:
 * six raw constructor parameters would have pushed [ScoringRepositoryImpl] over the detekt
 * `LongParameterList` threshold.
 */
@Singleton
data class MorningRecommendationDependencies
    @Inject
    constructor(
        val sleepSessionRepository: SleepSessionRepository,
        val computeSleepMetricsUseCase: ComputeSleepMetricsUseCase,
        val hrvResolver: CurrentNightHrvResolver,
        val workoutRepository: WorkoutRepository,
        val dailySummaryRepository: DailySummaryRepository,
        val getWorkoutDisplayMetricsUseCase: GetWorkoutDisplayMetricsUseCase,
    )
