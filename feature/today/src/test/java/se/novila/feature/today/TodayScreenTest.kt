package se.novila.feature.today

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.coach.DailyCoach
import se.novila.coach.dailyCoach
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.Goals
import se.novila.model.TrainingToday
import se.novila.model.analyzeSleep
import se.novila.ui.preview.PreviewData

@RunWith(RobolectricTestRunner::class)
class TodayScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(
        opened: MutableList<String>,
        coach: DailyCoach = dailyCoach(PreviewData.today, Goals(), TrainingToday(plannedMinutes = 30)),
    ) {
        composeRule.setContent {
            NovilaTheme {
                TodayScreen(
                    today = PreviewData.today,
                    history = PreviewData.history,
                    hour = 8,
                    name = PreviewData.profile.name,
                    goals = Goals(),
                    sleep = analyzeSleep(PreviewData.days()),
                    coach = coach,
                    movement = PreviewData.movement,
                    onAddWater = { opened += "water" },
                    onOpenTraining = { opened += "training" },
                    onOpenSleep = { opened += "sleep" },
                    onOpenNutrition = { opened += "nutrition" },
                    onOpenSettings = { opened += "settings" },
                    contentPadding = PaddingValues(0.dp),
                    training = TrainingToday(plannedMinutes = 30),
                )
            }
        }
    }

    @Test fun `the balance rings are the summary and open each part`() {
        val opened = mutableListOf<String>()
        show(opened)

        composeRule.onNodeWithText("Movement").performClick()
        composeRule.onNodeWithText("Sleep").performClick()
        composeRule.onNodeWithText("Nutrition").performClick()

        assertEquals(listOf("training", "sleep", "nutrition"), opened)
    }

    @Test fun `the balance does not repeat the checklist`() {
        show(mutableListOf())

        // The copy under the rings is gone; the checklist already says what to do next.
        composeRule.onNodeWithText("Log last night's sleep to complete your balance.").assertDoesNotExist()
    }

    @Test fun `the one coach connects the parts in a single suggestion`() {
        val night = PreviewData.today.copy(sleepHours = 5.5f)
        show(
            opened = mutableListOf(),
            coach = dailyCoach(night, Goals(), TrainingToday(plannedMinutes = 30)),
        )

        composeRule.onNodeWithText("Your coach").assertExists()
        // One connected suggestion: the short night, the lighter session and the earlier bedtime in one voice.
        composeRule
            .onNodeWithText(
                "You slept 5.5 h last night. Keep today's 30 min session light, or make it a walk.",
                substring = true,
            )
            .assertExists()
        composeRule.onNodeWithText("Aim to be in bed by", substring = true).assertExists()
    }
}
