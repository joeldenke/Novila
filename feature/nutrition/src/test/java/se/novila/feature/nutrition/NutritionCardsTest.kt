package se.novila.feature.nutrition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.Drink
import se.novila.model.DrinkKind
import se.novila.model.Goals
import se.novila.model.MealPick
import se.novila.model.withDrink
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class NutritionCardsTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(content: @Composable () -> Unit) {
        composeRule.setContent {
            NovilaTheme { Column(Modifier.verticalScroll(rememberScrollState())) { content() } }
        }
    }

    @Test fun `the log card explains what a serving is for each food group`() {
        show {
            FoodLogCard(
                today = DayEntry(LocalDate.of(2026, 10, 19)),
                onPortions = { _, _ -> },
                onTreats = {},
            )
        }

        composeRule.onNodeWithText("One serving is about a handful, or 100 g. Potatoes don't count.").assertExists()
        composeRule.onNodeWithText("One serving is a meal: breakfast, lunch or dinner.").assertExists()
        composeRule.onNodeWithText(
            "One serving is about 100 g fish, poultry or meat, an egg, or a handful of beans or lentils.",
        ).assertExists()
        composeRule.onNodeWithText("One serving is a slice of whole grain bread or a portion of pasta, rice or oats.")
            .assertExists()
        composeRule.onNodeWithText("Water").assertDoesNotExist()
    }

    @Test fun `a drink of any kind and amount is added, counted towards the water goal and can be taken back`() {
        val added = mutableListOf<Pair<DrinkKind, Int>>()
        var undone = false
        val day = DayEntry(LocalDate.of(2026, 10, 19))
            .withDrink(Drink(DrinkKind.Water, 500, LocalTime.of(9, 0)))
            .withDrink(Drink(DrinkKind.Juice, 200, LocalTime.of(9, 5)))
        show {
            val actions = DrinkActions(onAdd = { kind, ml -> added += kind to ml }, onUndo = { undone = true })
            DrinksCard(day, Goals(), actions)
        }

        composeRule.onNodeWithText("0.5 l of 1.6 l").assertExists()
        composeRule.onNodeWithText("All drinks: 0.7 l").assertExists()
        composeRule.onNodeWithText("Coffee").performScrollTo().performClick()
        composeRule.onNodeWithText("5 dl").performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("More").performScrollTo().performClick()
        composeRule.onNodeWithText("Add 5.5 dl coffee").performScrollTo().performClick()
        composeRule.onNodeWithText("Remove the last drink").performScrollTo().performClick()

        assertEquals(listOf(DrinkKind.Coffee to 550), added)
        assertTrue(undone)
    }

    @Test fun `a usual meal is logged again with one tap, and the star keeps it first`() {
        val logged = mutableListOf<String>()
        val starred = mutableListOf<String>()
        show {
            QuickPicksCard(
                picks = listOf(MealPick("Gröt, bär", favorite = true), MealPick("Lax, potatis", favorite = false)),
                onLogMeal = { logged += it },
                onToggleFavorite = { starred += it },
            )
        }

        composeRule.onNodeWithContentDescription("Log Lax, potatis").performClick()
        composeRule.onNodeWithContentDescription("Star Lax, potatis").performClick()
        composeRule.onNodeWithContentDescription("Remove the star from Gröt, bär").performClick()

        assertEquals(listOf("Lax, potatis"), logged)
        assertEquals(listOf("Lax, potatis", "Gröt, bär"), starred)
    }

    @Test fun `no usual meals shows nothing`() {
        show { QuickPicksCard(picks = emptyList(), onLogMeal = {}, onToggleFavorite = {}) }

        composeRule.onNodeWithText("Your usual").assertDoesNotExist()
    }

    @Test fun `a recent drink is logged again with one tap`() {
        val added = mutableListOf<Pair<DrinkKind, Int>>()
        show {
            DrinksCard(
                day = DayEntry(LocalDate.of(2026, 10, 19)),
                goals = Goals(),
                actions = DrinkActions(onAdd = { kind, ml -> added += kind to ml }),
                recent = listOf(Drink(DrinkKind.Coffee, 150)),
            )
        }

        composeRule.onNodeWithText("Coffee · 1.5 dl").performScrollTo().performClick()

        assertEquals(listOf(DrinkKind.Coffee to 150), added)
    }

    @Test fun `late caffeine today comes with a word on sleep`() {
        val day = DayEntry(LocalDate.of(2026, 10, 19)).withDrink(Drink(DrinkKind.Coffee, 150, LocalTime.of(15, 30)))
        show { DrinksCard(day, Goals(), DrinkActions()) }

        composeRule.onNodeWithText("Caffeine after 2 pm", substring = true).assertExists()
    }
}
