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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.novila.data.CoachStore
import se.novila.data.GoalsStore
import se.novila.data.HealthStore
import se.novila.data.HealthStore.SavedDays
import se.novila.data.ProfileStore
import se.novila.data.SavedCoach
import se.novila.data.Week
import se.novila.data.trainingWeek
import se.novila.exercise.ExerciseRepository
import se.novila.exercise.ExerciseState
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.SLEEP_WINDOW_NIGHTS
import se.novila.model.SleepAnalysis
import se.novila.model.TrainingToday
import se.novila.model.analyzeSleep
import se.novila.model.withPortions
import se.novila.movement.MovementRepository
import se.novila.movement.MovementState
import se.novila.recovery.RecoveryRepository
import se.novila.recovery.RecoveryState
import se.novila.recovery.RecoveryStatus
import se.novila.recovery.analyzeRecovery
import se.novila.sleep.SleepRepository
import se.novila.sleep.SleepState
import se.novila.sleep.nightsByDate
import se.novila.sleep.withNight
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The dashboard: the week so far, and [hour] of day when it was read, the user's [name] for the greeting, the
 * [goals] the day is measured against, what the user's logs say about their [sleep] and today's [training] from the
 * week's program. Nights a watch recorded fill in the ones the user didn't log.
 */
data class TodayUiState(
    val week: Week,
    val hour: Int,
    val name: String = "",
    val goals: Goals = Goals(),
    val sleep: SleepAnalysis = SleepAnalysis(change = null, links = emptyList(), bedtimes = null, nightsLogged = 0),
    val training: TrainingToday = TrainingToday(),
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
    private val exerciseRepository: ExerciseRepository,
    private val recoveryRepository: RecoveryRepository,
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
            ::read,
        ).stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): TodayUiState? {
        val days = store.days.value ?: return null
        val profile = profileStore.profile.value ?: return null
        val goals = goalsStore.goals.value ?: return null
        val answers = coachStore.answers.value ?: return null
        val inputs = TodayInputs(now.value, days, profile.name, goals, sleepRepository.state.value)
        return read(inputs, answers, exerciseRepository.state.value, recoveryRepository.state.value)
    }

    val movement: StateFlow<MovementState> = movementRepository.state

    private fun read(
        inputs: TodayInputs,
        answers: SavedCoach,
        recorded: ExerciseState,
        recovery: RecoveryState,
    ): TodayUiState {
        val nights = inputs.watch.nightsByDate
        val days = inputs.days.showing { it.withNight(nights[it.date]) }
        val today = inputs.now.toLocalDate()
        val sleep = analyzeSleep(days.lastDays(SLEEP_WINDOW_NIGHTS + 1, today).reversed(), inputs.goals)
        // Planned the same way as the Training tab, so both say the same about today's session.
        val status = (recovery as? RecoveryState.Connected)?.let { analyzeRecovery(it.days, today) }
            ?: RecoveryStatus.Unknown
        val fromSource = (recorded as? ExerciseState.Connected)?.exercises.orEmpty()
        val training = trainingWeek(inputs.days, answers, fromSource, today, recovery = status).trainingOn(today)
        return TodayUiState(days.week(today), inputs.now.hour, inputs.name, inputs.goals, sleep, training)
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
    fun addWater() = store.update(LocalDate.now()) { it.withPortions(FoodGroup.Water, +1) }
}
