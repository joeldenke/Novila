package se.novila.feature.nutrition.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.serialization.Serializable
import se.novila.feature.nutrition.DrinkActions
import se.novila.feature.nutrition.NutritionScreen
import se.novila.feature.nutrition.NutritionViewModel
import se.novila.feature.nutrition.RecipeActions

/** The Nutrition tab. */
@Serializable data object NutritionRoute : NavKey

/**
 * The Nutrition tab's entry, with its own [NutritionViewModel] scoped to the entry. [onOpenFoodPreferences] opens the
 * food preferences in Settings, which `:app` connects.
 */
fun EntryProviderScope<NavKey>.nutritionEntry(onOpenFoodPreferences: () -> Unit, contentPadding: PaddingValues) {
    entry<NutritionRoute> {
        val viewModel = metroViewModel<NutritionViewModel>()
        LifecycleResumeEffect(viewModel) {
            viewModel.refresh()
            onPauseOrDispose { }
        }
        // Nothing to show until the saved days are read, a moment at app start.
        val state = viewModel.state.collectAsStateWithLifecycle().value ?: return@entry
        val setupIncomplete = viewModel.setupIncomplete.collectAsStateWithLifecycle().value
        NutritionScreen(
            day = state.day,
            history = state.history,
            goals = state.goals,
            onPortions = viewModel::changePortions,
            onTreats = viewModel::changeTreats,
            drinkActions = DrinkActions(onAdd = viewModel::addDrink, onUndo = viewModel::removeLastDrink),
            contentPadding = contentPadding,
            today = state.today,
            onSelectDay = viewModel::selectDay,
            onRange = viewModel::selectRange,
            recipes = state.recipes,
            foodPreferences = state.foodPreferences,
            recipeActions = RecipeActions(onLog = viewModel::logRecipe, onOpenPreferences = onOpenFoodPreferences),
            setupIncomplete = setupIncomplete,
            onCompleteSetup = viewModel::openSetup,
        )
    }
}
