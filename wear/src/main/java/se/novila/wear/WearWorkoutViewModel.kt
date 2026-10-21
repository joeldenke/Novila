package se.novila.wear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import se.novila.exercise.PlannedSession
import se.novila.exercise.StandaloneSession
import se.novila.exercise.StrengthLevel
import se.novila.exercise.Workout
import se.novila.exercise.WorkoutStep
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.time.Clock

/** What the watch shows: the session to start, the step running now, or the finished session's summary. */
data class WearWorkoutUiState(
    val day: DayOfWeek,
    val level: StrengthLevel = StrengthLevel.Intermediate,
    val second: Boolean = StandaloneSession.second(day),
    val workout: Workout? = null,
    val secondsLeft: Int? = null,
) {
    /** The session the start screen describes, or the one running. */
    val planned: PlannedSession get() = StandaloneSession.of(workout?.day ?: day, level, second)

    val isFinished: Boolean get() = workout?.isFinished == true

    /** The step to show, or null on the start screen and after the last step. */
    val step: WorkoutStep? get() = workout?.step
}

/**
 * Runs one strength session on the watch, step by step, with the same [Workout] steps and rest countdown as the
 * phone's workout mode. There is no program to read on the watch yet, so [StandaloneSession] builds the day's
 * session at the selected level. The ViewModel survives rotation and keeps the countdown alive while the screen is
 * on; leaving the watch app clears it, and nothing is saved anywhere.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class WearWorkoutViewModel(private val clock: Clock) : ViewModel() {
    private val _state = MutableStateFlow(WearWorkoutUiState(day = LocalDate.now().dayOfWeek))
    val state: StateFlow<WearWorkoutUiState> = _state.asStateFlow()
    private var timer: Job? = null

    /** The level applies to the next session; a session already running keeps its dose. */
    fun setLevel(level: StrengthLevel) {
        if (_state.value.workout != null) return
        _state.update { it.copy(level = level) }
    }

    /** Picks the week's first (A) or second (B) session, before it starts. */
    fun setSecond(second: Boolean) {
        if (_state.value.workout != null) return
        _state.update { it.copy(second = second) }
    }

    fun start() {
        state.value.let { show(StandaloneSession.start(it.day, it.level, clock.now(), it.second)) }
    }

    /** The user finished a set or skipped a rest; a timed step that runs out calls this on its own. */
    fun next() {
        val workout = _state.value.workout ?: return
        if (workout.isFinished) return
        show(workout.next(clock.now()))
    }

    /** Back to the start screen, with the level and session kept for the next one. */
    fun reset() {
        timer?.cancel()
        _state.update { WearWorkoutUiState(day = LocalDate.now().dayOfWeek, level = it.level) }
    }

    override fun onCleared() {
        timer?.cancel()
    }

    /**
     * Shows [workout] and, for a timed step, counts down once a second and moves on when it runs out, just like the
     * phone. The watch keeps the screen on while a session runs, so the countdown is visible; it does not keep
     * running in the background once the app is left.
     */
    private fun show(workout: Workout) {
        timer?.cancel()
        _state.update { it.copy(workout = workout, secondsLeft = workout.secondsLeft(clock.now())) }
        val endsAt = workout.endsAt ?: return
        timer = viewModelScope.launch {
            while (true) {
                val now = clock.now()
                if (now >= endsAt) break
                _state.update { it.copy(secondsLeft = workout.secondsLeft(now)) }
                // Wake just after the shown second changes.
                delay((endsAt - now).inWholeMilliseconds % 1000L + 1L)
            }
            show(workout.next(clock.now()))
        }
    }
}
