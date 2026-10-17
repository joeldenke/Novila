package se.novila.feature.sleep

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.ui.common.DayPicker
import se.novila.ui.common.ScreenList
import se.novila.ui.common.SleepHistoryCard
import se.novila.ui.preview.PreviewData
import java.time.LocalDate

/**
 * The shown [day]'s sleep and the [history] up to it, over a week, four weeks or a year ([onRange]). [today] is the
 * day it really is: the user can go back to an earlier day ([onSelectDay]) and fill in a night that was forgotten.
 */
@Composable
fun SleepScreen(
    day: DayEntry,
    history: History,
    goals: Goals,
    onSleep: (Float) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    today: LocalDate = day.date,
    onSelectDay: (LocalDate) -> Unit = {},
    onRange: (HistoryRange) -> Unit = {},
) {
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "day") { DayPicker(day.date, today, onSelectDay, NovilaTheme.pillars.sleep) }
        item(key = "sleep") {
            NovilaReveal(order = 0) { SleepCard(day.sleepHours, goals, onSleep, isToday = day.date == today) }
        }
        item(key = "history") { NovilaReveal(order = 1) { SleepHistoryCard(history, goals, onRange) } }
    }
}

@Preview(showBackground = true)
@Composable
private fun SleepScreenPreview() {
    NovilaTheme {
        SleepScreen(
            PreviewData.today,
            PreviewData.history(HistoryRange.Week),
            Goals(),
            onSleep = {},
            contentPadding = PaddingValues(),
        )
    }
}

/** Gone back to the day before yesterday. */
@Preview(showBackground = true)
@Composable
private fun SleepScreenEarlierDayPreview() {
    NovilaTheme {
        SleepScreen(
            day = PreviewData.history[1],
            history = PreviewData.history(HistoryRange.Week, until = PreviewData.history[1].date),
            goals = Goals(),
            onSleep = {},
            contentPadding = PaddingValues(),
            today = PreviewData.date,
        )
    }
}

/** The last twelve months, a point per month. */
@Preview(showBackground = true)
@Composable
private fun SleepScreenYearPreview() {
    NovilaTheme {
        SleepScreen(
            PreviewData.today,
            PreviewData.history(HistoryRange.Year),
            Goals(),
            onSleep = {},
            contentPadding = PaddingValues(),
        )
    }
}
