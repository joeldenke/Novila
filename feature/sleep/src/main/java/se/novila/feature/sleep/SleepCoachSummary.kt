package se.novila.feature.sleep

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import se.novila.sleep.SleepCoachWeek
import se.novila.sleep.WindDownMoment
import se.novila.ui.common.clockText
import java.time.LocalTime

/**
 * What the Sleep tab says about the sleep coach: whether it is set up ([active]), the [bedtime] and [wakeTime] goals,
 * this week's [week] near the bedtime and the evening's next step ([tonight]).
 */
data class SleepCoachCardState(
    val active: Boolean,
    val bedtime: LocalTime,
    val wakeTime: LocalTime,
    val week: SleepCoachWeek,
    val tonight: WindDownMoment?,
    val hasSteps: Boolean,
)

/**
 * The sleep coach in one line, under its way in on the Sleep tab: while it isn't set up what it is, after that the
 * evening's next step, the goal while there is no routine, or that the evening's steps are done.
 */
@Composable
fun sleepCoachSummary(coach: SleepCoachCardState): String = when {
    !coach.active -> stringResource(R.string.sleep_coach_card_intro)

    coach.tonight != null -> stringResource(
        R.string.sleep_coach_moment,
        clockText(coach.tonight.time),
        stringResource(coach.tonight.step.label),
    )

    !coach.hasSteps -> stringResource(
        R.string.sleep_coach_goal_value,
        clockText(coach.bedtime),
        clockText(coach.wakeTime),
    )

    else -> stringResource(R.string.sleep_coach_evening_done)
}
