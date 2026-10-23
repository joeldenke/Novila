package se.novila.designsystem.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import se.novila.designsystem.R
import se.novila.designsystem.theme.NovilaHeroColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The sky over the hero card at [hour] of day: the morning sun inside rings of light, the midday sun with rays, the
 * setting sun with the launcher icon's cuts, and the moon at night.
 */
enum class NovilaSky {
    Morning,
    Day,
    Evening,
    Night,
    ;

    companion object {
        /** The sky at [hour] (0–23): morning 05–10, day 10–17, evening 17–22, otherwise night. */
        fun at(hour: Int): NovilaSky = when (hour) {
            in 5..9 -> Morning
            in 10..16 -> Day
            in 17..21 -> Evening
            else -> Night
        }
    }
}

/** The sky's sun or moon, shared with the widget. */
internal val NovilaSky.icon: Int
    @DrawableRes get() = when (this) {
        NovilaSky.Morning -> R.drawable.ic_novila_sun_morning
        NovilaSky.Day -> R.drawable.ic_novila_sun
        NovilaSky.Evening -> R.drawable.ic_novila_sun_evening
        NovilaSky.Night -> R.drawable.ic_novila_moon
    }

/** How strongly the sky's light shines in the hero's corner: the moon gives a fainter light than the sun. */
internal val NovilaSky.glow: Float get() = if (this == NovilaSky.Night) 0.14f else 0.28f

/** The [sky]'s sun or moon in the hero's top corner, rising into place as the card is [reveal]ed. */
@Composable
internal fun NovilaSkyIcon(sky: NovilaSky, hero: NovilaHeroColors, reveal: Float) {
    NovilaIcon(
        painter = painterResource(sky.icon),
        contentDescription = null,
        modifier = Modifier.size(SkyIconSize).graphicsLayer {
            alpha = reveal
            translationY = (1f - reveal) * SkyIconSize.toPx() / 2f
            // Only the rays turn; the cuts and the moon stay level.
            if (sky == NovilaSky.Day) rotationZ = (1f - reveal) * -90f
        },
        tint = hero.sun,
    )
}

/**
 * Today's background: the [sky]'s glow in the top corner and contour lines rising towards the bottom right corner,
 * like a map of the hills around the day.
 */
internal fun Modifier.heroContours(hero: NovilaHeroColors, sky: NovilaSky): Modifier = drawWithCache {
    val contours = Path()
    contourLines(
        center = Offset(size.width - 64.dp.toPx(), size.height - 16.dp.toPx()),
        step = 18.dp.toPx(),
        moveTo = contours::moveTo,
        lineTo = contours::lineTo,
        close = contours::close,
    )
    val line = Stroke(1.dp.toPx())
    val color = hero.content.copy(alpha = 0.09f)
    onSky(hero, sky) { drawPath(contours, color, style = line) }
}

/**
 * The contour lines around [center], [step] apart, as path commands, so the app (Compose) and the widget
 * (android.graphics) draw the same hills.
 */
internal fun contourLines(
    center: Offset,
    step: Float,
    moveTo: (Float, Float) -> Unit,
    lineTo: (Float, Float) -> Unit,
    close: () -> Unit,
) {
    for (ring in 1..CONTOURS) {
        val radius = step * ring
        for (point in 0..CONTOUR_POINTS) {
            val angle = 2 * PI * point / CONTOUR_POINTS
            // Gentle waves that shift a little from ring to ring, so the lines read as terrain, not circles.
            val wobble = 1 +
                0.10 * sin(3 * angle + 1.49 + ring * 0.15) +
                0.06 * sin(5 * angle + 3.42) +
                0.04 * sin(2 * angle + 2.32 + ring * 0.3)
            val x = center.x + (radius * wobble * cos(angle)).toFloat()
            val y = center.y + (radius * wobble * CONTOUR_FLATTEN * sin(angle)).toFloat()
            if (point == 0) moveTo(x, y) else lineTo(x, y)
        }
        close()
    }
}

/**
 * Training's background: the [sky]'s glow and parallel lines sweeping up towards it, fading out below a single
 * line in the sun's color, like movement.
 */
internal fun Modifier.heroMotionLines(hero: NovilaHeroColors, sky: NovilaSky): Modifier = drawWithCache {
    // Drawn on a 358 × 370 frame (a phone-width panel) and stretched to fit.
    val sx = size.width / 358f
    val sy = size.height / 370f
    fun sweep(offset: Float) = Path().apply {
        moveTo(-20f * sx, (372f + offset) * sy)
        cubicTo(120f * sx, (350f + offset) * sy, 220f * sx, (250f + offset) * sy, 380f * sx, (110f + offset) * sy)
    }
    val lines = (0 until MOTION_LINES).map { sweep(it * 12f) }
    val lead = sweep(-12f)
    val thin = Stroke(1.4.dp.toPx())
    val bold = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round)
    onSky(hero, sky) {
        lines.forEachIndexed { i, path -> drawPath(path, hero.content.copy(alpha = 0.13f - 0.012f * i), style = thin) }
        drawPath(lead, hero.sun.copy(alpha = 0.5f), style = bold)
    }
}

/**
 * Sleep's background: a night lake. The [sky]'s glow above a still horizon, and ripples that lie closer together
 * towards it and grow towards the viewer.
 */
internal fun Modifier.heroRipples(hero: NovilaHeroColors, sky: NovilaSky): Modifier = drawWithCache {
    val horizon = size.height * RIPPLE_HORIZON
    val step = 6.dp.toPx()
    val ripples = (1..RIPPLES).map { ring ->
        val depth = ring / RIPPLES.toFloat()
        val y = horizon + (size.height - horizon) * depth * depth
        val amplitude = (1f + 3f * depth).dp.toPx()
        val wave = (40f + 50f * depth).dp.toPx()
        val path = Path()
        var x = 0f
        while (x <= size.width + step) {
            val dy = amplitude * sin(2 * PI * x / wave + ring * 1.3).toFloat()
            if (x == 0f) path.moveTo(x, y + dy) else path.lineTo(x, y + dy)
            x += step
        }
        path to depth
    }
    val thin = Stroke(1.2.dp.toPx())
    onSky(hero, sky) {
        drawLine(hero.content.copy(alpha = 0.16f), Offset(0f, horizon), Offset(size.width, horizon), 1.dp.toPx())
        ripples.forEach { (path, depth) ->
            drawPath(path, hero.content.copy(alpha = 0.13f - 0.06f * depth), style = thin)
        }
    }
}

/**
 * Nutrition's background: the [sky]'s glow over a quiet horizon, and the furrows of a field running from the bottom
 * edge towards one point on it, like rows of something growing.
 */
internal fun Modifier.heroFurrows(hero: NovilaHeroColors, sky: NovilaSky): Modifier = drawWithCache {
    val horizon = size.height * FURROW_HORIZON
    val vanishing = Offset(size.width * 0.7f, horizon)
    val furrows = (0 until FURROWS).map { i ->
        val bottom = Offset(size.width * (-0.5f + 2f * i / (FURROWS - 1)), size.height)
        // Starting a little below the horizon, so the rows don't run together into a point.
        val start = Offset(
            vanishing.x + (bottom.x - vanishing.x) * FURROW_START,
            vanishing.y + (bottom.y - vanishing.y) * FURROW_START,
        )
        start to bottom
    }
    val thin = 1.2.dp.toPx()
    onSky(hero, sky) {
        furrows.forEach { (start, end) ->
            drawLine(
                Brush.linearGradient(
                    listOf(hero.content.copy(alpha = 0.04f), hero.content.copy(alpha = 0.14f)),
                    start,
                    end,
                ),
                start,
                end,
                thin,
            )
        }
        drawLine(hero.content.copy(alpha = 0.16f), Offset(0f, horizon), Offset(size.width, horizon), 1.dp.toPx())
    }
}

/** Draws the [sky]'s glow behind the content, then the background's own [lines]. */
private fun CacheDrawScope.onSky(
    hero: NovilaHeroColors,
    sky: NovilaSky,
    lines: DrawScope.() -> Unit,
): DrawResult {
    val center = Offset(size.width - 36.dp.toPx(), 36.dp.toPx())
    val radius = 120.dp.toPx()
    val brush = Brush.radialGradient(listOf(hero.sun.copy(alpha = sky.glow), hero.sun.copy(alpha = 0f)), center, radius)
    return onDrawBehind {
        drawCircle(brush, radius = radius, center = center)
        lines()
    }
}

private val SkyIconSize = 28.dp
private const val CONTOURS = 12
private const val CONTOUR_POINTS = 45
private const val CONTOUR_FLATTEN = 0.8
private const val MOTION_LINES = 8
private const val RIPPLES = 7
private const val RIPPLE_HORIZON = 0.5f
private const val FURROWS = 13
private const val FURROW_HORIZON = 0.42f
private const val FURROW_START = 0.06f
