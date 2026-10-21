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
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaListRow
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.recipes.Allergen
import se.novila.recipes.Diet
import se.novila.recipes.FoodPreferences
import se.novila.recipes.MealSlot
import se.novila.recipes.OwnRecipe
import se.novila.recipes.Recipe
import se.novila.recipes.RecipeBook
import se.novila.recipes.RecipeSuggestions
import se.novila.recipes.key
import se.novila.recipes.suggestRecipes
import se.novila.ui.common.ScreenList
import se.novila.ui.preview.PreviewData

/**
 * Today's recipe suggestions for the user's food preferences: logging one of Novila's ([onLog]) or the user's own
 * ([onLogOwn]), changing the preferences, and writing ([onWrite]) or opening ([onEdit], by id) the user's own recipes.
 */
data class RecipeActions(
    val onLog: (Recipe) -> Unit = {},
    val onLogOwn: (OwnRecipe) -> Unit = {},
    val onOpenPreferences: () -> Unit = {},
    val onWrite: () -> Unit = {},
    val onEdit: (Int) -> Unit = {},
)

/**
 * Today's [recipes] for what is still missing, chosen for the user's food [preferences], opened from the Nutrition
 * tab, and the user's own recipes in [book]. Tapping a suggestion shows its ingredients and steps ([expandedRecipe],
 * a recipe's key, is open at first); [actions] log one serving, open the food preferences or the user's recipes.
 */
@Composable
fun RecipesScreen(
    recipes: RecipeSuggestions,
    preferences: FoodPreferences,
    book: RecipeBook,
    actions: RecipeActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    expandedRecipe: String? = null,
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
                    actions = actions.copy(
                        onLog = {
                            expanded = null
                            actions.onLog(it)
                        },
                        onLogOwn = {
                            expanded = null
                            actions.onLogOwn(it)
                        },
                    ),
                )
            }
        }
        item(key = "own") {
            NovilaReveal(order = 1) {
                OwnRecipesCard(book, onWrite = actions.onWrite, onEdit = actions.onEdit)
            }
        }
    }
}

/**
 * The recipes the user wrote, each opening in the editor, and a button to write one more. What a recipe gives is
 * counted from its ingredients, so the card says that up front.
 */
@Composable
private fun OwnRecipesCard(book: RecipeBook, onWrite: () -> Unit, onEdit: (Int) -> Unit) {
    val nutrition = NovilaTheme.pillars.nutrition
    NovilaCard(
        title = stringResource(R.string.own_recipes_title),
        icon = painterResource(R.drawable.ic_recipe),
        role = nutrition,
    ) {
        Hint(stringResource(if (book.recipes.isEmpty()) R.string.own_recipes_empty else R.string.own_recipes_hint))
        book.recipes.forEach { recipe ->
            NovilaListRow(
                title = recipe.name,
                summary = stringResource(R.string.own_recipe_row, recipe.minutes, mealSlotName(recipe.slot)),
                onClick = { onEdit(recipe.id) },
                onClickLabel = stringResource(R.string.own_recipe_edit),
            )
        }
        if (book.isFull) {
            Hint(stringResource(R.string.own_recipes_full))
        } else {
            NovilaButton(text = stringResource(R.string.own_recipe_write), onClick = onWrite, role = nutrition)
        }
    }
}

private val previewBook = RecipeBook()
    .saved(
        OwnRecipe(
            name = "Mormors linssoppa",
            minutes = 30,
            ingredients = listOf("2 dl röda linser", "1 morot", "1 burk krossade tomater", "1 dl grädde"),
            steps = listOf("Koka linser och morot i 20 minuter.", "Rör ner tomater och grädde."),
            allergens = setOf(Allergen.Milk),
        ),
    )
    .saved(OwnRecipe(name = "Pannkakor", slot = MealSlot.Breakfast, ingredients = listOf("mjöl")))

@Preview(showBackground = true, heightDp = 1200)
@Composable
private fun RecipesScreenPreview() {
    NovilaTheme {
        RecipesScreen(
            recipes = suggestRecipes(PreviewData.today, hour = 12, preferences = FoodPreferences(), book = previewBook),
            preferences = FoodPreferences(),
            book = previewBook,
            actions = RecipeActions(),
            contentPadding = PaddingValues(),
        )
    }
}

/** A vegetarian without nuts, with the first recipe opened, and no recipes of their own yet. */
@Preview(showBackground = true, heightDp = 1600)
@Composable
private fun RecipesScreenOpenPreview() {
    val preferences = FoodPreferences(Diet.Vegetarian, setOf(Allergen.TreeNuts), maxMinutes = 30)
    val recipes = suggestRecipes(PreviewData.today, hour = 12, preferences = preferences)
    NovilaTheme {
        RecipesScreen(
            recipes = recipes,
            preferences = preferences,
            book = RecipeBook(),
            actions = RecipeActions(),
            contentPadding = PaddingValues(),
            expandedRecipe = recipes.recipes.firstOrNull()?.recipe?.key,
        )
    }
}
