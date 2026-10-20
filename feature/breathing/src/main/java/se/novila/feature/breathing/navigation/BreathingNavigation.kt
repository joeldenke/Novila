package se.novila.feature.breathing.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import kotlinx.serialization.Serializable
import se.novila.breathing.BreathingCatalog
import se.novila.feature.breathing.BreathingActions
import se.novila.feature.breathing.BreathingScreen
import se.novila.feature.breathing.BreathingSessionScreen
import se.novila.feature.breathing.BreathingSessionViewModel
import se.novila.feature.breathing.R
import se.novila.feature.breathing.sessionName
import se.novila.navigation.Navigator

/** Breathing and relaxation: the list of guided sessions. */
@Serializable data object BreathingRoute : NavKey

/** One guided session, in [sessionId] from `BreathingCatalog`. */
@Serializable data class BreathingSessionRoute(val sessionId: String) : NavKey

/**
 * Breathing and relaxation and the sessions opened from it. The list is only data; the session gets its own ViewModel
 * from Metro, scoped to the entry (`assistedMetroViewModel()`), so the timer lives as long as the entry is on the back
 * stack and stops when it is popped.
 */
fun EntryProviderScope<NavKey>.breathingEntries(navigator: Navigator, contentPadding: PaddingValues) {
    entry<BreathingRoute> {
        BreathingScreen(
            actions = BreathingActions(onOpenSession = { navigator.navigate(BreathingSessionRoute(it)) }),
            contentPadding = contentPadding,
        )
    }
    entry<BreathingSessionRoute> { route ->
        val viewModel = assistedMetroViewModel<BreathingSessionViewModel, BreathingSessionViewModel.Factory> {
            create(route.sessionId)
        }
        val state by viewModel.state.collectAsStateWithLifecycle()
        BreathingSessionScreen(
            state = state,
            actions = BreathingActions(onToggle = viewModel::toggle, onRestart = viewModel::restart),
            contentPadding = contentPadding,
        )
    }
}

/** The top bar title of a screen inside Breathing and relaxation, or null when [route] is not one of them. */
@Composable
fun breathingDetailTitle(route: NavKey?): String? = when (route) {
    BreathingRoute -> stringResource(R.string.breathing_title)
    is BreathingSessionRoute -> {
        val session = BreathingCatalog.session(route.sessionId) ?: BreathingCatalog.calm
        stringResource(sessionName(session))
    }
    else -> null
}
