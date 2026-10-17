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
import se.novila.data.GoalsStore
import se.novila.data.HealthStore
import se.novila.data.HealthStore.SavedDays
import se.novila.data.ProfileStore
import se.novila.data.Week
import se.novila.model.Goals
import se.novila.model.SLEEP_WINDOW_NIGHTS
import se.novila.model.SleepAnalysis
import se.novila.model.analyzeSleep
import se.novila.movement.MovementRepository
import se.novila.movement.MovementState
import se.novila.sleep.SleepRepository
import se.novila.sleep.SleepState
import se.novila.sleep.nightsByDate
import se.novila.sleep.withNight
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * The dashboard: the week so far, and [hour] of day when it was read, the user's [name] for the greeting, the
 * [goals] the day is measured against and what the user's logs say about their [sleep]. Nights a watch recorded fill
 * in the ones the user didn't log.
 */
data class TodayUiState(
    val week: Week,
    val hour: Int,
    val name: String = "",
    val goals: Goals = Goals(),
    val sleep: SleepAnalysis = SleepAnalysis(change = null, links = emptyList(), bedtimes = null, nightsLogged = 0),
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
) : ViewModel() {
    private val now = MutableStateFlow(LocalDateTime.now())

    /** The dashboard, or null before the stores' first reads, so the greeting never changes once shown. */
    val state: StateFlow<TodayUiState?> =
        combine(
            now,
            store.days.filterNotNull(),
            profileStore.profile.filterNotNull(),
            goalsStore.goals.filterNotNull(),
            sleepRepository.state,
        ) { now, days, profile, goals, watch ->
            read(days, now, profile.name, goals, watch)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): TodayUiState? {
        val days = store.days.value ?: return null
        val profile = profileStore.profile.value ?: return null
        val goals = goalsStore.goals.value ?: return null
        return read(days, now.value, profile.name, goals, sleepRepository.state.value)
    }

    val movement: StateFlow<MovementState> = movementRepository.state

    private fun read(
        saved: SavedDays,
        now: LocalDateTime,
        name: String,
        goals: Goals,
        watch: SleepState,
    ): TodayUiState {
        val nights = watch.nightsByDate
        val days = saved.showing { it.withNight(nights[it.date]) }
        val today = now.toLocalDate()
        val sleep = analyzeSleep(days.lastDays(SLEEP_WINDOW_NIGHTS + 1, today).reversed(), goals)
        return TodayUiState(days.week(today), now.hour, name, goals, sleep)
    }

    /**
     * Called on resume, so "today" and the greeting stay correct. Health Connect only allows reads while the app is
     * in the foreground, so movement and the watch's nights are read here too.
     */
    fun refresh() {
        now.value = LocalDateTime.now()
        viewModelScope.launch { movementRepository.refresh(LocalDate.now()) }
        viewModelScope.launch { sleepRepository.refresh(LocalDate.now()) }
    }

    fun setMood(mood: Int) = store.update(LocalDate.now()) { it.withMood(mood) }
}
