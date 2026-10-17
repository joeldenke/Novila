package se.novila.feature.sleep

import androidx.compose.foundation.layout.PaddingValues
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
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.HistoryRange
import se.novila.model.SleepAnalysis
import se.novila.model.SleepFactor
import se.novila.model.analyzeSleep
import se.novila.ui.preview.PreviewData
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class SleepScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(
        day: DayEntry = PreviewData.today,
        range: HistoryRange = HistoryRange.Week,
        analysis: SleepAnalysis = analyzeSleep(PreviewData.days()),
        actions: SleepActions = SleepActions(),
    ) {
        composeRule.setContent {
            NovilaTheme {
                SleepScreen(
                    day = day,
                    history = PreviewData.history(range, until = day.date),
                    goals = Goals(),
                    analysis = analysis,
                    actions = actions,
                    contentPadding = PaddingValues(),
                    today = PreviewData.date,
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

    @Test fun `factors and quality go to the entry`() {
        val factors = mutableListOf<Pair<SleepFactor, Boolean>>()
        val qualities = mutableListOf<Int>()
        show(actions = SleepActions(onFactor = { f, on -> factors += f to on }, onQuality = { qualities += it }))
        scrollTo("Late caffeine").performClick()
        assertEquals(listOf(SleepFactor.LateCaffeine to true), factors)
    }

    @Test fun `the findings say what the logs show, and on how many nights`() {
        show()
        scrollTo("After days you ate well", substring = true).assertExists()
        scrollTo("nights with", substring = true).assertExists()
        scrollTo("A link isn't proof of cause", substring = true).assertExists()
    }

    @Test fun `without enough nights the findings say how far there is to go`() {
        show(analysis = analyzeSleep(PreviewData.days(count = 5)))
        scrollTo("of 14 nights logged", substring = true).assertExists()
    }

    @Test fun `picks a longer range and compares it with the one before`() {
        val ranges = mutableListOf<HistoryRange>()
        show(range = HistoryRange.Month, actions = SleepActions(onRange = { ranges += it }))
        scrollTo("Sleep, last four weeks").assertExists()
        scrollTo("the four weeks before", substring = true).assertExists()
        scrollTo("Year").performClick()
        assertEquals(listOf(HistoryRange.Year), ranges)
    }
}
