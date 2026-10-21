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
import se.novila.recipes.OwnRecipe
import se.novila.recipes.Recipe
import se.novila.recipes.RecipeBook
import se.novila.recipes.RecipeSuggestions
import se.novila.recipes.key
import se.novila.recipes.suggestRecipes
import se.novila.recipes.withRecipe
import java.time.LocalTime

/** Today's [recipes] for what is still missing, chosen for the user's food [preferences], and their own [book]. */
data class RecipesUiState(val recipes: RecipeSuggestions, val preferences: FoodPreferences, val book: RecipeBook)

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

    private val food = combine(
        foodPreferencesStore.preferences.filterNotNull(),
        foodPreferencesStore.recipes.filterNotNull(),
        ::Pair,
    )

    /** The recipes, the preferences and the user's own recipes, or null before the stores' first reads. */
    val state: StateFlow<RecipesUiState?> =
        combine(
            store.days.filterNotNull(),
            goalsStore.goals.filterNotNull(),
            food,
            day.today,
            hour,
        ) { saved, goals, (preferences, book), today, hour -> read(saved.day(today), goals, preferences, book, hour) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): RecipesUiState? {
        val saved = store.days.value ?: return null
        val goals = goalsStore.goals.value ?: return null
        val preferences = foodPreferencesStore.preferences.value ?: return null
        val book = foodPreferencesStore.recipes.value ?: return null
        return read(saved.day(day.today.value), goals, preferences, book, hour.value)
    }

    private fun read(today: DayEntry, goals: Goals, preferences: FoodPreferences, book: RecipeBook, hour: Int) =
        RecipesUiState(suggestRecipes(today, hour, preferences, goals, book), preferences, book)

    /** Called on resume so "today" stays correct after midnight, and the recipes fit the time of day. */
    fun refresh() {
        day.refresh()
        hour.value = LocalTime.now().hour
    }

    /**
     * Logs one serving of [recipe] today: its portions in each food group, and a meal unless it is a snack. It is
     * remembered as cooked, so it comes first among recipes worth as much.
     */
    fun logRecipe(recipe: Recipe) = log(recipe.key) { it.withRecipe(recipe) }

    /** Logs one serving of the user's own [recipe] today, counted from its ingredients, and remembers it as cooked. */
    fun logOwnRecipe(recipe: OwnRecipe) = log(recipe.key) { it.withRecipe(recipe) }

    private fun log(key: String, change: (DayEntry) -> DayEntry) {
        val date = day.today.value
        store.update(date, change)
        foodPreferencesStore.updateRecipes { it.withCooked(key, date) }
    }
}
