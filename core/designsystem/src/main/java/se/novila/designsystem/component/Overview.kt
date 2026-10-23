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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaHeroColors
import se.novila.designsystem.theme.NovilaTheme

/**
 * Which part of the app a [NovilaHeroPanel] is the overview of. Each has its own ground and lines: Training is pine
 * with lines sweeping up like movement, Sleep a night lake with ripples, Nutrition falu red with the furrows of a
 * field running towards the horizon.
 */
enum class NovilaHeroPart { Training, Sleep, Nutrition }

/** The part the hero panel around is for, so its rings, days and bars take the part's color. */
private val LocalHeroPart = staticCompositionLocalOf { NovilaHeroPart.Training }

/** The panel's gradient for [part]. */
private fun NovilaHeroColors.ground(part: NovilaHeroPart): List<Color> = when (part) {
    NovilaHeroPart.Training -> listOf(start, end)
    NovilaHeroPart.Sleep -> listOf(sleepStart, sleepEnd)
    NovilaHeroPart.Nutrition -> listOf(nutritionStart, nutritionEnd)
}

/** The part's light color on its panel: progress, done days and bars. */
private fun NovilaHeroColors.accent(part: NovilaHeroPart): Color = when (part) {
    NovilaHeroPart.Training -> training
    NovilaHeroPart.Sleep -> sleep
    NovilaHeroPart.Nutrition -> nutrition
}

/**
 * The hero panel at the top of a part's tab: the [part]'s ground and lines towards the glow of the [sky] behind a
 * [title] with the [sky]'s sun or moon, and whatever overview [content] goes below. A [NovilaAccentButton] on it is
 * the screen's main action.
 */
@Composable
fun NovilaHeroPanel(
    title: String,
    modifier: Modifier = Modifier,
    sky: NovilaSky = NovilaSky.Day,
    part: NovilaHeroPart = NovilaHeroPart.Training,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hero = NovilaTheme.hero
    val reveal by rememberReveal(durationMillis = 1600)
    val lines = when (part) {
        NovilaHeroPart.Training -> Modifier.heroMotionLines(hero, sky)
        NovilaHeroPart.Sleep -> Modifier.heroRipples(hero, sky)
        NovilaHeroPart.Nutrition -> Modifier.heroFurrows(hero, sky)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(NovilaTheme.shapes.large)
            .background(Brush.linearGradient(hero.ground(part)))
            .then(lines)
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
        CompositionLocalProvider(LocalHeroPart provides part) { content() }
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
        val color = hero.accent(LocalHeroPart.current)
        Rings(listOf(NovilaHeroSegment(unit, progress, color)), hero.track, reveal, Modifier.size(size))
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
    val part = LocalHeroPart.current
    val accent = hero.accent(part)
    val ground = hero.ground(part).first()
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
                            drawCircle(accent, radius * 0.78f)
                            val check = Path().apply {
                                moveTo(center.x - radius * 0.32f, center.y + radius * 0.02f)
                                lineTo(center.x - radius * 0.08f, center.y + radius * 0.26f)
                                lineTo(center.x + radius * 0.34f, center.y - radius * 0.22f)
                            }
                            val line = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            drawPath(check, ground, style = line)
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

/**
 * One day in [NovilaHeroBars]: a short [label] ("Mon"), its [value] (null when nothing is logged) and what screen
 * readers say about it. The [current] day is the one the panel is about.
 */
@Immutable
data class NovilaHeroBar(
    val label: String,
    val value: Float?,
    val current: Boolean,
    val description: String,
)

/**
 * The week as slim bars on the hero panel, each rising to its value out of [max] in the part's color, with a dot for
 * a day without a value. The [current] day's bar is in the sun's color. With a [goal], a dashed line runs across the
 * bars at its height, so the days that reached it read at a glance.
 */
@Composable
fun NovilaHeroBars(
    bars: List<NovilaHeroBar>,
    max: Float,
    modifier: Modifier = Modifier,
    goal: Float? = null,
) {
    val hero = NovilaTheme.hero
    val accent = hero.accent(LocalHeroPart.current)
    val reveal by rememberReveal(durationMillis = 1200, delayMillis = 200)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        bars.forEach { bar ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xs),
                modifier = Modifier
                    .padding(horizontal = NovilaTheme.spacing.xxs)
                    .clearAndSetSemantics { contentDescription = bar.description },
            ) {
                Canvas(Modifier.size(width = 30.dp, height = 56.dp)) {
                    val width = 8.dp.toPx()
                    val x = center.x
                    val top = width / 2f
                    val bottom = size.height - width / 2f
                    drawLine(hero.track, Offset(x, top), Offset(x, bottom), width, StrokeCap.Round)
                    val value = bar.value
                    if (value != null && value > 0f && max > 0f) {
                        val y = bottom - (bottom - top) * (value / max).coerceIn(0f, 1f) * reveal
                        val color = if (bar.current) hero.sun else accent
                        drawLine(color, Offset(x, y), Offset(x, bottom), width, StrokeCap.Round)
                    } else {
                        drawCircle(hero.content.copy(alpha = 0.5f), 2.dp.toPx(), Offset(x, bottom))
                    }
                    if (goal != null && max > 0f) {
                        val y = bottom - (bottom - top) * (goal / max).coerceIn(0f, 1f)
                        val dash = 3.dp.toPx()
                        var from = 0f
                        while (from < size.width) {
                            val to = (from + dash).coerceAtMost(size.width)
                            drawLine(hero.content.copy(alpha = 0.6f), Offset(from, y), Offset(to, y), 1.dp.toPx())
                            from += dash * 2
                        }
                    }
                }
                Text(
                    bar.label,
                    style = NovilaTheme.typography.labelSmall,
                    color = if (bar.current) hero.content else hero.content.copy(alpha = 0.75f),
                )
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
