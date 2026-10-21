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
import kotlinx.coroutines.launch
import se.novila.data.HealthStore
import se.novila.exercise.ExerciseRepository
import se.novila.exercise.ExerciseState
import se.novila.model.CardioLog
import se.novila.movement.MovementRepository
import se.novila.movement.MovementState
import se.novila.recovery.RecoveryRepository
import se.novila.recovery.RecoveryState
import java.time.LocalDate
import java.time.LocalTime

/**
 * The Training tab: movement and workouts from Health Connect, recovery from the watch, the week's program and
 * today's cardio. Scoped to the tab's root entry.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class TrainingViewModel(
    training: TrainingRepository,
    private val movementRepository: MovementRepository,
    private val exerciseRepository: ExerciseRepository,
    private val recoveryRepository: RecoveryRepository,
    private val store: HealthStore,
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())
    private val now = MutableStateFlow(LocalTime.now().hour)

    /** The week's program, or null before the stores' first read. */
    val state: StateFlow<TrainingUiState?> = training.states(date)
        .stateIn(viewModelScope, SharingStarted.Eagerly, training.current(date.value))

    val movement: StateFlow<MovementState> = movementRepository.state

    val exercise: StateFlow<ExerciseState> = exerciseRepository.state

    val recovery: StateFlow<RecoveryState> = recoveryRepository.state

    /** The hour of day when the tab was last shown, for the sun (or moon) over the week's overview. */
    val hour: StateFlow<Int> = now

    /**
     * Called on resume, so "today" stays correct after midnight. Health Connect only allows reads while the app is in
     * the foreground, so movement, workouts and recovery are read here too.
     */
    fun refresh() {
        val today = LocalDate.now()
        date.value = today
        now.value = LocalTime.now().hour
        viewModelScope.launch { movementRepository.refresh(today) }
        viewModelScope.launch { exerciseRepository.refresh(today) }
        viewModelScope.launch { recoveryRepository.refresh(today) }
    }

    /** Result of Health Connect's permission dialog: whether the user gave access to anything. */
    fun onHealthPermissionsResult(granted: Boolean) {
        val today = LocalDate.now()
        viewModelScope.launch { movementRepository.onPermissionResult(granted, today) }
        viewModelScope.launch { exerciseRepository.onPermissionResult(granted, today) }
        viewModelScope.launch { recoveryRepository.onPermissionResult(granted, today) }
    }

    /** Result of the recovery card's own permission dialog, which asks only for resting heart rate and HRV. */
    fun onRecoveryPermissionsResult(granted: Boolean) {
        viewModelScope.launch { recoveryRepository.onPermissionResult(granted, LocalDate.now()) }
    }

    /** The user did a cardio session ("I did it"): saved as today's, so the program builds on it. */
    fun logCardio(log: CardioLog) {
        store.update(date.value) { it.withCardio(log) }
    }
}
