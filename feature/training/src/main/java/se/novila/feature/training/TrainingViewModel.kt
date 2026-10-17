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
import java.time.LocalDate

/**
 * The Training tab: movement and workouts from Health Connect, the week's program and today's cardio. Scoped to the
 * tab's root entry.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class TrainingViewModel(
    training: TrainingRepository,
    private val movementRepository: MovementRepository,
    private val exerciseRepository: ExerciseRepository,
    private val store: HealthStore,
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    /** The week's program, or null before the stores' first read. */
    val state: StateFlow<TrainingUiState?> = training.states(date)
        .stateIn(viewModelScope, SharingStarted.Eagerly, training.current(date.value))

    val movement: StateFlow<MovementState> = movementRepository.state

    val exercise: StateFlow<ExerciseState> = exerciseRepository.state

    /**
     * Called on resume, so "today" stays correct after midnight. Health Connect only allows reads while the app is in
     * the foreground, so movement and workouts are read here too.
     */
    fun refresh() {
        val today = LocalDate.now()
        date.value = today
        viewModelScope.launch { movementRepository.refresh(today) }
        viewModelScope.launch { exerciseRepository.refresh(today) }
    }

    /** Result of Health Connect's permission dialog: whether the user gave access to anything. */
    fun onHealthPermissionsResult(granted: Boolean) {
        val today = LocalDate.now()
        viewModelScope.launch { movementRepository.onPermissionResult(granted, today) }
        viewModelScope.launch { exerciseRepository.onPermissionResult(granted, today) }
    }

    /** The user did a cardio session ("I did it"): saved as today's, so the program builds on it. */
    fun logCardio(log: CardioLog) {
        store.update(date.value) { it.withCardio(log) }
    }

    /** Takes back the last cardio session logged today. */
    fun undoCardio() {
        store.update(date.value) { it.withoutLastCardio() }
    }
}
