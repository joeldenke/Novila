package se.novila.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme

/** Secondary action, tonal in the [role] color of the part of the app it belongs to. */
@Composable
fun NovilaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    role: NovilaColorRole = NovilaTheme.pillars.training,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        // Material buttons default to a full pill; Novila buttons follow the theme's 14 dp radius.
        shape = NovilaTheme.shapes.medium,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = role.container,
            contentColor = role.onContainer,
        ),
        modifier = modifier.semantics { if (contentDescription != null) this.contentDescription = contentDescription },
    ) {
        Text(text, style = NovilaTheme.typography.labelLarge)
    }
}

/** The main call to action, filled with the midsummer sun. Use at most one per screen. */
@Composable
fun NovilaAccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = NovilaTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = NovilaTheme.accent.sun,
            contentColor = NovilaTheme.accent.onSun,
        ),
        modifier = modifier.semantics { if (contentDescription != null) this.contentDescription = contentDescription },
    ) {
        Text(text, style = NovilaTheme.typography.labelLarge)
    }
}

/** Selectable chip for one choice of a few, or one of several options that can be turned on and off. */
@Composable
fun NovilaChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    role: NovilaColorRole = NovilaTheme.pillars.training,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = NovilaTheme.typography.labelLarge) },
        shape = NovilaTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = role.container,
            selectedLabelColor = role.onContainer,
        ),
        modifier = modifier.semantics { if (contentDescription != null) this.contentDescription = contentDescription },
    )
}

@Composable
fun NovilaSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    modifier: Modifier = Modifier,
    role: NovilaColorRole = NovilaTheme.pillars.training,
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        colors = SliderDefaults.colors(
            thumbColor = role.color,
            activeTrackColor = role.color,
            activeTickColor = role.onColor,
            inactiveTrackColor = role.container,
            inactiveTickColor = role.color,
        ),
        modifier = modifier,
    )
}

/** Determinate progress, [progress] in 0f..1f. */
@Composable
fun NovilaProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    role: NovilaColorRole = NovilaTheme.pillars.training,
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth(),
        color = role.color,
        trackColor = role.container,
    )
}

/**
 * A field to type in, with its [label] above the text and [suffix] (a unit like "cm") after it. [error] replaces
 * [supportingText] below the field and marks it as wrong, so screen readers say what to fix. [numeric] opens the
 * number keyboard; other fields start words with a capital, as names do.
 */
@Composable
fun NovilaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    supportingText: String? = null,
    error: String? = null,
    numeric: Boolean = false,
    last: Boolean = false,
    role: NovilaColorRole = NovilaTheme.pillars.training,
) {
    val below = error ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = suffix?.let { { Text(it) } },
        supportingText = below?.let { { Text(it) } },
        isError = error != null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = if (numeric) KeyboardCapitalization.None else KeyboardCapitalization.Words,
            keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text,
            imeAction = if (last) ImeAction.Done else ImeAction.Next,
        ),
        textStyle = NovilaTheme.typography.bodyLarge,
        shape = NovilaTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = role.color,
            focusedLabelColor = role.color,
            cursorColor = role.color,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
