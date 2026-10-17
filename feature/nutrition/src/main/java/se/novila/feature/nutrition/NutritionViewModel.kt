package se.novila.feature.nutrition

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
import se.novila.data.SelectedDay
import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.withPortions
import se.novila.model.withTreats
import java.time.LocalDate

/**
 * The [history] over the chosen range up to the shown day (today, or an earlier one the user went back to), [today]
 * and the user's [goals] (water counts towards its own).
 */
data class NutritionUiState(val history: History, val goals: Goals, val today: LocalDate) {
    /** The shown day, the last in the history. */
    val day: DayEntry get() = history.days.last()
}

/** The Nutrition tab, scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class NutritionViewModel(private val store: HealthStore, private val goalsStore: GoalsStore) : ViewModel() {
    private val day = SelectedDay()
    private val range = MutableStateFlow(HistoryRange.Week)

    /** The history and the goals, or null before the stores' first reads. */
    val state: StateFlow<NutritionUiState?> =
        combine(store.histories(day.dates, range), goalsStore.goals.filterNotNull(), day.today, ::NutritionUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): NutritionUiState? {
        val history = store.history(range.value, day.value) ?: return null
        return goalsStore.goals.value?.let { NutritionUiState(history, it, day.today.value) }
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() = day.refresh()

    /** Shows and changes [date] instead, to fill in what was eaten on a day that was forgotten. */
    fun selectDay(date: LocalDate) = day.select(date)

    /** Shows the scores over a week, four weeks or a year. */
    fun selectRange(range: HistoryRange) {
        this.range.value = range
    }

    fun changePortions(group: FoodGroup, delta: Int) = store.update(day.value) { it.withPortions(group, delta) }

    fun changeTreats(delta: Int) = store.update(day.value) { it.withTreats(delta) }
}
