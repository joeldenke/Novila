package se.novila.feature.nutrition

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.Goals
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class NutritionCardsTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(content: @Composable () -> Unit) {
        composeRule.setContent { NovilaTheme { content() } }
    }

    @Test fun `the log card explains what a serving is for each food group`() {
        show {
            FoodLogCard(
                today = DayEntry(LocalDate.of(2026, 10, 19)),
                goals = Goals(),
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
        composeRule.onNodeWithText("One serving is a glass, about 2 dl.").assertExists()
    }
}
