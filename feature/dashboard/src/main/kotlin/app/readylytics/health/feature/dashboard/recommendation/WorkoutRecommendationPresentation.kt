package app.readylytics.health.feature.dashboard.recommendation

/**
 * Presentation for a single past-workout example shown under a [WorkoutRecommendationPresentation].
 *
 * All fields are already-resolved display text; this type carries zero domain logic and performs
 * no repository reads. [workoutId] is the only non-display field — it is the stable identifier
 * echoed back through the card's click callback so the caller can navigate to that exact workout.
 */
data class WorkoutRecommendationExamplePresentation(
    val workoutId: String,
    val typeLabel: String,
    val recordedSessionDescription: String,
    val openWorkoutLabel: String,
)

/**
 * Presentation for the dashboard's HRV-guided workout recommendation card.
 *
 * This is a pure display model: every field is a resolved, localized string. The card that
 * consumes it ([WorkoutRecommendationCard]) performs no recommendation logic and reads no
 * `DailySummary`/`WorkoutRecommendationSnapshot` directly — mapping from the stored snapshot to
 * this presentation happens in the app-owned content slot (see `WorkoutRecommendationCardContent`
 * in the app module), which is the only place allowed to resolve app string resources for this
 * card.
 */
data class WorkoutRecommendationPresentation(
    val title: String,
    val category: String,
    val explanation: String,
    val info: String,
    val infoToggleLabel: String,
    val examples: List<WorkoutRecommendationExamplePresentation>,
)
