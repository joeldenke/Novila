package se.novila.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.ProfileForm

@RunWith(RobolectricTestRunner::class)
class ProfileScreenTest {
    @get:Rule val composeRule = createComposeRule()

    /** Shows the screen, holding the form as the ViewModel would, and returns what was typed. */
    private fun show(initial: ProfileForm = ProfileForm()): () -> ProfileForm {
        var form by mutableStateOf(initial)
        composeRule.setContent {
            NovilaTheme {
                ProfileScreen(
                    form = form,
                    currentYear = 2026,
                    onChange = { form = it },
                    contentPadding = PaddingValues(),
                )
            }
        }
        return { form }
    }

    private fun field(label: String) = composeRule.onNode(hasSetTextAction() and hasText(label))

    @Test fun `typing a name changes the form`() {
        val form = show()

        field("Name").performTextInput("Alex")

        assertEquals("Alex", form().name)
    }

    @Test fun `a height out of range says which range is right`() {
        show(ProfileForm(height = "17"))

        composeRule.onNodeWithText("Enter a height between 100 and 250 cm.").assertExists()
    }

    @Test fun `a birth year in range shows no error`() {
        show(ProfileForm(birthYear = "1990"))

        composeRule.onNodeWithText("Enter a year between 1906 and 2026.").assertDoesNotExist()
    }
}
