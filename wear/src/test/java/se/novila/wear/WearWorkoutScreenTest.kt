package se.novila.wear

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.exercise.StandaloneSession
import se.novila.exercise.StrengthLevel
import java.time.DayOfWeek
import kotlin.time.Instant

/** Renders the watch screen in Novila's Wear theme: the plan shows its dose, and a running step shows its timer. */
@RunWith(RobolectricTestRunner::class)
class WearWorkoutScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val monday = WearWorkoutUiState(day = DayOfWeek.MONDAY)

    @Test
    fun thePlanShowsTheSessionsDose() {
        compose.setContent {
            NovilaWearTheme {
                WearWorkoutScreen(
                    state = monday,
                    onLevel = {},
                    onSession = {},
                    onStart = {},
                    onNext = {},
                    onNewSession = {},
                )
            }
        }

        compose.onNodeWithText("Strength session").assertIsDisplayed()
        compose.onNodeWithText("Pass A · 5 exercises · about 30 min").assertIsDisplayed()
    }

    @Test
    fun aRunningWarmUpShowsTheCountdown() {
        val start = Instant.fromEpochSeconds(0)
        val workout = StandaloneSession.start(DayOfWeek.MONDAY, StrengthLevel.Beginner, start)
        val state = monday.copy(workout = workout, secondsLeft = workout.secondsLeft(start))
        compose.setContent {
            NovilaWearTheme {
                WearWorkoutScreen(
                    state = state,
                    onLevel = {},
                    onSession = {},
                    onStart = {},
                    onNext = {},
                    onNewSession = {},
                )
            }
        }

        compose.onNodeWithText("Warm up").assertIsDisplayed()
        compose.onNodeWithText("3:00").assertIsDisplayed()
    }
}
