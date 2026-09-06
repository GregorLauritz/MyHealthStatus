package app.readylytics.health.feature.dashboard

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.readylytics.health.core.model.domain.dashboard.CardConfiguration
import app.readylytics.health.core.model.domain.dashboard.CardId
import app.readylytics.health.core.model.domain.dashboard.DashboardCardCatalog
import app.readylytics.health.core.model.domain.dashboard.DashboardCardDisplayMode
import app.readylytics.health.core.model.domain.dashboard.displayName
import app.readylytics.health.core.ui.components.ManagementBottomSheet
import app.readylytics.health.core.ui.components.ManagementItem
import app.readylytics.health.core.ui.components.ManagementSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardManagementBottomSheet(
    cards: List<CardConfiguration>,
    onCardVisibilityChanged: (CardId, Boolean) -> Unit,
    onCardDisplayModeChanged: (CardId, DashboardCardDisplayMode) -> Unit,
    onResetToDefaults: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier,
    // App-owned titles for cards whose UI label cannot live in a feature-module resource (e.g. a
    // card whose title is defined in the app module). Checked before the feature resource
    // fallback below; every card is guaranteed a title by the final displayName() fallback.
    titleOverrides: Map<CardId, String> = emptyMap(),
) {
    ManagementBottomSheet(
        title = stringResource(R.string.manage_cards),
        sections =
            listOf(
                ManagementSection(
                    title = stringResource(R.string.manage_cards),
                    items =
                        cards.sortedBy { it.position }.map { card ->
                            ManagementItem(
                                key = "card_${card.cardId.name}",
                                label =
                                    titleOverrides[card.cardId]
                                        ?: card.cardId.displayNameResId?.let { stringResource(it) }
                                        ?: card.cardId.displayName(),
                                isVisible = card.isVisible,
                                supportedModes = DashboardCardCatalog.spec(card.cardId)?.supportedModes.orEmpty(),
                                requestedMode = DashboardCardCatalog.requestedMode(card),
                                onVisibilityChanged = { onCardVisibilityChanged(card.cardId, it) },
                                onDisplayModeChanged = { onCardDisplayModeChanged(card.cardId, it) },
                            )
                        },
                ),
            ),
        onResetToDefaults = onResetToDefaults,
        onDismiss = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
    )
}
