package se.novila.feature.nutrition

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import se.novila.designsystem.component.NovilaSky
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.DrinkKind
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.MAX_DRINKS
import se.novila.model.MealPick
import se.novila.model.QuickPicks
import se.novila.model.averageNutrition
import se.novila.model.fishDaysThisWeek
import se.novila.navigation.SharedScreen
import se.novila.recipes.FoodPreferences
import se.novila.recipes.RecipeSuggestions
import se.novila.recipes.suggestRecipes
import se.novila.ui.common.DayPicker
import se.novila.ui.common.ScreenList
import se.novila.ui.common.foodPreferencesSummary
import se.novila.ui.common.setupHint
import se.novila.ui.preview.PreviewData
import java.time.LocalDate
import se.novila.ui.R as UiR

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
 * food preferences. Food ([onLogMeal], [onPortions], [onTreats], [onRedMeat], [onFish]) and drinks ([drinkActions])
 * are logged in a sheet ([NutritionSheet]), [openSheet] at first, each with the [quickPicks] to log again with one tap
 * and a star that keeps a meal first ([onToggleFavorite]). [today] is the day it really is: the user can go back to an
 * earlier day ([onSelectDay]) and fill in what was forgotten. When the introduction was skipped ([setupIncomplete]), a
 * quiet card under the day picker leads to finishing the setup ([onCompleteSetup]).
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
    quickPicks: QuickPicks = QuickPicks(),
    onToggleFavorite: (String) -> Unit = {},
    /** The introduction was skipped, so the setup is unfinished: one quiet card offers to finish it. */
    setupIncomplete: Boolean = false,
    onCompleteSetup: () -> Unit = {},
    sky: NovilaSky = NovilaSky.Day,
    onRedMeat: (Int) -> Unit = {},
    onFish: (Int) -> Unit = {},
) {
    val isToday = day.date == today
    var sheet by rememberSaveable(openSheet) { mutableStateOf(openSheet) }
    ScreenList(
        contentPadding = contentPadding,
        modifier = modifier,
        aside = {
            item(key = "entries") {
                NovilaReveal(order = 2) {
                    // Recipes are for today only.
                    val recipesToday = recipes.takeIf { isToday }
                    NovilaEntries(nutritionHubEntries(history, goals, recipesToday, foodPreferences, navigation))
                }
            }
        },
    ) {
        item(key = "day") { DayPicker(day.date, today, onSelectDay, NovilaTheme.pillars.nutrition) }
        setupHint(setupIncomplete, onCompleteSetup)
        item(key = "score") {
            NovilaReveal(order = 0) { NutritionOverview(day, history.days, goals, isToday, sky = sky) }
        }
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
    }
    when (sheet) {
        NutritionSheet.Food -> NovilaBottomSheet(
            title = stringResource(R.string.nutrition_sheet_food),
            onDismiss = { sheet = null },
        ) {
            FoodSheet(
                day = day,
                goals = goals,
                onPortions = onPortions,
                onTreats = onTreats,
                onLogMeal = onLogMeal,
                isToday = isToday,
                picks = quickPicks.meals,
                onToggleFavorite = onToggleFavorite,
                onRedMeat = onRedMeat,
                onFish = onFish,
                fishDays = fishDaysThisWeek(history.days.filter { it.date != day.date } + day, day.date),
            )
        }

        NutritionSheet.Drinks -> NovilaBottomSheet(
            title = stringResource(R.string.nutrition_sheet_drinks),
            onDismiss = { sheet = null },
        ) {
            DrinksCard(day, goals, drinkActions, isToday, quickPicks.drinks)
        }

        null -> Unit
    }
}

/**
 * The usual meals with one tap, then food in the user's own words or per food group, and last how the day's score
 * adds up, as the sheet shows it.
 */
@Composable
internal fun FoodSheet(
    day: DayEntry,
    goals: Goals,
    onPortions: (FoodGroup, Int) -> Unit,
    onTreats: (Int) -> Unit,
    onLogMeal: (String) -> Unit,
    isToday: Boolean,
    picks: List<MealPick> = emptyList(),
    onToggleFavorite: (String) -> Unit = {},
    onRedMeat: (Int) -> Unit = {},
    onFish: (Int) -> Unit = {},
    fishDays: Int? = null,
) {
    QuickPicksCard(picks, onLogMeal, onToggleFavorite)
    MealLogCard(onLogMeal)
    FoodLogCard(day, onPortions, onTreats, isToday, onRedMeat, onFish)
    ScoreBreakdownCard(day, goals, isToday, fishDays)
}

/** Food and other drinks open their sheets; water, logged most often, is one tap. */
@Composable
private fun LogActions(day: DayEntry, onFood: () -> Unit, onWater: () -> Unit, onDrink: () -> Unit) {
    val nutrition = NovilaTheme.pillars.nutrition
    val water = DrinkKind.Water
    NovilaQuickActions(
        title = stringResource(R.string.nutrition_log_title),
        icon = painterResource(UiR.drawable.ic_nutrition),
        role = nutrition,
    ) {
        NovilaButton(
            text = stringResource(R.string.nutrition_log_food),
            onClick = onFood,
            role = nutrition,
            modifier = Modifier.weight(1f).fillMaxHeight(),
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
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        NovilaButton(
            text = stringResource(R.string.nutrition_log_drink),
            onClick = onDrink,
            role = nutrition,
            modifier = Modifier.weight(1f).fillMaxHeight(),
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
                sharedKey = SharedScreen.Recipes,
                summary = it.recipes.firstOrNull()?.let { first -> stringResource(first.recipe.text.name) }
                    ?: stringResource(R.string.nutrition_entry_recipes_none),
            )
        },
        NovilaEntry(
            title = stringResource(R.string.nutrition_entry_trends),
            icon = painterResource(UiR.drawable.ic_trend),
            role = nutrition,
            onClick = navigation.onOpenTrends,
            sharedKey = SharedScreen.NutritionTrends,
            summary = average?.let { stringResource(R.string.nutrition_entry_trends_summary, it) }
                ?: stringResource(R.string.nutrition_entry_trends_empty),
        ),
        NovilaEntry(
            title = stringResource(R.string.nutrition_entry_preferences),
            icon = painterResource(UiR.drawable.ic_nutrition),
            role = nutrition,
            onClick = navigation.onOpenPreferences,
            sharedKey = SharedScreen.FoodPreferences,
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

/** The food sheet as it slides up: the usual meals, a meal in the user's own words, and the food groups. */
@Preview(showBackground = true, heightDp = 1700)
@Composable
private fun FoodSheetPreview() {
    NovilaTheme {
        NovilaSheetContent(stringResource(R.string.nutrition_sheet_food)) {
            FoodSheet(
                PreviewData.today,
                Goals(),
                onPortions = { _, _ -> },
                onTreats = {},
                onLogMeal = {},
                isToday = true,
                picks = listOf(
                    MealPick("Frukost: gröt, bär", favorite = true),
                    MealPick("Lunch: lax, potatis, sallad", favorite = false),
                ),
            )
        }
    }
}

/** The food sheet before any meal is written in the user's own words: no usual meals yet. */
@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun FoodSheetFirstTimePreview() {
    NovilaTheme {
        NovilaSheetContent(stringResource(R.string.nutrition_sheet_food)) {
            FoodSheet(
                PreviewData.today,
                Goals(),
                onPortions = { _, _ -> },
                onTreats = {},
                onLogMeal = {},
                isToday = true,
            )
        }
    }
}
