package se.novila.feature.breathing

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme

@RunWith(RobolectricTestRunner::class)
class BreathingScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text))
    }

    @Test fun `the list shows every session with its length and a start button`() {
        composeRule.setContent {
            NovilaTheme { BreathingScreen(BreathingActions(), PaddingValues()) }
        }

        scrollTo("Calm breathing")
        composeRule.onNodeWithText("Calm breathing").assertIsDisplayed()
        composeRule.onNodeWithText("About 1 min").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Start Calm breathing").performScrollTo().assertIsDisplayed()

        scrollTo("4-7-8 breathing")
        composeRule.onNodeWithText("4-7-8 breathing").assertIsDisplayed()
        composeRule.onNodeWithText("About 1 min 16 s").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Start 4-7-8 breathing").performScrollTo().assertIsDisplayed()
    }

    @Test fun `starting a session opens it by id`() {
        var opened: String? = null
        composeRule.setContent {
            NovilaTheme { BreathingScreen(BreathingActions(onOpenSession = { opened = it }), PaddingValues()) }
        }

        scrollTo("Box breathing")
        composeRule.onNodeWithContentDescription("Start Box breathing").performScrollTo().performClick()
        assertEquals("box", opened)
    }

    @Test fun `a running session shows its phase, count, round and time left`() {
        composeRule.setContent {
            NovilaTheme { BreathingSessionScreen(breathingPreviewState(), BreathingActions(), PaddingValues()) }
        }

        // Six seconds into box breathing is the hold after the in-breath.
        composeRule.onNodeWithText("Hold").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Round 1 of 5").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("1:14 left").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Pause").performScrollTo().assertIsDisplayed()
    }

    @Test fun `pause and restart are wired to the screen's actions`() {
        val preview = breathingPreviewState()
        var toggled = 0
        var restarted = 0
        composeRule.setContent {
            NovilaTheme {
                BreathingSessionScreen(
                    preview.copy(running = false),
                    BreathingActions(onToggle = { toggled++ }, onRestart = { restarted++ }),
                    PaddingValues(),
                )
            }
        }

        composeRule.onNodeWithText("Continue").performScrollTo().performClick()
        composeRule.onNodeWithText("Start again").performScrollTo().performClick()
        assertEquals(1, toggled)
        assertEquals(1, restarted)
    }

    @Test fun `a finished session says so and can start again`() {
        composeRule.setContent {
            NovilaTheme {
                BreathingSessionScreen(
                    breathingPreviewState().copy(moment = null, running = false),
                    BreathingActions(),
                    PaddingValues(),
                )
            }
        }

        composeRule.onNodeWithText("Session complete").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Start again").performScrollTo().assertIsDisplayed()
    }
}
