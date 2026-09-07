package app.readylytics.health.feature.dashboard.recommendation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class WorkoutRecommendationCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun example(
        workoutId: String = "run-42",
        typeLabel: String = "Run",
        recordedSessionDescription: String = "45 minutes",
        openWorkoutLabel: String = "Open recorded workout",
    ) = WorkoutRecommendationExamplePresentation(workoutId, typeLabel, recordedSessionDescription, openWorkoutLabel)

    private fun presentation(
        title: String = "Guidance",
        category: String = "Easy workout",
        explanation: String = "Low HRV",
        info: String = "Info",
        examples: List<WorkoutRecommendationExamplePresentation> = emptyList(),
    ) = WorkoutRecommendationPresentation(title, category, explanation, info, examples)

    @Test
    fun exampleOpensItsStableId() {
        var clicked: String? = null
        val row = WorkoutRecommendationExamplePresentation("run-42", "Run", "45 minutes", "Open recorded workout")
        val text =
            WorkoutRecommendationPresentation(
                "Guidance",
                "Easy workout",
                "Low HRV",
                "Info",
                listOf(row),
            )
        composeRule.setContent {
            MaterialTheme { WorkoutRecommendationCard(text) { clicked = it } }
        }
        composeRule.onNodeWithText("Run").performClick()
        assertEquals("run-42", clicked)
    }

    @Test
    fun restStateHasNoExampleRow() {
        val text = presentation(category = "Rest", examples = emptyList())
        composeRule.setContent {
            MaterialTheme { WorkoutRecommendationCard(text) {} }
        }
        composeRule.onNodeWithText("Run").assertDoesNotExist()
    }

    @Test
    fun infoTextStartsCollapsed() {
        val text = presentation(info = "Detailed explanation of the guidance rules.")
        composeRule.setContent {
            MaterialTheme { WorkoutRecommendationCard(text) {} }
        }
        composeRule.onNodeWithText("Detailed explanation of the guidance rules.").assertDoesNotExist()
    }

    @Test
    fun informationToggleShowsTheInfoText() {
        val text = presentation(info = "Detailed explanation of the guidance rules.")
        composeRule.setContent {
            MaterialTheme { WorkoutRecommendationCard(text) {} }
        }

        composeRule.onNodeWithContentDescription("More information").performClick()
        composeRule.onNodeWithText("Detailed explanation of the guidance rules.").assertIsDisplayed()
    }

    @Test
    fun infoActionAllocatesAnAccessibleTouchTarget() {
        composeRule.setContent {
            MaterialTheme { WorkoutRecommendationCard(presentation()) {} }
        }

        composeRule
            .onNodeWithContentDescription("More information")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun absentSupportingTextDoesNotLeaveEmptySemantics() {
        val text = presentation(explanation = "", examples = listOf(example(recordedSessionDescription = "")))
        composeRule.setContent {
            MaterialTheme { WorkoutRecommendationCard(text) {} }
        }

        composeRule.onNodeWithText("", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("Run").assertHasClickAction().assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun rendersUpToThreeExamples() {
        val clicked = mutableListOf<String>()
        val text =
            presentation(
                examples =
                    listOf(
                        example(workoutId = "run-1", typeLabel = "Run"),
                        example(workoutId = "ride-1", typeLabel = "Cycling"),
                        example(workoutId = "swim-1", typeLabel = "Swim"),
                    ),
            )
        composeRule.setContent {
            MaterialTheme { WorkoutRecommendationCard(text) { clicked += it } }
        }

        composeRule.onNodeWithText("Run").assertIsDisplayed()
        composeRule.onNodeWithText("Cycling").assertIsDisplayed()
        composeRule.onNodeWithText("Swim").assertIsDisplayed()
        composeRule.onNodeWithText("Run").performClick()
        composeRule.onNodeWithText("Cycling").performClick()
        composeRule.onNodeWithText("Swim").performClick()
        assertEquals(listOf("run-1", "ride-1", "swim-1"), clicked)
        // Three workout actions plus info; decorative affordances must not add actions.
        composeRule.onAllNodes(hasClickAction()).assertCountEquals(4)
    }

    @Test
    fun noExamplesRendersNoExampleRow() {
        val text = presentation(examples = emptyList())
        composeRule.setContent {
            MaterialTheme { WorkoutRecommendationCard(text) {} }
        }
        composeRule.onNodeWithText("45 minutes").assertDoesNotExist()
    }

    @Test
    fun exampleRowIsClickableAndCarriesAccessibilityLabel() {
        val row = example(openWorkoutLabel = "Open recorded Run workout")
        val text = presentation(examples = listOf(row))
        composeRule.setContent {
            MaterialTheme { WorkoutRecommendationCard(text) {} }
        }

        val node = composeRule.onNodeWithText("Run")
        node.assertHasClickAction()
        val label =
            node
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsActions.OnClick)
                ?.label
        assertEquals("Open recorded Run workout", label)
    }

    @Test
    fun unavailableCategoryAndExplanationAreDisplayedVerbatim() {
        val text =
            presentation(
                category = "Recommendation not calculated yet",
                explanation = "We haven't computed today's guidance yet.",
                examples = emptyList(),
            )
        composeRule.setContent {
            MaterialTheme { WorkoutRecommendationCard(text) {} }
        }

        composeRule.onNodeWithText("Recommendation not calculated yet").assertIsDisplayed()
        composeRule.onNodeWithText("We haven't computed today's guidance yet.").assertIsDisplayed()
    }

    @Test
    fun longLocalizedTextDoesNotCrashAndIsDisplayed() {
        val longExplanation =
            "This is an unusually long localized explanation string meant to exercise wrapping " +
                "behavior across multiple lines within the card without truncation or a crash, " +
                "simulating a verbose translation in another language."
        val text = presentation(explanation = longExplanation)
        composeRule.setContent {
            MaterialTheme { WorkoutRecommendationCard(text) {} }
        }
        composeRule.onNodeWithText(longExplanation).assertIsDisplayed()
    }

    @Test
    fun clickedIdIsNullBeforeAnyClick() {
        var clicked: String? = null
        val text = presentation(examples = listOf(example()))
        composeRule.setContent {
            MaterialTheme { WorkoutRecommendationCard(text) { clicked = it } }
        }
        assertNull(clicked)
    }
}
