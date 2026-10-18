package se.novila.feature.today

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.TrainingToday
import se.novila.model.dayChecklist
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class DayChecklistCardTest {
    @get:Rule val composeRule = createComposeRule()

    private val date = LocalDate.of(2026, 10, 18)

    private fun show(day: DayEntry, training: TrainingToday, actions: ChecklistActions) {
        composeRule.setContent {
            NovilaTheme {
                // The card can be taller than the test's screen.
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    DayChecklistCard(dayChecklist(day, Goals(), training), actions)
                }
            }
        }
    }

    @Test fun `each open task has the button that does it`() {
        val opened = mutableListOf<String>()
        show(
            DayEntry(date, waterGlasses = 3),
            TrainingToday(plannedMinutes = 30),
            ChecklistActions(
                onOpenSleep = { opened += "sleep" },
                onOpenTraining = { opened += "training" },
                onOpenNutrition = { opened += "nutrition" },
                onAddWater = { opened += "water" },
                onMood = { opened += "mood" },
            ),
        )

        composeRule.onNodeWithText("0 of 5 done").assertExists()
        composeRule.onNodeWithText("30 min planned").assertExists()
        composeRule.onNodeWithText("3 of 8 glasses").assertExists()
        listOf("Log", "Start", "Recipes", "+1 glass", "Answer").forEach {
            composeRule.onNodeWithText(it).performScrollTo().performClick()
        }

        assertEquals(listOf("sleep", "training", "nutrition", "water", "mood"), opened)
    }

    @Test fun `done tasks keep their place without a button`() {
        show(DayEntry(date, sleepHours = 7.5f, mood = 4), TrainingToday(), ChecklistActions())

        composeRule.onNodeWithText("Last night").assertExists()
        composeRule.onNodeWithText("Rest day").assertExists()
        composeRule.onNodeWithText("3 of 5 done").assertExists()
        composeRule.onNodeWithText("Log").assertDoesNotExist()
        composeRule.onNodeWithText("Start").assertDoesNotExist()
    }
}
