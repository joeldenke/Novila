package se.novila.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import kotlin.math.absoluteValue
import kotlin.math.floor

/**
 * The pine hero card without the score: the same dusk forest and midsummer glow as [NovilaHeroCard], around any
 * [content]. Text inside takes the hero's light content color unless it sets its own.
 */
@Composable
fun NovilaHeroBanner(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val hero = NovilaTheme.hero
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
        verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
    ) {
        CompositionLocalProvider(LocalContentColor provides hero.content) { content() }
    }
}

/**
 * Where the user is among [pageCount] pages: a dot per page, the [currentPage] stretched into a pill in the [role]
 * color. With [offset] (how far the pager has moved from [currentPage], read while drawing) the pill follows the
 * finger and stretches across to the next dot while swiping. Read by screen readers as [contentDescription],
 * e.g. "Step 2 of 7".
 */
@Composable
fun NovilaPageIndicator(
    pageCount: Int,
    currentPage: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    role: NovilaColorRole = NovilaTheme.pillars.training,
    offset: () -> Float = { 0f },
) {
    val color by animateColorAsState(
        targetValue = role.color,
        animationSpec = tween(NovilaMotion.ENTER_MILLIS, easing = NovilaMotion.Settle),
        label = "page indicator color",
    )
    val width = DOT * pageCount + (PILL - DOT) + GAP * (pageCount - 1)
    Canvas(
        modifier
            .size(width, DOT)
            .clearAndSetSemantics { this.contentDescription = contentDescription },
    ) {
        val position = currentPage + offset()
        val dot = DOT.toPx()
        val stretch = (PILL - DOT).toPx()
        val gap = GAP.toPx()
        var x = 0f
        repeat(pageCount) { page ->
            val near = (1f - (page - position).absoluteValue).coerceIn(0f, 1f)
            val w = dot + stretch * near
            drawRoundRect(
                color = color.copy(alpha = INACTIVE_ALPHA + (1f - INACTIVE_ALPHA) * near),
                topLeft = Offset(x, 0f),
                size = Size(w, dot),
                cornerRadius = CornerRadius(dot / 2f),
            )
            x += w + gap
        }
    }
}

private val DOT = 8.dp
private val PILL = 24.dp
private val GAP = 6.dp
private const val INACTIVE_ALPHA = 0.25f

/**
 * A soft wash of color from the top of a pager, one color per page from [colors] (usually each part's container
 * color). It blends from one page's color to the next with [position] (the pager's page plus its offset, read while
 * drawing), so the light shifts as the user swipes between parts. Behind [content].
 */
@Composable
fun NovilaPagerBackdrop(
    colors: List<Color>,
    position: () -> Float,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier.drawBehind {
            val p = position().coerceIn(0f, (colors.size - 1).toFloat())
            val from = floor(p).toInt()
            val to = (from + 1).coerceAtMost(colors.size - 1)
            val wash = lerp(colors[from], colors[to], p - from)
            drawRect(
                Brush.verticalGradient(
                    0f to wash.copy(alpha = WASH_ALPHA),
                    1f to Color.Transparent,
                    endY = size.height * WASH_DEPTH,
                ),
            )
        },
    ) {
        content()
    }
}

/** How strong the wash is at the very top. */
private const val WASH_ALPHA = 0.55f

/** How far down the screen the wash reaches. */
private const val WASH_DEPTH = 0.6f

/** A quiet text-only action next to the main one, such as "Skip". */
@Composable
fun NovilaTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: NovilaColorRole = NovilaTheme.pillars.training,
) {
    TextButton(onClick = onClick, modifier = modifier, shape = NovilaTheme.shapes.medium) {
        Text(text, style = NovilaTheme.typography.labelLarge, color = role.color)
    }
}

/**
 * One choice of a few, as a card: an [icon] in the [role] color, a [title] and a [body]. The chosen card fills with
 * the role's container and gets a check, so it stands out by more than color. Screen readers hear a radio button.
 */
@Composable
fun NovilaChoiceCard(
    title: String,
    body: String,
    icon: Painter,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    role: NovilaColorRole = NovilaTheme.pillars.training,
) {
    val spec = tween<Color>(NovilaMotion.ENTER_MILLIS / 2, easing = NovilaMotion.Settle)
    val container by animateColorAsState(
        if (selected) role.container else NovilaTheme.surfaces.card,
        spec,
        label = "choice container",
    )
    val outline by animateColorAsState(
        if (selected) role.color else NovilaTheme.surfaces.cardOutline,
        spec,
        label = "choice outline",
    )
    Surface(
        selected = selected,
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { this.role = Role.RadioButton },
        shape = NovilaTheme.shapes.medium,
        color = container,
        contentColor = if (selected) role.onContainer else NovilaTheme.colors.onSurface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, outline),
    ) {
        Row(
            modifier = Modifier.padding(NovilaTheme.spacing.l),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
        ) {
            IconBadge(icon, role, size = 44.dp, iconSize = 22.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
                Text(title, style = NovilaTheme.typography.titleMedium)
                Text(
                    body,
                    style = NovilaTheme.typography.bodySmall,
                    color = if (selected) role.onContainer else NovilaTheme.colors.onSurfaceVariant,
                )
            }
            Check(selected, role)
        }
    }
}

/** A round check: an empty ring, or filled in the role color with a tick when chosen. */
@Composable
internal fun Check(selected: Boolean, role: NovilaColorRole) {
    val outline = NovilaTheme.colors.outline
    Canvas(Modifier.size(22.dp)) {
        val stroke = 2.dp.toPx()
        val radius = size.minDimension / 2f
        if (selected) {
            drawCircle(role.color, radius)
            val tick = Path().apply {
                moveTo(size.width * 0.28f, size.height * 0.52f)
                lineTo(size.width * 0.44f, size.height * 0.67f)
                lineTo(size.width * 0.73f, size.height * 0.36f)
            }
            drawPath(tick, role.onColor, style = Stroke(stroke, cap = StrokeCap.Round))
        } else {
            drawCircle(outline, radius - stroke / 2f, style = Stroke(stroke))
        }
    }
}
