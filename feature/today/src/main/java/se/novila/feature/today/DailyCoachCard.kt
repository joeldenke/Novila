package se.novila.feature.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.coach.CoachAdvice
import se.novila.coach.CoachFocus
import se.novila.coach.DailyCoach
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.today.R
import se.novila.ui.common.clockText
import se.novila.ui.common.durationText
import se.novila.ui.common.hoursText
import se.novila.ui.common.nextPortionText
import se.novila.ui.R as UiR

/**
 * The one coach on Today: a single suggestion that reads across the parts ([coach]), in the order the coach says it,
 * e.g. "You slept 5.5 h last night. Keep today's 30 min session light, or make it a walk. Aim to be in bed by 22:30."
 * The day's actions stay in the checklist below, so the coach speaks with one voice without repeating them.
 */
@Composable
fun DailyCoachCard(coach: DailyCoach) {
    val role = coach.role()
    // The pieces are read in order, as one connected suggestion.
    val suggestion = coach.advice.map { adviceText(it) }.joinToString(" ")
    NovilaCard(title = stringResource(R.string.coach_title), icon = painterResource(coach.icon), role = role) {
        NovilaText(suggestion, style = NovilaTheme.typography.bodyMedium)
    }
}

/** The part the day is about, so the card carries that part's colour and icon. */
@Composable
private fun DailyCoach.role(): NovilaColorRole = when (focus) {
    CoachFocus.Recover -> NovilaTheme.pillars.sleep
    CoachFocus.Train, CoachFocus.Balance -> NovilaTheme.pillars.training
    CoachFocus.Nutrition -> NovilaTheme.pillars.nutrition
}

/** The part the day is about, so the card carries that part's colour and icon. */
private val DailyCoach.icon: Int
    get() = when (focus) {
        CoachFocus.Recover -> UiR.drawable.ic_sleep
        CoachFocus.Train, CoachFocus.Balance -> UiR.drawable.ic_training
        CoachFocus.Nutrition -> UiR.drawable.ic_nutrition
    }

/** One piece of the coach's suggestion as a sentence in the user's language. */
@Composable
private fun adviceText(advice: CoachAdvice): String = when (advice) {
    is CoachAdvice.ShortNight -> stringResource(R.string.coach_short_night, hoursText(advice.hours))
    CoachAdvice.LowRecovery -> stringResource(R.string.coach_low_recovery)
    is CoachAdvice.LighterSession -> stringResource(R.string.coach_lighter_session, durationText(advice.minutes))
    is CoachAdvice.PlannedSession -> stringResource(R.string.coach_planned_session, durationText(advice.minutes))
    CoachAdvice.RestDay -> stringResource(R.string.coach_rest_day)
    is CoachAdvice.EarlierBedtime -> stringResource(R.string.coach_earlier_bedtime, clockText(advice.time))
    is CoachAdvice.AddPortion -> nextPortionText(advice.next)
    CoachAdvice.WellDone -> stringResource(R.string.coach_well_done)
}
