package se.novila.feature.training

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.ExerciseType
import se.novila.exercise.PlannedSession
import se.novila.exercise.TrainingProgram
import se.novila.feature.training.R
import java.time.DayOfWeek
import se.novila.ui.R as UiR

/**
 * This week's program: one row per workout, with today's in the training color and the days already trained marked
 * done ([done]). With [onOpenSession], a strength day's row opens its session.
 */
@Composable
fun TrainingProgramCard(
    program: TrainingProgram,
    today: DayOfWeek,
    done: Map<DayOfWeek, Int> = emptyMap(),
    onOpenSession: ((DayOfWeek) -> Unit)? = null,
) {
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
        if (program.lighterWeek) {
            NovilaText(
                stringResource(R.string.program_lighter_week),
                style = NovilaTheme.typography.bodyMedium,
                color = training.color,
            )
        }
        program.sessions.forEach { session ->
            val open = onOpenSession?.takeIf { session.exercises.isNotEmpty() }
            SessionRow(session, isToday = session.day == today, done = (done[session.day] ?: 0) > 0, onOpen = open)
        }
        NovilaText(
            stringResource(R.string.program_basis),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionRow(session: PlannedSession, isToday: Boolean, done: Boolean, onOpen: ((DayOfWeek) -> Unit)?) {
    val locale = LocalConfiguration.current.locales[0]
    val color = if (isToday) NovilaTheme.pillars.training.color else NovilaTheme.colors.onSurface
    val style = if (isToday) NovilaTheme.typography.titleSmall else NovilaTheme.typography.bodyMedium
    val name = stringResource(session.type.label)
    // A strength day's row opens its session; the whole row is the touch target.
    val clickable = if (onOpen != null) {
        Modifier.clickable(onClickLabel = stringResource(R.string.session_open), role = Role.Button) {
            onOpen(session.day)
        }
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(NovilaTheme.shapes.small)
            .then(clickable)
            .heightIn(min = 48.dp)
            .padding(vertical = NovilaTheme.spacing.xxs)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
    ) {
        NovilaText(dayName(session.day, locale), style = style, color = color, modifier = Modifier.weight(1f))
        NovilaText(name, style = style, color = color, modifier = Modifier.weight(3f))
        val minutes = stringResource(R.string.minutes_value, session.minutes)
        NovilaText(
            if (done) stringResource(R.string.program_done) else minutes,
            style = style,
            color = if (done) NovilaTheme.pillars.training.color else color,
        )
    }
}

internal val ExerciseType.label: Int
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
