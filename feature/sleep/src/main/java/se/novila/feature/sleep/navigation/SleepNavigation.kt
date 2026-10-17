package se.novila.feature.sleep.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.feature.sleep.SleepActions
import se.novila.feature.sleep.SleepScreen
import se.novila.feature.sleep.SleepViewModel

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
            ),
            contentPadding = contentPadding,
            today = state.today,
        )
    }
}
