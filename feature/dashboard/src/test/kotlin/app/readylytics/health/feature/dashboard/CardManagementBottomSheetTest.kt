package app.readylytics.health.feature.dashboard

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.readylytics.health.core.model.domain.dashboard.CardConfiguration
import app.readylytics.health.core.model.domain.dashboard.CardId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Exercises CardManagementBottomSheet's real title-resolution precedence
 * (`titleOverrides[card.cardId] ?: card.cardId.displayNameResId?.let { stringResource(it) }
 * ?: card.cardId.displayName()`) directly and unmocked.
 *
 * CardId.WORKOUT_RECOMMENDATION's core `displayName()` fallback and its real app-supplied
 * override (`app/strings.xml`'s `card_title_workout_recommendation`) both happen to be the
 * English string "Workout Recommendation", so no test asserting on that literal text could ever
 * distinguish "the override reached here" from "the override silently broke and we fell all the
 * way back to the core fallback". These tests use a value that could never coincidentally match
 * either fallback, so they would fail if the `titleOverrides` precedence were ever dropped or
 * reordered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CardManagementBottomSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val workoutRecommendationCard =
        listOf(CardConfiguration(cardId = CardId.WORKOUT_RECOMMENDATION, isVisible = true, position = 0))

    @Test
    fun titleOverrideTakesPrecedenceOverTheCoreFallback() {
        composeRule.setContent {
            CardManagementBottomSheet(
                cards = workoutRecommendationCard,
                onCardVisibilityChanged = { _, _ -> },
                onCardDisplayModeChanged = { _, _ -> },
                onResetToDefaults = {},
                onDismiss = {},
                sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
                titleOverrides = mapOf(CardId.WORKOUT_RECOMMENDATION to "TEST_OVERRIDE_TITLE"),
            )
        }

        // If the override wiring were silently broken anywhere, this would render
        // CardId.WORKOUT_RECOMMENDATION.displayName()'s hardcoded "Workout Recommendation"
        // instead — this distinguishable value proves the override path is actually live.
        composeRule.onNodeWithText("TEST_OVERRIDE_TITLE").assertIsDisplayed()
    }

    @Test
    fun coreFallbackIsUsedOnlyWhenNoOverrideIsSupplied() {
        composeRule.setContent {
            CardManagementBottomSheet(
                cards = workoutRecommendationCard,
                onCardVisibilityChanged = { _, _ -> },
                onCardDisplayModeChanged = { _, _ -> },
                onResetToDefaults = {},
                onDismiss = {},
                sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden),
                // titleOverrides defaults to emptyMap() — no entry for WORKOUT_RECOMMENDATION.
            )
        }

        // WORKOUT_RECOMMENDATION has no feature-owned displayNameResId (it is null, by design —
        // see CardIdExtensionsUi.kt), so with no override this must fall through all the way to
        // the core CardId.displayName() internal fallback.
        composeRule.onNodeWithText("Workout Recommendation").assertIsDisplayed()
    }
}
