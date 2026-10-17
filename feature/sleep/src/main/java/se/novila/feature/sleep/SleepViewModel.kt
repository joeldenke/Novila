package se.novila.feature.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import se.novila.data.GoalsStore
import se.novila.data.HealthStore
import se.novila.data.SelectedDay
import se.novila.data.Week
import se.novila.model.Goals
import java.time.LocalDate

/** The week up to the shown day (today, or an earlier one the user went back to), [today] and the user's [goals]. */
data class SleepUiState(val week: Week, val goals: Goals, val today: LocalDate)

/** The Sleep tab, scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class SleepViewModel(private val store: HealthStore, private val goalsStore: GoalsStore) : ViewModel() {
    private val day = SelectedDay()

    /** The week and the goals, or null before the stores' first reads. */
    val state: StateFlow<SleepUiState?> =
        combine(store.weeks(day.dates), goalsStore.goals.filterNotNull(), day.today, ::SleepUiState)
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): SleepUiState? {
        val week = store.week(day.value) ?: return null
        return goalsStore.goals.value?.let { SleepUiState(week, it, day.today.value) }
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() = day.refresh()

    /** Shows and changes [date] instead, to fill in a night that was forgotten. */
    fun selectDay(date: LocalDate) = day.select(date)

    /** Sets the shown day's sleep. */
    fun setSleep(hours: Float) = store.update(day.value) { it.withSleep(hours) }
}
