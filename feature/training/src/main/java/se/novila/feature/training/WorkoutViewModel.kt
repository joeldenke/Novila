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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.novila.data.HealthStore
import se.novila.exercise.ExerciseType
import se.novila.exercise.Workout
import se.novila.model.MAX_EFFORT
import se.novila.model.MAX_PAIN
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Clock

/**
 * A guided strength session for the program's [day]. Scoped to the workout entry: the session and its timer survive
 * rotation, and leaving the screen without finishing clears the ViewModel, which discards the session (nothing was
 * saved yet) and stops the timer. Once it is done, the user can say how hard it was and how much it hurt, which is
 * saved with the session for the coach.
 */
@AssistedInject
class WorkoutViewModel(
    @Assisted day: DayOfWeek,
    training: TrainingRepository,
    private val store: HealthStore,
    /** [Clock.System] in the app; a test can step time instead of waiting for it. */
    private val clock: Clock,
) : ViewModel() {
    private val _state = MutableStateFlow<WorkoutUiState?>(null)

    /** The session in progress, or null when the day has no strength session. */
    val state: StateFlow<WorkoutUiState?> = _state.asStateFlow()
    private var timer: Job? = null

    /** The rehab phase the session belongs to, or 0 when it isn't a rehab session. */
    private var rehabPhase = 0

    init {
        // The stores are almost always read by now; only a screen restored at app start waits for them.
        val today = LocalDate.now()
        training.current(today)?.let { start(it, day) } ?: viewModelScope.launch { start(training.read(today), day) }
    }

    private fun start(state: TrainingUiState, day: DayOfWeek) {
        val session = state.program.sessions.firstOrNull { it.day == day && it.exercises.isNotEmpty() } ?: return
        if (session.type == ExerciseType.Rehab && state.coach.active) rehabPhase = state.coach.preferences.rehabPhase
        show(Workout.start(session, clock.now()))
    }

    /** The user finished a set or skipped a rest. */
    fun next() {
        _state.value?.let { show(it.workout.next(clock.now())) }
    }

    /** How hard the finished session was, RPE 1–10. */
    fun setEffort(effort: Int) {
        _state.update { it?.copy(feedback = it.feedback.copy(effort = effort.coerceIn(1, MAX_EFFORT))) }
    }

    /** How much the finished session hurt, 0–10. */
    fun setPain(pain: Int) {
        _state.update { it?.copy(feedback = it.feedback.copy(pain = pain.coerceIn(0, MAX_PAIN))) }
    }

    /**
     * Saves a finished session as today's strength training, with what the user said about it, so next week's program
     * builds on it and the coach can adjust the dose.
     */
    fun finish() {
        val state = _state.value ?: return
        val workout = state.workout
        val now = clock.now()
        if (workout.isFinished) {
            store.update(LocalDate.now()) {
                it.withStrengthSession(workout.minutes(now))
                    .withFeedback(state.feedback.effort, state.feedback.pain, rehabPhase)
            }
        }
        timer?.cancel()
        _state.value = null
    }

    /** Shows [workout] and, for a timed step, counts down once a second and moves on when it runs out. */
    private fun show(workout: Workout) {
        timer?.cancel()
        _state.value = WorkoutUiState(workout, workout.secondsLeft(clock.now()))
        val endsAt = workout.endsAt ?: return
        timer = viewModelScope.launch {
            while (true) {
                val now = clock.now()
                if (now >= endsAt) break
                _state.value = WorkoutUiState(workout, workout.secondsLeft(now))
                // Wake just after the shown second changes.
                delay((endsAt - now).inWholeMilliseconds % 1000L + 1L)
            }
            show(workout.next(clock.now()))
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(@Assisted day: DayOfWeek): WorkoutViewModel
    }
}
