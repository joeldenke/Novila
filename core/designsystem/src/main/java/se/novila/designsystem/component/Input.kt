package se.novila.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme

/**
 * A row for logging a count: [title] and [value] (e.g. "3 of 5 portions") with round minus and
 * plus buttons. The plus is filled in the [role] color, since adding is what the row is for. Either button can be
 * turned off at the end of a range.
 */
@Composable
fun NovilaStepper(
    title: String,
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementDescription: String,
    incrementDescription: String,
    role: NovilaColorRole,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
            Text(title, style = NovilaTheme.typography.titleSmall)
            Text(value, style = NovilaTheme.typography.bodySmall, color = NovilaTheme.colors.onSurfaceVariant)
        }
        RoundButton(
            plus = false,
            onClick = onDecrement,
            enabled = decrementEnabled,
            description = decrementDescription,
            container = role.container,
            content = role.onContainer,
        )
        RoundButton(
            plus = true,
            onClick = onIncrement,
            enabled = incrementEnabled,
            description = incrementDescription,
            container = role.color,
            content = role.onColor,
        )
    }
}

@Composable
private fun RoundButton(
    plus: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    description: String,
    container: Color,
    content: Color,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) container else container.copy(alpha = 0.4f),
        modifier = Modifier.size(40.dp).semantics { contentDescription = description },
    ) {
        // Drawn rather than typed, so the symbol is optically centered and crisp at any density.
        Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
            val arm = 6.dp.toPx()
            val stroke = 2.dp.toPx()
            val color = if (enabled) content else content.copy(alpha = 0.5f)
            drawLine(color, center - Offset(arm, 0f), center + Offset(arm, 0f), stroke, StrokeCap.Round)
            if (plus) drawLine(color, center - Offset(0f, arm), center + Offset(0f, arm), stroke, StrokeCap.Round)
        }
    }
}

/**
 * Picks one of a few ordered [levels] (e.g. how you feel, bad to great) as segments that fill
 * up to the choice and deepen in color towards the top. [selected] is 1-based, 0 for none; the
 * chosen level's name is shown below, or [hint] before anything is chosen.
 */
@Composable
fun NovilaLevelPicker(
    levels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    hint: String,
    role: NovilaColorRole,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp).selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            levels.forEachIndexed { index, label ->
                val level = index + 1
                val depth = 0.35f + 0.65f * level / levels.size
                val color by animateColorAsState(
                    targetValue = if (level <= selected) role.color.copy(alpha = depth) else role.container,
                    animationSpec = tween(durationMillis = 300 + index * 60, easing = NovilaMotion.Settle),
                    label = "level",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(NovilaTheme.shapes.small)
                        .background(color)
                        .selectable(
                            selected = level == selected,
                            role = Role.RadioButton,
                            onClick = { onSelect(level) },
                        )
                        .semantics { contentDescription = label },
                )
            }
        }
        Text(
            levels.getOrNull(selected - 1) ?: hint,
            style = NovilaTheme.typography.titleSmall,
            color = if (selected > 0) NovilaTheme.colors.onSurface else NovilaTheme.colors.onSurfaceVariant,
        )
    }
}
