package se.novila.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import se.novila.designsystem.theme.NovilaTheme

@Composable
fun NovilaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics { if (contentDescription != null) this.contentDescription = contentDescription },
    ) {
        Text(text, style = NovilaTheme.typography.labelLarge)
    }
}

/** Selectable chip. [contentDescription] is required when [label] is not readable text (e.g. an emoji). */
@Composable
fun NovilaChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = NovilaTheme.typography.titleLarge) },
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
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
    )
}

/** Determinate progress, [progress] in 0f..1f. */
@Composable
fun NovilaProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier.fillMaxWidth(),
        color = NovilaTheme.colors.primary,
        trackColor = NovilaTheme.colors.surfaceContainerHighest,
    )
}
