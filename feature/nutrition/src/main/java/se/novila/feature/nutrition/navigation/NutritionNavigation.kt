package se.novila.feature.nutrition.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.designsystem.component.NovilaSky
import se.novila.feature.nutrition.DrinkActions
import se.novila.feature.nutrition.NutritionNavigation
import se.novila.feature.nutrition.NutritionScreen
import se.novila.feature.nutrition.NutritionTrendsScreen
import se.novila.feature.nutrition.NutritionTrendsViewModel
import se.novila.feature.nutrition.NutritionViewModel
import se.novila.feature.nutrition.OwnRecipeActions
import se.novila.feature.nutrition.OwnRecipeScreen
import se.novila.feature.nutrition.OwnRecipeViewModel
import se.novila.feature.nutrition.R
import se.novila.feature.nutrition.RecipeActions
import se.novila.feature.nutrition.RecipesScreen
import se.novila.feature.nutrition.RecipesViewModel
import se.novila.navigation.Navigator
import se.novila.navigation.SharedScreen
import se.novila.navigation.sharedScreen

/** The Nutrition tab. */
@Serializable data object NutritionRoute : NavKey

/** Today's recipes for what is still missing. */
@Serializable data object RecipesRoute : NavKey

/** Writing a recipe of the user's own, or changing the one with [recipeId] (0 for a new one). */
@Serializable data class OwnRecipeRoute(val recipeId: Int = 0) : NavKey

/** The nutrition scores over a week, four weeks or a year. */
@Serializable data object NutritionTrendsRoute : NavKey

/**
 * The Nutrition tab and the screens opened from it. Each entry gets its own ViewModel from Metro, scoped to the entry
 * (`metroViewModel()`). Navigation inside the tab goes through [navigator]; [onOpenFoodPreferences] opens the food
 * preferences in Settings, which `:app` connects.
 */
fun EntryProviderScope<NavKey>.nutritionEntries(
    navigator: Navigator,
    onOpenFoodPreferences: () -> Unit,
    contentPadding: PaddingValues,
) {
    entry<NutritionRoute> {
        val viewModel = metroViewModel<NutritionViewModel>()
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        // Nothing to show until the saved days are read, a moment at app start.
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        val setupIncomplete = viewModel.setupIncomplete.collectAsStateWithLifecycle().value
        val quickPicks = viewModel.quickPicks.collectAsStateWithLifecycle().value
        NutritionScreen(
            day = state.day,
            history = state.history,
            goals = state.goals,
            onPortions = viewModel::changePortions,
            onTreats = viewModel::changeTreats,
            onRedMeat = viewModel::changeRedMeat,
            onFish = viewModel::changeFish,
            onLogMeal = viewModel::logMeal,
            drinkActions = DrinkActions(onAdd = viewModel::addDrink, onUndo = viewModel::removeLastDrink),
            contentPadding = contentPadding,
            today = state.today,
            onSelectDay = viewModel::selectDay,
            recipes = state.recipes,
            foodPreferences = state.foodPreferences,
            navigation = NutritionNavigation(
                onOpenRecipes = { navigator.navigate(RecipesRoute) },
                onOpenTrends = { navigator.navigate(NutritionTrendsRoute) },
                onOpenPreferences = onOpenFoodPreferences,
            ),
            setupIncomplete = setupIncomplete,
            onCompleteSetup = viewModel::openSetup,
            quickPicks = quickPicks,
            onToggleFavorite = viewModel::toggleFavorite,
            sky = NovilaSky.at(state.hour),
        )
    }
    entry<RecipesRoute>(metadata = sharedScreen(SharedScreen.Recipes)) {
        val viewModel = metroViewModel<RecipesViewModel>()
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        RecipesScreen(
            recipes = state.recipes,
            preferences = state.preferences,
            book = state.book,
            actions = RecipeActions(
                onLog = viewModel::logRecipe,
                onLogOwn = viewModel::logOwnRecipe,
                onOpenPreferences = onOpenFoodPreferences,
                onWrite = { navigator.navigate(OwnRecipeRoute()) },
                onEdit = { navigator.navigate(OwnRecipeRoute(it)) },
            ),
            contentPadding = contentPadding,
        )
    }
    entry<OwnRecipeRoute> { route ->
        val viewModel = assistedMetroViewModel<OwnRecipeViewModel, OwnRecipeViewModel.Factory> {
            create(route.recipeId)
        }
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        OwnRecipeScreen(
            draft = state.draft,
            new = state.new,
            actions = OwnRecipeActions(
                onChange = viewModel::change,
                onSave = {
                    viewModel.save()
                    navigator.goBack()
                },
                onDelete = {
                    viewModel.delete()
                    navigator.goBack()
                },
            ),
            contentPadding = contentPadding,
        )
    }
    entry<NutritionTrendsRoute>(metadata = sharedScreen(SharedScreen.NutritionTrends)) {
        val viewModel = metroViewModel<NutritionTrendsViewModel>()
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        NutritionTrendsScreen(
            history = state.history,
            goals = state.goals,
            onRange = viewModel::selectRange,
            contentPadding = contentPadding,
        )
    }
}

/** The top bar title of a screen inside the Nutrition tab, or null when [route] is not one of them. */
@Composable
fun nutritionDetailTitle(route: NavKey?): String? = when (route) {
    RecipesRoute -> stringResource(R.string.nutrition_entry_recipes)

    is OwnRecipeRoute -> stringResource(
        if (route.recipeId == 0) R.string.own_recipe_title_new else R.string.own_recipe_title_edit,
    )

    NutritionTrendsRoute -> stringResource(R.string.nutrition_entry_trends)

    else -> null
}
