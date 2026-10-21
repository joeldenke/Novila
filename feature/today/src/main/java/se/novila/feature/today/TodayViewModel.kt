package se.novila.feature.today

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
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.novila.coach.DailyCoach
import se.novila.coach.dailyCoach
import se.novila.data.CoachStore
import se.novila.data.GoalsStore
import se.novila.data.HealthStore
import se.novila.data.HealthStore.SavedDays
import se.novila.data.ProfileStore
import se.novila.data.SavedCoach
import se.novila.data.SavedSleepCoach
import se.novila.data.SettingsStore
import se.novila.data.SleepCoachStore
import se.novila.data.Week
import se.novila.data.trainingWeek
import se.novila.exercise.ExerciseRepository
import se.novila.exercise.ExerciseState
import se.novila.model.CONNECTION_WINDOW_DAYS
import se.novila.model.Connection
import se.novila.model.Drink
import se.novila.model.DrinkKind
import se.novila.model.Goals
import se.novila.model.SLEEP_WINDOW_NIGHTS
import se.novila.model.SleepAnalysis
import se.novila.model.TrainingToday
import se.novila.model.analyzeSleep
import se.novila.model.connections
import se.novila.model.withDrink
import se.novila.movement.MovementRepository
import se.novila.movement.MovementState
import se.novila.recovery.RecoveryRepository
import se.novila.recovery.RecoveryState
import se.novila.recovery.RecoveryStatus
import se.novila.recovery.analyzeRecovery
import se.novila.sleep.SleepCoachPreferences
import se.novila.sleep.SleepRepository
import se.novila.sleep.SleepState
import se.novila.sleep.nightsByDate
import se.novila.sleep.withNight
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * The dashboard: the week so far, and [hour] of day when it was read, the user's [name] for the greeting, the
 * [goals] the day is measured against, what the user's logs say about their [sleep] and today's [training] from the
 * week's program, and the [connections] between the parts. Nights a watch recorded fill in the ones the user didn't
 * log.
 */
data class TodayUiState(
    val week: Week,
    val hour: Int,
    val name: String = "",
    val goals: Goals = Goals(),
    val sleep: SleepAnalysis = SleepAnalysis(change = null, links = emptyList(), bedtimes = null, nightsLogged = 0),
    val training: TrainingToday = TrainingToday(),
    val coach: DailyCoach,
    val connections: List<Connection> = emptyList(),
)

/** What Today reads from the stores and the watch, together. */
private data class TodayInputs(
    val now: LocalDateTime,
    val days: SavedDays,
    val name: String,
    val goals: Goals,
    val watch: SleepState,
)

/** The Today tab. Scoped to its entry; reads the same stores as the other tabs, so their changes show up here. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class TodayViewModel(
    private val store: HealthStore,
    private val movementRepository: MovementRepository,
    private val profileStore: ProfileStore,
    private val goalsStore: GoalsStore,
    private val sleepRepository: SleepRepository,
    private val coachStore: CoachStore,
    private val sleepCoachStore: SleepCoachStore,
    private val exerciseRepository: ExerciseRepository,
    private val recoveryRepository: RecoveryRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val now = MutableStateFlow(LocalDateTime.now())

    /** The dashboard, or null before the stores' first reads, so the greeting never changes once shown. */
    val state: StateFlow<TodayUiState?> =
        combine(
            combine(
                now,
                store.days.filterNotNull(),
                profileStore.profile.filterNotNull(),
                goalsStore.goals.filterNotNull(),
                sleepRepository.state,
            ) { now, days, profile, goals, watch -> TodayInputs(now, days, profile.name, goals, watch) },
            coachStore.answers.filterNotNull(),
            exerciseRepository.state,
            recoveryRepository.state,
            sleepCoachStore.answers.filterNotNull(),
            ::read,
        ).stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): TodayUiState? {
        val days = store.days.value ?: return null
        val profile = profileStore.profile.value ?: return null
        val goals = goalsStore.goals.value ?: return null
        val answers = coachStore.answers.value ?: return null
        val sleepCoach = sleepCoachStore.answers.value ?: return null
        val inputs = TodayInputs(now.value, days, profile.name, goals, sleepRepository.state.value)
        return read(inputs, answers, exerciseRepository.state.value, recoveryRepository.state.value, sleepCoach)
    }

    val movement: StateFlow<MovementState> = movementRepository.state

    /**
     * Whether the introduction was skipped, so the name, the goals and the coaches are not set up: Today then shows
     * one quiet card that finishes the setup. False until the settings are read, and for a phone that finished the
     * intro.
     */
    val setupIncomplete: StateFlow<Boolean> = settingsStore.settings
        .map { it?.setupIncomplete == true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private fun read(
        inputs: TodayInputs,
        answers: SavedCoach,
        recorded: ExerciseState,
        recovery: RecoveryState,
        sleepCoach: SavedSleepCoach,
    ): TodayUiState {
        val nights = inputs.watch.nightsByDate
        val days = inputs.days.showing { it.withNight(nights[it.date]) }
        val today = inputs.now.toLocalDate()
        // Three months of days, oldest first, for the sleep links and the connections between the parts.
        val window = days.lastDays(maxOf(CONNECTION_WINDOW_DAYS, SLEEP_WINDOW_NIGHTS) + 1, today).reversed()
        val sleep = analyzeSleep(window, inputs.goals)
        // Planned the same way as the Training tab, so both say the same about today's session.
        val status = (recovery as? RecoveryState.Connected)?.let { analyzeRecovery(it.days, today) }
            ?: RecoveryStatus.Unknown
        val fromSource = (recorded as? ExerciseState.Connected)?.exercises.orEmpty()
        val training = trainingWeek(inputs.days, answers, fromSource, today, recovery = status).trainingOn(today)
        // One coach for all parts: the day's sleep, training and food weighed together.
        val coach = dailyCoach(
            day = days.day(today),
            goals = inputs.goals,
            training = training,
            recovery = status,
            bedtimeGoal = sleepCoach.preferences?.bedtime ?: SleepCoachPreferences.DEFAULT_BEDTIME,
        )
        return TodayUiState(
            week = days.week(today),
            hour = inputs.now.hour,
            name = inputs.name,
            goals = inputs.goals,
            sleep = sleep,
            training = training,
            coach = coach,
            connections = connections(window, inputs.goals),
        )
    }

    /**
     * Called on resume, so "today" and the greeting stay correct. Health Connect only allows reads while the app is
     * in the foreground, so movement, workouts, recovery and the watch's nights are read here too.
     */
    fun refresh() {
        now.value = LocalDateTime.now()
        val today = LocalDate.now()
        viewModelScope.launch { movementRepository.refresh(today) }
        viewModelScope.launch { sleepRepository.refresh(today) }
        viewModelScope.launch { exerciseRepository.refresh(today) }
        viewModelScope.launch { recoveryRepository.refresh(today) }
    }

    /** One more glass of water today, from the checklist. */
    fun addWater() =
        store.update(LocalDate.now()) { it.withDrink(Drink(DrinkKind.Water, Goals.GLASS_ML, LocalTime.now())) }

    /** Reopens the intro so the setup that was skipped earlier can be finished. */
    fun openSetup() {
        settingsStore.setOnboarded(false)
    }
}
