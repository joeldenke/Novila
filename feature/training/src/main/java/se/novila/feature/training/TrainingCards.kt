package se.novila.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.ExerciseType
import se.novila.exercise.PlannedSession
import se.novila.exercise.TrainingProgram
import se.novila.feature.training.R
import java.time.DayOfWeek
import java.time.format.TextStyle
import se.novila.ui.R as UiR

/** This week's generated program: one row per workout, with today's in the training color. */
@Composable
fun TrainingProgramCard(program: TrainingProgram, today: DayOfWeek) {
    val training = NovilaTheme.pillars.training
    NovilaCard(
        title = stringResource(R.string.program_title),
        icon = painterResource(UiR.drawable.ic_training),
        role = training,
    ) {
        NovilaText(
            stringResource(
                R.string.program_summary,
                program.cardioMinutes,
                program.sessions.count { it.type == ExerciseType.Strength },
            ),
            style = NovilaTheme.typography.bodyMedium,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
        program.sessions.forEach { session -> SessionRow(session, isToday = session.day == today) }
        NovilaText(
            stringResource(R.string.program_basis),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionRow(session: PlannedSession, isToday: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val color = if (isToday) NovilaTheme.pillars.training.color else NovilaTheme.colors.onSurface
    val style = if (isToday) NovilaTheme.typography.titleSmall else NovilaTheme.typography.bodyMedium
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
    ) {
        NovilaText(
            session.day.getDisplayName(TextStyle.SHORT, locale).trimEnd('.').replaceFirstChar { it.titlecase(locale) },
            style = style,
            color = color,
            modifier = Modifier.weight(1f),
        )
        NovilaText(stringResource(session.type.label), style = style, color = color, modifier = Modifier.weight(3f))
        NovilaText(stringResource(R.string.minutes_value, session.minutes), style = style, color = color)
    }
}

private val ExerciseType.label: Int
    get() = when (this) {
        ExerciseType.Walking -> R.string.exercise_walking
        ExerciseType.Running -> R.string.exercise_running
        ExerciseType.Cycling -> R.string.exercise_cycling
        ExerciseType.Swimming -> R.string.exercise_swimming
        ExerciseType.Strength -> R.string.exercise_strength
        ExerciseType.Rehab -> R.string.exercise_rehab
        ExerciseType.Mobility -> R.string.exercise_mobility
        ExerciseType.Other -> R.string.exercise_other
    }
