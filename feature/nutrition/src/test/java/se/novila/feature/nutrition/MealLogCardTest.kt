package se.novila.feature.nutrition

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme

@RunWith(RobolectricTestRunner::class)
class MealLogCardTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(onLogMeal: (String) -> Unit = {}) {
        composeRule.setContent {
            NovilaTheme {
                MealLogCard(onLogMeal = onLogMeal)
            }
        }
    }

    @Test fun `a typed meal shows the groups it fills in`() {
        show()

        composeRule.onNode(hasSetTextAction()).performTextInput("Frukost: gröt, bär")

        composeRule.onNodeWithText("Adds Meals +1 · Whole grain +1 · Vegetables & fruit +1").assertExists()
        composeRule.onNodeWithText("Add the meal").assertIsEnabled()
    }

    @Test fun `adding the meal sends the text and empties the field`() {
        var logged: String? = null
        show { logged = it }
        composeRule.onNode(hasSetTextAction()).performTextInput("Lunch: kyckling, ris")

        composeRule.onNodeWithText("Add the meal").performClick()

        assertEquals("Lunch: kyckling, ris", logged)
        composeRule.onNodeWithText("Adds Meals +1 · Whole grain +1").assertDoesNotExist()
        composeRule.onNodeWithText("Add the meal").assertIsNotEnabled()
    }

    @Test fun `words that are not recognized are listed instead of counted`() {
        show()

        composeRule.onNode(hasSetTextAction()).performTextInput("yoghurt")

        composeRule.onNodeWithText("Not recognized: yoghurt").assertExists()
        composeRule.onNodeWithText("Add the meal").assertIsNotEnabled()
    }
}
