package se.novila.feature.today.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.feature.today.DiscoveriesScreen
import se.novila.feature.today.DiscoveriesViewModel
import se.novila.feature.today.R
import se.novila.feature.today.TodayScreen
import se.novila.feature.today.TodayViewModel
import se.novila.feature.today.TrendsScreen
import se.novila.feature.today.TrendsViewModel
import se.novila.navigation.Navigator

/** The Today tab: what to do today, and how the day and week look. */
@Serializable data object TodayRoute : NavKey

/** Everything the logs say: the week's insights, what the nights say and the connections between the parts. */
@Serializable data object DiscoveriesRoute : NavKey

/** Sleep, food and training over the same range, with what the trends say together. */
@Serializable data object TrendsRoute : NavKey

/**
 * Today and the discoveries and trends opened from it, each with its own ViewModel scoped to the entry. Navigation
 * inside the tab goes through [navigator]; the other tabs and Settings are opened through the callbacks, so this
 * module never depends on another feature.
 */
fun EntryProviderScope<NavKey>.todayEntries(
    navigator: Navigator,
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
        val setupIncomplete = viewModel.setupIncomplete.collectAsStateWithLifecycle().value
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
            onOpenDiscoveries = { navigator.navigate(DiscoveriesRoute) },
            onOpenTrends = { navigator.navigate(TrendsRoute) },
            upcoming = state.upcoming,
            setupIncomplete = setupIncomplete,
            onCompleteSetup = viewModel::openSetup,
        )
    }
    entry<DiscoveriesRoute> {
        val viewModel = metroViewModel<DiscoveriesViewModel>()
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        DiscoveriesScreen(
            insights = state.insights,
            sleep = state.sleep,
            connections = state.connections,
            contentPadding = contentPadding,
        )
    }
    entry<TrendsRoute> {
        val viewModel = metroViewModel<TrendsViewModel>()
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        TrendsScreen(
            history = state.history,
            goals = state.goals,
            feedback = state.feedback,
            connections = state.connections,
            onRange = viewModel::selectRange,
            contentPadding = contentPadding,
        )
    }
}

/** The top bar title of a screen inside the Today tab, or null when [route] is not one of them. */
@Composable
fun todayDetailTitle(route: NavKey?): String? = when (route) {
    DiscoveriesRoute -> stringResource(R.string.discoveries_title)
    TrendsRoute -> stringResource(R.string.trends_title)
    else -> null
}
