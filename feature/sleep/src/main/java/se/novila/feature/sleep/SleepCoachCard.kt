package se.novila.feature.sleep

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.sleep.SleepCoachWeek
import se.novila.sleep.WindDownMoment
import se.novila.ui.common.clockText
import java.time.LocalTime
import se.novila.ui.R as UiR

/**
 * What the sleep coach on the Sleep tab shows: whether it is set up ([active]), the [bedtime] and [wakeTime] goals,
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
 * The sleep coach in short on the Sleep tab: while it isn't set up a line and a way in, after that the goal, how
 * many nights were near it this week and the next wind-down step. The full coach opens from the button.
 */
@Composable
fun SleepCoachCard(coach: SleepCoachCardState, onOpenCoach: () -> Unit) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaCard(
        title = stringResource(R.string.sleep_coach_title),
        icon = painterResource(UiR.drawable.ic_sleep),
        role = sleep,
    ) {
        if (!coach.active) {
            NovilaText(
                stringResource(R.string.sleep_coach_card_intro),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        } else {
            NovilaText(
                stringResource(
                    R.string.sleep_coach_goal_value,
                    clockText(coach.bedtime),
                    clockText(coach.wakeTime),
                ),
                style = NovilaTheme.typography.bodyMedium,
            )
            val week = coach.week
            NovilaText(
                if (week.logged == 0) {
                    stringResource(R.string.sleep_coach_week_none)
                } else {
                    stringResource(R.string.sleep_coach_week_value, week.nearGoal, week.logged)
                },
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
            when {
                !coach.hasSteps -> NovilaText(
                    stringResource(R.string.sleep_coach_card_hint),
                    style = NovilaTheme.typography.bodySmall,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )

                coach.tonight != null -> NovilaText(
                    stringResource(
                        R.string.sleep_coach_moment,
                        clockText(coach.tonight.time),
                        stringResource(coach.tonight.step.label),
                    ),
                    style = NovilaTheme.typography.bodyMedium,
                )

                else -> NovilaText(
                    stringResource(R.string.sleep_coach_evening_done),
                    style = NovilaTheme.typography.bodySmall,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )
            }
        }
        NovilaButton(
            text = stringResource(if (coach.active) R.string.sleep_coach_adjust else R.string.sleep_coach_open),
            onClick = onOpenCoach,
            role = sleep,
        )
    }
}
