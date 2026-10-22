package se.novila.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.data.SavedCycle
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.CycleLog
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class CycleScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(cycle: SavedCycle, onEnabled: (Boolean) -> Unit) = composeRule.setContent {
        NovilaTheme { CycleScreen(cycle = cycle, onEnabled = onEnabled, contentPadding = PaddingValues()) }
    }

    @Test fun `the switch turns the cycle on`() {
        val changes = mutableListOf<Boolean>()
        show(SavedCycle()) { changes += it }
        composeRule.onNodeWithText("Your cycle stays on this phone. Turning it off deletes what you logged.")
            .performClick()
        assertEquals(listOf(true), changes)
    }

    @Test fun `turning off a cycle with something logged asks first, since it deletes it`() {
        val changes = mutableListOf<Boolean>()
        val logged = CycleLog().withPeriod(LocalDate.of(2026, 10, 9), on = true)
        show(SavedCycle(enabled = true, log = logged)) { changes += it }
        composeRule.onNodeWithText("Your last period began", substring = true).assertExists()

        composeRule.onNodeWithText("Your cycle stays on this phone. Turning it off deletes what you logged.")
            .performClick()
        composeRule.onNodeWithText("Cancel").performClick()
        assertEquals(emptyList<Boolean>(), changes)

        composeRule.onNodeWithText("Your cycle stays on this phone. Turning it off deletes what you logged.")
            .performClick()
        composeRule.onNodeWithText("Turn off").performClick()
        assertEquals(listOf(false), changes)
    }
}
