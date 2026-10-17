package se.novila.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.ExerciseState
import se.novila.feature.training.R
import se.novila.movement.MovementState
import se.novila.movement.TodayMovement
import se.novila.ui.common.kcalText
import se.novila.ui.common.stepsText
import se.novila.ui.R as UiR

/**
 * Today's steps and active calories from Health Connect, or what the user can do to see them, and whether the
 * program counts the workouts Health Connect recorded ([exercise]). Shows nothing on devices without Health Connect.
 */
@Composable
fun MovementCard(
    movement: MovementState,
    onConnect: () -> Unit,
    onOpenSettings: () -> Unit,
    onInstall: () -> Unit,
    exercise: ExerciseState = ExerciseState.Unavailable,
) {
    if (movement == MovementState.Unavailable) return
    val training = NovilaTheme.pillars.training
    NovilaCard(
        title = stringResource(R.string.movement_title),
        icon = painterResource(UiR.drawable.ic_training),
        role = training,
    ) {
        when (movement) {
            is MovementState.Connected -> {
                MovementValues(movement.today)
                Workouts(exercise, onConnect, onOpenSettings)
            }

            is MovementState.NeedsPermission -> {
                MovementBody(stringResource(R.string.movement_connect_body))
                if (movement.denied) {
                    MovementBody(stringResource(R.string.movement_denied_body))
                    NovilaAccentButton(
                        text = stringResource(UiR.string.movement_open_settings),
                        onClick = onOpenSettings,
                    )
                } else {
                    NovilaAccentButton(text = stringResource(R.string.movement_connect), onClick = onConnect)
                }
            }

            MovementState.NeedsInstall -> {
                MovementBody(stringResource(R.string.movement_install_body))
                NovilaAccentButton(text = stringResource(R.string.movement_install), onClick = onInstall)
            }

            MovementState.Unavailable -> Unit
        }
    }
}

@Composable
private fun MovementValues(today: TodayMovement) {
    Row(horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xl)) {
        MovementValue(stepsText(today.steps), stringResource(UiR.string.movement_steps))
        MovementValue(kcalText(today.activeKilocalories), stringResource(R.string.movement_active_calories))
    }
    NovilaText(
        stringResource(R.string.movement_source),
        style = NovilaTheme.typography.bodySmall,
        color = NovilaTheme.colors.onSurfaceVariant,
    )
}

/** Access to workouts was added after steps and calories, so someone who connected before is asked for it here. */
@Composable
private fun Workouts(exercise: ExerciseState, onConnect: () -> Unit, onOpenSettings: () -> Unit) {
    when (exercise) {
        is ExerciseState.Connected -> NovilaText(
            stringResource(R.string.movement_workouts_connected, exercise.exercises.size),
            style = NovilaTheme.typography.bodySmall,
            color = NovilaTheme.colors.onSurfaceVariant,
        )

        is ExerciseState.NeedsPermission -> {
            MovementBody(stringResource(R.string.movement_workouts_body))
            NovilaButton(
                text = stringResource(
                    if (exercise.denied) UiR.string.movement_open_settings else R.string.movement_workouts_connect,
                ),
                onClick = if (exercise.denied) onOpenSettings else onConnect,
                role = NovilaTheme.pillars.training,
            )
        }

        ExerciseState.Unavailable -> Unit
    }
}

@Composable
private fun MovementValue(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
        NovilaText(value, style = NovilaTheme.typography.headlineSmall)
        NovilaText(label, style = NovilaTheme.typography.labelMedium, color = NovilaTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun MovementBody(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}
