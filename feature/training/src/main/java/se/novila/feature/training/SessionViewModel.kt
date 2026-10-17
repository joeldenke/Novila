package se.novila.feature.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import se.novila.exercise.PlannedSession
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A strength or rehab day of this week's program, to look at before starting: its [session] (null when the day has
 * none, for example after the coach moved it), and [minutesDone] when it is today's and already done.
 */
data class SessionUiState(val session: PlannedSession?, val minutesDone: Int?)

/** One day's session from the week's program. Scoped to the session entry. */
@AssistedInject
class SessionViewModel(@Assisted private val day: DayOfWeek, training: TrainingRepository) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    /** The day's session, or null before the stores' first read. */
    val state: StateFlow<SessionUiState?> = training.states(date)
        .map(::read)
        .stateIn(viewModelScope, SharingStarted.Eagerly, training.current(date.value)?.let(::read))

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        date.value = LocalDate.now()
    }

    private fun read(state: TrainingUiState) = SessionUiState(
        session = state.program.sessions.firstOrNull { it.day == day && it.exercises.isNotEmpty() },
        minutesDone = state.strengthMinutesToday.takeIf { state.today == day && it > 0 },
    )

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(@Assisted day: DayOfWeek): SessionViewModel
    }
}
