package se.novila.feature.nutrition

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.recipes.Allergen
import se.novila.recipes.Diet
import se.novila.recipes.FoodPreferences
import se.novila.recipes.Recipe
import se.novila.recipes.RecipeSuggestions
import se.novila.recipes.suggestRecipes
import se.novila.ui.common.DayPicker
import se.novila.ui.common.NutritionHistoryCard
import se.novila.ui.common.ScreenList
import se.novila.ui.common.ScreenSection
import se.novila.ui.common.section
import se.novila.ui.preview.PreviewData
import java.time.LocalDate

/** Today's recipe suggestions for the user's food preferences, what logging one does and where to change them. */
data class RecipeActions(
    val onLog: (Recipe) -> Unit = {},
    val onOpenPreferences: () -> Unit = {},
)

/**
 * The Nutrition tab in the parts every tab has ([ScreenSection]): the shown [day]'s nutrition score with its
 * breakdown, a meal typed in the user's own words ([onLogMeal]), logging per food group and drinks
 * ([drinkActions]), and the scores in the [history] up to it, over a week, four weeks or a year ([onRange]), water
 * against [goals]. [today] is the day it really is: the user can go back to an earlier day ([onSelectDay]) and fill
 * in what was forgotten. On today, the plan is [recipes] for what is still missing, chosen for [foodPreferences];
 * [expandedRecipe] is the one shown open at first.
 */
@Composable
fun NutritionScreen(
    day: DayEntry,
    history: History,
    goals: Goals,
    onPortions: (FoodGroup, Int) -> Unit,
    onTreats: (Int) -> Unit,
    onLogMeal: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    drinkActions: DrinkActions = DrinkActions(),
    today: LocalDate = day.date,
    onSelectDay: (LocalDate) -> Unit = {},
    onRange: (HistoryRange) -> Unit = {},
    recipes: RecipeSuggestions? = null,
    foodPreferences: FoodPreferences = FoodPreferences(),
    recipeActions: RecipeActions = RecipeActions(),
    expandedRecipe: Recipe? = null,
) {
    val isToday = day.date == today
    var expanded by rememberSaveable(expandedRecipe) { mutableStateOf(expandedRecipe) }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "day") { DayPicker(day.date, today, onSelectDay, NovilaTheme.pillars.nutrition) }
        section(ScreenSection.Overview)
        item(key = "score") { NovilaReveal(order = 0) { NutritionScoreCard(day, goals, isToday) } }
        section(ScreenSection.Log)
        item(key = "meal") { NovilaReveal(order = 1) { MealLogCard(onLogMeal) } }
        item(key = "log") { NovilaReveal(order = 2) { FoodLogCard(day, onPortions, onTreats, isToday) } }
        item(key = "drinks") { NovilaReveal(order = 2) { DrinksCard(day, goals, drinkActions, isToday) } }
        if (isToday && recipes != null) {
            section(ScreenSection.Plan)
            item(key = "recipes") {
                NovilaReveal(order = 3) {
                    RecipeCard(
                        suggestions = recipes,
                        preferences = foodPreferences,
                        expanded = expanded,
                        onExpand = { expanded = it },
                        onLog = {
                            expanded = null
                            recipeActions.onLog(it)
                        },
                        onOpenPreferences = recipeActions.onOpenPreferences,
                    )
                }
            }
        }
        section(ScreenSection.Trends)
        item(key = "history") { NovilaReveal(order = 4) { NutritionHistoryCard(history, goals, onRange) } }
    }
}

@Preview(showBackground = true, heightDp = 2200)
@Composable
private fun NutritionScreenPreview() {
    NovilaTheme {
        NutritionScreen(
            PreviewData.today,
            PreviewData.history(HistoryRange.Week),
            Goals(),
            onPortions = { _, _ -> },
            onTreats = {},
            onLogMeal = {},
            contentPadding = PaddingValues(),
            recipes = suggestRecipes(PreviewData.today, hour = 12, preferences = FoodPreferences()),
        )
    }
}

/** A vegetarian without nuts, with the first recipe opened. */
@Preview(showBackground = true, heightDp = 2600)
@Composable
private fun NutritionScreenRecipePreview() {
    val preferences = FoodPreferences(Diet.Vegetarian, setOf(Allergen.TreeNuts), maxMinutes = 30)
    val recipes = suggestRecipes(PreviewData.today, hour = 12, preferences = preferences)
    NovilaTheme {
        NutritionScreen(
            PreviewData.today,
            PreviewData.history(HistoryRange.Week),
            Goals(),
            onPortions = { _, _ -> },
            onTreats = {},
            onLogMeal = {},
            contentPadding = PaddingValues(),
            recipes = recipes,
            foodPreferences = preferences,
            expandedRecipe = recipes.recipes.firstOrNull()?.recipe,
        )
    }
}

/** Gone back to yesterday. */
@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun NutritionScreenYesterdayPreview() {
    NovilaTheme {
        NutritionScreen(
            day = PreviewData.history.first(),
            history = PreviewData.history(HistoryRange.Week, until = PreviewData.history.first().date),
            goals = Goals(),
            onPortions = { _, _ -> },
            onTreats = {},
            onLogMeal = {},
            contentPadding = PaddingValues(),
            today = PreviewData.date,
        )
    }
}

/** The last four weeks, a bar per week. */
@Preview(showBackground = true, heightDp = 1800)
@Composable
private fun NutritionScreenMonthPreview() {
    NovilaTheme {
        NutritionScreen(
            PreviewData.today,
            PreviewData.history(HistoryRange.Month),
            Goals(),
            onPortions = { _, _ -> },
            onTreats = {},
            onLogMeal = {},
            contentPadding = PaddingValues(),
        )
    }
}
