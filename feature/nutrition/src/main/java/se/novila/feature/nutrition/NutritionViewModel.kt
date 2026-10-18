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
import se.novila.data.FoodPreferencesStore
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
import se.novila.recipes.FoodPreferences
import se.novila.recipes.Recipe
import se.novila.recipes.RecipeSuggestions
import se.novila.recipes.suggestRecipes
import se.novila.recipes.withRecipe
import java.time.LocalDate
import java.time.LocalTime

/**
 * The [history] over the chosen range up to the shown day (today, or an earlier one the user went back to), [today],
 * the user's [goals] (water counts towards its own) and [foodPreferences], and the [hour] it is now.
 */
data class NutritionUiState(
    val history: History,
    val goals: Goals,
    val today: LocalDate,
    val foodPreferences: FoodPreferences,
    val hour: Int,
) {
    /** The shown day, the last in the history. */
    val day: DayEntry get() = history.days.last()

    /** Recipes for what the shown day is missing, now. */
    val recipes: RecipeSuggestions = suggestRecipes(day, hour, foodPreferences, goals)
}

/** The Nutrition tab, scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class NutritionViewModel(
    private val store: HealthStore,
    private val goalsStore: GoalsStore,
    private val foodPreferencesStore: FoodPreferencesStore,
) : ViewModel() {
    private val day = SelectedDay()
    private val range = MutableStateFlow(HistoryRange.Week)
    private val hour = MutableStateFlow(LocalTime.now().hour)

    /** The history, the goals and the food preferences, or null before the stores' first reads. */
    val state: StateFlow<NutritionUiState?> =
        combine(
            store.histories(day.dates, range),
            goalsStore.goals.filterNotNull(),
            day.today,
            foodPreferencesStore.preferences.filterNotNull(),
            hour,
            ::NutritionUiState,
        ).stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): NutritionUiState? {
        val history = store.history(range.value, day.value) ?: return null
        val goals = goalsStore.goals.value ?: return null
        val preferences = foodPreferencesStore.preferences.value ?: return null
        return NutritionUiState(history, goals, day.today.value, preferences, hour.value)
    }

    /** Called on resume so "today" stays correct after midnight, and the recipes fit the time of day. */
    fun refresh() {
        day.refresh()
        hour.value = LocalTime.now().hour
    }

    /** Shows and changes [date] instead, to fill in what was eaten on a day that was forgotten. */
    fun selectDay(date: LocalDate) = day.select(date)

    /** Shows the scores over a week, four weeks or a year. */
    fun selectRange(range: HistoryRange) {
        this.range.value = range
    }

    fun changePortions(group: FoodGroup, delta: Int) = store.update(day.value) { it.withPortions(group, delta) }

    fun changeTreats(delta: Int) = store.update(day.value) { it.withTreats(delta) }

    /** Logs one serving of [recipe]: its portions in each food group, and a meal unless it is a snack. */
    fun logRecipe(recipe: Recipe) = store.update(day.value) { it.withRecipe(recipe) }
}
