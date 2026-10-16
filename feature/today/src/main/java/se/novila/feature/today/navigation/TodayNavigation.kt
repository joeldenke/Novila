package se.novila.feature.today.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import se.novila.feature.today.TodayScreen
import se.novila.model.DayEntry
import se.novila.movement.MovementState

/** The Today tab: the dashboard. */
@Serializable data object TodayRoute : NavKey

/**
 * Today's entry. The other tabs are opened through the callbacks, so this module never depends on another feature.
 */
fun EntryProviderScope<NavKey>.todayEntry(
    today: DayEntry,
    history: List<DayEntry>,
    hour: Int,
    movement: MovementState,
    onMood: (Int) -> Unit,
    onOpenTraining: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenNutrition: () -> Unit,
    contentPadding: PaddingValues,
) {
    entry<TodayRoute> {
        TodayScreen(
            today = today,
            history = history,
            hour = hour,
            movement = movement,
            onMood = onMood,
            onOpenTraining = onOpenTraining,
            onOpenSleep = onOpenSleep,
            onOpenNutrition = onOpenNutrition,
            contentPadding = contentPadding,
        )
    }
}
