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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.novila.data.HealthStore
import se.novila.exercise.Workout
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * A guided strength session for the program's [day]. Scoped to the workout entry: the session and its timer survive
 * rotation, and leaving the screen without finishing clears the ViewModel, which discards the session (nothing was
 * saved yet) and stops the timer.
 */
@AssistedInject
class WorkoutViewModel(
    @Assisted day: DayOfWeek,
    training: TrainingRepository,
    private val store: HealthStore,
) : ViewModel() {
    private val _state = MutableStateFlow<WorkoutUiState?>(null)

    /** The session in progress, or null when the day has no strength session. */
    val state: StateFlow<WorkoutUiState?> = _state.asStateFlow()
    private var timer: Job? = null

    init {
        training.read(LocalDate.now()).program.sessions
            .firstOrNull { it.day == day && it.exercises.isNotEmpty() }
            ?.let { show(Workout.start(it, System.currentTimeMillis())) }
    }

    /** The user finished a set or skipped a rest. */
    fun next() {
        _state.value?.let { show(it.workout.next(System.currentTimeMillis())) }
    }

    /** Saves a finished session as today's strength training, so next week's program builds on it. */
    fun finish() {
        val workout = _state.value?.workout ?: return
        val now = System.currentTimeMillis()
        if (workout.isFinished) store.update(LocalDate.now()) { it.withStrengthSession(workout.minutes(now)) }
        timer?.cancel()
        _state.value = null
    }

    /** Shows [workout] and, for a timed step, counts down once a second and moves on when it runs out. */
    private fun show(workout: Workout) {
        timer?.cancel()
        _state.value = WorkoutUiState(workout, workout.secondsLeft(System.currentTimeMillis()))
        val endsAt = workout.endsAtMillis ?: return
        timer = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                if (now >= endsAt) break
                _state.value = WorkoutUiState(workout, workout.secondsLeft(now))
                delay((endsAt - now) % 1000L + 1L)
            }
            show(workout.next(System.currentTimeMillis()))
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(@Assisted day: DayOfWeek): WorkoutViewModel
    }
}
