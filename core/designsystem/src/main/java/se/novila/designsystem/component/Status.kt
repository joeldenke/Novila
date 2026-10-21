package se.novila.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme

/**
 * Whether a source is connected, as a small pill: a filled dot in the [role] color when it is ([active]), an empty
 * ring in an outlined pill when it isn't, so "not connected" reads as a state and not as missing numbers.
 */
@Composable
fun NovilaStatus(text: String, active: Boolean, role: NovilaColorRole, modifier: Modifier = Modifier) {
    val dot = if (active) role.color else NovilaTheme.colors.onSurfaceVariant
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = if (active) role.container else NovilaTheme.surfaces.card,
        contentColor = if (active) role.onContainer else NovilaTheme.colors.onSurfaceVariant,
        border = if (active) null else BorderStroke(1.dp, NovilaTheme.colors.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = NovilaTheme.spacing.m, vertical = NovilaTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s),
        ) {
            Canvas(Modifier.size(8.dp)) {
                if (active) {
                    drawCircle(dot)
                } else {
                    drawCircle(dot, radius = size.minDimension / 2 - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                }
            }
            NovilaText(text, style = NovilaTheme.typography.labelMedium)
        }
    }
}

/**
 * One measured value with its [label] below, e.g. "8 412" over "Steps". Without a value ([value] null) it shows a
 * quiet dash, so a card keeps its shape and shows what it will show once the source is connected.
 */
@Composable
fun NovilaMetric(value: String?, label: String, missing: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs),
    ) {
        NovilaText(
            value ?: missing,
            style = NovilaTheme.typography.headlineSmall,
            color = if (value == null) NovilaTheme.colors.onSurfaceVariant else NovilaTheme.colors.onSurface,
        )
        NovilaText(label, style = NovilaTheme.typography.labelMedium, color = NovilaTheme.colors.onSurfaceVariant)
    }
}

/**
 * The user's avatar: their [initials] on the pine of the hero card with a ring of sun, or the [placeholder] icon
 * while they have no name. Read out as [contentDescription]. [compact] is the size for a row in a list.
 */
@Composable
fun NovilaAvatar(
    initials: String,
    placeholder: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val hero = NovilaTheme.hero
    val size = if (compact) 44.dp else 72.dp
    Box(
        modifier = modifier
            .size(size)
            .border(BorderStroke(2.dp, hero.sun), CircleShape)
            .padding(if (compact) 3.dp else 4.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(hero.start, hero.end)))
            .clearAndSetSemantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        if (initials.isEmpty()) {
            NovilaIcon(placeholder, contentDescription = null, modifier = Modifier.size(size / 2), tint = hero.content)
        } else {
            NovilaText(
                initials,
                style = if (compact) NovilaTheme.typography.titleSmall else NovilaTheme.typography.headlineSmall,
                color = hero.content,
            )
        }
    }
}
