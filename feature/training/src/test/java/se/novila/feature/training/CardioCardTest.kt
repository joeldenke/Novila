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
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.ExerciseType
import se.novila.exercise.PlannedSession
import se.novila.exercise.Workout
import se.novila.model.CardioLog
import se.novila.model.CardioType
import java.time.DayOfWeek
import kotlin.time.Instant

// Tall enough for the whole card and the finished session, so nothing has to be scrolled to.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1600dp")
class CardioCardTest {
    @get:Rule val composeRule = createComposeRule()

    private val run = PlannedSession(DayOfWeek.TUESDAY, ExerciseType.Running, minutes = 30)

    @Test fun `I did it logs today's planned cardio, adjusted by the user`() {
        val logged = mutableListOf<CardioLog>()
        composeRule.setContent {
            NovilaTheme { CardioCard(CardioToday(), run, onLog = { logged += it }, onUndo = {}) }
        }
        composeRule.onNodeWithContentDescription("Five minutes more").performClick()
        composeRule.onNodeWithText("I did it").performClick()
        composeRule.onNodeWithText("Swimming").performClick()
        composeRule.onNodeWithText("I did it").performClick()
        assertEquals(listOf(CardioLog(CardioType.Running, 35), CardioLog(CardioType.Swimming, 35)), logged)
    }

    @Test fun `a logged session can be taken back`() {
        var undone = false
        composeRule.setContent {
            NovilaTheme {
                CardioCard(
                    CardioToday(logged = listOf(CardioLog(CardioType.Walking, 20))),
                    planned = null,
                    onLog = {},
                    onUndo = { undone = true },
                )
            }
        }
        composeRule.onNodeWithText("Logged today: 20 minutes. Next week's program builds on it.").assertExists()
        composeRule.onNodeWithText("Undo").performClick()
        assertEquals(true, undone)
    }

    @Test fun `a finished session asks for effort and pain before saving`() {
        val session = TrainingPreviewData.program.sessions.first { it.exercises.isNotEmpty() }
        var workout = Workout.start(session, Instant.fromEpochSeconds(0))
        while (!workout.isFinished) workout = workout.next(Instant.fromEpochSeconds(600))
        var saved = false
        composeRule.setContent {
            NovilaTheme {
                WorkoutScreen(
                    state = WorkoutUiState(workout, secondsLeft = null, SessionFeedbackDraft(effort = 9, pain = 7)),
                    onNext = {},
                    onEffort = {},
                    onPain = {},
                    onFinish = { saved = true },
                    onShowHow = {},
                    contentPadding = PaddingValues(),
                )
            }
        }
        composeRule.onNodeWithText("9 of 10, close to all you have").assertExists()
        composeRule.onNodeWithText("7 of 10, too much").assertExists()
        composeRule.onNodeWithText("Save the session").performClick()
        assertEquals(true, saved)
    }
}
