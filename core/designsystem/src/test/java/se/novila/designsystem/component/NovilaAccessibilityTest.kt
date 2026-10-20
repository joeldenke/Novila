package se.novila.designsystem.component

import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertRangeInfoEquals
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme

@RunWith(RobolectricTestRunner::class)
class NovilaAccessibilityTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `stepper buttons are comfortable touch targets`() {
        composeRule.setContent {
            NovilaTheme {
                NovilaStepper(
                    title = "Vegetables",
                    value = "3 of 6",
                    onDecrement = {},
                    onIncrement = {},
                    decrementDescription = "One serving less",
                    incrementDescription = "One serving more",
                    role = NovilaTheme.pillars.nutrition,
                )
            }
        }

        composeRule.onNodeWithContentDescription("One serving less")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithContentDescription("One serving more")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test fun `level segments are comfortable touch targets`() {
        composeRule.setContent {
            NovilaTheme {
                NovilaLevelPicker(
                    levels = listOf("Low", "Medium", "High"),
                    selected = 0,
                    onSelect = {},
                    hint = "Pick one",
                    role = NovilaTheme.pillars.training,
                )
            }
        }

        composeRule.onNodeWithContentDescription("High").assertHeightIsAtLeast(48.dp)
    }

    @Test fun `a score ring is announced as progress with its number`() {
        composeRule.setContent {
            NovilaTheme {
                NovilaScoreRing(
                    score = 72,
                    progress = 0.72f,
                    caption = "of 100",
                    role = NovilaTheme.pillars.nutrition,
                )
            }
        }

        composeRule.onNodeWithText("72")
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.72f, 0f..1f))
    }

    @Test fun `a meter is announced as progress`() {
        composeRule.setContent {
            NovilaTheme {
                NovilaMeter(
                    label = "Protein",
                    value = "12/20",
                    progress = 0.6f,
                    role = NovilaTheme.pillars.nutrition,
                )
            }
        }

        composeRule.onNodeWithText("Protein")
            .assertRangeInfoEquals(ProgressBarRangeInfo(0.6f, 0f..1f))
    }

    @Test fun `a task row says whether it is done`() {
        composeRule.setContent {
            NovilaTheme {
                NovilaTaskRow(title = "Log last night", done = true, stateDescription = "Done")
            }
        }

        composeRule.onNodeWithText("Log last night")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Done"))
    }

    @Test fun `a list row says whether it is expanded`() {
        composeRule.setContent {
            NovilaTheme {
                NovilaListRow(
                    title = "Overnight oats",
                    onClick = {},
                    onClickLabel = "Hide recipe",
                    stateDescription = "Expanded",
                )
            }
        }

        composeRule.onNodeWithText("Overnight oats")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"))
    }
}
