package se.novila.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.novila.designsystem.R
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme

/**
 * The pine hero panel, like the dashboard's hero card: the forest at dusk and the midsummer sun's glow behind a
 * [title] with the sun emblem, and whatever overview [content] goes below. A [NovilaAccentButton] on it is the
 * screen's main action.
 */
@Composable
fun NovilaHeroPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hero = NovilaTheme.hero
    val reveal by rememberReveal(durationMillis = 1600)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NovilaTheme.shapes.large)
            .background(Brush.linearGradient(listOf(hero.start, hero.end)))
            .drawBehind {
                drawSunGlow(hero.sun)
                drawForest(hero.content.copy(alpha = 0.07f))
            }
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
            NovilaIcon(
                painter = painterResource(R.drawable.ic_novila_sun),
                contentDescription = null,
                modifier = Modifier.size(26.dp).graphicsLayer { rotationZ = (1f - reveal) * -90f },
                tint = hero.sun,
            )
        }
        content()
    }
}

/** Text on the hero panel: a [headline] and, below it, a quieter [body]. */
@Composable
fun NovilaHeroText(headline: String, modifier: Modifier = Modifier, body: String? = null) {
    val hero = NovilaTheme.hero
    Column(modifier, verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
        Text(headline, style = NovilaTheme.typography.titleLarge, color = hero.content)
        if (body != null) {
            Text(body, style = NovilaTheme.typography.bodyMedium, color = hero.content.copy(alpha = 0.85f))
        }
    }
}

/** One ring on the hero panel that sweeps in to [progress], around a [value] and its [unit]. */
@Composable
fun NovilaHeroRing(
    progress: Float,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    size: Dp = 104.dp,
) {
    val hero = NovilaTheme.hero
    val reveal by rememberReveal(durationMillis = 1600)
    Box(modifier.size(size).novilaProgressSemantics(progress), contentAlignment = Alignment.Center) {
        Rings(listOf(NovilaHeroSegment(unit, progress, hero.training)), hero.track, reveal, Modifier.size(size))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = NovilaTheme.typography.headlineSmall, color = hero.content)
            Text(unit, style = NovilaTheme.typography.labelSmall, color = hero.content.copy(alpha = 0.8f))
        }
    }
}

/** How a day of the week stands: something [Done], something [Planned], or a [Rest] day. */
enum class NovilaDayMark { Done, Planned, Rest }

/**
 * One day in a [NovilaWeekStrip]: a short [label] ("Mon"), its [mark] and what screen readers say about it. With
 * [onClick], the day opens what is planned for it, and [clickLabel] says what.
 */
@Immutable
data class NovilaWeekDay(
    val label: String,
    val mark: NovilaDayMark,
    val isToday: Boolean,
    val description: String,
    val onClick: (() -> Unit)? = null,
    val clickLabel: String? = null,
)

/**
 * The week at a glance on the hero panel: a check for each day with something done, a ring for what is planned and
 * a dot for rest. Today is circled in the sun's color. Days with an [NovilaWeekDay.onClick] can be tapped.
 */
@Composable
fun NovilaWeekStrip(days: List<NovilaWeekDay>, modifier: Modifier = Modifier) {
    val hero = NovilaTheme.hero
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEach { day ->
            val onClick = day.onClick
            val tap = if (onClick != null) {
                Modifier
                    .clip(NovilaTheme.shapes.small)
                    .clickable(onClickLabel = day.clickLabel, role = Role.Button, onClick = onClick)
            } else {
                Modifier
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs),
                modifier = tap
                    .heightIn(min = 48.dp)
                    .padding(horizontal = NovilaTheme.spacing.xxs)
                    // The day's sentence replaces the short label; the tap above stays.
                    .clearAndSetSemantics { contentDescription = day.description },
            ) {
                Text(
                    day.label,
                    style = NovilaTheme.typography.labelSmall,
                    color = if (day.isToday) hero.content else hero.content.copy(alpha = 0.75f),
                )
                Canvas(Modifier.size(30.dp)) {
                    val radius = size.minDimension / 2f
                    val stroke = 2.dp.toPx()
                    when (day.mark) {
                        NovilaDayMark.Done -> {
                            drawCircle(hero.training, radius * 0.78f)
                            val check = Path().apply {
                                moveTo(center.x - radius * 0.32f, center.y + radius * 0.02f)
                                lineTo(center.x - radius * 0.08f, center.y + radius * 0.26f)
                                lineTo(center.x + radius * 0.34f, center.y - radius * 0.22f)
                            }
                            val line = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            drawPath(check, hero.start, style = line)
                        }

                        NovilaDayMark.Planned -> {
                            val ring = radius * 0.78f - stroke / 2f
                            drawCircle(hero.content.copy(alpha = 0.7f), ring, style = Stroke(stroke))
                        }

                        NovilaDayMark.Rest -> drawCircle(hero.content.copy(alpha = 0.4f), radius * 0.16f)
                    }
                    if (day.isToday) drawCircle(hero.sun, radius - stroke / 2f, style = Stroke(stroke))
                }
            }
        }
    }
}

/** A small action that belongs with something else on the screen, like "Log another workout". Tonal in [role]. */
@Composable
fun NovilaActionChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: NovilaColorRole = NovilaTheme.pillars.training,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = NovilaTheme.typography.labelLarge) },
        shape = NovilaTheme.shapes.small,
        colors = AssistChipDefaults.assistChipColors(containerColor = role.container, labelColor = role.onContainer),
        border = null,
        modifier = modifier,
    )
}

/**
 * Plays or pauses something, like an exercise figure: a round button in the [role] color with a drawn play triangle
 * or pause bars, so the symbol stays crisp and centered. [description] says what a tap does now.
 */
@Composable
fun NovilaPlayButton(
    playing: Boolean,
    onClick: () -> Unit,
    description: String,
    role: NovilaColorRole,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = role.color,
        modifier = modifier.size(48.dp).semantics { contentDescription = description },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val unit = 6.dp.toPx()
            if (playing) {
                val bar = 2.5.dp.toPx()
                listOf(-1f, 1f).forEach { side ->
                    val x = center.x + side * unit * 0.55f
                    drawLine(role.onColor, Offset(x, center.y - unit), Offset(x, center.y + unit), bar, StrokeCap.Round)
                }
            } else {
                // Shifted right a little, so the triangle looks centered.
                val left = center.x - unit * 0.7f
                val triangle = Path().apply {
                    moveTo(left, center.y - unit)
                    lineTo(left + unit * 1.75f, center.y)
                    lineTo(left, center.y + unit)
                    close()
                }
                drawPath(triangle, role.onColor)
            }
        }
    }
}
