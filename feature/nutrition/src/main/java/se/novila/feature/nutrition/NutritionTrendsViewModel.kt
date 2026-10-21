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
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange

/** The nutrition scores over the chosen range up to today, water against the user's [goals]. */
data class NutritionTrendsUiState(val history: History, val goals: Goals)

/** The nutrition scores over time, opened from the Nutrition tab, scoped to their entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class NutritionTrendsViewModel(
    private val store: HealthStore,
    private val goalsStore: GoalsStore,
) : ViewModel() {
    private val day = SelectedDay()
    private val range = MutableStateFlow(HistoryRange.Week)

    /** The history and the goals, or null before the stores' first reads. */
    val state: StateFlow<NutritionTrendsUiState?> =
        combine(store.histories(day.dates, range), goalsStore.goals.filterNotNull(), ::NutritionTrendsUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): NutritionTrendsUiState? {
        val history = store.history(range.value, day.value) ?: return null
        val goals = goalsStore.goals.value ?: return null
        return NutritionTrendsUiState(history, goals)
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() = day.refresh()

    /** Shows the scores over a week, four weeks or a year. */
    fun selectRange(range: HistoryRange) {
        this.range.value = range
    }
}
