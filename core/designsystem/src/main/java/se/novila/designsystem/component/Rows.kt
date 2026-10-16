package se.novila.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme

/** Rows are at least this tall, so each one is a comfortable touch target. */
private val MinRowHeight = 48.dp

/** An icon that does something, like settings in a top bar. [contentDescription] says what for screen readers. */
@Composable
fun NovilaIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        NovilaIcon(painter = painter, contentDescription = contentDescription)
    }
}

/**
 * A row in a list of settings: a [title], an optional [summary] below it and something at the end, like a value or
 * a chevron. The whole row is the touch target when [onClick] is set.
 */
@Composable
fun NovilaListRow(
    title: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val clickable = if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier
    RowLayout(title, summary, modifier.then(clickable), trailing)
}

/** A setting that is on or off. The whole row toggles, and screen readers announce it as a switch. */
@Composable
fun NovilaSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    role: NovilaColorRole = NovilaTheme.pillars.training,
) {
    RowLayout(
        title = title,
        summary = summary,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange,
        ),
    ) {
        // The row handles the toggle, so the switch itself is only the picture of it.
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = role.onColor,
                checkedTrackColor = role.color,
            ),
        )
    }
}

@Composable
private fun RowLayout(
    title: String,
    summary: String?,
    modifier: Modifier,
    trailing: @Composable (() -> Unit)?,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinRowHeight)
            .padding(vertical = NovilaTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs),
        ) {
            NovilaText(title, style = NovilaTheme.typography.bodyLarge)
            if (summary != null) {
                NovilaText(
                    summary,
                    style = NovilaTheme.typography.bodyMedium,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

/**
 * Asks before something that cannot be undone. The confirm button is in the error color, so it never reads as the
 * safe choice; [onDismiss] runs for the cancel button, back and a tap outside.
 */
@Composable
fun NovilaConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = NovilaTheme.colors.error),
            ) {
                Text(confirmLabel, style = NovilaTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel, style = NovilaTheme.typography.labelLarge) }
        },
        title = { Text(title, style = NovilaTheme.typography.headlineSmall) },
        text = { Text(text, style = NovilaTheme.typography.bodyMedium) },
        shape = NovilaTheme.shapes.extraLarge,
        containerColor = NovilaTheme.surfaces.card,
    )
}
