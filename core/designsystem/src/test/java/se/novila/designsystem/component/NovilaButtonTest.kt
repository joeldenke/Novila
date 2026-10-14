package se.novila.designsystem.component

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

/** Compose UI test on Robolectric: runs as a plain unit test, no emulator. */
@RunWith(RobolectricTestRunner::class)
class NovilaButtonTest {
    @get:Rule val composeRule = createComposeRule()

    @Test fun `click invokes onClick`() {
        var clicks = 0
        composeRule.setContent {
            NovilaTheme { NovilaButton(text = "Save", onClick = { clicks++ }) }
        }

        composeRule.onNodeWithText("Save").performClick()

        assertEquals(1, clicks)
    }

    @Test fun `disabled button cannot be clicked`() {
        var clicks = 0
        composeRule.setContent {
            NovilaTheme { NovilaButton(text = "Save", onClick = { clicks++ }, enabled = false) }
        }

        composeRule.onNodeWithText("Save").assertIsNotEnabled().performClick()

        assertEquals(0, clicks)
    }

    @Test fun `content description is exposed to accessibility`() {
        composeRule.setContent {
            NovilaTheme { NovilaButton(text = "+", onClick = {}, contentDescription = "Add water") }
        }

        composeRule.onNodeWithContentDescription("Add water").assertExists()
    }
}
