package app.readylytics.health.feature.dashboard.recommendation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.readylytics.health.core.designsystem.dimens
import app.readylytics.health.core.designsystem.spacing
import app.readylytics.health.core.ui.components.MetricTooltip

/**
 * Dashboard card showing the day's HRV-guided workout recommendation.
 *
 * Purely presentational: it consumes [WorkoutRecommendationPresentation] (already-resolved,
 * localized strings) and an [onWorkoutClick] callback carrying the clicked example's stable
 * workout id. It calculates no recommendation logic, reads no `DailySummary` or
 * `WorkoutRecommendationSnapshot`, and imports nothing from `:app` — all of that mapping happens
 * in the app-owned content slot that calls this composable.
 *
 * The info action uses the same transient tooltip pattern as the other dashboard cards.
 */
@Composable
fun WorkoutRecommendationCard(
    presentation: WorkoutRecommendationPresentation,
    onWorkoutClick: (String) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(MaterialTheme.dimens.borderThin, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.medium, vertical = MaterialTheme.spacing.smallMedium),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = presentation.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                MetricTooltip(
                    description = presentation.info,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = presentation.category,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
            Text(
                text = presentation.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (presentation.examples.isNotEmpty()) {
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.smallMedium))
                Column {
                    presentation.examples.forEach { example ->
                        WorkoutRecommendationExampleRow(example = example, onWorkoutClick = onWorkoutClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutRecommendationExampleRow(
    example: WorkoutRecommendationExamplePresentation,
    onWorkoutClick: (String) -> Unit,
) {
    ListItem(
        headlineContent = { Text(example.typeLabel) },
        supportingContent = { Text(example.recordedSessionDescription) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = example.openWorkoutLabel) {
                    onWorkoutClick(example.workoutId)
                },
    )
}
