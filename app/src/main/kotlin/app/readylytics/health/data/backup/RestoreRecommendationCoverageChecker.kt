package app.readylytics.health.data.backup

import app.readylytics.health.core.database.data.local.HealthDatabase
import app.readylytics.health.core.database.data.mapper.WorkoutRecommendationCodec
import app.readylytics.health.core.model.domain.util.logE
import app.readylytics.health.core.model.workers.WorkerScheduler
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Task 5: after a [LocalRestoreManager] restore commits, decides whether the restored
 * `daily_summaries` rows need a local workout-recommendation backfill.
 *
 * Deliberately checked against the restored rows themselves via [WorkoutRecommendationCodec] --
 * never against the restored `scoringVersion` preference, which a backup can carry incorrectly
 * (e.g. it predates a rule-version bump the restored `scoringVersion` doesn't reflect). This is a
 * genuine "is the data there" check, not a "does the label claim it's there" check. An empty
 * backup (no daily summaries at all) has nothing to backfill.
 *
 * Best-effort and never fails the restore that already succeeded: worst case, the next launch's
 * startup gate or a later manual resync converges it through the same
 * [app.readylytics.health.core.model.data.preferences.SettingsDefaults.CURRENT_SCORING_VERSION]
 * marker as the normal upgrade path.
 */
@Singleton
class RestoreRecommendationCoverageChecker
    @Inject
    constructor(
        private val healthDatabase: HealthDatabase,
        private val workerScheduler: WorkerScheduler,
    ) {
        suspend fun scheduleRecomputeIfIncomplete() {
            try {
                val summaries = healthDatabase.dailySummaryDao().getAllSummaries()
                val incomplete =
                    summaries.isNotEmpty() &&
                        summaries.any { WorkoutRecommendationCodec.decode(it.workoutRecommendationJson) == null }
                if (incomplete) {
                    workerScheduler.scheduleResyncWorker(recomputeOnly = true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logE(TAG, e) {
                    "Failed to check restored recommendation coverage; a later resync still converges"
                }
            }
        }

        private companion object {
            const val TAG = "RestoreRecommendationCoverageChecker"
        }
    }
