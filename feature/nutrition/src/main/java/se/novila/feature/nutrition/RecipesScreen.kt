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
import se.novila.recipes.Allergen
import se.novila.recipes.Diet
import se.novila.recipes.FoodPreferences
import se.novila.recipes.Recipe
import se.novila.recipes.RecipeSuggestions
import se.novila.recipes.suggestRecipes
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData

/**
 * Today's [recipes] for what is still missing, chosen for the user's food [preferences], opened from the Nutrition
 * tab. Tapping one shows its ingredients and steps ([expandedRecipe] is open at first); [actions] log one serving or
 * open the food preferences.
 */
@Composable
fun RecipesScreen(
    recipes: RecipeSuggestions,
    preferences: FoodPreferences,
    actions: RecipeActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    expandedRecipe: Recipe? = null,
) {
    var expanded by rememberSaveable(expandedRecipe) { mutableStateOf(expandedRecipe) }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "recipes") {
            NovilaReveal(order = 0) {
                RecipeCard(
                    suggestions = recipes,
                    preferences = preferences,
                    expanded = expanded,
                    onExpand = { expanded = it },
                    onLog = {
                        expanded = null
                        actions.onLog(it)
                    },
                    onOpenPreferences = actions.onOpenPreferences,
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun RecipesScreenPreview() {
    NovilaTheme {
        RecipesScreen(
            recipes = suggestRecipes(PreviewData.today, hour = 12, preferences = FoodPreferences()),
            preferences = FoodPreferences(),
            actions = RecipeActions(),
            contentPadding = PaddingValues(),
        )
    }
}

/** A vegetarian without nuts, with the first recipe opened. */
@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun RecipesScreenOpenPreview() {
    val preferences = FoodPreferences(Diet.Vegetarian, setOf(Allergen.TreeNuts), maxMinutes = 30)
    val recipes = suggestRecipes(PreviewData.today, hour = 12, preferences = preferences)
    NovilaTheme {
        RecipesScreen(
            recipes = recipes,
            preferences = preferences,
            actions = RecipeActions(),
            contentPadding = PaddingValues(),
            expandedRecipe = recipes.recipes.firstOrNull()?.recipe,
        )
    }
}
