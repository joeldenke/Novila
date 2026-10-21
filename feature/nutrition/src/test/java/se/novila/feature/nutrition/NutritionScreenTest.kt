package se.novila.feature.nutrition

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DrinkKind
import se.novila.model.Goals
import se.novila.model.HistoryRange
import se.novila.recipes.FoodPreferences
import se.novila.recipes.suggestRecipes
import se.novila.ui.preview.PreviewData

// Tall enough for the day picker and the card below it, so nothing has to be scrolled to.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class NutritionScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(
        setupIncomplete: Boolean = false,
        onCompleteSetup: () -> Unit = {},
        drinkActions: DrinkActions = DrinkActions(),
        navigation: NutritionNavigation = NutritionNavigation(),
    ) {
        composeRule.setContent {
            NovilaTheme {
                NutritionScreen(
                    day = PreviewData.today,
                    history = PreviewData.history(HistoryRange.Week),
                    goals = Goals(),
                    onPortions = { _, _ -> },
                    onTreats = {},
                    onLogMeal = {},
                    contentPadding = PaddingValues(),
                    drinkActions = drinkActions,
                    recipes = suggestRecipes(PreviewData.today, hour = 12, preferences = FoodPreferences()),
                    navigation = navigation,
                    setupIncomplete = setupIncomplete,
                    onCompleteSetup = onCompleteSetup,
                )
            }
        }
    }

    @Test fun `a skipped setup offers to finish it`() {
        var completions = 0
        show(setupIncomplete = true, onCompleteSetup = { completions++ })

        composeRule.onNodeWithText("Finish setup").performClick()

        assertEquals(1, completions)
    }

    @Test fun `nothing asks for the setup when it is finished`() {
        show()

        composeRule.onNodeWithText("Setup not finished").assertDoesNotExist()
    }

    @Test fun `one tap logs a glass of water`() {
        val added = mutableListOf<Pair<DrinkKind, Int>>()
        show(drinkActions = DrinkActions(onAdd = { kind, ml -> added += kind to ml }))

        composeRule.onNodeWithText("+2 dl water").performClick()

        assertEquals(listOf(DrinkKind.Water to DrinkKind.Water.defaultMl), added)
    }

    @Test fun `the entries open the recipes and the trends`() {
        val opened = mutableListOf<String>()
        show(
            navigation = NutritionNavigation(
                onOpenRecipes = { opened += "recipes" },
                onOpenTrends = { opened += "trends" },
            ),
        )

        composeRule.onNodeWithText("Recipes").performClick()
        composeRule.onNodeWithText("Trends").performClick()

        assertEquals(listOf("recipes", "trends"), opened)
    }

    @Test fun `logging food opens its sheet`() {
        show()

        composeRule.onNodeWithText("Log food").performClick()

        composeRule.onNodeWithText("What did you eat?").assertExists()
    }
}
