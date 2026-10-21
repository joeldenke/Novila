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
import se.novila.model.Goals
import se.novila.model.HistoryRange
import se.novila.model.SleepAnalysis
import se.novila.model.analyzeSleep
import se.novila.ui.preview.PreviewData

@RunWith(RobolectricTestRunner::class)
class SleepTrendsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(
        range: HistoryRange = HistoryRange.Week,
        analysis: SleepAnalysis = analyzeSleep(PreviewData.days()),
        onRange: (HistoryRange) -> Unit = {},
    ) {
        composeRule.setContent {
            NovilaTheme {
                SleepTrendsScreen(
                    history = PreviewData.history(range),
                    goals = Goals(),
                    analysis = analysis,
                    onRange = onRange,
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

    @Test fun `the findings say what the logs show, and on how many nights`() {
        show()
        scrollTo("After days you ate well", substring = true).assertExists()
        // Every link says how many nights it stands on.
        val links = composeRule.onAllNodesWithText("nights with", substring = true).fetchSemanticsNodes()
        assertTrue(links.isNotEmpty())
        scrollTo("A link isn't proof of cause", substring = true).assertExists()
    }

    @Test fun `without enough nights the findings say how far there is to go`() {
        show(analysis = analyzeSleep(PreviewData.days(count = 5)))
        scrollTo("of 14 nights logged", substring = true).assertExists()
    }

    @Test fun `picks a longer range and compares it with the one before`() {
        val ranges = mutableListOf<HistoryRange>()
        show(range = HistoryRange.Month, onRange = { ranges += it })
        scrollTo("Sleep, last four weeks").assertExists()
        scrollTo("the four weeks before", substring = true).assertExists()
        scrollTo("Year").performClick()
        assertEquals(listOf(HistoryRange.Year), ranges)
    }
}
