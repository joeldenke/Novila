package se.novila.feature.sleep

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaBottomSheet
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaEntries
import se.novila.designsystem.component.NovilaEntry
import se.novila.designsystem.component.NovilaEntryLayout
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.component.NovilaSheetContent
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.SleepFactor
import se.novila.model.averageSleep
import se.novila.sleep.SleepCoachPreferences
import se.novila.sleep.SleepCoachWeek
import se.novila.sleep.SleepState
import se.novila.sleep.WindDownMoment
import se.novila.sleep.WindDownStep
import se.novila.ui.common.DayPicker
import se.novila.ui.common.ScreenList
import se.novila.ui.common.clockText
import se.novila.ui.common.hoursText
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
    val onConnectHealth: () -> Unit = {},
    val onOpenHealthSettings: () -> Unit = {},
    val onOpenCoach: () -> Unit = {},
    val onOpenBreathing: () -> Unit = {},
    val onOpenTrends: () -> Unit = {},
)

/**
 * The Sleep tab: the shown [day]'s night in short with the one button that logs or changes it, and the ways into the
 * rest: the sleep [coach], the breathing sessions and the trends (the [history], a week up to the day, and what the
 * logs say). The night is logged in a sheet: times and hours, how it felt and what may have affected it
 * ([openSheet] at first). [today] is the day it really is: the user can go back to an earlier day and fill in a
 * night that was forgotten. Nights a watch recorded ([watch]) fill in the ones the user didn't log, [fromWatch] when
 * the shown one is.
 */
@Composable
fun SleepScreen(
    day: DayEntry,
    history: History,
    goals: Goals,
    coach: SleepCoachCardState,
    actions: SleepActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    today: LocalDate = day.date,
    watch: SleepState = SleepState.Unavailable,
    fromWatch: Boolean = false,
    openSheet: Boolean = false,
) {
    val isToday = day.date == today
    var logging by rememberSaveable(openSheet) { mutableStateOf(openSheet) }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "day") { DayPicker(day.date, today, actions.onSelectDay, NovilaTheme.pillars.sleep) }
        item(key = "night") {
            NovilaReveal(order = 0) {
                NightCard(day, goals, isToday, watch, fromWatch, actions, onLog = { logging = true })
            }
        }
        item(key = "entries") {
            NovilaReveal(order = 1) { NovilaEntries(sleepHubEntries(history, coach, actions)) }
        }
    }
    if (logging) {
        NovilaBottomSheet(title = stringResource(R.string.sleep_sheet_title), onDismiss = { logging = false }) {
            NightSheet(day, goals, actions, isToday)
        }
    }
}

/** The night as the sheet logs it: times and hours, then how it felt and what may have affected it. */
@Composable
internal fun NightSheet(day: DayEntry, goals: Goals, actions: SleepActions, isToday: Boolean) {
    SleepCard(day, goals, actions, isToday = isToday)
    SleepDetailsCard(day, actions)
}

/**
 * The night in short: the hours against the goal, when the user slept and how it felt, and the button that logs it,
 * the sun while nothing is logged. Asks for nights from Health Connect while it isn't allowed to.
 */
@Composable
private fun NightCard(
    day: DayEntry,
    goals: Goals,
    isToday: Boolean,
    watch: SleepState,
    fromWatch: Boolean,
    actions: SleepActions,
    onLog: () -> Unit,
) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaCard(
        title = stringResource(if (isToday) R.string.sleep_title else R.string.sleep_title_day),
        icon = painterResource(UiR.drawable.ic_sleep),
        role = sleep,
    ) {
        NovilaText(hoursText(day.sleepHours), style = NovilaTheme.typography.headlineSmall)
        val bedtime = day.bedtime
        val wakeTime = day.wakeTime
        val details = listOfNotNull(
            if (bedtime != null && wakeTime != null) {
                stringResource(R.string.sleep_night_times, clockText(bedtime), clockText(wakeTime))
            } else {
                null
            },
            day.sleepQuality.takeIf { it in 1..QualityLevels.size }?.let { stringResource(QualityLevels[it - 1]) },
        )
        if (details.isNotEmpty()) {
            NovilaText(details.joinToString(" · "), style = NovilaTheme.typography.bodyMedium)
        }
        NovilaText(
            stringResource(R.string.sleep_goal, hoursText(goals.sleepHours)),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        if (fromWatch) {
            NovilaText(
                stringResource(R.string.sleep_from_watch),
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        if (day.sleepHours > 0f) {
            NovilaButton(
                text = stringResource(R.string.sleep_change_night),
                onClick = onLog,
                role = sleep,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            NovilaAccentButton(
                text = stringResource(R.string.sleep_log_night),
                onClick = onLog,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (watch is SleepState.NeedsPermission) WatchAccess(watch.denied, actions)
    }
}

/** The sleep coach, the breathing sessions and the trends, each with where it stands. */
@Composable
private fun sleepHubEntries(history: History, coach: SleepCoachCardState, actions: SleepActions): List<NovilaEntry> {
    val sleep = NovilaTheme.pillars.sleep
    val average = history.days.averageSleep()
    return listOf(
        NovilaEntry(
            title = stringResource(R.string.sleep_coach_title),
            icon = painterResource(UiR.drawable.ic_sleep),
            role = sleep,
            onClick = actions.onOpenCoach,
            summary = sleepCoachSummary(coach),
        ),
        NovilaEntry(
            title = stringResource(R.string.sleep_breathing_title),
            icon = painterResource(R.drawable.ic_sleep_breathing),
            role = sleep,
            onClick = actions.onOpenBreathing,
            summary = stringResource(R.string.sleep_breathing_body),
        ),
        NovilaEntry(
            title = stringResource(R.string.sleep_entry_trends),
            icon = painterResource(UiR.drawable.ic_trend),
            role = sleep,
            onClick = actions.onOpenTrends,
            summary = average?.let { stringResource(R.string.sleep_entry_trends_summary, hoursText(it)) }
                ?: stringResource(R.string.sleep_entry_trends_empty),
        ),
    )
}

/** The sleep coach before it is set up. */
private val NewCoach = SleepCoachCardState(
    active = false,
    bedtime = SleepCoachPreferences().bedtime,
    wakeTime = SleepCoachPreferences().wakeTime,
    week = SleepCoachWeek(nearGoal = 0, logged = 0),
    tonight = null,
    hasSteps = false,
)

@Composable
private fun SleepScreenContent(
    day: DayEntry = PreviewData.today,
    today: LocalDate = day.date,
    watch: SleepState = SleepState.Unavailable,
    fromWatch: Boolean = false,
    coach: SleepCoachCardState = NewCoach,
    entryLayout: NovilaEntryLayout = NovilaEntryLayout.Grid,
) {
    NovilaTheme(entryLayout = entryLayout) {
        SleepScreen(
            day = day,
            history = PreviewData.history(HistoryRange.Week, until = day.date),
            goals = Goals(),
            coach = coach,
            actions = SleepActions(),
            contentPadding = PaddingValues(),
            today = today,
            watch = watch,
            fromWatch = fromWatch,
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SleepScreenPreview() = SleepScreenContent()

/** The ways in as rows, as chosen in Settings. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SleepScreenRowsPreview() = SleepScreenContent(entryLayout = NovilaEntryLayout.Rows)

/** Gone back to the day before yesterday. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SleepScreenEarlierDayPreview() = SleepScreenContent(day = PreviewData.history[1], today = PreviewData.date)

/** Nothing logged for the night yet: the sun asks for it. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SleepScreenEmptyPreview() = SleepScreenContent(day = DayEntry(PreviewData.date))

/** Last night as a watch recorded it, through Health Connect. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SleepScreenFromWatchPreview() = SleepScreenContent(
    watch = SleepState.Connected(emptyList()),
    fromWatch = true,
)

/** Health Connect is connected, but not for sleep yet. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SleepScreenConnectWatchPreview() = SleepScreenContent(watch = SleepState.NeedsPermission())

/** The sleep coach is set up: its way in says tonight's next step. */
@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun SleepScreenCoachPreview() = SleepScreenContent(
    coach = NewCoach.copy(
        active = true,
        bedtime = LocalTime.of(22, 30),
        wakeTime = LocalTime.of(6, 30),
        week = SleepCoachWeek(nearGoal = 4, logged = 6),
        tonight = WindDownMoment(WindDownStep.ScreensOff, LocalTime.of(21, 30)),
        hasSteps = true,
    ),
)

/** A tablet: the content stays readable in the middle, and the tiles share the row. */
@Preview(showBackground = true, widthDp = 900, heightDp = 900)
@Composable
private fun SleepScreenTabletPreview() = SleepScreenContent()

/** The sheet the night is logged in. */
@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun NightSheetPreview() {
    NovilaTheme {
        NovilaSheetContent(stringResource(R.string.sleep_sheet_title)) {
            NightSheet(PreviewData.today, Goals(), SleepActions(), isToday = true)
        }
    }
}
