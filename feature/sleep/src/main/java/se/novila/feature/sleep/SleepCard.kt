package se.novila.feature.sleep

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaListRow
import se.novila.designsystem.component.NovilaSlider
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTimeDialog
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.sleep.R
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.MAX_SLEEP_HOURS
import se.novila.ui.common.hoursText
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import se.novila.ui.R as UiR

/** Where a time picker opens before the user has given that time. */
internal val DefaultBedtime: LocalTime = LocalTime.of(23, 0)
internal val DefaultWakeTime: LocalTime = LocalTime.of(7, 0)

/**
 * The night's sleep: when the user went to bed and woke up, the hours that follow, and the user's sleep goal from
 * [goals]. The half-hour slider sets the hours alone for a quick log. Last night when [isToday], otherwise the night
 * before the shown day.
 */
@Composable
fun SleepCard(
    day: DayEntry,
    goals: Goals,
    actions: SleepActions,
    isToday: Boolean = true,
) {
    val sleep = NovilaTheme.pillars.sleep
    var choosing by rememberSaveable { mutableStateOf<SleepTime?>(null) }
    NovilaCard(
        title = stringResource(if (isToday) R.string.sleep_title else R.string.sleep_title_day),
        icon = painterResource(UiR.drawable.ic_sleep),
        role = sleep,
    ) {
        NovilaText(hoursText(day.sleepHours), style = NovilaTheme.typography.headlineSmall)
        NovilaText(
            stringResource(R.string.sleep_goal, hoursText(goals.sleepHours)),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        TimeRow(R.string.sleep_bedtime, day.bedtime) { choosing = SleepTime.Bedtime }
        TimeRow(R.string.sleep_wake_time, day.wakeTime) { choosing = SleepTime.WakeTime }
        NovilaText(
            stringResource(R.string.sleep_hours_hint),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        NovilaSlider(
            value = day.sleepHours,
            onValueChange = actions.onSleep,
            valueRange = 0f..MAX_SLEEP_HOURS,
            steps = (MAX_SLEEP_HOURS * 2).toInt() - 1, // half-hour steps
            role = sleep,
        )
    }
    choosing?.let { which ->
        val current = when (which) {
            SleepTime.Bedtime -> day.bedtime ?: DefaultBedtime
            SleepTime.WakeTime -> day.wakeTime ?: DefaultWakeTime
        }
        NovilaTimeDialog(
            title = stringResource(
                if (which == SleepTime.Bedtime) R.string.sleep_bedtime_title else R.string.sleep_wake_time_title,
            ),
            hour = current.hour,
            minute = current.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
            confirmLabel = stringResource(R.string.sleep_save),
            dismissLabel = stringResource(R.string.sleep_cancel),
            onConfirm = { hour, minute ->
                choosing = null
                val time = LocalTime.of(hour, minute)
                if (which == SleepTime.Bedtime) actions.onBedtime(time) else actions.onWakeTime(time)
            },
            onDismiss = { choosing = null },
            role = sleep,
        )
    }
}

private enum class SleepTime { Bedtime, WakeTime }

@Composable
private fun TimeRow(title: Int, time: LocalTime?, onClick: () -> Unit) {
    NovilaListRow(
        title = stringResource(title),
        onClick = onClick,
        trailing = {
            NovilaText(
                time?.let { timeText(it) } ?: stringResource(UiR.string.value_missing),
                style = NovilaTheme.typography.titleMedium,
            )
        },
    )
}

/** The time as the phone shows it: "23:15", or "11:15 PM" with a 12-hour clock. */
@Composable
private fun timeText(time: LocalTime): String {
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(LocalContext.current)
    val formatter = remember(locale, is24Hour) {
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"), locale)
    }
    return time.format(formatter)
}
