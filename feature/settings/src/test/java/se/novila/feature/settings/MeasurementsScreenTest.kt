package se.novila.feature.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class MeasurementsScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val date = LocalDate.of(2026, 10, 16)
    private val logged = MeasurementEntry(date, BodyMeasurements(weightKg = 73, waistCm = 81))

    /**
     * Shows the screen, holding the form as the ViewModel would, and returns what was typed. Saving does nothing;
     * the tests that need it show their own screen and count the presses.
     */
    private fun show(
        initial: MeasurementForm = MeasurementForm(),
        entries: List<MeasurementEntry> = emptyList(),
    ): () -> MeasurementForm {
        var form by mutableStateOf(initial)
        composeRule.setContent {
            NovilaTheme {
                MeasurementsScreen(
                    form = form,
                    day = date,
                    today = date,
                    entries = entries,
                    onChange = { form = it },
                    onSelectDay = {},
                    onSave = {},
                    contentPadding = PaddingValues(),
                )
            }
        }
        return { form }
    }

    private fun field(label: String) = composeRule.onNode(hasSetTextAction() and hasText(label))

    @Test fun `typing a weight changes the form`() {
        val form = show()

        field("Weight").performTextInput("74")

        assertEquals("74", form().weight)
    }

    @Test fun `a waist out of range says which range is right`() {
        show(MeasurementForm(waist = "8"))

        composeRule.onNodeWithText("Enter a waist between 40 and 250 cm.").assertExists()
    }

    @Test fun `a neck out of range says which range is right`() {
        show(MeasurementForm(neck = "90"))

        composeRule.onNodeWithText("Enter a neck between 20 and 80 cm.").assertExists()
    }

    @Test fun `nothing is saved until something is typed`() {
        var form by mutableStateOf(MeasurementForm.of(logged.measurements))
        var saving = 0
        composeRule.setContent {
            NovilaTheme {
                MeasurementsScreen(
                    form = form,
                    day = date,
                    today = date,
                    entries = listOf(logged),
                    onChange = { form = it },
                    onSelectDay = {},
                    onSave = { saving++ },
                    contentPadding = PaddingValues(),
                )
            }
        }

        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        field("Weight").performTextInput("4")
        composeRule.onNodeWithText("Save").performClick()

        assertEquals(1, saving)
    }

    @Test fun `a logged day shows its values, or a dash when one was not measured`() {
        show(initial = MeasurementForm.of(logged.measurements), entries = listOf(logged))

        composeRule.onNodeWithText("73 kg").assertExists()
        composeRule.onNodeWithText("81 cm").assertExists()
        // Neck and hips were not measured, so both show the dash.
        composeRule.onAllNodesWithText("–").assertCountEquals(2)
    }

    @Test fun `without measurements, the history says nothing is logged`() {
        show()

        composeRule.onNodeWithText("Nothing logged yet. What you measure shows up here over time.").assertExists()
    }
}
