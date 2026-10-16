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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.novila.movement.MovementRepository
import se.novila.movement.MovementState
import java.time.LocalDate

/** The Training tab: movement from Health Connect and the week's program. Scoped to the tab's root entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class TrainingViewModel(
    training: TrainingRepository,
    private val movementRepository: MovementRepository,
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    val state: StateFlow<TrainingUiState> = combine(date, training.changes) { date, _ -> training.read(date) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, training.read(date.value))

    val movement: StateFlow<MovementState> = movementRepository.state

    /**
     * Called on resume, so "today" stays correct after midnight. Health Connect only allows reads while the app is in
     * the foreground, so movement is read here too.
     */
    fun refresh() {
        date.value = LocalDate.now()
        viewModelScope.launch { movementRepository.refresh(LocalDate.now()) }
    }

    /** Result of Health Connect's permission dialog: whether the user gave access to anything. */
    fun onMovementPermissionsResult(granted: Boolean) {
        viewModelScope.launch { movementRepository.onPermissionResult(granted, LocalDate.now()) }
    }
}
