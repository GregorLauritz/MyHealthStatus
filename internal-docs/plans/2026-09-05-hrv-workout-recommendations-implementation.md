# HRV Workout Recommendations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide stored daily HRV-guided workout guidance with up to three distinct, linked examples from the user's preceding 30 days of workouts.

**Architecture:** A pure evaluator and history selector consume an explicitly bounded morning snapshot. Daily scoring stores their result atomically in Room; the dashboard reads it and uses existing layout preferences and workout navigation. Upgrade backfill uses the existing local recompute worker and progress channel.

**Tech Stack:** Kotlin, kotlinx.serialization, Room, DataStore, WorkManager, Compose M3, existing scoring engine; no new external service or dependency.

**Spec:** [Approved design](2026-09-05-hrv-workout-recommendations-design.md). Read this together with root `AGENTS.md` and `internal-docs/DATA_FLOW.md` before execution.

## Global Constraints

- Offline-first Android; minSdk=26, targetSdk=37.
- Room is the single source of truth; Health Connect is ingestion-only.
- All business/calculation logic is pure Kotlin with zero Android dependencies.
- ViewModels expose StateFlow/SharedFlow; Compose uses `collectAsStateWithLifecycle`.
- Native M3 components, dynamic theme colors, `MaterialTheme.shapes.large`, explicit surface-container roles.
- All user-facing strings belong in `app/src/main/res/values/strings.xml`.
- No numeric TRIMP target, generated workout duration, or prescribed heart-rate target.
- Seven-day calibration; missing HRV takes priority over illness guidance.
- Morning sleep score below 60 or fatigue above `70 × fatigue gain` caps guidance at Easy.
- Reuse profile HRV deviation bounds and existing illness detection; do not change scoring coefficients.
- Easy examples: Very light/Light; harder examples: Moderate/Hard/Very hard; Rest has none.
- Maximum three examples; one per existing workout type; strictly longer than 15 minutes.
- New files target ≤400 lines; hard limit ≤800 lines. Resolve detekt issues in touched files structurally.
- Changes to schema, scoring, workers, or pipeline require synchronous `DATA_FLOW.md` updates.
- Update ABOUT, public site explanations, and in-app explanations together with the feature.
- Pull-to-refresh remains current-day only; automatic history backfill is local recompute-only.
- Run `codegraph index` after new files and `codegraph sync` after structural changes.
- This artifact authorizes planning only. Execution starts when requested by the user.

## Execution conventions and repository findings

All paths below are relative to the repository root. Tests mirror source packages.
No application code has been changed while preparing this plan.

- Database is currently version **18**; next migration is **18 → 19**.
- `SettingsDefaults.CURRENT_SCORING_VERSION` is **4**; this feature advances it to **5**.
- `docs/superpowers/` is ignored; design and plan are deliberately in `internal-docs/plans/`.
- `ResidualFatigueComputer.compute` evaluates next-day midnight. The morning path must not use it.
- `ReadinessSummaryCoordinator.resolveSleepAggregation` can include supplemental sleep.
- `CircadianConsistencyRepository` uses ≥180-minute sessions and at least three baseline sessions.
- `GetWorkoutDisplayMetricsUseCase` already returns the history classification as `classification.finalLoad`.
- `WorkerSchedulerImpl` uses APPEND_OR_REPLACE for local recomputes and KEEP for HC resyncs.
- `CardConfigurationRepositoryImpl` appends missing default cards without resetting existing visibility/order.
- `DashboardViewModel.kt` is 603 lines; `DashboardCardFactory.kt` is 477. Put new state/mapping/rendering in focused files.

At execution start, recheck schema/scoring versions against HEAD and adjust the new migration number if another change advanced them. Do not overwrite user changes. Use the worktree skill if isolation is needed at execution time.

Each task has a failing-test → implementation → passing-test cycle. Run the focused command first and confirm a relevant test/assertion failure, not an unrelated build failure. At a commit checkpoint run the mandatory full checks before staging only task-owned files:

```sh
./gradlew ktlintFormat
./gradlew detekt
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Run `lintRelease` at the end of all coding tasks. Review formatter output for unrelated changes. Do not add suppressions or baseline entries. The code blocks below define contracts and key algorithms; imports use the packages implied by the paths and the existing model types named explicitly.

## Task 1: Pure recommendation state and decision rules

**Create:**
- `core/model/src/main/kotlin/app/readylytics/health/core/model/domain/recommendation/WorkoutRecommendation.kt`
- `core/scoring/src/main/kotlin/app/readylytics/health/core/scoring/domain/recommendation/WorkoutRecommendationInput.kt`
- `core/scoring/src/main/kotlin/app/readylytics/health/core/scoring/domain/recommendation/ComputeWorkoutRecommendationUseCase.kt`
- `core/model/src/main/kotlin/app/readylytics/health/core/model/domain/scoring/ResidualFatigueThresholds.kt`
- `core/scoring/src/test/kotlin/app/readylytics/health/core/scoring/domain/recommendation/ComputeWorkoutRecommendationUseCaseTest.kt`

**Modify:**
- `feature/dashboard/src/main/kotlin/app/readylytics/health/feature/dashboard/usecase/ResidualFatiguePresentationFactory.kt`
- `internal-docs/DATA_FLOW.md` (recommendation rules and shared fatigue classification ownership).

**Interfaces:** `ComputeWorkoutRecommendationUseCase.compute(input: WorkoutRecommendationInput): WorkoutRecommendationDecision`. It performs no reads, clock access, or string localization.

- [ ] Define serializable enums/models in `:core:model`:

```kotlin
@Serializable
enum class WorkoutRecommendationState {
    REST, EASY, HARDER, NO_SLEEP, NO_HRV, CALIBRATING,
    NO_CIRCADIAN_BASELINE, NO_HRV_BASELINE,
}
@Serializable
enum class WorkoutRecommendationReason {
    POSSIBLE_ILLNESS, HRV_LOW, HRV_HIGH, SLEEP_LOW, FATIGUE_HIGH,
    SLEEP_SCORE_MISSING, FATIGUE_MISSING, WITHIN_USUAL_RANGE,
}
@Serializable
data class WorkoutRecommendationDecision(
    val state: WorkoutRecommendationState,
    val reasons: List<WorkoutRecommendationReason> = emptyList(),
)
```

- [ ] Define the evaluator input as a parameter object; null/non-finite numbers remain unknown:

```kotlin
data class WorkoutRecommendationInput(
    val hasSleep: Boolean,
    val nightlyHrv: Float?,
    val isCalibrating: Boolean,
    val hasCircadianBaseline: Boolean,
    val zLnHrv: Float?,
    val lowHrvBound: Float?,
    val highHrvBound: Float?,
    val sleepScore: Float?,
    val residualFatigue: Float?,
    val fatigueGain: Float,
    val recoveryFlags: Set<RecoveryFlag>,
)
```

- [ ] Write this first failing test with JUnit assertions and the existing `RecoveryFlag` enum:

```kotlin
@Test fun missingHrvPrecedesIllness() {
    val input = WorkoutRecommendationInput(
        true, null, false, true, null, -1.5f, 1.5f,
        80f, 20f, 1f, setOf(RecoveryFlag.ILLNESS_ONSET),
    )
    assertEquals(WorkoutRecommendationState.NO_HRV,
        ComputeWorkoutRecommendationUseCase().compute(input).state)
}
```

- [ ] Run `./gradlew :core:scoring:testDebugUnitTest --tests '*ComputeWorkoutRecommendationUseCaseTest'` and confirm the missing evaluator fails.
- [ ] Implement availability checks in order: no sleep, no positive finite nightly HRV, calibration, missing circadian baseline, invalid HRV z/bounds. Require finite ordered bounds and a usable underlying baseline; never coerce missing HRV into zero deviation.
- [ ] For available inputs, collect reasons in a stable order: illness, low/high HRV, low/missing sleep score, high/missing fatigue. Use the existing `scoreStatus()` warning/poor categories for sleep and the extracted fatigue classifier. Preserve all limiting reasons even when illness determines Rest.

```kotlin
val state = when {
    WorkoutRecommendationReason.POSSIBLE_ILLNESS in reasons -> WorkoutRecommendationState.REST
    reasons.isNotEmpty() -> WorkoutRecommendationState.EASY
    else -> WorkoutRecommendationState.HARDER
}
```

- [ ] Move existing gain-scaled 30/70 fatigue status comparisons into `ResidualFatigueThresholds.classify(value: Float?, gain: Float): MetricStatus`. Both UI and evaluator call it. Null/non-finite/negative fatigue or invalid gain returns NO_DATA; evaluator converts that to FATIGUE_MISSING. Valid-input UI classifications remain identical.
- [ ] Add table-driven tests at HRV bounds and immediately outside; sleep 59.999/60; fatigue 70/70.001 at gains 0.1/1/5; NaN/infinity; all missing states; illness; positive flags; multiple reasons. Test seven-day gate through the supplied calibration state, not a second independent day counter.
- [ ] Run focused tests plus existing residual-fatigue presentation tests, update data-flow ownership, and complete the commit checkpoint (`feat: add workout guidance rules`).

## Task 2: Deterministic workout-example selector

**Create:**
- `core/model/src/main/kotlin/app/readylytics/health/core/model/domain/recommendation/WorkoutRecommendationExample.kt`
- `core/scoring/src/main/kotlin/app/readylytics/health/core/scoring/domain/recommendation/SelectWorkoutRecommendationExamples.kt`
- `core/scoring/src/test/kotlin/app/readylytics/health/core/scoring/domain/recommendation/SelectWorkoutRecommendationExamplesTest.kt`

**Interfaces:** `select(state: WorkoutRecommendationState, candidates: List<WorkoutRecommendationExample>, fromMs: Long, throughMs: Long): List<WorkoutRecommendationExample>`. Candidate classification is the existing `WorkoutLoadLevel` final history classification, not a new score.

- [ ] Define the stored example contract:

```kotlin
@Serializable
data class WorkoutRecommendationExample(
    val workoutId: String,
    val exerciseType: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMinutes: Int,
    val averageHr: Float?,
    val finalLoad: WorkoutLoadLevel,
)
```

- [ ] Write a failing test proving deduplication chooses the newest eligible session:

```kotlin
@Test fun selectsNewestPerType() {
    val old = WorkoutRecommendationExample("old", "RUNNING", 100, 200, 45, 130f, WorkoutLoadLevel.LIGHT)
    val recent = old.copy(workoutId = "new", startTimeMs = 300, endTimeMs = 400)
    val actual = SelectWorkoutRecommendationExamples().select(
        WorkoutRecommendationState.EASY, listOf(old, recent), 0, 500,
    )
    assertEquals(listOf("new"), actual.map { it.workoutId })
}
```

- [ ] Run `./gradlew :core:scoring:testDebugUnitTest --tests '*SelectWorkoutRecommendationExamplesTest'`.
- [ ] Implement deterministic filtering/grouping. Use stored durationMinutes consistently with history. Eligibility uses start ≥ window start, completed end ≤ morning anchor, end > start, duration >15, and a nonblank type. Invalid classification never becomes Very light. Date-window construction belongs to the assembler, not this pure selector.

```kotlin
val allowed = when (state) {
    WorkoutRecommendationState.EASY -> setOf(WorkoutLoadLevel.VERY_LIGHT, WorkoutLoadLevel.LIGHT)
    WorkoutRecommendationState.HARDER -> setOf(WorkoutLoadLevel.MODERATE, WorkoutLoadLevel.HARD, WorkoutLoadLevel.VERY_HARD)
    else -> emptySet()
}
return candidates.asSequence()
    .filter { it.durationMinutes > 15 && it.exerciseType.isNotBlank() }
    .filter { it.startTimeMs >= fromMs && it.endTimeMs <= throughMs && it.endTimeMs > it.startTimeMs }
    .filter { it.finalLoad in allowed }
    .sortedWith(compareByDescending<WorkoutRecommendationExample> { it.endTimeMs }
        .thenByDescending { it.startTimeMs }.thenBy { it.workoutId })
    .distinctBy { it.exerciseType }
    .take(3).toList()
```

- [ ] Add boundary tests for 15/16 minutes, 30-day cutoff, future/in-progress sessions, more than three types, one/zero matches, Rest and every unavailable state, and permutation-independent tie ordering. Verify promotion to Hard still qualifies for harder guidance.
- [ ] Run focused tests and commit checkpoint (`feat: select recent workout examples`).

## Task 3: Assemble a reproducible morning recovery snapshot

**Create:**
- `core/model/src/main/kotlin/app/readylytics/health/core/model/domain/recommendation/WorkoutRecommendationSnapshot.kt`
- `core/scoring/src/main/kotlin/app/readylytics/health/core/scoring/domain/recommendation/SelectMorningSleepSession.kt`
- `core/scoring/src/main/kotlin/app/readylytics/health/core/scoring/domain/scoring/CircadianWakeBaseline.kt`
- `core/database/src/main/kotlin/app/readylytics/health/core/database/data/repository/recommendation/MorningRecommendationAssembler.kt`
- `core/database/src/main/kotlin/app/readylytics/health/core/database/data/repository/recommendation/MorningRecoveryLoader.kt`
- `core/database/src/main/kotlin/app/readylytics/health/core/database/data/repository/recommendation/WorkoutExampleLoader.kt`
- `core/scoring/src/test/kotlin/app/readylytics/health/core/scoring/domain/recommendation/SelectMorningSleepSessionTest.kt`
- `core/database/src/test/kotlin/app/readylytics/health/core/database/data/repository/recommendation/MorningRecommendationAssemblerTest.kt`

**Modify:**
- `core/scoring/src/main/kotlin/app/readylytics/health/core/scoring/domain/scoring/CircadianConsistencyRepository.kt`
- `core/scoring/src/main/kotlin/app/readylytics/health/core/scoring/domain/scoring/SleepMetricsTypes.kt`
- `core/scoring/src/main/kotlin/app/readylytics/health/core/scoring/domain/scoring/ComputeSleepMetricsUseCase.kt`
- `core/scoring/src/main/kotlin/app/readylytics/health/core/scoring/domain/scoring/sleep/SleepModifierResolver.kt`
- `core/database/src/main/kotlin/app/readylytics/health/core/database/data/repository/ResidualFatigueComputer.kt`
- `internal-docs/DATA_FLOW.md` (morning snapshot, date anchoring, determinism).

**Interfaces:**

```kotlin
@Serializable
data class WorkoutRecommendationSnapshot(
    val ruleVersion: Int = 1,
    val wakeSessionId: String?,
    val wakeTimeMs: Long?,
    val decision: WorkoutRecommendationDecision,
    val examples: List<WorkoutRecommendationExample> = emptyList(),
)
// Assembler in :core:database; ScoringDayContext is the existing repository type.
// suspend fun assemble(context: ScoringDayContext): WorkoutRecommendationSnapshot
// MorningRecoveryLoader.load(context: ScoringDayContext, session: SleepSession): WorkoutRecommendationInput
// WorkoutExampleLoader.load(fromMs: Long, throughMs: Long, prefs: UserPreferences): List<WorkoutRecommendationExample>
// SelectMorningSleepSession.select(sessions: List<SleepSession>, date: LocalDate,
//     zone: ZoneId, usualWakeMinutes: Int): SleepSession?
```

- [ ] Add failing tests before changing shared scoring. The morning fixture has a 07:00 wake, normal HRV, sleep score 80, and fatigue 20; a 14:00 nap and 18:00 workout must leave the complete snapshot unchanged. Replacing the selected sleep record's HRV must change the result. Use existing repository MockK fixtures; assert calls are bounded at the selected end time.

```kotlin
@Test fun choosesClosestRecordedWake() {
    val date = LocalDate.of(2026, 9, 5)
    val zone = ZoneId.of("UTC")
    val end = date.atTime(7, 0).atZone(zone).toInstant().toEpochMilli()
    val main = SleepSession("main", end - 8 * 3_600_000L, end, 480, 0.9f, 90, 90, 280, 20)
    val later = main.copy(id = "later", startTime = end + 3_600_000L, endTime = end + 7 * 3_600_000L)
    assertEquals("main", SelectMorningSleepSession().select(listOf(later, main), date, zone, 420)?.id)
}
```
- [ ] Run `./gradlew :core:scoring:testDebugUnitTest --tests '*SelectMorningSleepSessionTest'` and `./gradlew :core:database:testDebugUnitTest --tests '*MorningRecommendationAssemblerTest'`.
- [ ] Extract the current ≥180-minute/minimum-three-session circadian baseline selection/median routine into `CircadianWakeBaseline` without altering existing circadian score behavior. For recommendation anchor selection, supply only history ending before the target date starts. Respect existing baseline-count preference and scoring zone. Return no baseline when insufficient; do not substitute noon/07:00 or today's later sessions.
- [ ] Select a recorded session ending on the target local date using circular clock distance to that prior-history usual wake time. Tie-break by earliest end time then stable ID. Preserve sleep-source filtering already applied in Room. Store the chosen session ID; for an existing recommendation keep its source session on ordinary daytime appends, but use that source's corrected timestamps/data. If the source is deleted, rerun selection. Do not let a later nap silently replace the selected morning source.

```kotlin
val wakeMinutes = Instant.ofEpochMilli(session.endTime).atZone(zone).let { it.hour * 60 + it.minute }
val direct = kotlin.math.abs(wakeMinutes - usualWakeMinutes)
val distanceMinutes = minOf(direct, 1440 - direct)
```

- [ ] Add optional bounded request context to sleep scoring, defaulting to existing behavior for ordinary callers. Thread `dayEndMs = wakeTimeMs` and prefiltered sessions through HRV/RHR reads, baseline window, regularity modifiers, and flags. Use the selected session ID for nightly HRV. Do not copy the completed day's `zLnHrv`, sleep score, or illness flag if later sleep can affect them. Reuse frozen date-appropriate baseline/profile values; set request preferences to the frozen profile before building emergency thresholds.
- [ ] Reuse `CurrentNightHrvResolver`'s floating mean and existing z-score calculation. Do not derive z from rounded UI HRV. Use `EmergencyFlagThresholds.illnessZHrvThreshold` and `.strongRecoveryZHrvThreshold` from the effective profile as the recommendation's bounds, without treating a positive recovery flag as permission for harder exercise.
- [ ] Add `ResidualFatigueComputer.computeAt(evaluationTimeMs: Long, prefs: UserPreferences): Float?`, sharing the existing exact fallback and unbackfilled-data gate. Call at wake time. Do not move the shared day-end walk-forward accumulator backwards. A dedicated prefetched morning accumulator can be introduced only with equality tests against computeAt; the initial implementation may use the exact fallback.
- [ ] Load examples from `wakeTime.atZone(zone).minusDays(30)` through wake time. Call `GetWorkoutDisplayMetricsUseCase.execute(workout, preferences = prefs)` for finalLoad, reusing its canonical display computation. Filter missing classifications; omit nonpositive/non-finite average HR from copy. Restrict candidate rows before invoking expensive metrics; memoize duplicate workout IDs within a historical pass with preferences in the cache key.
- [ ] Compose loader/evaluator/selector; unavailable and Rest states skip example loading:

```kotlin
val decision = evaluator.compute(recoveryInput)
val examples = if (decision.state in setOf(WorkoutRecommendationState.EASY, WorkoutRecommendationState.HARDER)) {
    selector.select(decision.state, exampleLoader.load(fromMs, wakeTimeMs, context.prefs), fromMs, wakeTimeMs)
} else emptyList()
return WorkoutRecommendationSnapshot(1, session.id, wakeTimeMs, decision, examples)
```

- [ ] Test missing baseline/HRV/calibration, DST/midnight selection, split sessions, source correction/deletion, no future inputs, unbackfilled canonical TRIMP, and exact day-at-a-time versus historical replay. CancellationException must propagate. Missing optional metrics produce Easy; operational read failures retry the outer computation rather than falsely storing “no data.”
- [ ] Run existing sleep/fatigue suites once after shared changes. Update DATA_FLOW and complete checkpoint (`feat: compute morning workout guidance`).

## Task 4: Store recommendation atomically with daily summary

**Create:**
- `core/database/src/main/kotlin/app/readylytics/health/core/database/data/mapper/WorkoutRecommendationCodec.kt`
- `core/database/src/main/kotlin/app/readylytics/health/core/database/data/local/migration/Migration18To19.kt`
- `core/database/src/test/kotlin/app/readylytics/health/core/database/data/local/Migration18To19Test.kt`
- `core/database/src/test/kotlin/app/readylytics/health/core/database/data/mapper/WorkoutRecommendationCodecTest.kt`

**Modify:**
- `core/model/src/main/kotlin/app/readylytics/health/core/model/domain/model/DailySummary.kt`
- `core/database-schema/src/main/kotlin/app/readylytics/health/core/databaseschema/data/local/entity/DailySummaryEntity.kt`
- `core/database/src/main/kotlin/app/readylytics/health/core/database/data/mapper/DailySummaryMapper.kt`
- `core/database/src/main/kotlin/app/readylytics/health/core/database/data/local/HealthDatabase.kt`
- `core/database/src/main/kotlin/app/readylytics/health/core/database/data/local/DatabaseMigrations.kt`
- `core/database/src/main/kotlin/app/readylytics/health/core/database/data/repository/ScoringRepositoryImpl.kt`
- `core/database/src/androidTest/kotlin/app/readylytics/health/core/database/data/local/DatabaseMigrationInstrumentedTest.kt`
- Generate `core/database/schemas/app.readylytics.health.core.database.data.local.HealthDatabase/19.json`.
- `internal-docs/DATA_FLOW.md` (schema/processing/persistence).

**Interfaces:** nullable `DailySummary.workoutRecommendation: WorkoutRecommendationSnapshot?`; entity nullable TEXT `workoutRecommendationJson`; codec `encode(snapshot): String` and `decode(raw: String?): WorkoutRecommendationSnapshot?`.

- [ ] Write codec round-trip and unknown/malformed input tests; add migration SQL verification and atomic daily-summary persistence test before implementation.

```kotlin
@Test fun roundTripsMissingHrv() {
    val value = WorkoutRecommendationSnapshot(
        wakeSessionId = "sleep-1", wakeTimeMs = 1000,
        decision = WorkoutRecommendationDecision(WorkoutRecommendationState.NO_HRV),
    )
    assertEquals(value, WorkoutRecommendationCodec.decode(WorkoutRecommendationCodec.encode(value)))
}
```

- [ ] Run `./gradlew :core:database:testDebugUnitTest --tests '*WorkoutRecommendationCodecTest' --tests '*Migration18To19Test'`.
- [ ] Add nullable properties with defaults for source/backup compatibility. Codec uses existing kotlinx JSON configuration with unknown-key tolerance. Reject unknown rule versions and invalid payload invariants as absent, never as HARDER. Persist reason ordering and maximum-three distinct examples.

```kotlin
val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE daily_summaries ADD COLUMN workoutRecommendationJson TEXT DEFAULT NULL")
    }
}
```

- [ ] Register migration, advance DB version, regenerate schema with `./gradlew :core:database:kspDebugKotlin`. Mapper converts the payload both ways. No separate example table or orphan-prone write is necessary: one daily-summary upsert commits category and examples together.
- [ ] Wire assembler after existing daily TRIMP processing and summary assembly, before returning the computed summary; do not recursively call computeDailySummary from it:

```kotlin
return finalSummary.copy(workoutRecommendation = morningRecommendationAssembler.assemble(context))
```

- [ ] In every summary reconstruction/copy/export path preserve the new field. Serialization of old entities must yield null. Keep a null migrated recommendation distinct from CALIBRATING in presentation (“Recommendation not calculated yet”).
- [ ] Test category/reasons/examples replaced together, failed assembly leaves the prior persisted row intact, repeated computation produces identical JSON, and ordinary existing metrics are unchanged. Instrumented migration test opens v18 data under v19 and verifies existing values preserved.
- [ ] Run targeted mapper/schema/repository tests, update DATA_FLOW, and checkpoint (`feat: persist daily workout guidance`).

## Task 5: Upgrade backfill, corrections, and restore

**Modify:**
- `core/model/src/main/kotlin/app/readylytics/health/core/model/data/preferences/SettingsDefaults.kt`
- `app/src/main/kotlin/app/readylytics/health/DatabaseReadyStartupInitializer.kt`
- `app/src/main/kotlin/app/readylytics/health/workers/HealthResyncWorker.kt`
- `app/src/main/kotlin/app/readylytics/health/data/backup/LocalRestoreManager.kt`
- `app/src/test/kotlin/app/readylytics/health/DatabaseReadyStartupInitializerScoringVersionTest.kt`
- `app/src/test/kotlin/app/readylytics/health/workers/HealthResyncWorkerScoringVersionTest.kt`
- `app/src/test/kotlin/app/readylytics/health/data/backup/LocalRestoreApplicationTest.kt`
- `core/model/src/main/kotlin/app/readylytics/health/core/model/domain/sync/ScoreInvalidation.kt`
- `internal-docs/DATA_FLOW.md`, `docs/privacy.md` (local derived data/backup compatibility).

**Interfaces:** reuse `WorkerScheduler.scheduleResyncWorker(recomputeOnly = true)`; no new worker or progress flow. Scoring version 5 marks a successful full retained-history recompute, not merely any bounded recompute.

- [ ] Extend startup tests so stored version 4 enqueues local recompute and stays 4 until completion; same-version data does not enqueue needlessly. Reuse the existing fixture and MockK `coVerify` against `scheduleResyncWorker(recomputeOnly = true)`.

```kotlin
// Add inside DatabaseReadyStartupInitializerScoringVersionTest, using its existing helpers.
@Test fun versionFourNeedsRecommendationBackfill() = runTest {
    val scheduler = FakeWorkerScheduler()
    val initializer = initializerWith(storedScoringVersion = 4, scheduler = scheduler)
    initializer.initializeIfReady(DatabaseReadiness.Ready)
    assertEquals(1, scheduler.recomputeOnlyRequests)
}
```
- [ ] Run `./gradlew :app:testDebugUnitTest --tests '*DatabaseReadyStartupInitializerScoringVersionTest' --tests '*HealthResyncWorkerScoringVersionTest'`.
- [ ] Bump current scoring version to 5. Preserve the existing APPEND_OR_REPLACE local scheduling and shared progress. Ensure a bounded cleanup/settings recompute cannot falsely mark the full-history version complete. Pass whether the completed range covers RetentionBounds into post-recompute state recording:

```kotlin
if (coversRetainedHistory && prefs.scoringVersion < SettingsDefaults.CURRENT_SCORING_VERSION) {
    settings.updateScoringVersion(SettingsDefaults.CURRENT_SCORING_VERSION)
}
```

- [ ] Maintain stale-version retry on killed/failed workers and cancellation propagation. Daily sync still computes day=1; historical local recalculation performs cooperative date iteration and existing sync-mutex serialization. Do not add another banner.
- [ ] Extend existing local invalidation dependency coverage to include example selection: correcting/deleting a workout can affect recommendations for the next 30 days; bound to today and retention. Preserve any longer pre-existing scoring dependency horizon. The recompute worker follows that range; it must not perform HC fetches merely to repair examples.
- [ ] On restore, treat absent or unknown-version recommendation payloads as requiring local recompute, even if restored preferences claim scoring version 5. Keep valid old backups readable. Use retained-summary coverage checks rather than globally dropping their scoring data; successful backfill converges through the same version marker.
- [ ] Add tests for bounded-pass version not advanced, queued active resync, failure/resume, correction fan-out, and old backup with missing payload and current marker. Test that all progress still reaches `ForegroundSyncController.onBackgroundRecalc*`.
- [ ] Update docs and complete checkpoint (`feat: backfill workout guidance locally`).

## Task 6: Dashboard card, visibility, and workout navigation

**Create:**
- `feature/dashboard/src/main/kotlin/app/readylytics/health/feature/dashboard/recommendation/WorkoutRecommendationCard.kt`
- `feature/dashboard/src/main/kotlin/app/readylytics/health/feature/dashboard/recommendation/WorkoutRecommendationPresentation.kt`
- `app/src/main/kotlin/app/readylytics/health/ui/scaffold/WorkoutRecommendationCardContent.kt`
- `feature/dashboard/src/test/kotlin/app/readylytics/health/feature/dashboard/recommendation/WorkoutRecommendationCardTest.kt`

**Modify:**
- `core/model/src/main/kotlin/app/readylytics/health/core/model/domain/dashboard/CardConfiguration.kt`
- `core/model/src/main/kotlin/app/readylytics/health/core/model/domain/dashboard/CardIdExtensions.kt`
- `core/model/src/main/kotlin/app/readylytics/health/core/model/domain/dashboard/DashboardCardCatalog.kt`
- `core/model/src/main/kotlin/app/readylytics/health/core/model/data/preferences/SettingsDefaults.kt`
- `feature/dashboard/src/main/kotlin/app/readylytics/health/feature/dashboard/CardIdExtensionsUi.kt`
- `feature/dashboard/src/main/kotlin/app/readylytics/health/feature/dashboard/CardManagementBottomSheet.kt`
- `feature/dashboard/src/main/kotlin/app/readylytics/health/feature/dashboard/DashboardCardFactory.kt`
- `feature/dashboard/src/main/kotlin/app/readylytics/health/feature/dashboard/DashboardCardDisplayHelpers.kt`
- `feature/dashboard/src/main/kotlin/app/readylytics/health/feature/dashboard/DashboardScreen.kt`
- `feature/dashboard/src/main/kotlin/app/readylytics/health/feature/dashboard/DashboardViewModel.kt`
- `app/src/main/kotlin/app/readylytics/health/ui/scaffold/DashboardNavDestinations.kt`
- `app/src/test/kotlin/app/readylytics/health/data/preferences/CardConfigurationRepositoryTest.kt`
- `app/src/main/res/values/strings.xml`

**Interfaces:** the app-owned content slot consumes the selected day's `WorkoutRecommendationSnapshot?` and resolves app strings with `stringResource`. The feature card consumes `WorkoutRecommendationPresentation` and `onWorkoutClick: (String) -> Unit`. It calculates no recommendation logic and performs no repository reads. Define these presentation models in the feature file:

```kotlin
data class WorkoutRecommendationExamplePresentation(
    val workoutId: String, val typeLabel: String,
    val recordedSessionDescription: String, val openWorkoutLabel: String,
)
data class WorkoutRecommendationPresentation(
    val title: String, val category: String, val explanation: String,
    val info: String, val infoToggleLabel: String,
    val examples: List<WorkoutRecommendationExamplePresentation>,
)
// @Composable fun WorkoutRecommendationCard(
//     presentation: WorkoutRecommendationPresentation, onWorkoutClick: (String) -> Unit)
```

- [ ] Add a Compose test that presents an Easy snapshot with one example and verifies the row sends its exact ID. Add tests that Rest has no example row and info text starts collapsed. Read expected strings from resources using the existing Compose test resource pattern.

```kotlin
// Robolectric runner and createComposeRule follow DashboardVisualizationRegressionTestBase.
@Test fun exampleOpensItsStableId() {
    var clicked: String? = null
    val row = WorkoutRecommendationExamplePresentation("run-42", "Run", "45 minutes", "Open recorded workout")
    val text = WorkoutRecommendationPresentation("Guidance", "Easy workout", "Low HRV", "Info", "About guidance", listOf(row))
    composeRule.setContent {
        MaterialTheme { WorkoutRecommendationCard(text) { clicked = it } }
    }
    composeRule.onNodeWithText("Run").performClick()
    assertEquals("run-42", clicked)
}
```
- [ ] Run `./gradlew :feature:dashboard:testDebugUnitTest --tests '*WorkoutRecommendationCardTest'`.
- [ ] Add `CardId.WORKOUT_RECOMMENDATION`, visible default position 23 (append without disturbing existing order). Use the bespoke Insights card path, not numeric gauge modes. Existing proto serializes card ID as a string, so no proto enum migration or second visibility preference is needed. Update exhaustive CardId mappings, keeping UI text resource-backed. Supply a localized `Map<CardId, String>` title override from the app to card management. Make `displayNameResId` nullable for the app-owned new title and resolve the override before the existing resource fallback in `CardManagementBottomSheet`; require a title for every card. The core displayName extension's new value is an internal fallback only, never the new card's UI label.
- [ ] Add an app-owned recommendation content slot alongside the existing `insightsCard` slot. `WorkoutRecommendationCardContent.kt` resolves all new app resources, maps stored states/reasons/example values to presentation text, then calls the feature card. This avoids an invalid feature → app dependency and avoids duplicate strings in feature resources. Existing workout-type labels can use the shared type-label helper/resources.
- [ ] Implement native M3 card and ListItem example rows. The info toggle uses `rememberSaveable` and does not enter the VM. Category, reasons, workout type, duration, historical average HR, click label, unavailable states, and info text all use resources.

```kotlin
ListItem(
    headlineContent = { Text(typeLabel) },
    supportingContent = { Text(recordedSessionDescription) },
    modifier = Modifier.clickable(onClickLabel = openWorkoutLabel) {
        onWorkoutClick(example.workoutId)
    },
)
```

- [ ] Add the callback through existing dashboard callback holders to avoid multiplying long parameter lists. In `DashboardNavDestinations` wire:

```kotlin
onWorkoutClick = { workoutId -> navController.navigate(AppDestination.WorkoutDetail(workoutId)) }
```

- [ ] Keep the dashboard back-stack entry and selected date intact; reuse the existing workout detail destination's popBackStack. Existing detail missing-record handling must display an unavailable state when a referenced workout was deleted, never navigate by approximate date/type. Add a regression test in the workout detail module if its loader currently lacks that behavior.
- [ ] Expose the selected summary payload directly through dashboard state. Keep new presentation types out of the 603-line ViewModel. Existing hide/restore/edit layout path persists the card configuration, including across backup/restore. New defaults must not re-show a user-hidden card.
- [ ] Verify one/three/no examples, unavailable states, info disclosure, average-HR omission, long localized text, accessibility, history date, tapping/back navigation, missing source, and default/hidden persistence. Complete checkpoint (`feat: show daily workout recommendations`).

## Task 7: Documentation, regression suite, and handoff

**Modify:**
- `ABOUT.md`
- `docs/about.md`, `docs/index.md`, `docs/privacy.md`
- `internal-docs/DATA_FLOW.md`
- `app/src/main/res/values/strings.xml` (`about_*`, `tooltip_*`, recommendation copy).
- `internal-docs/plans/2026-09-05-hrv-workout-recommendations-design.md` (approved replacement; numeric target stays deferred).
- `app/src/test/kotlin/app/readylytics/health/docs/DocumentationDriftTest.kt`

- [ ] Document the final behavior together: morning HRV/profile bounds, conservative caps, missing-data precedence, history backfill, optional visibility, distinct >15-minute past examples, and original-workout links.
- [ ] Include the research limitation and historical-HR distinction. Do not call these “safe,” “optimal,” or validated strength prescriptions. State that processing remains local and backups include derived recommendations; no cloud feature or telemetry is introduced.
- [ ] Add assertions to the existing documentation test using its existing file-reading setup; check required explanation concepts are present across ABOUT/site/in-app resources. Keep formula constants solely in their Kotlin source and link them from DATA_FLOW. Correct the stale DATA_FLOW schema heading (currently says v17 although code is v18) to the implemented v19.

- [ ] Add a targeted documentation assertion for the new recommendation section and `workout_recommendation_info` string. Verify the exact scope wording in ABOUT/site/in-app resources; pre-existing mentions elsewhere must not satisfy the test.
- [ ] Run the focused documentation suite, followed by the mandatory full checks:

```sh
./gradlew :app:testDebugUnitTest --tests '*DocumentationDriftTest'
./gradlew ktlintFormat
./gradlew detekt
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintRelease
```

- [ ] Run Room migration and navigation/device checks on an emulator or authorized debug installation. Never uninstall `app.readylytics.health`. If no device exists, record that limitation rather than claiming those checks passed.
- [ ] Run `codegraph index`, then `codegraph sync`, then `git diff --check`. Review all changed files, schema output, string coverage, and detekt baseline unchanged. Resolve failures before marking tasks complete.
- [ ] Complete final checkpoint (`docs: explain workout recommendations`) and report implementation, validation results, and any device-test limitation. Do not merge/publish/deploy without corresponding user authorization.

## Coverage and dependency review

Task order: **1 → 2 → 3 → 4 → 5 → 6 → 7**. Task 2 consumes Task 1 states; Task 3 consumes both; Task 4 owns storage; Task 5 owns durable history; Task 6 owns UI/navigation. No UI computes HRV rules or calls HC.

| Approved requirement | Task |
| --- | --- |
| Reuse HRV baseline/profile bounds, conservative sleep/fatigue, illness and missing data | 1, 3 |
| Morning selection, no later activity drift, corrected inputs, seven-day gate | 3 |
| Last 30 days, final classifications, >15 min, distinct types, most recent, max 3 | 2, 3 |
| Rest/no data has no examples; show fewer when fewer qualify | 1, 2, 6 |
| Stored historic guidance, migration, retries, restore, progress | 4, 5 |
| Default-visible/hideable card, info toggle, reasons, exact workout links | 6 |
| Evidence limits, synchronized documentation, mandatory verification | 7 |

Plan self-review: every approved design section maps to tasks above. New interfaces are declared in their producing tasks; no feature implementation has run as part of planning.
