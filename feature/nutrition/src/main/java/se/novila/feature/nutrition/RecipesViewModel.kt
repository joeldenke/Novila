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
import se.novila.model.Goals
import se.novila.recipes.FoodPreferences
import se.novila.recipes.Recipe
import se.novila.recipes.RecipeSuggestions
import se.novila.recipes.suggestRecipes
import se.novila.recipes.withRecipe
import java.time.LocalTime

/** Today's [recipes] for what is still missing, chosen for the user's food [preferences]. */
data class RecipesUiState(val recipes: RecipeSuggestions, val preferences: FoodPreferences)

/** The recipes, opened from the Nutrition tab, scoped to their entry. Always for today. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class RecipesViewModel(
    private val store: HealthStore,
    private val goalsStore: GoalsStore,
    private val foodPreferencesStore: FoodPreferencesStore,
) : ViewModel() {
    private val day = SelectedDay()
    private val hour = MutableStateFlow(LocalTime.now().hour)

    /** The recipes and the preferences, or null before the stores' first reads. */
    val state: StateFlow<RecipesUiState?> =
        combine(
            store.days.filterNotNull(),
            goalsStore.goals.filterNotNull(),
            foodPreferencesStore.preferences.filterNotNull(),
            day.today,
            hour,
        ) { saved, goals, preferences, today, hour -> read(saved.day(today), goals, preferences, hour) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): RecipesUiState? {
        val saved = store.days.value ?: return null
        val goals = goalsStore.goals.value ?: return null
        val preferences = foodPreferencesStore.preferences.value ?: return null
        return read(saved.day(day.today.value), goals, preferences, hour.value)
    }

    private fun read(today: DayEntry, goals: Goals, preferences: FoodPreferences, hour: Int) =
        RecipesUiState(suggestRecipes(today, hour, preferences, goals), preferences)

    /** Called on resume so "today" stays correct after midnight, and the recipes fit the time of day. */
    fun refresh() {
        day.refresh()
        hour.value = LocalTime.now().hour
    }

    /** Logs one serving of [recipe] today: its portions in each food group, and a meal unless it is a snack. */
    fun logRecipe(recipe: Recipe) = store.update(day.today.value) { it.withRecipe(recipe) }
}
