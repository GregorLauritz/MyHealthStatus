package app.readylytics.health.feature.dashboard.recommendation

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Presentation for a single past-workout example shown under a [WorkoutRecommendationPresentation].
 *
 * Display text and the optional activity icon are already resolved; this type carries zero domain logic and performs
 * no repository reads. [workoutId] is the only non-display field — it is the stable identifier
 * echoed back through the card's click callback so the caller can navigate to that exact workout.
 */
data class WorkoutRecommendationExamplePresentation(
    val workoutId: String,
    val typeLabel: String,
    val recordedSessionDescription: String,
    val openWorkoutLabel: String,
    val activityIcon: ImageVector? = null,
)

/**
 * Presentation for the dashboard's HRV-guided workout recommendation card.
 *
 * This is a pure display model: it contains localized strings and example display models. The card that
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
    val examples: List<WorkoutRecommendationExamplePresentation>,
)
