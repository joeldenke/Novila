package se.novila.designsystem.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme

/**
 * Picks a time of day by typing hours and minutes, in a dialog with a [title]. Starts at [hour]:[minute] and gives
 * the choice to [onConfirm]; [onDismiss] runs for the cancel button, back and a tap outside. [is24Hour] follows the
 * phone's setting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovilaTimeDialog(
    title: String,
    hour: Int,
    minute: Int,
    is24Hour: Boolean,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    role: NovilaColorRole = NovilaTheme.pillars.training,
) {
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = is24Hour)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(confirmLabel, style = NovilaTheme.typography.labelLarge, color = role.color)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel, style = NovilaTheme.typography.labelLarge) }
        },
        title = { Text(title, style = NovilaTheme.typography.headlineSmall) },
        text = {
            TimeInput(
                state = state,
                colors = TimePickerDefaults.colors(
                    timeSelectorSelectedContainerColor = role.container,
                    timeSelectorSelectedContentColor = role.onContainer,
                ),
            )
        },
        shape = NovilaTheme.shapes.extraLarge,
        containerColor = NovilaTheme.surfaces.card,
    )
}
