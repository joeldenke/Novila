package se.novila.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import java.text.NumberFormat
import kotlin.math.roundToInt

/**
 * One part's share of the hero score: a ring with its [label], [progress] in 0f..1f and its [color]. With
 * [onClick], the part's legend row opens what it is about, and [clickLabel] says what a tap does.
 */
@Immutable
data class NovilaHeroSegment(
    val label: String,
    val progress: Float,
    val color: Color,
    val onClick: (() -> Unit)? = null,
    val clickLabel: String? = null,
)

/**
 * The pine hero card at the top of the dashboard: concentric rings, one per part, around a score
 * that counts up, a legend with each part's percentage and an optional short [body]. Each part with a
 * [NovilaHeroSegment.onClick] opens its own screen, so the rings are the summary and the way in. Behind it,
 * contour lines and the glow of the [sky]: the sun by day, the moon at night.
 */
@Composable
fun NovilaHeroCard(
    title: String,
    value: Int,
    unit: String,
    segments: List<NovilaHeroSegment>,
    modifier: Modifier = Modifier,
    body: String? = null,
    sky: NovilaSky = NovilaSky.Day,
) {
    val hero = NovilaTheme.hero
    val reveal by rememberReveal(durationMillis = 1600)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NovilaTheme.shapes.large)
            .background(Brush.linearGradient(listOf(hero.start, hero.end)))
            .heroContours(hero, sky)
            .padding(NovilaTheme.spacing.xl),
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.l),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = NovilaTheme.typography.labelLarge,
                color = hero.content,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            NovilaSkyIcon(sky, hero, reveal)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xl),
        ) {
            Box(
                Modifier.size(148.dp).novilaProgressSemantics(value / 100f),
                contentAlignment = Alignment.Center,
            ) {
                Rings(segments, hero.track, reveal, Modifier.size(148.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        (value * reveal).roundToInt().toString(),
                        style = NovilaTheme.typography.headlineLarge,
                        color = hero.content,
                    )
                    Text(unit, style = NovilaTheme.typography.labelSmall, color = hero.content.copy(alpha = 0.8f))
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
            ) {
                val percent = rememberPercentFormat()
                segments.forEach { segment ->
                    LegendRow(segment, percent.format(segment.progress.coerceIn(0f, 1f)), hero.content)
                }
            }
        }
        body?.let { Text(it, style = NovilaTheme.typography.bodyMedium, color = hero.content) }
    }
}

@Composable
private fun LegendRow(segment: NovilaHeroSegment, percent: String, content: Color) {
    val tap = if (segment.onClick != null) {
        Modifier
            .fillMaxWidth()
            .clip(NovilaTheme.shapes.small)
            .clickable(onClickLabel = segment.clickLabel, role = Role.Button, onClick = segment.onClick)
    } else {
        Modifier
    }
    Row(
        modifier = tap.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(segment.color))
        Column {
            Text(segment.label, style = NovilaTheme.typography.labelMedium, color = content.copy(alpha = 0.8f))
            Text(percent, style = NovilaTheme.typography.titleMedium, color = content)
        }
    }
}

/** Concentric progress rings, outermost first, that sweep in one after another. */
@Composable
internal fun Rings(segments: List<NovilaHeroSegment>, track: Color, reveal: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val stroke = 9.dp.toPx()
        val gap = 5.dp.toPx()
        segments.forEachIndexed { i, segment ->
            val inset = stroke / 2f + i * (stroke + gap)
            val topLeft = Offset(inset, inset)
            val arc = Size(size.width - 2 * inset, size.height - 2 * inset)
            drawArc(track, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
            val stagger = 0.15f
            val sweep = (reveal * (1f + stagger * segments.size) - stagger * i).coerceIn(0f, 1f)
            val angle = 360f * segment.progress.coerceIn(0f, 1f) * sweep
            if (angle > 0f) {
                drawArc(segment.color, -90f, angle, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun rememberPercentFormat(): NumberFormat {
    // Observable locale, so the format follows the app language.
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { NumberFormat.getPercentInstance(locale) }
}

/**
 * A tappable tile for one part of the app in the part's [role] colors: its icon, a value and a
 * label, and below them the week's [trend] as a small curve, or today's [progress] as a bar.
 */
@Composable
fun NovilaStatTile(
    icon: Painter,
    value: String,
    label: String,
    role: NovilaColorRole,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trend: List<Float?> = emptyList(),
    trendRange: ClosedFloatingPointRange<Float> = 0f..1f,
    progress: Float? = null,
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
            Text(value, style = NovilaTheme.typography.titleLarge)
            Text(label, style = NovilaTheme.typography.labelMedium)
            val chart = Modifier.fillMaxWidth().height(28.dp)
            when {
                trend.count { it != null } >= 2 -> NovilaSparkline(trend, trendRange, role.color, chart)

                progress != null -> Box(chart, contentAlignment = Alignment.Center) {
                    ProgressLine(progress, role.color, Modifier.fillMaxWidth().height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun ProgressLine(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val reveal by rememberReveal(delayMillis = 200)
    Canvas(modifier) {
        val stroke = size.height
        val y = size.height / 2f
        val start = stroke / 2f
        val end = size.width - stroke / 2f
        drawLine(color.copy(alpha = 0.2f), Offset(start, y), Offset(end, y), stroke, StrokeCap.Round)
        val fraction = progress.coerceIn(0f, 1f) * reveal
        if (fraction > 0f) {
            drawLine(color, Offset(start, y), Offset(start + (end - start) * fraction, y), stroke, StrokeCap.Round)
        }
    }
}

/** A single ring around a score that counts up, with a [caption] below the number. */
@Composable
fun NovilaScoreRing(
    score: Int,
    progress: Float,
    caption: String,
    role: NovilaColorRole,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
) {
    val reveal by rememberReveal(durationMillis = 1400)
    Box(modifier.size(size).novilaProgressSemantics(progress), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 11.dp.toPx()
            val inset = stroke / 2f
            val topLeft = Offset(inset, inset)
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(role.container, 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
            val angle = 360f * progress.coerceIn(0f, 1f) * reveal
            if (angle > 0f) {
                val soft = role.color.copy(alpha = 0.55f)
                drawArc(
                    Brush.sweepGradient(listOf(soft, role.color, soft)),
                    -90f,
                    angle,
                    false,
                    topLeft,
                    arc,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                (score * reveal).roundToInt().toString(),
                style = NovilaTheme.typography.displaySmall,
                color = NovilaTheme.colors.onSurface,
            )
            Text(
                caption,
                style = NovilaTheme.typography.labelSmall,
                color = NovilaTheme.colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** A labelled value with a thin bar below, e.g. one part of a score: "Protein  12/20". */
@Composable
fun NovilaMeter(
    label: String,
    value: String,
    progress: Float,
    role: NovilaColorRole,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().novilaProgressSemantics(progress),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = NovilaTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(value, style = NovilaTheme.typography.labelMedium, color = NovilaTheme.colors.onSurfaceVariant)
        }
        ProgressLine(progress, role.color, Modifier.fillMaxWidth().height(6.dp))
    }
}

/** One observation about the user's week, with the [role] of the part it is about. */
@Composable
fun NovilaInsight(
    icon: Painter,
    text: String,
    role: NovilaColorRole,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
    ) {
        IconBadge(icon, role, size = 36.dp, iconSize = 18.dp)
        Text(text, style = NovilaTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
