package se.novila.feature.settings

import androidx.compose.foundation.layout.Column
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
class MeasurementsCardsTest {
    @get:Rule val composeRule = createComposeRule()

    private val date = LocalDate.of(2026, 10, 16)
    private val logged = MeasurementEntry(date, BodyMeasurements(weightKg = 73, waistCm = 81))

    /**
     * Shows the sheet's content, holding the form as the ViewModel would, and returns what was typed and how often
     * it was saved.
     */
    private fun showSheet(
        initial: MeasurementForm = MeasurementForm(),
        entries: List<MeasurementEntry> = emptyList(),
    ): Pair<() -> MeasurementForm, () -> Int> {
        var form by mutableStateOf(initial)
        var saves = 0
        composeRule.setContent {
            NovilaTheme {
                Column {
                    MeasurementsSheet(
                        state = MeasurementsUiState(form, date, date, entries),
                        onChange = { form = it },
                        onSelectDay = {},
                        onSave = { saves++ },
                    )
                }
            }
        }
        return Pair({ form }, { saves })
    }

    private fun field(label: String) = composeRule.onNode(hasSetTextAction() and hasText(label))

    @Test fun `typing a weight changes the form`() {
        val (form, _) = showSheet()

        field("Weight").performTextInput("74")

        assertEquals("74", form().weight)
    }

    @Test fun `a waist out of range says which range is right`() {
        showSheet(MeasurementForm(waist = "8"))

        composeRule.onNodeWithText("Enter a waist between 40 and 250 cm.").assertExists()
    }

    @Test fun `a neck out of range says which range is right`() {
        showSheet(MeasurementForm(neck = "90"))

        composeRule.onNodeWithText("Enter a neck between 20 and 80 cm.").assertExists()
    }

    @Test fun `nothing is saved until something is typed`() {
        val (_, saves) = showSheet(MeasurementForm.of(logged.measurements), listOf(logged))

        composeRule.onNodeWithText("Save").assertIsNotEnabled()

        field("Weight").performTextInput("4")
        composeRule.onNodeWithText("Save").performClick()

        assertEquals(1, saves())
    }

    @Test fun `a logged day shows its values, or a dash when one was not measured`() {
        composeRule.setContent { NovilaTheme { MeasurementsHistoryCard(listOf(logged)) } }

        composeRule.onNodeWithText("73 kg").assertExists()
        composeRule.onNodeWithText("81 cm").assertExists()
        // Neck and hips were not measured, so both show the dash.
        composeRule.onAllNodesWithText("–").assertCountEquals(2)
    }

    @Test fun `the history shows the weight's trend in neutral words, not the day's swing`() {
        composeRule.setContent { NovilaTheme { MeasurementsHistoryCard(sampleMeasurements) } }

        // Weighed 76, 75, 74 and 73 kg over three weeks: the running average trails the scale a little, while the
        // weekly change is the least squares line through the weigh-ins (23, 16, 7 and 0 days ago), 0.9 kg a week.
        composeRule.onNodeWithText("Your weight's trend is 73.8 kg, about 0.9 kg a week lower over the last 3 weeks.")
            .assertExists()
        composeRule.onNodeWithText("running average", substring = true).assertExists()
    }

    @Test fun `a single weigh-in has no trend yet`() {
        composeRule.setContent { NovilaTheme { MeasurementsHistoryCard(listOf(logged)) } }

        composeRule.onNodeWithText("trend", substring = true).assertDoesNotExist()
    }

    @Test fun `without measurements, the latest card shows dashes`() {
        composeRule.setContent { NovilaTheme { LatestMeasurementsCard(emptyList(), onLog = {}) } }

        // Weight, waist, neck and hips all wait for their first measurement.
        composeRule.onAllNodesWithText("–").assertCountEquals(4)
    }
}
