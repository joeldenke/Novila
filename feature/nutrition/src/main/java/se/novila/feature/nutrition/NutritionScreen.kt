package se.novila.feature.nutrition

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaBottomSheet
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaEntries
import se.novila.designsystem.component.NovilaEntry
import se.novila.designsystem.component.NovilaEntryLayout
import se.novila.designsystem.component.NovilaQuickActions
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaSheetContent
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.DrinkKind
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.MAX_DRINKS
import se.novila.model.averageNutrition
import se.novila.recipes.FoodPreferences
import se.novila.recipes.Recipe
import se.novila.recipes.RecipeSuggestions
import se.novila.recipes.suggestRecipes
import se.novila.ui.common.DayPicker
import se.novila.ui.common.ScreenList
import se.novila.ui.common.foodPreferencesSummary
import se.novila.ui.common.setupHint
import se.novila.ui.preview.PreviewData
import java.time.LocalDate
import se.novila.ui.R as UiR

/** Today's recipe suggestions for the user's food preferences, what logging one does and where to change them. */
data class RecipeActions(
    val onLog: (Recipe) -> Unit = {},
    val onOpenPreferences: () -> Unit = {},
)

/** Where the Nutrition tab leads: the recipes, the history and the food preferences. */
data class NutritionNavigation(
    val onOpenRecipes: () -> Unit = {},
    val onOpenTrends: () -> Unit = {},
    val onOpenPreferences: () -> Unit = {},
)

/** What slides up over the Nutrition tab to log, so the tab itself stays short. */
enum class NutritionSheet { Food, Drinks }

/**
 * The Nutrition tab: the shown [day]'s nutrition score with its breakdown, then the quick ways to log (food, a glass
 * of water in one tap, any other drink) and the ways into the rest ([navigation]): today's [recipes] for what is still
 * missing, chosen for [foodPreferences], the scores over time from the [history] (a week up to the shown day) and the
 * food preferences. Food ([onLogMeal], [onPortions], [onTreats]) and drinks ([drinkActions]) are logged in a sheet
 * ([NutritionSheet]), [openSheet] at first. [today] is the day it really is: the user can go back to an earlier day
 * ([onSelectDay]) and fill in what was forgotten. When the introduction was skipped ([setupIncomplete]), a quiet card
 * under the day picker leads to finishing the setup ([onCompleteSetup]).
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
    recipes: RecipeSuggestions? = null,
    foodPreferences: FoodPreferences = FoodPreferences(),
    navigation: NutritionNavigation = NutritionNavigation(),
    openSheet: NutritionSheet? = null,
    /** The introduction was skipped, so the setup is unfinished: one quiet card offers to finish it. */
    setupIncomplete: Boolean = false,
    onCompleteSetup: () -> Unit = {},
) {
    val isToday = day.date == today
    var sheet by rememberSaveable(openSheet) { mutableStateOf(openSheet) }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "day") { DayPicker(day.date, today, onSelectDay, NovilaTheme.pillars.nutrition) }
        setupHint(setupIncomplete, onCompleteSetup)
        item(key = "score") { NovilaReveal(order = 0) { NutritionScoreCard(day, goals, isToday) } }
        item(key = "log") {
            NovilaReveal(order = 1) {
                LogActions(
                    day = day,
                    onFood = { sheet = NutritionSheet.Food },
                    onWater = { drinkActions.onAdd(DrinkKind.Water, DrinkKind.Water.defaultMl) },
                    onDrink = { sheet = NutritionSheet.Drinks },
                )
            }
        }
        item(key = "entries") {
            NovilaReveal(order = 2) {
                NovilaEntries(nutritionHubEntries(history, goals, recipes.takeIf { isToday }, foodPreferences, navigation))
            }
        }
    }
    when (sheet) {
        NutritionSheet.Food -> NovilaBottomSheet(
            title = stringResource(R.string.nutrition_sheet_food),
            onDismiss = { sheet = null },
        ) {
            FoodSheet(day, onPortions, onTreats, onLogMeal, isToday)
        }

        NutritionSheet.Drinks -> NovilaBottomSheet(
            title = stringResource(R.string.nutrition_sheet_drinks),
            onDismiss = { sheet = null },
        ) {
            DrinksCard(day, goals, drinkActions, isToday)
        }

        null -> Unit
    }
}

/** Food in the user's own words or per food group, as the sheet shows it. */
@Composable
internal fun FoodSheet(
    day: DayEntry,
    onPortions: (FoodGroup, Int) -> Unit,
    onTreats: (Int) -> Unit,
    onLogMeal: (String) -> Unit,
    isToday: Boolean,
) {
    MealLogCard(onLogMeal)
    FoodLogCard(day, onPortions, onTreats, isToday)
}

/** Food and other drinks open their sheets; water, logged most often, is one tap. */
@Composable
private fun LogActions(day: DayEntry, onFood: () -> Unit, onWater: () -> Unit, onDrink: () -> Unit) {
    val nutrition = NovilaTheme.pillars.nutrition
    val water = DrinkKind.Water
    NovilaQuickActions {
        NovilaButton(
            text = stringResource(R.string.nutrition_log_food),
            onClick = onFood,
            role = nutrition,
            modifier = Modifier.weight(1f),
        )
        NovilaButton(
            text = stringResource(R.string.nutrition_log_water, decilitersText(water.defaultMl)),
            onClick = onWater,
            enabled = day.drinks.size < MAX_DRINKS,
            role = nutrition,
            contentDescription = stringResource(
                R.string.drinks_add,
                decilitersText(water.defaultMl),
                drinkName(water).lowercase(),
            ),
            modifier = Modifier.weight(1f),
        )
        NovilaButton(
            text = stringResource(R.string.nutrition_log_drink),
            onClick = onDrink,
            role = nutrition,
            modifier = Modifier.weight(1f),
        )
    }
}

/** The recipes (on today), the scores over time and the food preferences, each with where it stands. */
@Composable
private fun nutritionHubEntries(
    history: History,
    goals: Goals,
    recipes: RecipeSuggestions?,
    preferences: FoodPreferences,
    navigation: NutritionNavigation,
): List<NovilaEntry> {
    val nutrition = NovilaTheme.pillars.nutrition
    val average = history.days.averageNutrition(goals)
    return listOfNotNull(
        recipes?.let {
            NovilaEntry(
                title = stringResource(R.string.nutrition_entry_recipes),
                icon = painterResource(R.drawable.ic_recipe),
                role = nutrition,
                onClick = navigation.onOpenRecipes,
                summary = it.recipes.firstOrNull()?.let { first -> stringResource(first.recipe.text.name) }
                    ?: stringResource(R.string.nutrition_entry_recipes_none),
            )
        },
        NovilaEntry(
            title = stringResource(R.string.nutrition_entry_trends),
            icon = painterResource(UiR.drawable.ic_trend),
            role = nutrition,
            onClick = navigation.onOpenTrends,
            summary = average?.let { stringResource(R.string.nutrition_entry_trends_summary, it) }
                ?: stringResource(R.string.nutrition_entry_trends_empty),
        ),
        NovilaEntry(
            title = stringResource(R.string.nutrition_entry_preferences),
            icon = painterResource(UiR.drawable.ic_nutrition),
            role = nutrition,
            onClick = navigation.onOpenPreferences,
            summary = foodPreferencesSummary(preferences),
        ),
    )
}

@Composable
private fun NutritionScreenContent(
    day: DayEntry = PreviewData.today,
    today: LocalDate = day.date,
    entryLayout: NovilaEntryLayout = NovilaEntryLayout.Grid,
) {
    NovilaTheme(entryLayout = entryLayout) {
        NutritionScreen(
            day = day,
            history = PreviewData.history(HistoryRange.Week, until = day.date),
            goals = Goals(),
            onPortions = { _, _ -> },
            onTreats = {},
            onLogMeal = {},
            contentPadding = PaddingValues(),
            today = today,
            recipes = suggestRecipes(day, hour = 12, preferences = FoodPreferences()),
        )
    }
}

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun NutritionScreenPreview() = NutritionScreenContent()

/** The ways in as rows, as chosen in Settings. */
@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun NutritionScreenRowsPreview() = NutritionScreenContent(entryLayout = NovilaEntryLayout.Rows)

/** Gone back to yesterday: no recipes, they are for today. */
@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun NutritionScreenYesterdayPreview() = NutritionScreenContent(
    day = PreviewData.history.first(),
    today = PreviewData.date,
)

/** A tablet: the content stays readable in the middle, and the tiles take four to a row. */
@Preview(showBackground = true, widthDp = 900, heightDp = 1000)
@Composable
private fun NutritionScreenTabletPreview() = NutritionScreenContent()

/** The food sheet as it slides up: a meal in the user's own words, and the food groups. */
@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun FoodSheetPreview() {
    NovilaTheme {
        NovilaSheetContent(stringResource(R.string.nutrition_sheet_food)) {
            FoodSheet(PreviewData.today, onPortions = { _, _ -> }, onTreats = {}, onLogMeal = {}, isToday = true)
        }
    }
}
