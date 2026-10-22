package se.novila.feature.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.HistoryRange
import se.novila.model.historyOf
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class TrainingTrendsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val today = LocalDate.of(2026, 10, 22)

    private fun show(minutes: Map<LocalDate, Int>, includesWatch: Boolean, onRange: (HistoryRange) -> Unit = {}) {
        val history = historyOf(HistoryRange.Week, today) { DayEntry(it) }
        composeRule.setContent {
            NovilaTheme {
                TrainingTrendsScreen(
                    state = TrainingTrendsUiState(history, minutes, includesWatch),
                    onRange = onRange,
                    contentPadding = PaddingValues(),
                )
            }
        }
    }

    @Test fun `the watch's workouts count with the week, and against the week before`() {
        // Nothing logged in the app: every minute is the watch's.
        show(
            minutes = mapOf(today to 40, today.minusDays(2) to 30, today.minusDays(9) to 50),
            includesWatch = true,
        )

        composeRule.onNodeWithText("1 h 10 min", substring = true).assertExists()
        composeRule.onNodeWithText("recorded by your watch", substring = true).assertExists()
        composeRule.onNodeWithText("20 min more than", substring = true).assertExists()
    }

    @Test fun `without a watch the card says it counts what was logged in Novila`() {
        show(minutes = mapOf(today to 30), includesWatch = false)

        composeRule.onNodeWithText("Strength and cardio logged in Novila").assertExists()
    }

    @Test fun `the range can be changed`() {
        val ranges = mutableListOf<HistoryRange>()
        show(minutes = emptyMap(), includesWatch = false, onRange = { ranges += it })

        composeRule.onNodeWithText("Year").performClick()

        assertEquals(listOf(HistoryRange.Year), ranges)
    }
}
