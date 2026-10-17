package se.novila.feature.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import se.novila.data.HealthStore
import se.novila.model.CardioLog
import java.time.LocalDate

/** Logging a workout by hand: today's cardio so far and the planned one. Scoped to the log entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class CardioLogViewModel(training: TrainingRepository, private val store: HealthStore) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    /** Today's training, or null before the stores' first read. */
    val state: StateFlow<TrainingUiState?> = training.states(date)
        .stateIn(viewModelScope, SharingStarted.Eagerly, training.current(date.value))

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        date.value = LocalDate.now()
    }

    /** Saves [log] as today's, so the program builds on it. */
    fun log(log: CardioLog) {
        store.update(date.value) { it.withCardio(log) }
    }

    /** Takes back the last workout logged today. */
    fun undo() {
        store.update(date.value) { it.withoutLastCardio() }
    }
}
