package se.novila.feature.sleep

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaSlider
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.sleep.R
import se.novila.model.Goals
import se.novila.model.MAX_SLEEP_HOURS
import se.novila.ui.common.hoursText
import se.novila.ui.R as UiR

/**
 * The night's hours on a half-hour slider, and the user's sleep goal from [goals]. Last night when [isToday],
 * otherwise the night before the shown day.
 */
@Composable
fun SleepCard(hours: Float, goals: Goals, onChange: (Float) -> Unit, isToday: Boolean = true) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaCard(
        title = stringResource(if (isToday) R.string.sleep_title else R.string.sleep_title_day),
        icon = painterResource(UiR.drawable.ic_sleep),
        role = sleep,
    ) {
        NovilaText(hoursText(hours), style = NovilaTheme.typography.headlineSmall)
        NovilaText(
            stringResource(R.string.sleep_goal, hoursText(goals.sleepHours)),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        NovilaSlider(
            value = hours,
            onValueChange = onChange,
            valueRange = 0f..MAX_SLEEP_HOURS,
            steps = (MAX_SLEEP_HOURS * 2).toInt() - 1, // half-hour steps
            role = sleep,
        )
    }
}
