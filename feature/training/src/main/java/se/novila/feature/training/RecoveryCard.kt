package se.novila.feature.training

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaAccentButton
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaTheme
import se.novila.recovery.RecoveryDay
import se.novila.recovery.RecoveryState
import se.novila.recovery.RecoveryStatus
import se.novila.ui.R as UiR

/**
 * The watch's recovery: resting heart rate and heart rate variability, and whether today's session is lighter for it.
 * Read from Health Connect, or what the user can do to connect. Shows nothing on devices without Health Connect.
 */
@Composable
fun RecoveryCard(
    recovery: RecoveryState,
    status: RecoveryStatus,
    onConnect: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (recovery == RecoveryState.Unavailable) return
    val training = NovilaTheme.pillars.training
    NovilaCard(
        title = stringResource(R.string.recovery_title),
        icon = painterResource(R.drawable.ic_recovery),
        role = training,
        modifier = modifier,
    ) {
        when (recovery) {
            is RecoveryState.Connected -> {
                val latest = recovery.days.lastOrNull { !it.isEmpty }
                if (latest == null) {
                    RecoveryBody(stringResource(R.string.recovery_no_data))
                } else {
                    RecoveryValues(latest)
                    RecoveryBody(stringResource(statusLabel(status)))
                    RecoveryBody(stringResource(R.string.movement_source))
                }
            }

            is RecoveryState.NeedsPermission -> {
                RecoveryBody(stringResource(R.string.recovery_connect_body))
                if (recovery.denied) {
                    RecoveryBody(stringResource(R.string.recovery_denied_body))
                    NovilaAccentButton(
                        text = stringResource(UiR.string.movement_open_settings),
                        onClick = onOpenSettings,
                    )
                } else {
                    NovilaAccentButton(text = stringResource(R.string.recovery_connect), onClick = onConnect)
                }
            }

            RecoveryState.Unavailable -> Unit
        }
    }
}

/** A sentence for [status], the same one the coach uses to explain why today is lighter. */
private fun statusLabel(status: RecoveryStatus): Int = when (status) {
    RecoveryStatus.Normal -> R.string.recovery_normal
    RecoveryStatus.Low -> R.string.recovery_low
    RecoveryStatus.Unknown -> R.string.recovery_unknown
}

@Composable
private fun RecoveryValues(day: RecoveryDay) {
    Row(horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xl)) {
        day.restingHeartRate?.let {
            RecoveryValue(
                stringResource(R.string.recovery_bpm, it),
                stringResource(R.string.recovery_resting_heart_rate),
            )
        }
        day.hrvMillis?.let {
            RecoveryValue(stringResource(R.string.recovery_millis, it), stringResource(R.string.recovery_hrv))
        }
    }
}

@Composable
private fun RecoveryValue(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
        NovilaText(value, style = NovilaTheme.typography.headlineSmall)
        NovilaText(label, style = NovilaTheme.typography.labelMedium, color = NovilaTheme.colors.onSurfaceVariant)
    }
}

@Composable
private fun RecoveryBody(text: String) {
    NovilaText(text, style = NovilaTheme.typography.bodyMedium, color = NovilaTheme.colors.onSurfaceVariant)
}
