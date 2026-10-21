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
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.TrendFeedback
import se.novila.model.connections
import se.novila.model.trendFeedback
import se.novila.sleep.SleepRepository
import se.novila.sleep.SleepState
import se.novila.sleep.nightsByDate
import se.novila.sleep.withNight
import java.time.LocalDate

/**
 * Sleep, food and training over the same range up to today ([history]), what that says against the range before
 * ([feedback]) and the strongest [connections] between the parts, measured against the user's [goals].
 */
data class TrendsUiState(
    val history: History,
    val goals: Goals,
    val feedback: List<TrendFeedback>,
    val connections: List<Connection>,
)

/**
 * The trends of all parts together, opened from Today's insights, scoped to its entry. Nights a watch recorded fill
 * in the ones the user didn't log, as on Today; this only shows what was already read.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class TrendsViewModel(
    private val store: HealthStore,
    private val goalsStore: GoalsStore,
    private val sleepRepository: SleepRepository,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())
    private val range = MutableStateFlow(HistoryRange.Week)

    /** The trends, or null before the stores' first reads. */
    val state: StateFlow<TrendsUiState?> =
        combine(
            store.days.filterNotNull(),
            sleepRepository.state,
            goalsStore.goals.filterNotNull(),
            today,
            range,
            ::read,
        ).stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): TrendsUiState? {
        val saved = store.days.value ?: return null
        val goals = goalsStore.goals.value ?: return null
        return read(saved, sleepRepository.state.value, goals, today.value, range.value)
    }

    private fun read(
        saved: SavedDays,
        watch: SleepState,
        goals: Goals,
        today: LocalDate,
        range: HistoryRange,
    ): TrendsUiState {
        val nights = watch.nightsByDate
        val shown = saved.showing { it.withNight(nights[it.date]) }
        val history = shown.history(range, today)
        return TrendsUiState(
            history = history,
            goals = goals,
            feedback = trendFeedback(history, goals),
            // The connection window, oldest first, as Discoveries reads it.
            connections = connections(shown.lastDays(CONNECTION_WINDOW_DAYS + 1, today).reversed(), goals),
        )
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        today.value = LocalDate.now()
    }

    /** Shows every part over a week, four weeks or a year. */
    fun selectRange(range: HistoryRange) {
        this.range.value = range
    }
}
