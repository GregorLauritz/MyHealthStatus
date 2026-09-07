package app.readylytics.health.feature.workouts

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.readylytics.health.core.designsystem.FitDashboardTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression coverage for a workout that was deleted (or never existed) after the dashboard/
 * workout list already navigated to its detail screen. [WorkoutDetailViewModel.loadWorkout]
 * already has a controlled-state regression test (`unknown workout ID produces controlled error
 * state and never throws`): `workout == null` + `isLoading == false`. This covers the screen layer
 * built on top of that state — it must show an explicit unavailable message, never a blank body
 * and never a mismatched-record navigation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutDetailMissingRecordTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun missingWorkoutShowsUnavailableStateInsteadOfABlankScreen() {
        composeRule.setContent {
            FitDashboardTheme {
                WorkoutDetailScreen(
                    uiState = WorkoutDetailUiState(workout = null, isLoading = false),
                )
            }
        }

        composeRule.onNodeWithText("This workout is no longer available.").assertIsDisplayed()
    }
}
