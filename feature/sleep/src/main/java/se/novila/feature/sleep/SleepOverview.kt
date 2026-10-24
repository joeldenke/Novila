package se.novila.feature.sleep

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaHeroBar
import se.novila.designsystem.component.NovilaHeroBars
import se.novila.designsystem.component.NovilaHeroPanel
import se.novila.designsystem.component.NovilaHeroPart
import se.novila.designsystem.component.NovilaHeroRing
import se.novila.designsystem.component.NovilaHeroText
import se.novila.designsystem.component.NovilaSky
import se.novila.designsystem.component.novilaActionWidth
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.sleep.R
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.ui.common.clockText
import se.novila.ui.common.hoursText
import se.novila.ui.common.weekdayLabels
import java.time.format.TextStyle

/**
 * The night at a glance at the top of Sleep, on the night lake under the moon: the hours against the goal, when the
 * user slept and how it felt, the nights of the week up to the shown [day] ([week]) against the goal, and the button
 * that logs the night, the sun while nothing is logged. Says so when the night is one a watch recorded ([fromWatch]).
 */
@Composable
internal fun SleepOverview(
    day: DayEntry,
    week: List<DayEntry>,
    goals: Goals,
    isToday: Boolean,
    fromWatch: Boolean,
    onLog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val logged = day.sleepHours > 0f
    NovilaHeroPanel(
        title = stringResource(if (isToday) R.string.sleep_title else R.string.sleep_title_day),
        modifier = modifier,
        // The overview is about the night, so the moon is up whatever the hour.
        sky = NovilaSky.Night,
        part = NovilaHeroPart.Sleep,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.l),
        ) {
            NovilaHeroRing(
                progress = if (goals.sleepHours > 0f) day.sleepHours / goals.sleepHours else 0f,
                value = hoursText(day.sleepHours),
                unit = stringResource(R.string.sleep_hero_of_goal, hoursText(goals.sleepHours)),
            )
            NovilaHeroText(
                headline = nightHeadline(day),
                body = nightBody(day, goals, fromWatch),
                modifier = Modifier.weight(1f),
            )
        }
        NovilaHeroBars(
            bars = nights(week, day),
            max = maxOf(goals.sleepHours + 2f, week.maxOfOrNull { it.sleepHours } ?: 0f),
            goal = goals.sleepHours,
        )
        if (logged) {
            NovilaButton(
                text = stringResource(R.string.sleep_change_night),
                onClick = onLog,
                role = NovilaTheme.pillars.sleep,
                modifier = Modifier.novilaActionWidth(),
            )
        } else {
            NovilaAccentButton(
                text = stringResource(R.string.sleep_log_night),
                onClick = onLog,
                modifier = Modifier.novilaActionWidth(),
            )
        }
    }
}

/** When the user slept, or that the night is logged (or not yet). */
@Composable
private fun nightHeadline(day: DayEntry): String {
    val bedtime = day.bedtime
    val wakeTime = day.wakeTime
    return when {
        day.sleepHours <= 0f -> stringResource(R.string.sleep_hero_empty)

        bedtime != null && wakeTime != null -> stringResource(
            R.string.sleep_night_times,
            clockText(bedtime),
            clockText(wakeTime),
        )

        else -> stringResource(R.string.sleep_hero_logged)
    }
}

/** How the night felt and where it came from, or the goal while there is nothing to say about it. */
@Composable
private fun nightBody(day: DayEntry, goals: Goals, fromWatch: Boolean): String {
    val details = listOfNotNull(
        day.sleepQuality.takeIf { it in 1..QualityLevels.size }?.let { stringResource(QualityLevels[it - 1]) },
        stringResource(R.string.sleep_hero_from_watch).takeIf { fromWatch },
    )
    return details.joinToString(" · ").ifEmpty { stringResource(R.string.sleep_goal, hoursText(goals.sleepHours)) }
}

/** A bar per night of the [week], the [shown] one in the sun's color. */
@Composable
private fun nights(week: List<DayEntry>, shown: DayEntry): List<NovilaHeroBar> {
    val locale = LocalConfiguration.current.locales[0]
    val labels = weekdayLabels(week)
    return week.mapIndexed { i, night ->
        val name = night.date.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        NovilaHeroBar(
            label = labels[i],
            value = night.sleepHours.takeIf { it > 0f },
            current = night.date == shown.date,
            description = if (night.sleepHours > 0f) {
                stringResource(R.string.sleep_hero_night, name, hoursText(night.sleepHours))
            } else {
                stringResource(R.string.sleep_hero_night_missing, name)
            },
        )
    }
}
