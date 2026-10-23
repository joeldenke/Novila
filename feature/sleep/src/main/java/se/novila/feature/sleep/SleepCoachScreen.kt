package se.novila.feature.sleep

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaInsight
import se.novila.designsystem.component.NovilaListRow
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.component.NovilaTimeDialog
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.bodyClock
import se.novila.sleep.SleepCoachPreferences
import se.novila.sleep.SleepCoachTip
import se.novila.sleep.WindDownMoment
import se.novila.sleep.WindDownStep
import se.novila.sleep.sleepCoachPlan
import se.novila.ui.common.ScreenList
import se.novila.ui.common.clockText
import se.novila.ui.preview.PreviewData
import java.time.LocalTime
import se.novila.ui.R as UiR

/** The sleep coach: the bedtime and wake-up goals, the wind-down routine and what the nights suggest. */
@Composable
fun SleepCoachScreen(
    state: SleepCoachUiState,
    onChange: (SleepCoachPreferences) -> Unit,
    onStart: () -> Unit,
    onReset: () -> Unit,
    onOpenBreathing: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val sleep = NovilaTheme.pillars.sleep
    var choosing by rememberSaveable { mutableStateOf<CoachTime?>(null) }
    ScreenList(contentPadding = contentPadding, modifier = modifier) {
        item(key = "intro") {
            NovilaCard(
                title = stringResource(R.string.sleep_coach_title),
                icon = painterResource(UiR.drawable.ic_sleep),
                role = sleep,
            ) {
                NovilaText(
                    stringResource(R.string.sleep_coach_intro),
                    style = NovilaTheme.typography.bodyMedium,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )
            }
        }
        item(key = "goal") {
            NovilaCard(title = stringResource(R.string.sleep_coach_goal_title)) {
                NovilaListRow(
                    title = stringResource(R.string.sleep_coach_bedtime),
                    onClick = { choosing = CoachTime.Bedtime },
                    trailing = {
                        NovilaText(clockText(state.preferences.bedtime), style = NovilaTheme.typography.titleMedium)
                    },
                )
                NovilaListRow(
                    title = stringResource(R.string.sleep_coach_wake_time),
                    onClick = { choosing = CoachTime.WakeTime },
                    trailing = {
                        NovilaText(clockText(state.preferences.wakeTime), style = NovilaTheme.typography.titleMedium)
                    },
                )
                state.bodyClockBedtime?.let { bedtime ->
                    NovilaText(
                        stringResource(R.string.sleep_coach_body_clock, clockText(bedtime)),
                        style = NovilaTheme.typography.bodySmall,
                        color = NovilaTheme.colors.onSurfaceVariant,
                    )
                }
            }
        }
        item(key = "routine") {
            RoutineCard(state.preferences, onChange, onOpenBreathing)
        }
        item(key = "week") {
            WeekCard(state)
        }
        item(key = "tips") {
            TipsCard(state, onStart, onReset)
        }
    }
    choosing?.let { which ->
        val current = if (which == CoachTime.Bedtime) state.preferences.bedtime else state.preferences.wakeTime
        NovilaTimeDialog(
            title = stringResource(
                if (which == CoachTime.Bedtime) R.string.sleep_coach_bedtime_title else R.string.sleep_coach_wake_title,
            ),
            hour = current.hour,
            minute = current.minute,
            is24Hour = DateFormat.is24HourFormat(LocalContext.current),
            confirmLabel = stringResource(R.string.sleep_save),
            dismissLabel = stringResource(R.string.sleep_cancel),
            onConfirm = { hour, minute ->
                choosing = null
                val time = LocalTime.of(hour, minute)
                val preferences = state.preferences
                onChange(
                    if (which == CoachTime.Bedtime) preferences.withBedtime(time) else preferences.withWakeTime(time),
                )
            },
            onDismiss = { choosing = null },
            role = sleep,
        )
    }
}

private enum class CoachTime { Bedtime, WakeTime }

/** The wind-down steps the user wants, with the times they belong before the bedtime goal. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RoutineCard(
    preferences: SleepCoachPreferences,
    onChange: (SleepCoachPreferences) -> Unit,
    onOpenBreathing: () -> Unit,
) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaCard(title = stringResource(R.string.sleep_coach_routine_title), role = sleep) {
        NovilaText(
            stringResource(R.string.sleep_coach_routine_hint),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
            verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        ) {
            WindDownStep.entries.forEach { step ->
                NovilaChoiceChip(
                    label = stringResource(step.label),
                    selected = step in preferences.windDown,
                    onClick = { onChange(preferences.toggle(step)) },
                    role = sleep,
                )
            }
        }
        momentsOf(preferences).forEach { moment ->
            NovilaText(
                stringResource(R.string.sleep_coach_moment, clockText(moment.time), stringResource(moment.step.label)),
                style = NovilaTheme.typography.bodySmall,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        }
        // A wind-down step in its own right, but guided: the sessions open in their own feature.
        NovilaButton(
            text = stringResource(R.string.sleep_coach_breathing),
            onClick = onOpenBreathing,
            role = sleep,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** How many of this week's logged nights were near the bedtime goal. */
@Composable
private fun WeekCard(state: SleepCoachUiState) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaCard(
        title = stringResource(R.string.sleep_coach_week_title),
        icon = painterResource(UiR.drawable.ic_sleep),
        role = sleep,
    ) {
        val week = state.plan.week
        if (week.logged == 0) {
            NovilaText(
                stringResource(R.string.sleep_coach_week_none),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        } else {
            NovilaText(
                stringResource(R.string.sleep_coach_week_value, week.nearGoal, week.logged),
                style = NovilaTheme.typography.bodyMedium,
            )
        }
    }
}

/**
 * What the user's own nights suggest. Before the coach is set up the answers are only tried out, and [onStart]
 * makes the plan stick; after that every change applies right away and [onReset] turns the coach off.
 */
@Composable
private fun TipsCard(state: SleepCoachUiState, onStart: () -> Unit, onReset: () -> Unit) {
    val sleep = NovilaTheme.pillars.sleep
    NovilaCard(title = stringResource(R.string.sleep_coach_tips_title), role = sleep) {
        if (state.plan.tips.isEmpty()) {
            NovilaText(
                stringResource(R.string.sleep_coach_tips_empty),
                style = NovilaTheme.typography.bodyMedium,
                color = NovilaTheme.colors.onSurfaceVariant,
            )
        } else {
            state.plan.tips.forEach { tip ->
                NovilaInsight(painterResource(UiR.drawable.ic_sleep), stringResource(tip.label), sleep)
            }
        }
        NovilaText(
            stringResource(R.string.sleep_coach_tips_footer),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        if (state.active) {
            NovilaButton(
                stringResource(R.string.sleep_coach_reset),
                onClick = onReset,
                role = sleep,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            NovilaAccentButton(
                stringResource(R.string.sleep_coach_start),
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The routine with each step's clock time, earliest in the evening first. */
private fun momentsOf(preferences: SleepCoachPreferences): List<WindDownMoment> =
    sleepCoachPlan(days = emptyList(), preferences = preferences).routine

/** The wind-down step's name. */
internal val WindDownStep.label: Int
    get() = when (this) {
        WindDownStep.PrepareTomorrow -> R.string.sleep_coach_step_prepare
        WindDownStep.Shower -> R.string.sleep_coach_step_shower
        WindDownStep.ScreensOff -> R.string.sleep_coach_step_screens
        WindDownStep.Quiet -> R.string.sleep_coach_step_quiet
        WindDownStep.WorryNote -> R.string.sleep_coach_step_worry
        WindDownStep.Read -> R.string.sleep_coach_step_read
    }

/** The tip's advice. */
internal val SleepCoachTip.label: Int
    get() = when (this) {
        SleepCoachTip.RegularBedtime -> R.string.sleep_coach_tip_regular_bedtime
        SleepCoachTip.FixedWakeTime -> R.string.sleep_coach_tip_fixed_wake
        SleepCoachTip.OutOfBed -> R.string.sleep_coach_tip_out_of_bed
        SleepCoachTip.ShortNights -> R.string.sleep_coach_tip_short_nights
        SleepCoachTip.WeekendShift -> R.string.sleep_coach_tip_weekend_shift
        SleepCoachTip.LateCaffeine -> R.string.sleep_coach_tip_late_caffeine
        SleepCoachTip.Screens -> R.string.sleep_coach_tip_screens
        SleepCoachTip.Alcohol -> R.string.sleep_coach_tip_alcohol
        SleepCoachTip.Stress -> R.string.sleep_coach_tip_stress
        SleepCoachTip.LateMeal -> R.string.sleep_coach_tip_late_meal
        SleepCoachTip.LateExercise -> R.string.sleep_coach_tip_late_exercise
    }

private fun coachState(
    active: Boolean,
    preferences: SleepCoachPreferences = SleepCoachPreferences(),
    nights: List<DayEntry> = emptyList(),
) = SleepCoachUiState(
    preferences = preferences,
    active = active,
    plan = sleepCoachPlan(nights, preferences, now = LocalTime.of(20, 0)),
    bodyClockBedtime = bodyClock(nights, PreviewData.today.date)?.bedtimeFor(8f),
)

@Preview(showBackground = true, heightDp = 1400)
@Composable
private fun SleepCoachScreenNewPreview() {
    NovilaTheme {
        SleepCoachScreen(
            state = coachState(active = false),
            onChange = {},
            onStart = {},
            onReset = {},
            onOpenBreathing = {},
            contentPadding = PaddingValues(),
        )
    }
}

@Preview(showBackground = true, heightDp = 2200)
@Composable
private fun SleepCoachScreenPreview() {
    val preferences = SleepCoachPreferences(windDown = WindDownStep.entries.toSet())
    NovilaTheme {
        SleepCoachScreen(
            state = coachState(active = true, preferences = preferences, nights = PreviewData.days()),
            onChange = {},
            onStart = {},
            onReset = {},
            onOpenBreathing = {},
            contentPadding = PaddingValues(),
        )
    }
}
