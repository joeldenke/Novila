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
import se.novila.data.Week
import se.novila.model.Goals
import java.time.LocalDate

/** The week so far, and the user's [goals]. */
data class SleepUiState(val week: Week, val goals: Goals)

/** The Sleep tab, scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class SleepViewModel(private val store: HealthStore, private val goalsStore: GoalsStore) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    /** The week and the goals, or null before the stores' first reads. */
    val state: StateFlow<SleepUiState?> =
        combine(store.weeks(date), goalsStore.goals.filterNotNull()) { week, goals -> SleepUiState(week, goals) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): SleepUiState? {
        val week = store.week(date.value) ?: return null
        return goalsStore.goals.value?.let { SleepUiState(week, it) }
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        date.value = LocalDate.now()
    }

    fun setSleep(hours: Float) = store.update(LocalDate.now()) { it.withSleep(hours) }
}
