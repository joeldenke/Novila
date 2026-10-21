package se.novila.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaMetric
import se.novila.designsystem.component.NovilaStatus
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.exercise.ExerciseState
import se.novila.movement.MovementState
import se.novila.recovery.RecoveryState
import se.novila.recovery.RecoveryStatus
import se.novila.ui.common.kcalText
import se.novila.ui.common.stepsText
import se.novila.ui.R as UiR

/** What the card can ask for: access in Health Connect, its settings, or installing it. */
data class HealthDataActions(
    val onConnect: () -> Unit = {},
    val onConnectRecovery: () -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    val onInstall: () -> Unit = {},
)

/**
 * Today's movement and the watch's recovery from Health Connect in one card: steps, active calories, resting heart
 * rate and HRV, with a pill that says whether Health Connect is connected. Not connected, the four values stay as
 * dashes, so the card shows what it will show, and one quiet button asks for access (or to install Health Connect).
 * Connected, a part that is still missing ([recovery] or the program's [exercise]) asks for itself. Shows nothing on
 * devices without Health Connect.
 */
@Composable
fun HealthDataCard(
    movement: MovementState,
    recovery: RecoveryState,
    exercise: ExerciseState,
    status: RecoveryStatus,
    actions: HealthDataActions,
    modifier: Modifier = Modifier,
) {
    if (movement == MovementState.Unavailable && recovery == RecoveryState.Unavailable) return
    val training = NovilaTheme.pillars.training
    val connected = movement is MovementState.Connected
    NovilaCard(
        title = stringResource(R.string.health_data_title),
        icon = painterResource(R.drawable.ic_recovery),
        role = training,
        modifier = modifier,
    ) {
        NovilaStatus(
            text = stringResource(
                when (movement) {
                    is MovementState.Connected -> R.string.health_data_connected
                    MovementState.NeedsInstall -> R.string.health_data_missing
                    else -> R.string.health_data_not_connected
                },
            ),
            active = connected,
            role = training,
        )
        val today = (movement as? MovementState.Connected)?.today
        val latest = (recovery as? RecoveryState.Connected)?.days?.lastOrNull { !it.isEmpty }
        MetricRow(
            first = today?.let { stepsText(it.steps) } to stringResource(UiR.string.movement_steps),
            second = today?.let { kcalText(it.activeKilocalories) } to
                stringResource(R.string.movement_active_calories),
        )
        MetricRow(
            first = latest?.restingHeartRate?.let { stringResource(R.string.recovery_bpm, it) } to
                stringResource(R.string.recovery_resting_heart_rate),
            second = latest?.hrvMillis?.let { stringResource(R.string.recovery_millis, it) } to
                stringResource(R.string.health_data_hrv),
        )
        when (movement) {
            is MovementState.Connected -> {
                Recovery(recovery, status, latest != null, actions)
                Workouts(exercise, actions)
                Footnote(stringResource(R.string.movement_source))
            }

            is MovementState.NeedsPermission -> {
                Body(
                    stringResource(
                        if (movement.denied) R.string.movement_denied_body else R.string.movement_connect_body,
                    ),
                )
                NovilaButton(
                    text = stringResource(
                        if (movement.denied) UiR.string.movement_open_settings else R.string.movement_connect,
                    ),
                    onClick = if (movement.denied) actions.onOpenSettings else actions.onConnect,
                    role = training,
                )
            }

            MovementState.NeedsInstall -> {
                Body(stringResource(R.string.movement_install_body))
                NovilaButton(
                    text = stringResource(R.string.movement_install),
                    onClick = actions.onInstall,
                    role = training,
                )
            }

            MovementState.Unavailable -> Recovery(recovery, status, latest != null, actions)
        }
    }
}

/** Two values side by side, each a value (or a dash while it is unknown) and its label. */
@Composable
private fun MetricRow(first: Pair<String?, String>, second: Pair<String?, String>) {
    val missing = stringResource(UiR.string.value_missing)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m)) {
        NovilaMetric(first.first, first.second, missing, Modifier.weight(1f))
        NovilaMetric(second.first, second.second, missing, Modifier.weight(1f))
    }
}

/** What the recovery says about today, or how to let Novila read it. */
@Composable
private fun Recovery(recovery: RecoveryState, status: RecoveryStatus, hasData: Boolean, actions: HealthDataActions) {
    when (recovery) {
        is RecoveryState.Connected -> Body(
            stringResource(if (hasData) statusLabel(status) else R.string.recovery_no_data),
        )

        is RecoveryState.NeedsPermission -> {
            Body(stringResource(if (recovery.denied) R.string.recovery_denied_body else R.string.recovery_connect_body))
            NovilaButton(
                text = stringResource(
                    if (recovery.denied) UiR.string.movement_open_settings else R.string.recovery_connect,
                ),
                onClick = if (recovery.denied) actions.onOpenSettings else actions.onConnectRecovery,
                role = NovilaTheme.pillars.training,
            )
        }

        RecoveryState.Unavailable -> Unit
    }
}

/** A sentence for [status], the same one the coach uses to explain why today is lighter. */
private fun statusLabel(status: RecoveryStatus): Int = when (status) {
    RecoveryStatus.Normal -> R.string.recovery_normal
    RecoveryStatus.Low -> R.string.recovery_low
    RecoveryStatus.Unknown -> R.string.recovery_unknown
}

/** Access to workouts was added after steps and calories, so someone who connected before is asked for it here. */
@Composable
private fun Workouts(exercise: ExerciseState, actions: HealthDataActions) {
    when (exercise) {
        is ExerciseState.Connected -> Footnote(
            stringResource(R.string.movement_workouts_connected, exercise.exercises.size),
        )

        is ExerciseState.NeedsPermission -> {
            Body(stringResource(R.string.movement_workouts_body))
            NovilaButton(
                text = stringResource(
                    if (exercise.denied) UiR.string.movement_open_settings else R.string.movement_workouts_connect,
                ),
                onClick = if (exercise.denied) actions.onOpenSettings else actions.onConnect,
                role = NovilaTheme.pillars.training,
            )
        }

        ExerciseState.Unavailable -> Unit
    }
}

@Composable
private fun Body(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}

@Composable
private fun Footnote(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodySmall, color = NovilaTheme.colors.onSurfaceVariant)
}
