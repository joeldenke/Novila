package se.novila.feature.sleep.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.feature.sleep.R
import se.novila.feature.sleep.SleepActions
import se.novila.feature.sleep.SleepCoachScreen
import se.novila.feature.sleep.SleepCoachViewModel
import se.novila.feature.sleep.SleepScreen
import se.novila.feature.sleep.SleepTrendsScreen
import se.novila.feature.sleep.SleepTrendsViewModel
import se.novila.feature.sleep.SleepViewModel
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.rememberHealthPermissionRequest
import se.novila.navigation.Navigator
import se.novila.navigation.SharedScreen
import se.novila.navigation.sharedScreen

/** The Sleep tab. */
@Serializable data object SleepRoute : NavKey

/** The sleep coach: bedtime and wake-up goals, the wind-down routine and what the nights suggest. */
@Serializable data object SleepCoachRoute : NavKey

/** The sleep over a week, four weeks or a year, and what the logs say about it. */
@Serializable data object SleepTrendsRoute : NavKey

/**
 * The Sleep tab and the screens opened from it. Each entry gets its own ViewModel from Metro, scoped to the entry
 * (`metroViewModel()`), so it lives as long as the entry is on the back stack. Navigation inside the tab goes
 * through [navigator].
 */
fun EntryProviderScope<NavKey>.sleepEntries(
    navigator: Navigator,
    contentPadding: PaddingValues,
    onOpenBreathing: () -> Unit,
) {
    entry<SleepRoute> {
        val viewModel = metroViewModel<SleepViewModel>()
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        // Nothing to show until the saved days are read, a moment at app start.
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        val context = LocalContext.current
        val connectHealth = rememberHealthPermissionRequest(viewModel::onHealthPermissionsResult)
        SleepScreen(
            day = state.day,
            history = state.history,
            goals = state.goals,
            coach = state.coach,
            actions = SleepActions(
                onSleep = viewModel::setSleep,
                onBedtime = viewModel::setBedtime,
                onWakeTime = viewModel::setWakeTime,
                onQuality = viewModel::setQuality,
                onFactor = viewModel::setFactor,
                onSelectDay = viewModel::selectDay,
                onConnectHealth = connectHealth,
                onOpenHealthSettings = { HealthConnectSource.openSettings(context) },
                onOpenCoach = { navigator.navigate(SleepCoachRoute) },
                onOpenTrends = { navigator.navigate(SleepTrendsRoute) },
                onOpenBreathing = onOpenBreathing,
            ),
            contentPadding = contentPadding,
            today = state.today,
            watch = state.watch,
            fromWatch = state.fromWatch,
        )
    }
    entry<SleepCoachRoute>(metadata = sharedScreen(SharedScreen.SleepCoach)) {
        val viewModel = metroViewModel<SleepCoachViewModel>()
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        // Nothing to show until the days, goals and answers are read, a moment at app start.
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        SleepCoachScreen(
            state = state,
            onChange = viewModel::change,
            onStart = viewModel::start,
            onReset = viewModel::reset,
            onOpenBreathing = onOpenBreathing,
            contentPadding = contentPadding,
        )
    }
    entry<SleepTrendsRoute>(metadata = sharedScreen(SharedScreen.SleepTrends)) {
        val viewModel = metroViewModel<SleepTrendsViewModel>()
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        SleepTrendsScreen(
            history = state.history,
            goals = state.goals,
            analysis = state.analysis,
            regularity = state.regularity,
            onRange = viewModel::selectRange,
            contentPadding = contentPadding,
        )
    }
}

/** The top bar title of a screen inside the Sleep tab, or null when [route] is not one of them. */
@Composable
fun sleepDetailTitle(route: NavKey?): String? = when (route) {
    SleepCoachRoute -> stringResource(R.string.sleep_coach_title)
    SleepTrendsRoute -> stringResource(R.string.sleep_entry_trends)
    else -> null
}
