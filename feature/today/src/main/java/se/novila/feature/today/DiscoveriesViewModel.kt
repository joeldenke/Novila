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
import se.novila.data.GoalsStore
import se.novila.data.HealthStore
import se.novila.data.HealthStore.SavedDays
import se.novila.model.CONNECTION_WINDOW_DAYS
import se.novila.model.Connection
import se.novila.model.Goals
import se.novila.model.Insight
import se.novila.model.SLEEP_WINDOW_NIGHTS
import se.novila.model.SleepAnalysis
import se.novila.model.analyzeSleep
import se.novila.model.connections
import se.novila.model.weekInsights
import se.novila.movement.MovementRepository
import se.novila.movement.MovementState
import se.novila.sleep.SleepRepository
import se.novila.sleep.SleepState
import se.novila.sleep.nightsByDate
import se.novila.sleep.withNight
import java.time.LocalDate

/**
 * Everything the logs say, worked out on the phone: the week's [insights], what the nights say ([sleep]) and the
 * [connections] between the parts.
 */
data class DiscoveriesUiState(
    val insights: List<Insight>,
    val sleep: SleepAnalysis,
    val connections: List<Connection>,
)

/** All discoveries, opened from Today, scoped to their entry. Nights a watch recorded fill in the ones not logged. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class DiscoveriesViewModel(
    private val store: HealthStore,
    private val goalsStore: GoalsStore,
    private val sleepRepository: SleepRepository,
    private val movementRepository: MovementRepository,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())

    /** The discoveries, or null before the stores' first reads. */
    val state: StateFlow<DiscoveriesUiState?> =
        combine(
            store.days.filterNotNull(),
            goalsStore.goals.filterNotNull(),
            sleepRepository.state,
            movementRepository.state,
            today,
            ::read,
        ).stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): DiscoveriesUiState? {
        val days = store.days.value ?: return null
        val goals = goalsStore.goals.value ?: return null
        return read(days, goals, sleepRepository.state.value, movementRepository.state.value, today.value)
    }

    private fun read(
        saved: SavedDays,
        goals: Goals,
        watch: SleepState,
        movement: MovementState,
        today: LocalDate,
    ): DiscoveriesUiState {
        val nights = watch.nightsByDate
        val days = saved.showing { it.withNight(nights[it.date]) }
        // Three months of days, oldest first, for the sleep links and the connections between the parts.
        val window = days.lastDays(maxOf(CONNECTION_WINDOW_DAYS, SLEEP_WINDOW_NIGHTS) + 1, today).reversed()
        // The week oldest first, today last, as the insights read it.
        val week = days.lastDays(count = 7, today = today).reversed()
        val steps = (movement as? MovementState.Connected)?.today?.steps
        return DiscoveriesUiState(
            insights = weekInsights(week, steps, limit = Int.MAX_VALUE, goals = goals),
            sleep = analyzeSleep(window, goals),
            connections = connections(window, goals),
        )
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        today.value = LocalDate.now()
    }
}
