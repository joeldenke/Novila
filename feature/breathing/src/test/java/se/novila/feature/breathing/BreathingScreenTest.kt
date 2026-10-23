package se.novila.feature.breathing

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.novila.designsystem.component.NovilaEntryLayout
import se.novila.designsystem.theme.NovilaTheme

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1600dp")
class BreathingScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `the list shows every session with its length and what it does`() {
        composeRule.setContent {
            NovilaTheme(entryLayout = NovilaEntryLayout.Rows) { BreathingScreen(BreathingActions(), PaddingValues()) }
        }

        composeRule.onNodeWithText("Calm breathing").assertIsDisplayed()
        composeRule.onNodeWithText("About 1 min · ", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("4-7-8 breathing").assertIsDisplayed()
        composeRule.onNodeWithText("About 1 min 16 s · A long, slow out-breath.", substring = true).assertIsDisplayed()
    }

    @Test fun `tapping a tile opens the session by id`() = opensBox(NovilaEntryLayout.Grid)

    @Test fun `tapping a row opens the session by id`() = opensBox(NovilaEntryLayout.Rows)

    private fun opensBox(layout: NovilaEntryLayout) {
        var opened: String? = null
        composeRule.setContent {
            NovilaTheme(entryLayout = layout) {
                BreathingScreen(BreathingActions(onOpenSession = { opened = it }), PaddingValues())
            }
        }

        composeRule.onNodeWithText("Box breathing").performClick()
        assertEquals("box", opened)
    }

    @Test fun `a running session shows its phase, count, round and time left`() {
        composeRule.setContent {
            NovilaTheme { BreathingSessionScreen(breathingPreviewState(), BreathingActions(), PaddingValues()) }
        }

        // Six seconds into box breathing is the hold after the in-breath.
        composeRule.onNodeWithText("Hold").assertIsDisplayed()
        composeRule.onNodeWithText("Round 1 of 5").assertIsDisplayed()
        composeRule.onNodeWithText("1:14 left").assertIsDisplayed()
        composeRule.onNodeWithText("Pause").assertIsDisplayed()
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

        composeRule.onNodeWithText("Continue").performClick()
        composeRule.onNodeWithText("Start again").performClick()
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

        composeRule.onNodeWithText("Session complete").assertIsDisplayed()
        composeRule.onNodeWithText("Start again").assertIsDisplayed()
    }
}
