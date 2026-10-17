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
                    history = PreviewData.history,
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
                    history = PreviewData.history.drop(1),
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
}
