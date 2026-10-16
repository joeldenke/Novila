package se.novila.feature.onboarding

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.novila.coach.CoachGoal
import se.novila.coach.CoachPreferences
import se.novila.designsystem.theme.NovilaTheme
import se.novila.movement.MovementState

// A tall screen, so each page's list shows all of its items without scrolling.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1600dp")
class OnboardingScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val answers = OnboardingUiState(
        name = "",
        preferences = CoachPreferences(),
        movement = MovementState.NeedsPermission(),
    )

    private fun show(
        actions: OnboardingActions = OnboardingActions(),
        page: OnboardingPage = OnboardingPage.Welcome,
        state: OnboardingUiState = answers,
    ) {
        composeRule.setContent {
            NovilaTheme { OnboardingScreen(state = state, actions = actions, initialPage = page) }
        }
    }

    @Test fun `the main button walks through the tour`() {
        show()

        composeRule.onNodeWithText("Show me").performClick()

        composeRule.onNodeWithText("Cardio and strength along WHO's activity guidelines").assertExists()
    }

    @Test fun `skip jumps past the tour to the questions`() {
        show()

        composeRule.onNodeWithText("Skip").performClick()

        composeRule.onNodeWithText("What do you want from Novila?").assertExists()
    }

    @Test fun `choosing a goal changes the coach's answers`() {
        val chosen = mutableListOf<CoachPreferences>()
        show(OnboardingActions(onPreferences = { chosen += it }), page = OnboardingPage.You)

        composeRule.onNodeWithText("Get stronger").performClick()

        assertEquals(CoachGoal.Strength, chosen.single().goal)
    }

    @Test fun `typing a name passes it on`() {
        val typed = mutableListOf<String>()
        show(OnboardingActions(onName = { typed += it }), page = OnboardingPage.You)

        composeRule.onNode(hasSetTextAction() and hasText("What should we call you?")).performTextInput("Alex")

        assertEquals("Alex", typed.last())
    }

    @Test fun `without access, the movement page asks to connect`() {
        var connects = 0
        show(OnboardingActions(onConnectHealth = { connects++ }), page = OnboardingPage.Movement)

        composeRule.onNodeWithText("Connect Health Connect").performClick()

        assertEquals(1, connects)
    }

    @Test fun `the summary greets by name and describes the week`() {
        val state = answers.copy(
            name = "Alex",
            preferences = CoachPreferences(goal = CoachGoal.Endurance, daysPerWeek = 5, sessionMinutes = 45),
        )
        show(page = OnboardingPage.Ready, state = state)

        composeRule.onNodeWithText("Your week is ready, Alex").assertExists()
        composeRule.onNodeWithText("Better fitness, 5 days a week, about 45 minutes at a time.").assertExists()
    }

    @Test fun `get started on the last page finishes the intro`() {
        var finishes = 0
        show(OnboardingActions(onFinish = { finishes++ }), page = OnboardingPage.Ready)

        composeRule.onNodeWithText("Get started").performClick()

        assertEquals(1, finishes)
    }
}
