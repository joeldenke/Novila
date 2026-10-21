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
import se.novila.exercise.Dosage
import se.novila.exercise.Workout
import se.novila.exercise.WorkoutStep
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

// Tall enough for the whole card, so nothing has to be scrolled to.
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h1600dp")
class WorkoutScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private val session = TrainingPreviewData.program.sessions.first { it.exercises.isNotEmpty() }

    @Test fun `the warm-up comes before the first set and starts it`() {
        var advanced = 0
        composeRule.setContent {
            NovilaTheme {
                WorkoutScreen(
                    state = WorkoutUiState(Workout.start(session, Instant.fromEpochSeconds(0)), secondsLeft = 170),
                    onNext = { advanced++ },
                    onEffort = {},
                    onPain = {},
                    onFinish = {},
                    onShowHow = {},
                    contentPadding = PaddingValues(),
                )
            }
        }
        composeRule.onNodeWithText("Warm up").assertExists()
        composeRule.onNodeWithText("Start the first set").performClick()
        assertEquals(1, advanced)
    }

    @Test fun `the cool-down is the last step and ends the session`() {
        var workout = Workout.start(session, Instant.fromEpochSeconds(0))
        var now = Instant.fromEpochSeconds(0)
        while (workout.index < workout.steps.size - 1) {
            now += 60.seconds
            workout = workout.next(now)
        }
        var advanced = 0
        composeRule.setContent {
            NovilaTheme {
                WorkoutScreen(
                    state = WorkoutUiState(workout, secondsLeft = workout.secondsLeft(now + 20.seconds)),
                    onNext = { advanced++ },
                    onEffort = {},
                    onPain = {},
                    onFinish = {},
                    onShowHow = {},
                    contentPadding = PaddingValues(),
                )
            }
        }
        composeRule.onNodeWithText("Cool down").assertExists()
        composeRule.onNodeWithText("Finish the session").performClick()
        assertEquals(1, advanced)
    }

    @Test fun `a rest still counts down with a button to skip it`() {
        var workout = Workout.start(session, Instant.fromEpochSeconds(0))
        var now = Instant.fromEpochSeconds(0)
        while (workout.step !is WorkoutStep.Rest) {
            now += 60.seconds
            workout = workout.next(now)
        }
        var advanced = 0
        composeRule.setContent {
            NovilaTheme {
                WorkoutScreen(
                    state = WorkoutUiState(workout, secondsLeft = workout.secondsLeft(now + 5.seconds)),
                    onNext = { advanced++ },
                    onEffort = {},
                    onPain = {},
                    onFinish = {},
                    onShowHow = {},
                    contentPadding = PaddingValues(),
                )
            }
        }
        composeRule.onNodeWithText("Rest").assertExists()
        composeRule.onNodeWithText("Skip the rest").performClick()
        assertEquals(1, advanced)
    }

    private fun show(state: WorkoutUiState, onDone: (Int) -> Unit = {}) {
        composeRule.setContent {
            NovilaTheme {
                WorkoutScreen(
                    state = state,
                    onNext = {},
                    onEffort = {},
                    onPain = {},
                    onFinish = {},
                    onShowHow = {},
                    contentPadding = PaddingValues(),
                    onDone = onDone,
                )
            }
        }
    }

    @Test fun `a repetition set asks how many were done, starting from the plan`() {
        var workout = Workout.start(session, Instant.fromEpochSeconds(0))
        while ((workout.step as? WorkoutStep.Work)?.exercise?.dosage != Dosage.Repetitions) {
            workout = workout.next(Instant.fromEpochSeconds(0))
        }
        val planned = (workout.step as WorkoutStep.Work).exercise.amount
        var done: Int? = null
        show(WorkoutUiState(workout, secondsLeft = null)) { done = it }

        composeRule.onNodeWithText("You did").assertExists()
        composeRule.onNodeWithContentDescription("One repetition fewer").performClick()
        assertEquals(planned - 1, done)
    }

    @Test fun `a rehab set does not ask for repetitions`() {
        var workout = Workout.start(session, Instant.fromEpochSeconds(0))
        while ((workout.step as? WorkoutStep.Work)?.exercise?.dosage != Dosage.Repetitions) {
            workout = workout.next(Instant.fromEpochSeconds(0))
        }
        show(WorkoutUiState(workout, secondsLeft = null, rehab = true))

        composeRule.onNodeWithText("You did").assertDoesNotExist()
    }

    @Test fun `the finished session says how each exercise moves on next time`() {
        var workout = Workout.start(session, Instant.fromEpochSeconds(0))
        var now = Instant.fromEpochSeconds(0)
        while (!workout.isFinished) {
            now += 60.seconds
            workout = workout.next(now)
        }
        show(WorkoutUiState(workout, secondsLeft = null))

        composeRule.onNodeWithText("Next time").assertExists()
        val first = workout.nextChanges().first().first
        val more = if (first.dosage == Dosage.Seconds) "5 seconds more" else "1 repetition more"
        composeRule.onNodeWithText(more, substring = true).assertExists()
    }
}
