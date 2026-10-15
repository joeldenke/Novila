package se.novila.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import se.novila.designsystem.R
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme

/** One part's progress in [NovilaHeroCard], [progress] in 0f..1f, drawn in [color] (from [NovilaTheme.hero]). */
@Immutable
data class NovilaHeroSegment(val progress: Float, val color: Color)

/**
 * The pine hero card with the midsummer sun: a headline number with its [unit], one progress
 * segment per part and a short [body]. Read by screen readers as one block.
 */
@Composable
fun NovilaHeroCard(
    title: String,
    value: String,
    unit: String,
    body: String,
    segments: List<NovilaHeroSegment>,
    modifier: Modifier = Modifier,
) {
    val hero = NovilaTheme.hero
    val valueStyle = NovilaTheme.typography.displaySmall
    val unitStyle = NovilaTheme.typography.titleMedium
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NovilaTheme.shapes.large)
            .background(Brush.linearGradient(listOf(hero.start, hero.end)))
            .padding(NovilaTheme.spacing.xl)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = NovilaTheme.typography.labelLarge,
                color = hero.content,
                modifier = Modifier.weight(1f),
            )
            NovilaIcon(
                painter = painterResource(R.drawable.ic_novila_sun),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = hero.sun,
            )
        }
        Text(
            buildAnnotatedString {
                append(value)
                withStyle(SpanStyle(fontSize = unitStyle.fontSize, fontWeight = unitStyle.fontWeight)) {
                    append(" ")
                    append(unit)
                }
            },
            style = valueStyle,
            color = hero.content,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
            segments.forEach { segment ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(SegmentShape)
                        .background(hero.track),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(segment.progress.coerceIn(0f, 1f))
                            .clip(SegmentShape)
                            .background(segment.color),
                    )
                }
            }
        }
        Text(body, style = NovilaTheme.typography.bodyMedium, color = hero.content)
    }
}

private val SegmentShape = RoundedCornerShape(3.dp)

/** A tappable tile for one part of the app: its icon, a value and a label in the part's [role] colors. */
@Composable
fun NovilaStatTile(
    icon: Painter,
    value: String,
    label: String,
    role: NovilaColorRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = NovilaTheme.shapes.medium,
        color = role.container,
        contentColor = role.onContainer,
    ) {
        Column(
            modifier = Modifier.padding(NovilaTheme.spacing.m),
            verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs),
        ) {
            NovilaIcon(painter = icon, contentDescription = null, modifier = Modifier.size(22.dp), tint = role.color)
            Text(value, style = NovilaTheme.typography.titleLarge, maxLines = 1)
            Text(label, style = NovilaTheme.typography.labelMedium, maxLines = 1)
        }
    }
}
