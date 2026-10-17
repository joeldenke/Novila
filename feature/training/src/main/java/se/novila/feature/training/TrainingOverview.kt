package se.novila.feature.training

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaDayMark
import se.novila.designsystem.component.NovilaHeroPanel
import se.novila.designsystem.component.NovilaHeroRing
import se.novila.designsystem.component.NovilaHeroText
import se.novila.designsystem.component.NovilaWeekDay
import se.novila.designsystem.component.NovilaWeekStrip
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.ExerciseType
import se.novila.exercise.PlannedSession
import se.novila.exercise.TrainingProgram
import se.novila.feature.training.R
import se.novila.model.CardioLog
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/**
 * The week at a glance at the top of Training: minutes done against the week's plan, a check for each day trained,
 * and today's session with the one action that matters now ([plan]): start the session or log the cardio.
 */
@Composable
fun TrainingOverview(
    program: TrainingProgram,
    today: DayOfWeek,
    weekDone: Map<DayOfWeek, Int>,
    plan: TodayPlan,
    onStartWorkout: (DayOfWeek) -> Unit,
    onLogCardio: (CardioLog) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val planned = program.sessions.sumOf { it.minutes }
    val done = weekDone.values.sum()
    NovilaHeroPanel(title = stringResource(R.string.overview_title), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.l),
        ) {
            NovilaHeroRing(
                progress = if (planned > 0) done.toFloat() / planned else 0f,
                value = done.toString(),
                unit = stringResource(R.string.overview_minutes_of, planned),
            )
            NovilaHeroText(
                headline = planHeadline(plan),
                body = planBody(plan, locale),
                modifier = Modifier.weight(1f),
            )
        }
        NovilaWeekStrip(weekDays(program, today, weekDone, locale))
        when (plan) {
            is TodayPlan.Workout -> NovilaAccentButton(
                text = stringResource(R.string.workout_start),
                onClick = { onStartWorkout(plan.session.day) },
                modifier = Modifier.fillMaxWidth(),
            )

            is TodayPlan.Cardio -> NovilaAccentButton(
                text = stringResource(R.string.cardio_log),
                onClick = { onLogCardio(plan.log) },
                modifier = Modifier.fillMaxWidth(),
            )

            is TodayPlan.Mobility, is TodayPlan.Done, is TodayPlan.Rest -> Unit
        }
    }
}

@Composable
private fun planHeadline(plan: TodayPlan): String = when (plan) {
    is TodayPlan.Workout -> sessionText(R.string.overview_today, plan.session)
    is TodayPlan.Cardio -> sessionText(R.string.overview_today, plan.session)
    is TodayPlan.Mobility -> sessionText(R.string.overview_today, plan.session)
    is TodayPlan.Done -> stringResource(R.string.overview_done)
    is TodayPlan.Rest -> stringResource(R.string.overview_rest)
}

@Composable
private fun planBody(plan: TodayPlan, locale: Locale): String = when (plan) {
    is TodayPlan.Workout -> stringResource(
        if (plan.session.type == ExerciseType.Rehab) R.string.overview_rehab_body else R.string.overview_workout_body,
        plan.session.exercises.size,
    )

    is TodayPlan.Cardio -> stringResource(R.string.overview_cardio_body)

    is TodayPlan.Mobility -> stringResource(R.string.overview_mobility_body)

    is TodayPlan.Done -> plan.next?.let {
        stringResource(R.string.overview_done_next, plan.minutes, nextText(it, locale))
    } ?: stringResource(R.string.overview_done_week, plan.minutes)

    is TodayPlan.Rest -> plan.next?.let { nextText(it, locale) } ?: stringResource(R.string.overview_week_done)
}

/** "Strength, full body · 30 min" with [format] around it. */
@Composable
private fun sessionText(@StringRes format: Int, session: PlannedSession): String =
    stringResource(format, stringResource(session.type.label), session.minutes)

/** "Next: Thu, strength, full body, 30 min". */
@Composable
private fun nextText(session: PlannedSession, locale: Locale): String = stringResource(
    R.string.overview_next,
    dayName(session.day, locale),
    stringResource(session.type.label),
    session.minutes,
)

@Composable
private fun weekDays(
    program: TrainingProgram,
    today: DayOfWeek,
    weekDone: Map<DayOfWeek, Int>,
    locale: Locale,
): List<NovilaWeekDay> = DayOfWeek.entries.map { day ->
    val minutes = weekDone[day] ?: 0
    val session = program.sessions.firstOrNull { it.day == day }
    val name = day.getDisplayName(TextStyle.FULL, locale)
    val (mark, description) = when {
        minutes > 0 -> NovilaDayMark.Done to stringResource(R.string.overview_day_done, name, minutes)

        session != null -> NovilaDayMark.Planned to stringResource(
            R.string.overview_day_planned,
            name,
            stringResource(session.type.label),
            session.minutes,
        )

        else -> NovilaDayMark.Rest to stringResource(R.string.overview_day_rest, name)
    }
    NovilaWeekDay(
        label = dayName(day, locale),
        mark = mark,
        isToday = day == today,
        description = if (day == today) stringResource(R.string.overview_day_today, description) else description,
    )
}

/** A short weekday name with a capital letter and no dot: "Mon", "Mån". */
fun dayName(day: DayOfWeek, locale: Locale): String =
    day.getDisplayName(TextStyle.SHORT, locale).trimEnd('.').replaceFirstChar { it.titlecase(locale) }
