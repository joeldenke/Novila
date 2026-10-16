package se.novila.designsystem.component

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme

@RunWith(RobolectricTestRunner::class)
class NovilaRowsTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `tapping anywhere on a switch row toggles it`() {
        composeRule.setContent {
            NovilaTheme {
                var on by remember { mutableStateOf(false) }
                NovilaSwitchRow(
                    title = "Wallpaper colors",
                    summary = "Android 12+",
                    checked = on,
                    onCheckedChange = { on = it },
                )
            }
        }

        composeRule.onNodeWithText("Wallpaper colors").assertIsOff()
        composeRule.onNodeWithText("Android 12+").performClick()
        composeRule.onNodeWithText("Wallpaper colors").assertIsOn()
    }

    @Test fun `a list row with onClick is clickable`() {
        var clicks = 0
        composeRule.setContent { NovilaTheme { NovilaListRow(title = "About", onClick = { clicks++ }) } }

        composeRule.onNodeWithText("About").performClick()

        assertEquals(1, clicks)
    }

    @Test fun `the confirm dialog tells confirm from cancel`() {
        var confirmed = 0
        var dismissed = 0
        composeRule.setContent {
            NovilaTheme {
                NovilaConfirmDialog(
                    title = "Delete?",
                    text = "This cannot be undone.",
                    confirmLabel = "Delete",
                    dismissLabel = "Cancel",
                    onConfirm = { confirmed++ },
                    onDismiss = { dismissed++ },
                )
            }
        }

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Delete").performClick()

        assertEquals(1, confirmed)
        assertEquals(1, dismissed)
    }
}
