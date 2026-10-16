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
import se.novila.coach.AssistantAvailability
import se.novila.coach.CoachLanguageModel
import se.novila.coach.CoachPlan
import se.novila.coach.CoachPlanner
import se.novila.coach.CoachPreferences
import se.novila.coach.CoachPrompt
import se.novila.coach.CoachPromptInput
import se.novila.data.CoachStore
import se.novila.data.GeminiNanoCoachModel
import se.novila.data.HealthStore
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseType
import se.novila.exercise.TrainingProgram
import se.novila.exercise.TrainingProgramGenerator
import se.novila.exercise.Workout
import se.novila.feature.training.AssistantUiState
import se.novila.feature.training.CoachUiState
import se.novila.feature.training.WorkoutUiState
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.MOVEMENT_PERMISSIONS
import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.withPortions
import se.novila.model.withTreats
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
    /** This week's workouts: the coach's when it is set up, otherwise generated from recent ones. */
    val program: TrainingProgram,
    val coach: CoachUiState,
)

class HealthViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HealthStore(app)
    private val coachStore = CoachStore(app)

    /** Answers the user is trying out before the coach is set up. Not saved until they create the program. */
    private var coachDraft: CoachPreferences? = null
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
        val saved = coachStore.load()
        val preferences = saved ?: coachDraft ?: CoachPreferences()
        val plan = CoachPlanner.plan(preferences, history = strength, today = today)
        val program = if (saved != null) plan.program else TrainingProgramGenerator.generate(strength, today)
        return HealthUiState(
            today = days.first(),
            history = days.drop(1),
            hour = LocalTime.now().hour,
            program = program,
            coach = CoachUiState(preferences, plan, active = saved != null),
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

    /** Gemini Nano on the device. The coach's plan never depends on it; it only explains the plan in its own words. */
    private val languageModel: CoachLanguageModel = GeminiNanoCoachModel()
    private val _assistant = MutableStateFlow(AssistantUiState())
    val assistant: StateFlow<AssistantUiState> = _assistant.asStateFlow()

    /** Asks the system whether Gemini Nano can run here. Called when the coach is opened. */
    fun checkAssistant() {
        viewModelScope.launch {
            val availability = languageModel.availability()
            _assistant.value = _assistant.value.copy(availability = availability)
        }
    }

    /** The user asked for Gemini Nano: AICore downloads it and keeps it up to date. */
    fun downloadAssistant() {
        _assistant.value = _assistant.value.copy(availability = AssistantAvailability.Downloading, failed = false)
        viewModelScope.launch {
            val done = languageModel.download()
            _assistant.value = _assistant.value.copy(
                availability = if (done) AssistantAvailability.Available else languageModel.availability(),
                failed = !done,
            )
        }
    }

    /** Lets Gemini Nano explain the coach's current plan, from [input] in the user's language. */
    fun writeAssistantMessage(input: CoachPromptInput) {
        val coach = _state.value.coach
        _assistant.value = _assistant.value.copy(writing = true, message = null, failed = false)
        viewModelScope.launch {
            val answer = languageModel.write(CoachPrompt.build(coach.plan, coach.preferences, input))
                .map(CoachPrompt::clean)
                .getOrNull()
                ?.takeIf { it.isNotBlank() }
            // A message for a plan that changed meanwhile would explain the wrong week.
            val current = _state.value.coach.plan == coach.plan
            _assistant.value = _assistant.value.copy(
                writing = false,
                message = answer.takeIf { current },
                failed = answer == null,
            )
        }
    }

    /** The plan changed, so the message no longer fits it. */
    private fun forgetAssistantMessage() {
        _assistant.value = _assistant.value.copy(message = null, failed = false)
    }

    /** A changed answer. Once the coach is set up it is saved and changes the week's program right away. */
    fun changeCoach(preferences: CoachPreferences) {
        if (_state.value.coach.active) coachStore.save(preferences) else coachDraft = preferences
        _state.value = read()
        forgetAssistantMessage()
    }

    /** Saves the answers, which makes the coach's plan the week's program. */
    fun createCoachProgram() {
        coachStore.save(_state.value.coach.preferences)
        _state.value = read()
    }

    /** A new suggestion from the same answers, used right away. */
    fun nextCoachSuggestion() {
        coachStore.save(_state.value.coach.preferences.nextVariation())
        _state.value = read()
        forgetAssistantMessage()
    }

    /** Back to the standard program generated from recent workouts. */
    fun resetCoach() {
        coachStore.clear()
        coachDraft = null
        _state.value = read()
        forgetAssistantMessage()
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
