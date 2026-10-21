package se.novila.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.BodyMeasurements
import se.novila.model.MeasurementEntry
import se.novila.model.MeasurementForm
import se.novila.model.Profile
import se.novila.model.ProfileForm
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class ProfileScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val date = LocalDate.of(2026, 10, 16)

    /** Shows the screen, holding the form as the ViewModel would, and returns what was typed. */
    private fun show(
        initial: ProfileForm = ProfileForm(),
        profile: Profile = Profile(),
        entries: List<MeasurementEntry> = emptyList(),
        actions: ProfileActions = ProfileActions(),
    ): () -> ProfileForm {
        var form by mutableStateOf(initial)
        composeRule.setContent {
            NovilaTheme {
                ProfileScreen(
                    state = ProfileUiState(
                        form = form,
                        currentYear = 2026,
                        profile = profile,
                        measurements = MeasurementsUiState(MeasurementForm(), date, date, entries),
                    ),
                    actions = actions.copy(onChange = { form = it }),
                    contentPadding = PaddingValues(),
                )
            }
        }
        return { form }
    }

    private fun field(label: String) = composeRule.onNode(hasSetTextAction() and hasText(label))

    /** Scrolls the list to [text], so the lazy list composes it. */
    private fun scrollTo(text: String, substring: Boolean = false) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(text, substring = substring))
    }

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

    @Test fun `the header shows the initials and the profile in short`() {
        show(profile = Profile(name = "Alex Lind", birthYear = 1990, heightCm = 178, weightKg = 74))

        composeRule.onNodeWithContentDescription("Profile picture for Alex Lind").assertExists()
        composeRule.onNodeWithText("36 years · 178 cm · 74 kg").assertExists()
    }

    @Test fun `the newest measurements show on the profile`() {
        show(
            entries = listOf(
                MeasurementEntry(date, BodyMeasurements(weightKg = 73)),
                MeasurementEntry(date.minusDays(7), BodyMeasurements(weightKg = 74, waistCm = 82)),
            ),
        )

        scrollTo("Last measured", substring = true)
        // The weight is the newest day's; the waist the newest one measured. The history below repeats them.
        composeRule.onAllNodesWithText("73 kg")[0].assertExists()
        composeRule.onAllNodesWithText("82 cm")[0].assertExists()
    }

    @Test fun `my goals open from the profile`() {
        var opens = 0
        show(actions = ProfileActions(onOpenMyGoals = { opens++ }))

        scrollTo("My goals")
        composeRule.onNodeWithText("My goals").performClick()

        assertEquals(1, opens)
    }

    @Test fun `logging measurements opens the sheet`() {
        show()

        scrollTo("Log measurements")
        composeRule.onNodeWithText("Log measurements").performClick()

        composeRule.onNode(hasSetTextAction() and hasText("Waist")).assertExists()
    }
}
