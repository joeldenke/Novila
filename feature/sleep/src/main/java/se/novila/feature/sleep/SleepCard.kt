package se.novila.feature.sleep

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaButton
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
import se.novila.sleep.SleepState
import se.novila.ui.common.clockText
import se.novila.ui.common.hoursText
import se.novila.ui.common.timeText
import java.time.LocalTime
import se.novila.ui.R as UiR

/**
 * The night's sleep: when the user went to bed and woke up, the hours that follow, and the user's sleep goal from
 * [goals]. The half-hour slider sets the hours alone for a quick log. Last night when [isToday], otherwise the night
 * before the shown day. Says so when the night is one a watch recorded ([fromWatch]), and offers to read nights from
 * Health Connect while it isn't allowed to ([watch]).
 */
@Composable
fun SleepCard(
    day: DayEntry,
    goals: Goals,
    actions: SleepActions,
    isToday: Boolean = true,
    watch: SleepState = SleepState.Unavailable,
    fromWatch: Boolean = false,
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
        if (fromWatch) {
            NovilaText(
                stringResource(R.string.sleep_from_watch),
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
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
        if (watch is SleepState.NeedsPermission) WatchAccess(watch.denied, actions)
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

/**
 * Access to sleep was added after steps, calories and workouts, so someone who connected Health Connect before is
 * asked for it here. After a no, only Health Connect's settings can give it.
 */
@Composable
private fun WatchAccess(denied: Boolean, actions: SleepActions) {
    NovilaText(
        stringResource(R.string.sleep_watch_body),
        style = NovilaTheme.typography.bodyMedium,
        color = NovilaTheme.colors.onSurfaceVariant,
    )
    NovilaButton(
        text = stringResource(if (denied) UiR.string.movement_open_settings else R.string.sleep_watch_connect),
        onClick = if (denied) actions.onOpenHealthSettings else actions.onConnectHealth,
        role = NovilaTheme.pillars.sleep,
    )
}

@Composable
private fun TimeRow(title: Int, time: LocalTime?, onClick: () -> Unit) {
    NovilaListRow(
        title = stringResource(title),
        onClick = onClick,
        trailing = {
            NovilaText(
                time?.let { clockText(it) } ?: stringResource(UiR.string.value_missing),
                style = NovilaTheme.typography.titleMedium,
            )
        },
    )
}
