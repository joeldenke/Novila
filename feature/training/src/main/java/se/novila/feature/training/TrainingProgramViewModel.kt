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
import java.time.LocalDate

/** The week's program and the weekly review, opened from the Training tab, scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class TrainingProgramViewModel(training: TrainingRepository) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    /** The week's program, or null before the stores' first read. */
    val state: StateFlow<TrainingUiState?> = training.states(date)
        .stateIn(viewModelScope, SharingStarted.Eagerly, training.current(date.value))

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        date.value = LocalDate.now()
    }
}
