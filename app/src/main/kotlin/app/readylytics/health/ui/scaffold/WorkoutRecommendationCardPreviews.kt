package app.readylytics.health.ui.scaffold

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import app.readylytics.health.core.designsystem.FitDashboardTheme
import app.readylytics.health.core.designsystem.spacing
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationDecision
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationExample
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationReason
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationSnapshot
import app.readylytics.health.core.model.domain.recommendation.WorkoutRecommendationState
import app.readylytics.health.core.model.domain.scoring.WorkoutLoadLevel
import app.readylytics.health.feature.dashboard.recommendation.WorkoutRecommendationCard

@Preview(name = "Harder - light", showBackground = true, widthDp = 360)
@Preview(name = "Harder - dark", showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Harder - narrow 1.5x", showBackground = true, widthDp = 320, fontScale = 1.5f)
@Preview(name = "Harder - narrow 2x", showBackground = true, widthDp = 320, fontScale = 2f)
@Composable
internal fun HarderWorkoutRecommendationPreview() {
    RecommendationPreview(
        previewSnapshot(
            state = WorkoutRecommendationState.HARDER,
            reasons = listOf(WorkoutRecommendationReason.WITHIN_USUAL_RANGE),
            examples = listOf(previewExample("56", averageHr = 142f)),
        ),
    )
}

@Preview(name = "Easy - three examples, no HR", showBackground = true, widthDp = 360)
@Composable
internal fun EasyWorkoutRecommendationPreview() {
    RecommendationPreview(
        previewSnapshot(
            state = WorkoutRecommendationState.EASY,
            reasons = listOf(WorkoutRecommendationReason.HRV_LOW),
            examples = listOf(previewExample("56"), previewExample("8"), previewExample("73")),
        ),
    )
}

@Preview(name = "Rest - no examples", showBackground = true, widthDp = 360)
@Composable
internal fun RestWorkoutRecommendationPreview() {
    RecommendationPreview(
        previewSnapshot(WorkoutRecommendationState.REST, listOf(WorkoutRecommendationReason.SLEEP_LOW)),
    )
}

@Preview(name = "Not calculated", showBackground = true, widthDp = 320)
@Composable
internal fun NotCalculatedWorkoutRecommendationPreview() {
    RecommendationPreview(null)
}

@Preview(name = "Building baseline - 2x", showBackground = true, widthDp = 320, fontScale = 2f)
@Composable
internal fun BaselineWorkoutRecommendationPreview() {
    RecommendationPreview(previewSnapshot(WorkoutRecommendationState.NO_CIRCADIAN_BASELINE))
}

@Preview(name = "Dynamic colors", showBackground = true, widthDp = 360, apiLevel = 35)
@Composable
internal fun DynamicWorkoutRecommendationPreview() {
    RecommendationPreview(
        previewSnapshot(
            state = WorkoutRecommendationState.HARDER,
            reasons = listOf(WorkoutRecommendationReason.WITHIN_USUAL_RANGE),
            examples = listOf(previewExample("56", averageHr = 142f)),
        ),
        dynamicColor = true,
    )
}

@Composable
private fun RecommendationPreview(
    snapshot: WorkoutRecommendationSnapshot?,
    dynamicColor: Boolean = false,
) {
    FitDashboardTheme(dynamicColor = dynamicColor) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(MaterialTheme.spacing.medium),
        ) {
            WorkoutRecommendationCard(buildWorkoutRecommendationPresentation(snapshot, LocalContext.current)) {}
        }
    }
}

private fun previewSnapshot(
    state: WorkoutRecommendationState,
    reasons: List<WorkoutRecommendationReason> = emptyList(),
    examples: List<WorkoutRecommendationExample> = emptyList(),
) = WorkoutRecommendationSnapshot(
    wakeSessionId = "preview-sleep",
    wakeTimeMs = 86_400_000L,
    decision = WorkoutRecommendationDecision(state, reasons),
    examples = examples,
)

private fun previewExample(
    exerciseType: String,
    averageHr: Float? = null,
) = WorkoutRecommendationExample(
    workoutId = "preview-$exerciseType",
    exerciseType = exerciseType,
    startTimeMs = 0L,
    endTimeMs = 3_960_000L,
    durationMinutes = 66,
    averageHr = averageHr,
    finalLoad = if (averageHr == null) WorkoutLoadLevel.LIGHT else WorkoutLoadLevel.HARD,
)
