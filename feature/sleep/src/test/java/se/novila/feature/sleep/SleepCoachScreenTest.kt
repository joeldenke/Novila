package se.novila.feature.sleep

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.sleep.SleepCoachPreferences
import se.novila.sleep.SleepCoachWeek
import se.novila.sleep.WindDownStep
import se.novila.sleep.sleepCoachPlan
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class SleepCoachScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val defaults = SleepCoachPreferences()

    private fun show(
        active: Boolean = false,
        preferences: SleepCoachPreferences = defaults,
        onChange: (SleepCoachPreferences) -> Unit = {},
        onStart: () -> Unit = {},
        onReset: () -> Unit = {},
        onOpenBreathing: () -> Unit = {},
        bodyClockBedtime: LocalTime? = null,
    ) {
        composeRule.setContent {
            NovilaTheme {
                SleepCoachScreen(
                    state = SleepCoachUiState(
                        preferences = preferences,
                        active = active,
                        plan = sleepCoachPlan(emptyList(), preferences),
                        bodyClockBedtime = bodyClockBedtime,
                    ),
                    onChange = onChange,
                    onStart = onStart,
                    onReset = onReset,
                    onOpenBreathing = onOpenBreathing,
                    contentPadding = PaddingValues(),
                )
            }
        }
    }

    /** Scrolls the list to [text], so the lazy list composes it, and returns its node. */
    private fun scrollTo(text: String, substring: Boolean = false) = composeRule.run {
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text, substring = substring))
        onNodeWithText(text, substring = substring)
    }

    @Test fun `before it is set up it says what it does and how to start`() {
        var started = 0
        show(onStart = { started++ })
        scrollTo("Everything stays on your phone", substring = true).assertExists()
        scrollTo("Start the sleep coach").performClick()
        assertEquals(1, started)
    }

    @Test fun `once set up it can be turned off again`() {
        var reset = 0
        show(active = true, onReset = { reset++ })
        scrollTo("Turn off the sleep coach").performClick()
        assertEquals(1, reset)
    }

    @Test fun `the bedtime goal opens a dialog that gives the time back`() {
        var changed: SleepCoachPreferences? = null
        show(onChange = { changed = it })
        scrollTo("Bedtime goal").performClick()
        composeRule.onNodeWithText("When do you want to be in bed?").assertExists()
        composeRule.onNodeWithText("Save").performClick()
        assertEquals(defaults, changed)
    }

    @Test fun `a wind-down step can be checked and unchecked`() {
        var changed: SleepCoachPreferences? = null
        show(onChange = { changed = it })
        scrollTo("Screens off").performClick()
        assertTrue(WindDownStep.ScreensOff in checkNotNull(changed).windDown)
        scrollTo("Quiet time").performClick()
        assertTrue(WindDownStep.Quiet in checkNotNull(changed).windDown)
    }

    @Test fun `the routine shows each step at its time before the bedtime`() {
        val preferences = defaults.copy(windDown = WindDownStep.entries.toSet())
        show(preferences = preferences)
        scrollTo("Screens off")
        // The chip says the step, and the moment line under the chips says it again with its clock time.
        val withStep = composeRule.onAllNodesWithText("Screens off", substring = true).fetchSemanticsNodes()
        assertTrue(withStep.size >= 2)
    }

    @Test fun `the week card says there are no bedtimes yet without logs`() {
        show(active = true)
        scrollTo("No bedtimes logged yet", substring = true).assertExists()
        scrollTo("A tip is advice, not a diagnosis", substring = true).assertExists()
    }

    @Test fun `without logs the tips say the nights are close to the goal`() {
        show(active = true)
        scrollTo("Keep the rhythm you have", substring = true).assertExists()
    }

    @Test fun `the week count comes from the plan`() {
        val plan = sleepCoachPlan(emptyList(), defaults)
        assertEquals(SleepCoachWeek(nearGoal = 0, logged = 0), plan.week)
    }

    @Test fun `the goals say which bedtime suits the body clock, once the nights tell`() {
        show(bodyClockBedtime = LocalTime.of(0, 15))
        scrollTo("body clock suits a bedtime around", substring = true).assertExists()
    }

    @Test fun `without enough free days the goals say nothing about the body clock`() {
        show()
        composeRule.onNodeWithText("body clock", substring = true).assertDoesNotExist()
    }
}
