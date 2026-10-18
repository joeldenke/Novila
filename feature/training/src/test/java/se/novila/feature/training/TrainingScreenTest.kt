package se.novila.feature.training

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import se.novila.anatomy.ExerciseLibrary
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.CardioLog
import se.novila.model.CardioType
import se.novila.recovery.RecoveryState
import se.novila.recovery.RecoveryStatus
import se.novila.ui.preview.PreviewData
import java.time.DayOfWeek

// Tall enough for the overview and the cards below it, so nothing has to be scrolled to.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h3000dp")
class TrainingScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val state = TrainingUiState(
        program = TrainingPreviewData.program,
        coach = TrainingPreviewData.newCoach,
        today = DayOfWeek.MONDAY,
        strengthMinutesToday = 0,
    )

    private fun show(
        state: TrainingUiState,
        actions: TrainingActions,
        recovery: RecoveryState = RecoveryState.Unavailable,
    ) = composeRule.setContent {
        NovilaTheme {
            TrainingScreen(
                state,
                PreviewData.movement,
                actions,
                contentPadding = PaddingValues(),
                recovery = recovery,
            )
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

    @Test fun `a strength day in the week strip and the program opens its session`() {
        val opened = mutableListOf<DayOfWeek>()
        show(state, TrainingActions(onOpenSession = { opened += it }))
        composeRule.onNodeWithContentDescription("Thursday: Strength, full body, 30 minutes planned").performClick()
        composeRule.onNodeWithText("Thu").performClick()
        assertEquals(listOf(DayOfWeek.THURSDAY, DayOfWeek.THURSDAY), opened)
    }

    @Test fun `the library finds exercises by name and muscle`() {
        composeRule.setContent {
            NovilaTheme { ExerciseLibraryScreen(onOpenExercise = {}, contentPadding = PaddingValues()) }
        }
        composeRule.onNodeWithText("13 exercises").assertExists()
        composeRule.onNode(hasSetTextAction()).performTextInput("push")
        composeRule.onNodeWithText("Push-up").assertExists()
        composeRule.onNodeWithText("Squat").assertDoesNotExist()
        composeRule.onNode(hasSetTextAction()).performTextReplacement("calves")
        composeRule.onNodeWithText("Calf raise").assertExists()
        composeRule.onNodeWithContentDescription("Clear the search").performClick()
        composeRule.onNodeWithText("13 exercises").assertExists()
    }

    @Test fun `a low recovery shows the readings and says today's session is lighter`() {
        show(
            state.copy(recovery = RecoveryStatus.Low),
            TrainingActions(),
            recovery = TrainingPreviewData.recovery,
        )
        composeRule.onNodeWithText("Recovery").assertExists()
        composeRule.onNodeWithText("56 bpm").assertExists()
        composeRule.onNodeWithText("38 ms").assertExists()
        composeRule
            .onNodeWithText(
                "Your resting heart rate is up, or your HRV is down, compared with your usual. " +
                    "Today's session is a little lighter.",
            )
            .assertExists()
    }

    @Test fun `the recovery card connects while access is missing`() {
        var connected = false
        show(
            state,
            TrainingActions(onConnectRecovery = { connected = true }),
            recovery = RecoveryState.NeedsPermission(),
        )
        // The chip and the card's button both ask to connect.
        composeRule.onAllNodesWithText("Connect recovery")[0].performClick()
        assertTrue(connected)
    }
}
