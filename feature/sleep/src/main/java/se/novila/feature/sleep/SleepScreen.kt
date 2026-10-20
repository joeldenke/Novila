package se.novila.feature.sleep

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.SleepAnalysis
import se.novila.model.SleepFactor
import se.novila.model.analyzeSleep
import se.novila.sleep.SleepCoachPreferences
import se.novila.sleep.SleepCoachWeek
import se.novila.sleep.SleepState
import se.novila.sleep.WindDownMoment
import se.novila.sleep.WindDownStep
import se.novila.ui.common.DayPicker
import se.novila.ui.common.ScreenList
import se.novila.ui.common.ScreenSection
import se.novila.ui.common.SleepHistoryCard
import se.novila.ui.common.section
import se.novila.ui.preview.PreviewData
import java.time.LocalDate
import java.time.LocalTime
import se.novila.ui.R as UiR

/** What the Sleep screen can do. The entry connects these to the ViewModel. */
data class SleepActions(
    val onSleep: (Float) -> Unit = {},
    val onBedtime: (LocalTime) -> Unit = {},
    val onWakeTime: (LocalTime) -> Unit = {},
    val onQuality: (Int) -> Unit = {},
    val onFactor: (SleepFactor, Boolean) -> Unit = { _, _ -> },
    val onSelectDay: (LocalDate) -> Unit = {},
    val onRange: (HistoryRange) -> Unit = {},
    val onConnectHealth: () -> Unit = {},
    val onOpenHealthSettings: () -> Unit = {},
    val onOpenCoach: () -> Unit = {},
    val onOpenBreathing: () -> Unit = {},
)

/**
 * The Sleep tab in the parts every tab has ([ScreenSection]): the shown [day]'s night (times and hours), logging how it
 * felt and what may have affected it, the sleep [coach] in short, and the trends: the [history] up to the day, over a
 * week, four weeks or a year, and what the user's logs say about their sleep ([analysis]). [today] is the day it
 * really is: the user can go back to an earlier day and fill in a night that was forgotten. Nights a watch recorded
 * ([watch]) fill in the ones the user didn't log, [fromWatch] when the shown one is. [coach] is the sleep coach in
 * short, opened from the button on it.
 */
@Composable
fun SleepScreen(
    day: DayEntry,
    history: History,
    goals: Goals,
    analysis: SleepAnalysis,
    coach: SleepCoachCardState,
    actions: SleepActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    today: LocalDate = day.date,
    watch: SleepState = SleepState.Unavailable,
    fromWatch: Boolean = false,
) {
    val isToday = day.date == today
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "day") { DayPicker(day.date, today, actions.onSelectDay, NovilaTheme.pillars.sleep) }
        section(ScreenSection.Overview)
        item(key = "sleep") {
            NovilaReveal(order = 0) {
                SleepCard(day, goals, actions, isToday = isToday, watch = watch, fromWatch = fromWatch)
            }
        }
        section(ScreenSection.Log)
        item(key = "details") { NovilaReveal(order = 1) { SleepDetailsCard(day, actions) } }
        section(ScreenSection.Plan)
        item(key = "coach") { NovilaReveal(order = 2) { SleepCoachCard(coach, actions.onOpenCoach) } }
        item(key = "breathing") { NovilaReveal(order = 3) { BreathingLinkCard(actions.onOpenBreathing) } }
        section(ScreenSection.Trends)
        item(key = "history") { NovilaReveal(order = 4) { SleepHistoryCard(history, goals, actions.onRange) } }
        item(key = "findings") { NovilaReveal(order = 5) { SleepFindingsCard(analysis) } }
    }
}

/**
 * A quiet link to the guided breathing sessions, in Sleep's plan beside the coach: a few calm minutes fit the
 * evening routine. The sessions themselves live in their own feature; this only opens them.
 */
@Composable
private fun BreathingLinkCard(onOpen: () -> Unit) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaCard(
        title = stringResource(R.string.sleep_breathing_title),
        icon = painterResource(UiR.drawable.ic_sleep),
        role = sleep,
    ) {
        NovilaText(
            stringResource(R.string.sleep_breathing_body),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        NovilaButton(
            text = stringResource(R.string.sleep_breathing_open),
            onClick = onOpen,
            role = sleep,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SleepScreenContent(
    day: DayEntry,
    history: History,
    today: LocalDate = day.date,
    watch: SleepState = SleepState.Unavailable,
    fromWatch: Boolean = false,
    coach: SleepCoachCardState = SleepCoachCardState(
        active = false,
        bedtime = SleepCoachPreferences().bedtime,
        wakeTime = SleepCoachPreferences().wakeTime,
        week = SleepCoachWeek(nearGoal = 0, logged = 0),
        tonight = null,
        hasSteps = false,
    ),
) {
    NovilaTheme {
        SleepScreen(
            day = day,
            history = history,
            goals = Goals(),
            analysis = analyzeSleep(PreviewData.days(until = today)),
            coach = coach,
            actions = SleepActions(),
            contentPadding = PaddingValues(),
            today = today,
            watch = watch,
            fromWatch = fromWatch,
        )
    }
}

@Preview(showBackground = true, heightDp = 2200)
@Composable
private fun SleepScreenPreview() = SleepScreenContent(PreviewData.today, PreviewData.history(HistoryRange.Week))

/** Gone back to the day before yesterday. */
@Preview(showBackground = true, heightDp = 2200)
@Composable
private fun SleepScreenEarlierDayPreview() = SleepScreenContent(
    day = PreviewData.history[1],
    history = PreviewData.history(HistoryRange.Week, until = PreviewData.history[1].date),
    today = PreviewData.date,
)

/** The last twelve months, a point per month. */
@Preview(showBackground = true, heightDp = 2200)
@Composable
private fun SleepScreenYearPreview() = SleepScreenContent(PreviewData.today, PreviewData.history(HistoryRange.Year))

/** Last night as a watch recorded it, through Health Connect. */
@Preview(showBackground = true, heightDp = 2200)
@Composable
private fun SleepScreenFromWatchPreview() = SleepScreenContent(
    day = PreviewData.today,
    history = PreviewData.history(HistoryRange.Week),
    watch = SleepState.Connected(emptyList()),
    fromWatch = true,
)

/** Health Connect is connected, but not for sleep yet. */
@Preview(showBackground = true, heightDp = 2200)
@Composable
private fun SleepScreenConnectWatchPreview() = SleepScreenContent(
    day = PreviewData.today,
    history = PreviewData.history(HistoryRange.Week),
    watch = SleepState.NeedsPermission(),
)

/** The sleep coach is set up: goal, this week's nights and tonight's next step. */
@Preview(showBackground = true, heightDp = 2200)
@Composable
private fun SleepScreenCoachPreview() = SleepScreenContent(
    day = PreviewData.today,
    history = PreviewData.history(HistoryRange.Week),
    coach = SleepCoachCardState(
        active = true,
        bedtime = LocalTime.of(22, 30),
        wakeTime = LocalTime.of(6, 30),
        week = SleepCoachWeek(nearGoal = 4, logged = 6),
        tonight = WindDownMoment(WindDownStep.ScreensOff, LocalTime.of(21, 30)),
        hasSteps = true,
    ),
)
