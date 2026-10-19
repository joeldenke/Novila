package se.novila.feature.today.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.feature.today.TodayScreen
import se.novila.feature.today.TodayViewModel

/** The Today tab: what to do today, and how the day and week look. */
@Serializable data object TodayRoute : NavKey

/**
 * Today's entry, with its own [TodayViewModel] scoped to the entry. The other tabs and Settings are opened through
 * the callbacks, so this module never depends on another feature.
 */
fun EntryProviderScope<NavKey>.todayEntry(
    onOpenTraining: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenSettings: () -> Unit,
    contentPadding: PaddingValues,
) {
    entry<TodayRoute> {
        val viewModel = metroViewModel<TodayViewModel>()
        // Read again on resume so "today" is correct after midnight.
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        // Nothing to show until the saved days are read, a moment at app start.
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        val movement by viewModel.movement.collectAsStateWithLifecycle()
        TodayScreen(
            today = state.week.today,
            history = state.week.history,
            hour = state.hour,
            name = state.name,
            goals = state.goals,
            sleep = state.sleep,
            coach = state.coach,
            movement = movement,
            onAddWater = viewModel::addWater,
            onOpenTraining = onOpenTraining,
            onOpenSleep = onOpenSleep,
            onOpenNutrition = onOpenNutrition,
            onOpenSettings = onOpenSettings,
            contentPadding = contentPadding,
            training = state.training,
            connections = state.connections,
        )
    }
}
