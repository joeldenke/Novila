package se.novila.feature.nutrition

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.Goals
import se.novila.model.HistoryRange
import se.novila.ui.preview.PreviewData

// Tall enough for the day picker and the card below it, so nothing has to be scrolled to.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class NutritionScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(setupIncomplete: Boolean = false, onCompleteSetup: () -> Unit = {}) {
        composeRule.setContent {
            NovilaTheme {
                NutritionScreen(
                    day = PreviewData.today,
                    history = PreviewData.history(HistoryRange.Week),
                    goals = Goals(),
                    onPortions = { _, _ -> },
                    onTreats = {},
                    onLogMeal = {},
                    contentPadding = PaddingValues(),
                    setupIncomplete = setupIncomplete,
                    onCompleteSetup = onCompleteSetup,
                )
            }
        }
    }

    @Test fun `a skipped setup offers to finish it`() {
        var completions = 0
        show(setupIncomplete = true, onCompleteSetup = { completions++ })

        composeRule.onNodeWithText("Finish setup").performClick()

        assertEquals(1, completions)
    }

    @Test fun `nothing asks for the setup when it is finished`() {
        show()

        composeRule.onNodeWithText("Setup not finished").assertDoesNotExist()
    }
}
