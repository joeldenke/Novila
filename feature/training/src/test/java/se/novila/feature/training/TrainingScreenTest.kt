package se.novila.feature.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.novila.anatomy.ExerciseLibrary
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.CardioLog
import se.novila.model.CardioType
import se.novila.ui.preview.PreviewData
import java.time.DayOfWeek

// Tall enough for the overview and the cards below it, so nothing has to be scrolled to.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h2000dp")
class TrainingScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val state = TrainingUiState(
        program = TrainingPreviewData.program,
        coach = TrainingPreviewData.newCoach,
        today = DayOfWeek.MONDAY,
        strengthMinutesToday = 0,
    )

    private fun show(state: TrainingUiState, actions: TrainingActions) = composeRule.setContent {
        NovilaTheme {
            TrainingScreen(state, PreviewData.movement, actions, contentPadding = PaddingValues())
        }
    }

    @Test fun `a strength day's main action starts today's session`() {
        val started = mutableListOf<DayOfWeek>()
        show(state, TrainingActions(onStartWorkout = { started += it }))
        composeRule.onNodeWithText("Start the session").performClick()
        assertEquals(listOf(DayOfWeek.MONDAY), started)
    }

    @Test fun `a cardio day's main action logs the planned cardio in one tap`() {
        val logged = mutableListOf<CardioLog>()
        val tuesday = state.copy(today = DayOfWeek.TUESDAY)
        val run = tuesday.program.sessions.first { it.day == DayOfWeek.TUESDAY }
        show(tuesday, TrainingActions(onLogCardio = { logged += it }))
        composeRule.onNodeWithText("I did it").performClick()
        assertEquals(listOf(CardioLog(CardioType.Running, run.minutes)), logged)
    }

    @Test fun `quick actions open the log and the coach`() {
        var opened = ""
        show(state, TrainingActions(onOpenCardioLog = { opened += "log" }, onOpenCoach = { opened += "coach" }))
        composeRule.onNodeWithText("Log a workout").performClick()
        composeRule.onNodeWithText("Open the coach").performClick()
        assertEquals("logcoach", opened)
    }

    @Test fun `the exercise player plays once and can be dragged and played again`() {
        val squat = ExerciseLibrary.byId("squat")!!
        composeRule.setContent {
            NovilaTheme {
                AnatomyPlayer(squat, contentDescription = "Squat", repetitions = 3)
            }
        }
        // The test clock runs the three repetitions to the end, where the player stops.
        composeRule.onNodeWithText("Repetition 3 of 3").assertExists()
        composeRule.onNodeWithContentDescription("Play the movement").performClick()
        composeRule.onNodeWithText("Repetition 3 of 3").assertExists()
        composeRule.onNodeWithContentDescription("Drag through the movement").assertExists()
    }
}
