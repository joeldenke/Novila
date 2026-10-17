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
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.SLEEP_WINDOW_NIGHTS
import se.novila.model.SleepAnalysis
import se.novila.model.SleepFactor
import se.novila.model.analyzeSleep
import java.time.LocalDate
import java.time.LocalTime

/**
 * The [history] over the chosen range up to the shown day (today, or an earlier one the user went back to), [today],
 * the user's [goals] and what their logs up to today say about their sleep ([analysis]).
 */
data class SleepUiState(
    val history: History,
    val goals: Goals,
    val today: LocalDate,
    val analysis: SleepAnalysis,
) {
    /** The shown day, the last in the history. */
    val day: DayEntry get() = history.days.last()
}

/** The Sleep tab, scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class SleepViewModel(private val store: HealthStore, private val goalsStore: GoalsStore) : ViewModel() {
    private val day = SelectedDay()
    private val range = MutableStateFlow(HistoryRange.Week)

    /** The history, the goals and the analysis, or null before the stores' first reads. */
    val state: StateFlow<SleepUiState?> =
        combine(
            store.histories(day.dates, range),
            goalsStore.goals.filterNotNull(),
            day.today,
            store.days.filterNotNull(),
        ) { history, goals, today, saved -> SleepUiState(history, goals, today, analysis(saved, today, goals)) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): SleepUiState? {
        val saved = store.days.value ?: return null
        val goals = goalsStore.goals.value ?: return null
        val today = day.today.value
        return SleepUiState(saved.history(range.value, day.value), goals, today, analysis(saved, today, goals))
    }

    /** Every night in the window with the day before it, oldest first, up to today whatever day is shown. */
    private fun analysis(saved: SavedDays, today: LocalDate, goals: Goals): SleepAnalysis =
        analyzeSleep(saved.lastDays(SLEEP_WINDOW_NIGHTS + 1, today).reversed(), goals)

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() = day.refresh()

    /** Shows and changes [date] instead, to fill in a night that was forgotten. */
    fun selectDay(date: LocalDate) = day.select(date)

    /** Shows the sleep over a week, four weeks or a year. */
    fun selectRange(range: HistoryRange) {
        this.range.value = range
    }

    /** Sets the shown day's sleep in hours, without times. */
    fun setSleep(hours: Float) = store.update(day.value) { it.withSleep(hours) }

    /** Sets when the user went to bed; the wake time is the usual one until they give it. */
    fun setBedtime(time: LocalTime) = store.update(day.value) {
        it.withSleepTimes(time, it.wakeTime ?: DefaultWakeTime)
    }

    /** Sets when the user woke up; the bedtime is the usual one until they give it. */
    fun setWakeTime(time: LocalTime) = store.update(day.value) {
        it.withSleepTimes(it.bedtime ?: DefaultBedtime, time)
    }

    fun setQuality(quality: Int) = store.update(day.value) { it.withSleepQuality(quality) }

    fun setFactor(factor: SleepFactor, on: Boolean) = store.update(day.value) { it.withSleepFactor(factor, on) }
}
