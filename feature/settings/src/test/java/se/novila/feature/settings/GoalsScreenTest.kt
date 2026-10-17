package se.novila.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.Goals

@RunWith(RobolectricTestRunner::class)
class GoalsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    /** Shows the screen, holding the goals as the ViewModel would, and returns them. */
    private fun show(initial: Goals = Goals(), suggestion: Goals? = null): () -> Goals {
        var goals by mutableStateOf(initial)
        composeRule.setContent {
            NovilaTheme {
                GoalsScreen(
                    goals = goals,
                    suggestion = suggestion,
                    actions = GoalsActions(onChange = { goals = it(goals) }, onUseSuggestion = { goals = it }),
                    contentPadding = PaddingValues(),
                )
            }
        }
        return { goals }
    }

    /** Scrolls the list to [text], so the lazy list composes it, and returns its node. */
    private fun scrollTo(text: String) = composeRule.run {
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        onNodeWithText(text)
    }

    @Test fun `plus and minus change each goal`() {
        val goals = show()

        composeRule.onNodeWithContentDescription("Steps: raise the goal").performClick()
        composeRule.onNodeWithContentDescription("Sleep: lower the goal").performClick()
        composeRule.onNodeWithContentDescription("Water: raise the goal").performClick()

        assertEquals(Goals(steps = 8500, sleepHours = 7.5f, waterGlasses = 9), goals())
        composeRule.onNodeWithText("8,500 a day").assertExists()
    }

    @Test fun `a goal at the end of its range cannot go further`() {
        show(Goals(steps = Goals.STEPS.last))

        composeRule.onNodeWithContentDescription("Steps: raise the goal").assertIsNotEnabled()
    }

    @Test fun `the profile's suggestion can be used`() {
        val suggestion = Goals(steps = 7000, sleepHours = 7.5f)
        val goals = show(suggestion = suggestion)

        scrollTo("Use these goals").performClick()

        assertEquals(suggestion, goals())
        scrollTo("You are using these goals.").assertExists()
    }

    @Test fun `without age or weight the card says what to add`() {
        show()

        scrollTo("Add your birth year or weight to your profile, and Novila suggests goals that fit you.")
            .assertExists()
    }
}
