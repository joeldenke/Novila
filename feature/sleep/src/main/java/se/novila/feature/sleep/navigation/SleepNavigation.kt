package se.novila.feature.sleep.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.feature.sleep.SleepActions
import se.novila.feature.sleep.SleepScreen
import se.novila.feature.sleep.SleepViewModel
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.rememberHealthPermissionRequest

/** The Sleep tab. */
@Serializable data object SleepRoute : NavKey

/** The Sleep tab's entry, with its own [SleepViewModel] scoped to the entry. */
fun EntryProviderScope<NavKey>.sleepEntry(contentPadding: PaddingValues) {
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
            analysis = state.analysis,
            actions = SleepActions(
                onSleep = viewModel::setSleep,
                onBedtime = viewModel::setBedtime,
                onWakeTime = viewModel::setWakeTime,
                onQuality = viewModel::setQuality,
                onFactor = viewModel::setFactor,
                onSelectDay = viewModel::selectDay,
                onRange = viewModel::selectRange,
                onConnectHealth = connectHealth,
                onOpenHealthSettings = { HealthConnectSource.openSettings(context) },
            ),
            contentPadding = contentPadding,
            today = state.today,
            watch = state.watch,
            fromWatch = state.fromWatch,
        )
    }
}
