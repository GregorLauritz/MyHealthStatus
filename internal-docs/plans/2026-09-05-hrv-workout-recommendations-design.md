# Daily HRV-guided workout recommendations

Date: 2026-09-05

Status: Written design approved by the user on 2026-09-05; implementation planning authorized.

## Purpose and evidence

Replace the deferred numeric Target Strain concept with a daily, qualitative workout
recommendation. The card helps users choose workout intensity and offers matching
examples from their own workout history. It does not generate a TRIMP budget, new
session durations, heart-rate targets, or a training schedule.

The evidence and deferral rationale are recorded in the original Target Strain concept
history and in this design. Morning HRV relative to an individual
baseline has been studied for timing harder versus easier endurance sessions. Those
studies do not validate this app's combined rules, its profile thresholds, or application
to other workout types. Present the result as guidance, without safety guarantees or
claims of optimal training dose.

## Agreed product behavior

- A dedicated dashboard card is visible by default, with persistent hide/restore
  controls following the Insights pattern.
- The card supports today and historical dashboard dates.
- Categories are `Rest`, `Easy workout`, and `Ready for a harder workout`.
- Show a short explanation containing all contributing limiting reasons.
- An expandable info toggle says: “Based on research into endurance training. This
  guidance may also help you judge effort for other workouts, but it has not been
  validated for those activities.”
- Store categories and reason identifiers, not localized prose. All displayed text
  belongs in `app/src/main/res/values/strings.xml` and follows existing resource wiring.
- Use native M3 components, dynamic theme colors, `MaterialTheme.shapes.large`, and
  explicit surface-container roles. The info toggle's expansion is ephemeral UI state;
  the card's visibility is a persistent preference.

## Morning snapshot

For the selected date, choose the recorded sleep session whose end is closest to the
usual circadian wake-up time for that date. Reuse the existing circadian baseline
system. Do not substitute a fixed wake time when recorded sleep is absent, and do not
invent a usual wake time when the circadian baseline is unavailable.

The recommendation uses only recovery inputs through the selected session's end.
Compare that night's average HRV against the existing personal baseline and variability
calculation. Reuse the active profile's existing low/high HRV deviation thresholds.
Unusually high HRV is a limiting signal, not an automatic harder-workout recommendation.

Compute sleep score and residual fatigue for the morning snapshot using existing
calculations. Later supplemental sleep and workouts may change ordinary daily metrics
but do not change this snapshot. Corrections to the morning inputs do revise the
recommendation. Ordinary passage of time does not revise it.

The existing daily sleep pipeline aggregates core segments and supplemental sleep.
The new snapshot therefore needs an explicitly time-bounded input path; reading the
final daily sleep score or live fatigue from the dashboard would violate this contract.
Existing scoring formulas and ordinary daily sleep aggregation retain their semantics.

## Availability and recommendation rules

Required availability checks precede recommendation categories:

- No recorded sleep: recommendation unavailable with a missing-sleep explanation.
- No usable nightly HRV: `No HRV available`, including when illness is flagged.
- Fewer than seven days under the existing calibration system: `Calibrating`.
- No usable circadian wake baseline: `Not enough sleep history`.
- Required HRV baseline or variability unavailable: no workout category; explain the
  missing baseline rather than interpreting a missing value as normal HRV.

Once required data is available, evaluate these rules:

| Condition | Result |
| --- | --- |
| Existing possible-illness flag (`ILLNESS_ONSET`) | Rest |
| HRV below the profile's low bound or above its high bound | Easy workout |
| Morning sleep score below 60 | Easy workout |
| Morning residual fatigue above 70 × configured fatigue gain | Easy workout |
| Sleep score or morning fatigue unavailable | Easy workout, with missing-input reason |
| None of the above | Ready for a harder workout |

Apply the existing threshold comparisons at their boundaries. A score of 60 does not
trigger the sleep cap, and fatigue exactly equal to 70 × gain does not trigger the
fatigue cap. Illness overrides available-data workout categories. Informational or
positive recovery flags must not override a limiting rule.

The existing `STRONG_RECOVERY_SIGNAL` can coexist with high HRV; it does not cancel
the recommendation's agreed high-HRV cap. The info explanation must make the difference
between a recovery signal and workout guidance understandable.

## Workout examples from recent history

Show up to three matching recorded workouts from the preceding 30 days:

- `Easy workout`: final history classification is Very light or Light.
- `Ready for a harder workout`: final history classification is Moderate, Hard, or
  Very hard.
- `Rest` and all unavailable/calibrating states show no workout examples.
- Sessions must be longer than 15 minutes.
- Choose the most recent qualifying session for each distinct workout type.
- If more than three types qualify, show the three whose qualifying sessions are
  most recent. If fewer qualify, show only the available matches.
- With no matches, keep the general recommendation without invented examples.

Reuse `WorkoutLoadClassifier` and the history UI's `finalLoad`, including its intensity
promotions. Do not independently classify only total TRIMP or replace the existing
workout-type identity with a new taxonomy.

The examples describe actual recorded sessions, including their type, duration, and
recorded average heart rate when available. An average heart rate is historical context,
not a prescribed target. Low recorded cardiovascular load does not establish low
musculoskeletal demand for strength workouts; the info explanation must communicate
that limitation alongside the endurance-research scope.

Examples belong to the morning snapshot. Historical dates use their own preceding
30-day window, and workouts after the morning anchor cannot enter the examples.
Corrected or deleted source workouts must be reflected when the snapshot is recomputed.

Each example is tappable and opens that original workout's existing detail screen via
`AppDestination.WorkoutDetail(workoutId)`. Persist the stable workout ID for navigation;
do not look up a session by its displayed type, duration, or date. Back navigation returns
to the dashboard's selected date. The row needs an accessible click label. If the source
workout has been removed since the recommendation was stored, handle the missing record
without a crash or navigation to a different workout.

## Architecture and persistence

Use the existing Room → repository → ViewModel → Compose flow. Health Connect remains
ingestion-only. ViewModels expose StateFlow/SharedFlow and Compose collects lifecycle-aware.

Separate responsibilities:

1. A morning-input assembler resolves the selected date's sleep anchor, baseline,
   profile thresholds, sleep score, fatigue, and recovery flags from retained local data.
2. A pure-Kotlin recommendation evaluator receives an explicit input object and returns
   a category or unavailable state, plus explanation reason identifiers. A separate
   pure-Kotlin history selector filters, groups, and ranks eligible workout examples.
3. Daily scoring persists the recommendation alongside the daily summary atomically.
4. A presentation mapper resolves localized category/reason strings for the dashboard.

Persist the recommendation state, explanation reasons, wake-up anchor, rule version,
and selected workout references with the historical values needed to present examples.
Historical computation uses date-appropriate inputs and the existing frozen baseline/
profile semantics. It must not substitute today's live fatigue or today's baseline.
Recomputation replaces the result for the same date idempotently.

Relevant integration points include `ReadinessSummaryCoordinator`,
`ScoringRepositoryImpl`, `CurrentNightHrvResolver`, `CircadianConsistencyRepository`,
`DailySummary`, `DailySummaryEntity`, and `DailySummaryMapper`. The entity lives in
`:core:database-schema`; migration wiring lives in `:core:database`.

Extract the existing residual-fatigue classification thresholds into a shared pure
definition so the card and recommendation do not acquire independent 70 × gain rules.
Reuse existing HRV and sleep threshold definitions without changing their values.

## Migration, historical backfill, and recovery

Add a non-destructive Room migration. Existing summaries initially have no calculated
recommendation. Do not display an absent migrated value as a successful recommendation.

On upgrade, automatically schedule durable recalculation of retained local history.
Use the existing `HealthResyncWorker` recompute-only infrastructure, retention bounds,
serialization, and shared progress banner/notification path. This work needs no Health
Connect fetch. Do not widen pull-to-refresh beyond its current-day contract.

Backfill is idempotent, cancellable, and retryable. Mark its version complete only after
successful completion; a killed worker must not permanently skip outstanding dates.
Existing unique-work scheduling must not lose the pending backfill when another job
already occupies the queue. Preserve valid prior data on failure.

Historical days lacking retained inputs remain explicitly unavailable. Follow the
existing tier reconstruction contract; do not claim bit-identical reconstruction from
aggregated history. Old backups must remain readable with absent recommendation fields;
restored data participates in the same versioned local recalculation path.

## Verification and documentation

Pure unit tests cover category precedence, all threshold boundaries, non-finite/missing
inputs, seven-day calibration, missing circadian history, missing HRV with illness,
multiple limiting reasons, and the high-HRV/strong-recovery combination.

Integration tests cover session selection, time-zone/date boundaries, morning-only
inputs, corrections, unchanged recommendations after later naps/workouts, atomic storage,
historical frozen inputs, migration, backfill retry, and backup compatibility. UI checks
cover current/history dates, default visibility, hide/restore persistence, info expansion,
and unavailable states. History-selector tests cover the 30-day boundary, exclusion of
15-minute sessions, classification promotions, distinct types, most-recent selection,
ordering, zero-to-three matches, exclusion of workouts after the morning anchor, and
absence of examples for Rest and unavailable/calibrating states.
Navigation checks cover opening the exact referenced workout, returning to the selected
dashboard date, and a source workout removed after the example was stored.

Update `ABOUT.md`, `docs/about.md`, applicable public site pages, the relevant sections of
`internal-docs/DATA_FLOW.md`, and in-app explanation strings together during implementation.
Keep formulas in pure Kotlin and reference them from data-flow documentation. Run the
documentation drift tests and repository checks:

```sh
./gradlew ktlintFormat
./gradlew detekt
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintRelease
```

Run `codegraph index` after file creation and `codegraph sync` after structural changes.
No scoring suppression or detekt baseline expansion is authorized by this design.

## Scope boundary

This design introduces qualitative daily guidance and its stored history. Numeric
Target Strain remains deferred. Workout planning, notifications, generated session
prescriptions, and changes to existing scoring coefficients are outside this change.

The next artifact is the implementation plan, after review of this written design.
