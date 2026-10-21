package se.novila.feature.nutrition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.recipes.Allergen
import se.novila.recipes.FoodPreferences
import se.novila.recipes.MealSlot
import se.novila.recipes.OwnRecipe
import se.novila.recipes.OwnRecipeSuggestion
import se.novila.recipes.Recipe
import se.novila.recipes.RecipeSuggestion
import se.novila.recipes.RecipeSuggestions

@RunWith(RobolectricTestRunner::class)
class RecipeCardTest {
    @get:Rule val composeRule = createComposeRule()

    private val suggestions = RecipeSuggestions(
        MealSlot.Main,
        listOf(RecipeSuggestion(Recipe.LentilBolognese, points = 23), RecipeSuggestion(Recipe.BakedSalmon, points = 0)),
    )

    private fun show(suggestions: RecipeSuggestions, logged: MutableList<Any> = mutableListOf()) {
        var expanded by mutableStateOf<String?>(null)
        composeRule.setContent {
            NovilaTheme {
                // The open recipe is longer than the test's screen.
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    RecipeCard(
                        suggestions = suggestions,
                        preferences = FoodPreferences(),
                        expanded = expanded,
                        onExpand = { expanded = it },
                        actions = RecipeActions(onLog = { logged += it }, onLogOwn = { logged += it }),
                    )
                }
            }
        }
    }

    @Test fun `a recipe opens to its ingredients, steps and allergens and can be logged`() {
        val logged = mutableListOf<Any>()
        show(suggestions, logged)

        composeRule.onNodeWithText("30 min · +23 to the nutrition score").assertExists()
        composeRule.onNodeWithText("Lentil bolognese with whole grain pasta").performClick()
        composeRule.onNodeWithText("• ½ dl red lentils").assertExists()
        composeRule.onNodeWithText("Contains: Gluten").assertExists()
        composeRule.onNodeWithText("Gives: Vegetables & fruit 2, Meals 1, Protein 1, Whole grain 1").assertExists()
        composeRule.onNodeWithText("I ate this").performScrollTo().performClick()

        assertEquals(listOf(Recipe.LentilBolognese), logged)
    }

    @Test fun `without recipes the card says nothing fits`() {
        show(RecipeSuggestions(MealSlot.Main, emptyList()))

        composeRule.onNodeWithText("No recipe fits your food preferences right now.").assertExists()
    }

    @Test fun `the user's own recipe comes first, with what its ingredients give and the allergens they confirmed`() {
        val soup = OwnRecipe(
            name = "Grandma's lentil soup",
            minutes = 25,
            ingredients = listOf("2 dl lentils", "1 carrot", "1 dl cream"),
            steps = listOf("Boil everything."),
            allergens = setOf(Allergen.Milk, Allergen.Celery),
            id = 1,
        )
        val logged = mutableListOf<Any>()
        show(suggestions.copy(own = listOf(OwnRecipeSuggestion(soup, points = 12))), logged)

        composeRule.onNodeWithText("Your recipe · 25 min · +12 to the nutrition score").assertExists()
        composeRule.onNodeWithText("Grandma's lentil soup").performClick()
        composeRule.onNodeWithText("• 1 dl cream").assertExists()
        composeRule.onNodeWithText("Contains: Milk, Celery").assertExists()
        composeRule.onNodeWithText("Gives: Vegetables & fruit 1, Meals 1, Protein 1").assertExists()
        composeRule.onNodeWithText("I ate this").performScrollTo().performClick()

        assertEquals(listOf<Any>(soup), logged)
    }
}
