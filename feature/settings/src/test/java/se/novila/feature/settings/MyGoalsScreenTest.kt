package se.novila.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.coach.CoachPreferences
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.Goals
import se.novila.recipes.FoodPreferences

@RunWith(RobolectricTestRunner::class)
class MyGoalsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(state: MyGoalsUiState, actions: MyGoalsActions = MyGoalsActions()) {
        composeRule.setContent {
            NovilaTheme { MyGoalsScreen(state = state, actions = actions, contentPadding = PaddingValues()) }
        }
    }

    /** Scrolls the list to [text], so the lazy list composes it, and returns its node. */
    private fun scrollTo(text: String) = composeRule.run {
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
        onNodeWithText(text)
    }

    @Test fun `each part says what it is set to`() {
        show(MyGoalsUiState(Goals(), CoachPreferences(daysPerWeek = 3, sessionMinutes = 45), null, FoodPreferences()))

        scrollTo("8,000 steps · 8 h sleep · 8 glasses of water").assertExists()
        scrollTo("3 days a week, 45 min a session").assertExists()
        scrollTo("Not set up.").assertExists()
        scrollTo("Everything · No allergens chosen · Any time").assertExists()
        scrollTo("Off.").assertExists()
    }

    @Test fun `each part opens where it is changed`() {
        val opened = mutableListOf<String>()
        show(
            MyGoalsUiState(Goals(), null, null, FoodPreferences()),
            MyGoalsActions(
                onOpenGoals = { opened += "goals" },
                onOpenTrainingCoach = { opened += "training" },
                onOpenSleepCoach = { opened += "sleep" },
                onOpenFoodPreferences = { opened += "food" },
                onOpenCycle = { opened += "cycle" },
            ),
        )

        listOf(
            "Steps, sleep and water",
            "The training coach",
            "The sleep coach",
            "Food preferences",
            "Follow your cycle",
        ).forEach { scrollTo(it).performClick() }

        assertEquals(listOf("goals", "training", "sleep", "food", "cycle"), opened)
    }
}
