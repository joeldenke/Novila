package se.novila.feature.sleep

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaReveal
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.weekOf
import se.novila.ui.common.DayPicker
import se.novila.ui.common.ScreenList
import se.novila.ui.common.SleepWeekCard
import se.novila.ui.preview.PreviewData
import java.time.LocalDate

/**
 * The shown [day]'s sleep and the week up to it. [today] is the day it really is: the user can go back to an earlier
 * day ([onSelectDay]) and fill in a night that was forgotten.
 */
@Composable
fun SleepScreen(
    day: DayEntry,
    history: List<DayEntry>,
    goals: Goals,
    onSleep: (Float) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    today: LocalDate = day.date,
    onSelectDay: (LocalDate) -> Unit = {},
) {
    val week = remember(day, history) { weekOf(day, history) }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "day") { DayPicker(day.date, today, onSelectDay, NovilaTheme.pillars.sleep) }
        item(key = "sleep") {
            NovilaReveal(order = 0) { SleepCard(day.sleepHours, goals, onSleep, isToday = day.date == today) }
        }
        item(key = "week") { NovilaReveal(order = 1) { SleepWeekCard(week, goals) } }
    }
}

@Preview(showBackground = true)
@Composable
private fun SleepScreenPreview() {
    NovilaTheme {
        SleepScreen(PreviewData.today, PreviewData.history, Goals(), onSleep = {}, contentPadding = PaddingValues())
    }
}

/** Gone back to the day before yesterday. */
@Preview(showBackground = true)
@Composable
private fun SleepScreenEarlierDayPreview() {
    NovilaTheme {
        SleepScreen(
            day = PreviewData.history[1],
            history = PreviewData.history.drop(2),
            goals = Goals(),
            onSleep = {},
            contentPadding = PaddingValues(),
            today = PreviewData.date,
        )
    }
}
