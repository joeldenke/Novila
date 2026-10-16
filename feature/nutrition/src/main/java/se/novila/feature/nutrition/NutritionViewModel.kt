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
import se.novila.data.Week
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.withPortions
import se.novila.model.withTreats
import java.time.LocalDate

/** The week so far, and the user's [goals] (water counts towards its own). */
data class NutritionUiState(val week: Week, val goals: Goals)

/** The Nutrition tab, scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class NutritionViewModel(private val store: HealthStore, private val goalsStore: GoalsStore) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    /** The week and the goals, or null before the stores' first reads. */
    val state: StateFlow<NutritionUiState?> =
        combine(store.weeks(date), goalsStore.goals.filterNotNull()) { week, goals -> NutritionUiState(week, goals) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): NutritionUiState? {
        val week = store.week(date.value) ?: return null
        return goalsStore.goals.value?.let { NutritionUiState(week, it) }
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        date.value = LocalDate.now()
    }

    fun changePortions(group: FoodGroup, delta: Int) = store.update(LocalDate.now()) { it.withPortions(group, delta) }

    fun changeTreats(delta: Int) = store.update(LocalDate.now()) { it.withTreats(delta) }
}
