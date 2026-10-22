package se.novila.feature.sleep

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
import se.novila.data.SelectedDay
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.REGULARITY_NIGHTS
import se.novila.model.SLEEP_WINDOW_NIGHTS
import se.novila.model.SleepAnalysis
import se.novila.model.SleepRegularity
import se.novila.model.analyzeSleep
import se.novila.model.sleepRegularity
import se.novila.sleep.SleepRepository
import se.novila.sleep.SleepState
import se.novila.sleep.nightsByDate
import se.novila.sleep.withNight
import java.time.LocalDate

/**
 * The sleep in the [history] over the chosen range up to today, the [goals], what the logs say ([analysis]) and how
 * regular the nights are ([regularity]).
 */
data class SleepTrendsUiState(
    val history: History,
    val goals: Goals,
    val analysis: SleepAnalysis,
    val regularity: SleepRegularity,
)

/**
 * The sleep over time, opened from the Sleep tab, scoped to its entry. Nights a watch recorded fill in the ones the
 * user didn't log, as on the tab; the tab reads them from Health Connect, this only shows what it read.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class SleepTrendsViewModel(
    private val store: HealthStore,
    private val goalsStore: GoalsStore,
    private val sleepRepository: SleepRepository,
) : ViewModel() {
    private val day = SelectedDay()
    private val range = MutableStateFlow(HistoryRange.Week)

    /** The history, the goals and the analysis, or null before the stores' first reads. */
    val state: StateFlow<SleepTrendsUiState?> =
        combine(
            store.days.filterNotNull(),
            sleepRepository.state,
            goalsStore.goals.filterNotNull(),
            day.today,
            range,
        ) { saved, watch, goals, today, range -> read(saved, watch, goals, today, range) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): SleepTrendsUiState? {
        val saved = store.days.value ?: return null
        val goals = goalsStore.goals.value ?: return null
        return read(saved, sleepRepository.state.value, goals, day.today.value, range.value)
    }

    private fun read(
        saved: SavedDays,
        watch: SleepState,
        goals: Goals,
        today: LocalDate,
        range: HistoryRange,
    ): SleepTrendsUiState {
        val nights = watch.nightsByDate
        val shown = saved.showing { it.withNight(nights[it.date]) }
        return SleepTrendsUiState(
            history = shown.history(range, today),
            goals = goals,
            // Every night in the window with the day before it, oldest first.
            analysis = analyzeSleep(shown.lastDays(SLEEP_WINDOW_NIGHTS + 1, today).reversed(), goals),
            regularity = sleepRegularity(shown.lastDays(REGULARITY_NIGHTS.toInt(), today), today),
        )
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() = day.refresh()

    /** Shows the sleep over a week, four weeks or a year. */
    fun selectRange(range: HistoryRange) {
        this.range.value = range
    }
}
