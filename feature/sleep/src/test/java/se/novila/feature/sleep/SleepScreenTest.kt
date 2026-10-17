package se.novila.feature.sleep

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsNotEnabled
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
import se.novila.model.Goals
import se.novila.model.HistoryRange
import se.novila.ui.preview.PreviewData
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class SleepScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `goes back a day, and never past today`() {
        val selected = mutableListOf<LocalDate>()
        composeRule.setContent {
            NovilaTheme {
                SleepScreen(
                    day = PreviewData.today,
                    history = PreviewData.history(HistoryRange.Week),
                    goals = Goals(),
                    onSleep = {},
                    contentPadding = PaddingValues(),
                    today = PreviewData.date,
                    onSelectDay = { selected += it },
                )
            }
        }
        composeRule.onNodeWithText("Today").assertExists()
        composeRule.onNodeWithText("Sleep last night").assertExists()
        composeRule.onNodeWithContentDescription("Next day").assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Previous day").performClick()
        assertEquals(listOf(PreviewData.date.minusDays(1)), selected)
    }

    @Test fun `an earlier day says which night it is`() {
        composeRule.setContent {
            NovilaTheme {
                SleepScreen(
                    day = PreviewData.history.first(),
                    history = PreviewData.history(HistoryRange.Week, until = PreviewData.history.first().date),
                    goals = Goals(),
                    onSleep = {},
                    contentPadding = PaddingValues(),
                    today = PreviewData.date,
                )
            }
        }
        composeRule.onNodeWithText("Yesterday").assertExists()
        composeRule.onNodeWithText("Sleep the night before").assertExists()
    }

    @Test fun `picks a longer range and compares it with the one before`() {
        val ranges = mutableListOf<HistoryRange>()
        composeRule.setContent {
            NovilaTheme {
                SleepScreen(
                    day = PreviewData.today,
                    history = PreviewData.history(HistoryRange.Month),
                    goals = Goals(),
                    onSleep = {},
                    contentPadding = PaddingValues(),
                    onRange = { ranges += it },
                )
            }
        }
        composeRule.onNodeWithText("Sleep, last four weeks").assertExists()
        composeRule.onNodeWithText("the four weeks before", substring = true).assertExists()
        composeRule.onNodeWithText("Year").performClick()
        assertEquals(listOf(HistoryRange.Year), ranges)
    }
}
