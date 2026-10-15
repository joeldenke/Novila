package se.novila.designsystem.component

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme

@RunWith(RobolectricTestRunner::class)
class NovilaInputTest {
    @get:Rule val composeRule = createComposeRule()

    private val levels = listOf("Bad", "Okay", "Great")

    @Test fun `level picker reports the tapped level and names the choice`() {
        var picked = 0
        composeRule.setContent {
            NovilaTheme {
                NovilaLevelPicker(
                    levels = levels,
                    selected = 2,
                    onSelect = { picked = it },
                    hint = "How do you feel?",
                    role = NovilaTheme.pillars.training,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Okay").assertIsSelected()
        composeRule.onNodeWithText("Okay").assertExists()
        composeRule.onNodeWithContentDescription("Great").performClick()

        assertEquals(3, picked)
    }

    @Test fun `level picker shows the hint before anything is chosen`() {
        composeRule.setContent {
            NovilaTheme {
                NovilaLevelPicker(levels, selected = 0, onSelect = {
                }, hint = "How do you feel?", role = NovilaTheme.pillars.sleep)
            }
        }

        composeRule.onNodeWithText("How do you feel?").assertExists()
    }

    @Test fun `stepper buttons count up and down, and minus can be disabled`() {
        var count = 0
        composeRule.setContent {
            NovilaTheme {
                NovilaStepper(
                    title = "Protein",
                    value = "0 of 3",
                    onDecrement = { count-- },
                    onIncrement = { count++ },
                    decrementDescription = "Remove protein",
                    incrementDescription = "Add protein",
                    role = NovilaTheme.pillars.nutrition,
                    decrementEnabled = false,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Add protein").performClick()
        composeRule.onNodeWithContentDescription("Remove protein").assertIsNotEnabled().performClick()

        assertEquals(1, count)
    }
}
