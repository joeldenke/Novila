package se.novila

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseType
import se.novila.exercise.TrainingProgram
import se.novila.exercise.TrainingProgramGenerator
import se.novila.exercise.Workout
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.MOVEMENT_PERMISSIONS
import se.novila.movement.InMemoryMovementCache
import se.novila.movement.MovementRepository
import se.novila.movement.MovementState
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

data class HealthUiState(
    val today: DayEntry,
    val history: List<DayEntry>,
    /** Hour of day when the state was read, for the greeting. */
    val hour: Int,
    /** This week's workouts, generated from recent ones. */
    val program: TrainingProgram,
)

/** A strength session in progress, with the seconds left of a timed step (a rest or a hold). */
data class WorkoutUiState(val workout: Workout, val secondsLeft: Int?)

class HealthViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HealthStore(app)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<HealthUiState> = _state.asStateFlow()

    /**
     * Movement for the dashboard and Training. The data source and cache are chosen here and nowhere
     * else: swap HealthConnectSource for another MovementSource, or the cache for a persistent one.
     */
    private val movementRepository = MovementRepository(
        source = HealthConnectSource(app),
        cache = InMemoryMovementCache(),
    )
    val movement: StateFlow<MovementState> = movementRepository.state

    private fun read(): HealthUiState {
        val today = LocalDate.now()
        val days = store.lastDays(count = 7, today = today)
        // Strength sessions done in session mode are the history so far. No ExerciseSource is connected
        // yet (Google Health API needs sign-in first); once one is, its workouts join these.
        val strength = store.lastDays(count = HISTORY_DAYS, today = today)
            .filter { it.strengthMinutes > 0 }
            .map { Exercise(ExerciseType.Strength, it.date, Duration.ofMinutes(it.strengthMinutes.toLong())) }
        val program = TrainingProgramGenerator.generate(history = strength, today = today)
        return HealthUiState(
            today = days.first(),
            history = days.drop(1),
            hour = LocalTime.now().hour,
            program = program,
        )
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        _state.value = read()
        refreshMovement()
    }

    /** Health Connect only allows reads while the app is in the foreground, so this runs on resume. */
    private fun refreshMovement() {
        viewModelScope.launch { movementRepository.refresh(LocalDate.now()) }
    }

    /** Result of Health Connect's permission dialog, with the permissions granted so far. */
    fun onMovementPermissionsResult(granted: Set<String>) {
        viewModelScope.launch {
            movementRepository.onPermissionResult(granted.any { it in MOVEMENT_PERMISSIONS }, LocalDate.now())
        }
    }

    private fun update(change: (DayEntry) -> DayEntry) {
        val today = store.load(LocalDate.now())
        store.save(change(today))
        _state.value = read()
    }

    private val _workout = MutableStateFlow<WorkoutUiState?>(null)

    /** The strength session in progress, or null. Kept here so it survives rotation; saved only when finished. */
    val workout: StateFlow<WorkoutUiState?> = _workout.asStateFlow()
    private var workoutTimer: Job? = null

    fun startWorkout(day: DayOfWeek) {
        val session = _state.value.program.sessions.firstOrNull { it.day == day && it.exercises.isNotEmpty() } ?: return
        showWorkout(Workout.start(session, System.currentTimeMillis()))
    }

    /** The user finished a set or skipped a rest. */
    fun nextWorkoutStep() {
        _workout.value?.let { showWorkout(it.workout.next(System.currentTimeMillis())) }
    }

    /** Saves a finished session as today's strength training, so next week's program builds on it. */
    fun finishWorkout() {
        val workout = _workout.value?.workout ?: return
        if (workout.isFinished) update { it.withStrengthSession(workout.minutes(System.currentTimeMillis())) }
        stopWorkout()
    }

    /** Leaves the session without saving. */
    fun stopWorkout() {
        workoutTimer?.cancel()
        _workout.value = null
    }

    /** Shows [workout] and, for a timed step, counts down once a second and moves on when it runs out. */
    private fun showWorkout(workout: Workout) {
        workoutTimer?.cancel()
        _workout.value = WorkoutUiState(workout, workout.secondsLeft(System.currentTimeMillis()))
        val endsAt = workout.endsAtMillis ?: return
        workoutTimer = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                if (now >= endsAt) break
                _workout.value = WorkoutUiState(workout, workout.secondsLeft(now))
                delay((endsAt - now) % 1000L + 1L)
            }
            showWorkout(workout.next(System.currentTimeMillis()))
        }
    }

    fun changePortions(group: FoodGroup, delta: Int) = update { it.withPortions(group, delta) }
    fun changeTreats(delta: Int) = update { it.withTreats(delta) }
    fun setSleep(hours: Float) = update { it.withSleep(hours) }
    fun setMood(mood: Int) = update { it.withMood(mood) }

    private companion object {
        /** The program looks at four whole weeks before this one; five weeks covers them on any weekday. */
        const val HISTORY_DAYS = 35
    }
}
