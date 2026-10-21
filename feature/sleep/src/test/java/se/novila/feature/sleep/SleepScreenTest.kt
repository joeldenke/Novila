package se.novila.feature.sleep

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsNotEnabled
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
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.HistoryRange
import se.novila.model.SleepFactor
import se.novila.sleep.SleepCoachPreferences
import se.novila.sleep.SleepCoachWeek
import se.novila.sleep.SleepState
import se.novila.sleep.WindDownMoment
import se.novila.sleep.WindDownStep
import se.novila.ui.preview.PreviewData
import java.time.LocalDate
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class SleepScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val newCoach = SleepCoachCardState(
        active = false,
        bedtime = SleepCoachPreferences().bedtime,
        wakeTime = SleepCoachPreferences().wakeTime,
        week = SleepCoachWeek(nearGoal = 0, logged = 0),
        tonight = null,
        hasSteps = false,
    )

    private fun show(
        day: DayEntry = PreviewData.today,
        coach: SleepCoachCardState = newCoach,
        actions: SleepActions = SleepActions(),
        watch: SleepState = SleepState.Unavailable,
        fromWatch: Boolean = false,
        openSheet: Boolean = false,
    ) {
        composeRule.setContent {
            NovilaTheme {
                SleepScreen(
                    day = day,
                    history = PreviewData.history(HistoryRange.Week, until = day.date),
                    goals = Goals(),
                    coach = coach,
                    actions = actions,
                    contentPadding = PaddingValues(),
                    today = PreviewData.date,
                    watch = watch,
                    fromWatch = fromWatch,
                    openSheet = openSheet,
                )
            }
        }
    }

    /** Scrolls the list to [text], so the lazy list composes it, and returns its node. */
    private fun scrollTo(text: String, substring: Boolean = false) = composeRule.run {
        onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text, substring = substring))
        onNodeWithText(text, substring = substring)
    }

    @Test fun `goes back a day, and never past today`() {
        val selected = mutableListOf<LocalDate>()
        show(actions = SleepActions(onSelectDay = { selected += it }))
        composeRule.onNodeWithText("Today").assertExists()
        composeRule.onNodeWithText("Sleep last night").assertExists()
        composeRule.onNodeWithContentDescription("Next day").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Previous day").performClick()
        assertEquals(listOf(PreviewData.date.minusDays(1)), selected)
    }

    @Test fun `an earlier day says which night it is`() {
        show(day = PreviewData.history.first())
        composeRule.onNodeWithText("Yesterday").assertExists()
        composeRule.onNodeWithText("Sleep the night before").assertExists()
    }

    @Test fun `a night without sleep asks to log it, and the sheet logs it`() {
        show(day = DayEntry(PreviewData.date))
        composeRule.onNodeWithText("Log the night").performClick()
        composeRule.onNodeWithText("How did you sleep?").assertExists()
    }

    @Test fun `a logged night can be changed`() {
        show()
        composeRule.onNodeWithText("Change the night").assertExists()
    }

    @Test fun `factors and quality in the sheet go to the entry`() {
        val factors = mutableListOf<Pair<SleepFactor, Boolean>>()
        show(actions = SleepActions(onFactor = { f, on -> factors += f to on }), openSheet = true)
        composeRule.onNodeWithText("Late caffeine").performScrollTo().performClick()
        assertEquals(listOf(SleepFactor.LateCaffeine to true), factors)
    }

    @Test fun `a night from the watch says where it came from`() {
        show(watch = SleepState.Connected(emptyList()), fromWatch = true)
        composeRule.onNodeWithText("From your watch", substring = true).assertExists()
        composeRule.onNodeWithText("Read sleep from Health Connect").assertDoesNotExist()
    }

    @Test fun `asks for sleep from Health Connect until it is allowed`() {
        var connected = 0
        show(watch = SleepState.NeedsPermission(), actions = SleepActions(onConnectHealth = { connected++ }))
        scrollTo("Read sleep from Health Connect").performClick()
        assertEquals(1, connected)
    }

    @Test fun `after a no, Health Connect's settings give access to sleep`() {
        var settings = 0
        show(
            watch = SleepState.NeedsPermission(denied = true),
            actions = SleepActions(onOpenHealthSettings = { settings++ }),
        )
        scrollTo("Open Health Connect").performClick()
        assertEquals(1, settings)
    }

    @Test fun `the entries open the coach, the breathing sessions and the trends`() {
        val opened = mutableListOf<String>()
        show(
            actions = SleepActions(
                onOpenCoach = { opened += "coach" },
                onOpenBreathing = { opened += "breathing" },
                onOpenTrends = { opened += "trends" },
            ),
        )
        scrollTo("Sleep coach").performClick()
        scrollTo("Breathing and relaxation").performClick()
        scrollTo("Trends").performClick()
        assertEquals(listOf("coach", "breathing", "trends"), opened)
    }

    @Test fun `before the sleep coach is set up its way in says what it is`() {
        show()
        scrollTo("A bedtime to aim for", substring = true).assertExists()
    }

    @Test fun `the set up sleep coach says tonight's step`() {
        val coach = newCoach.copy(
            active = true,
            week = SleepCoachWeek(nearGoal = 4, logged = 6),
            tonight = WindDownMoment(WindDownStep.ScreensOff, LocalTime.of(21, 30)),
            hasSteps = true,
        )
        show(coach = coach)
        scrollTo("Screens off", substring = true).assertExists()
    }

    @Test fun `the sleep coach says the evening's steps are done when none are left`() {
        val coach = newCoach.copy(active = true, week = SleepCoachWeek(nearGoal = 2, logged = 3), hasSteps = true)
        show(coach = coach)
        scrollTo("The evening's steps are done", substring = true).assertExists()
    }
}
