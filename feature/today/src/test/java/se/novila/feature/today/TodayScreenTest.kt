package se.novila.feature.today

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import se.novila.coach.DailyCoach
import se.novila.coach.dailyCoach
import se.novila.coach.readiness
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.Discovery
import se.novila.model.DiscoveryProgress
import se.novila.model.Goals
import se.novila.model.HistoryRange
import se.novila.model.TrainingAfterSleep
import se.novila.model.TrainingToday
import se.novila.model.TrendFeedback
import se.novila.model.WeekdayMetric
import se.novila.model.WeekdayPattern
import se.novila.model.analyzeSleep
import se.novila.recovery.RecoveryStatus
import se.novila.ui.preview.PreviewData
import java.time.DayOfWeek

@RunWith(RobolectricTestRunner::class)
class TodayScreenTest {
    @get:Rule val composeRule = createComposeRule()

    private fun show(
        opened: MutableList<String>,
        coach: DailyCoach = dailyCoach(PreviewData.today, Goals(), TrainingToday(plannedMinutes = 30)),
        setupIncomplete: Boolean = false,
        onCompleteSetup: () -> Unit = {},
        upcoming: List<DiscoveryProgress> = emptyList(),
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
                    onOpenDiscoveries = { opened += "discoveries" },
                    onOpenTrends = { opened += "trends" },
                    upcoming = upcoming,
                    setupIncomplete = setupIncomplete,
                    onCompleteSetup = onCompleteSetup,
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

        // One connected suggestion at the top of the checklist: the short night, the lighter session and the
        // earlier bedtime in one voice.
        composeRule
            .onNodeWithText(
                "You slept 5.5 h last night. Keep today's 30 min session light, or make it a walk.",
                substring = true,
            )
            .assertExists()
        composeRule.onNodeWithText("Aim to be in bed by", substring = true).assertExists()
    }

    @Test fun `a calm day says why, in one sentence`() {
        val night = PreviewData.today.copy(sleepHours = 5.5f)
        val calm = readiness(night, Goals(), RecoveryStatus.Low)
        show(
            opened = mutableListOf(),
            coach = dailyCoach(
                night,
                Goals(),
                TrainingToday(plannedMinutes = 30),
                recovery = RecoveryStatus.Low,
                readiness = calm,
            ),
        )

        val reasons = "because you slept 5.5 h and your resting heart rate or HRV is off your usual."
        composeRule.onNodeWithText("A calm day is suggested, $reasons", substring = true).assertExists()
    }

    @Test fun `the coach leaves out what the checklist already says`() {
        show(mutableListOf())

        // The planned session is the checklist's training row, so the coach doesn't say it again.
        composeRule.onNodeWithText("that is the day's main thing", substring = true).assertDoesNotExist()
    }

    @Test fun `the insights lead to all discoveries`() {
        val opened = mutableListOf<String>()
        show(opened)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("All discoveries"))
        composeRule.onNodeWithText("All discoveries").performClick()

        assertEquals(listOf("discoveries"), opened)
    }

    @Test fun `the insights lead to the trends of all parts`() {
        val opened = mutableListOf<String>()
        show(opened)

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Trends and history"))
        composeRule.onNodeWithText("Trends and history").performClick()

        assertEquals(listOf("trends"), opened)
    }

    @Test fun `the insights show what the logs will unlock, until it is there`() {
        show(
            mutableListOf(),
            upcoming = listOf(
                DiscoveryProgress(Discovery.SleepLinks, logged = 3, needed = 8),
                DiscoveryProgress(Discovery.Weekdays, logged = 21, needed = 21),
            ),
        )

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Unlocks as you log"))
        composeRule.onNodeWithText("What affects your sleep").assertExists()
        composeRule.onNodeWithText("3 of 8 nights").assertExists()
        // Already unlocked, so it is not a promise any more.
        composeRule.onNodeWithText("The weekday that stands out").assertDoesNotExist()
    }

    @Test fun `the trends put the parts side by side and say what changed`() {
        val ranges = mutableListOf<HistoryRange>()
        composeRule.setContent {
            NovilaTheme {
                TrendsScreen(
                    history = PreviewData.history(HistoryRange.Week),
                    goals = Goals(),
                    feedback = listOf(TrendFeedback.Sleep(hours = 0.5f)),
                    connections = emptyList(),
                    onRange = { ranges += it },
                    contentPadding = PaddingValues(0.dp),
                )
            }
        }

        composeRule.onNodeWithText("What your trends say").assertExists()
        composeRule.onNodeWithText("You slept", substring = true).assertExists()
        composeRule.onNodeWithText("Month").performClick()
        assertEquals(listOf(HistoryRange.Month), ranges)
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Training this week"))
        composeRule.onNodeWithText("Training this week").assertExists()
    }

    @Test fun `the discoveries show what the parts say about each other`() {
        composeRule.setContent {
            NovilaTheme {
                DiscoveriesScreen(
                    insights = emptyList(),
                    sleep = analyzeSleep(PreviewData.days()),
                    connections = listOf(
                        TrainingAfterSleep(35.0, 10.0, 14, 9, 2.8),
                        WeekdayPattern(
                            WeekdayMetric.TrainingMinutes,
                            DayOfWeek.TUESDAY,
                            35.0,
                            DayOfWeek.SUNDAY,
                            5.0,
                            84,
                        ),
                    ),
                    contentPadding = PaddingValues(0.dp),
                )
            }
        }

        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Connections"))
        composeRule.onNodeWithText("Connections").assertExists()
        composeRule
            .onNodeWithText("On days after a full night you trained 25 min more (14 days against 9).")
            .assertExists()
        composeRule
            .onNodeWithText("You train most on Tuesday", substring = true)
            .assertExists()
    }

    @Test fun `the connections are not on Today itself`() {
        show(mutableListOf())

        composeRule.onNodeWithText("Connections").assertDoesNotExist()
    }

    @Test fun `a skipped setup offers to finish it`() {
        var completions = 0
        show(mutableListOf(), setupIncomplete = true, onCompleteSetup = { completions++ })

        composeRule.onNodeWithText("Finish setup").performClick()

        assertEquals(1, completions)
    }

    @Test fun `nothing asks for the setup when it is finished`() {
        show(mutableListOf())

        composeRule.onNodeWithText("Setup not finished").assertDoesNotExist()
    }
}
