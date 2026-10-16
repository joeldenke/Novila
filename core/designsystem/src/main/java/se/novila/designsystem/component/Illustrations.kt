package se.novila.designsystem.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import se.novila.designsystem.theme.NovilaSceneryColors
import se.novila.designsystem.theme.NovilaTheme
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin

/** The scenes Novila draws: the Swedish countryside, one for the app and one for each part of it. */
enum class NovilaScene {
    /** The midsummer sun rising over a still lake between the forest edges. The app as a whole. */
    Sunrise,

    /** A trail that winds up a hill to a flag on the summit. Training. */
    Trail,

    /** A crescent moon and stars over the lake at night. Sleep. */
    Night,

    /** A falu red cottage on a meadow, with lingonberry bushes in front. Nutrition. */
    Cottage,

    /** Footprints along a path across the meadow, towards the forest. Everyday movement. */
    Steps,
}

/**
 * A drawn landscape in the Skog & sol colors, in rounded corners. It is all vector drawing, so it is crisp at every
 * size and follows the theme: dark mode turns the scene towards dusk. When it appears the scene comes alive once (the
 * sun rises, the trail draws itself up the hill, stars light up one by one, footprints appear step by step) and then
 * stays still. Decorative: screen readers skip it.
 *
 * @param parallax how far the scene is from the middle of a pager, in pages (0 when centered). Far layers lag behind
 *   and near layers lead, so swiping between scenes has depth. Read while drawing, so it never recomposes.
 */
@Composable
fun NovilaIllustration(
    scene: NovilaScene,
    modifier: Modifier = Modifier,
    parallax: () -> Float = { 0f },
) {
    val colors = NovilaTheme.scenery
    val reveal = rememberReveal(durationMillis = 2200)
    Spacer(
        modifier
            .clip(NovilaTheme.shapes.extraLarge)
            .clearAndSetSemantics {}
            .drawBehind {
                val scenery = Scenery(this, colors, reveal.value, parallax())
                when (scene) {
                    NovilaScene.Sunrise -> scenery.sunrise()
                    NovilaScene.Trail -> scenery.trail()
                    NovilaScene.Night -> scenery.night()
                    NovilaScene.Cottage -> scenery.cottage()
                    NovilaScene.Steps -> scenery.steps()
                }
            },
    )
}

/**
 * Draws one scene at [progress] (0f to 1f) of its entrance, in layers from far to near. Every layer moves with the
 * page by its depth: far layers (the sky's sun and moon) lag behind by half a page, near ones (bushes in front) lead.
 */
private class Scenery(
    private val scope: DrawScope,
    private val c: NovilaSceneryColors,
    private val progress: Float,
    private val shift: Float,
) {
    private val w = scope.size.width
    private val h = scope.size.height
    private val eased = NovilaMotion.Settle.transform(progress)

    /** Draws [block] moved by this layer's [depth]: 0.5 lags half as fast as the page, negative leads it. */
    private inline fun layer(depth: Float, crossinline block: DrawScope.() -> Unit) {
        scope.translate(left = -shift * depth * w) { block() }
    }

    /** How far [progress] has come between [start] and [end], as 0f..1f, for parts that arrive one after another. */
    private fun stage(start: Float, end: Float): Float = ((progress - start) / (end - start)).coerceIn(0f, 1f)

    fun sunrise() = with(scope) {
        val horizon = h * 0.64f
        drawRect(Brush.verticalGradient(listOf(c.dawnTop, c.dawnBottom), 0f, horizon))
        val sunX = w * 0.6f
        val sunRadius = w * 0.085f
        layer(0.5f) { drawSun(Offset(sunX, lerp(horizon + sunRadius, h * 0.34f, eased)), sunRadius, c.sun) }
        layer(0.35f) { drawHills(horizon - h * 0.04f, h * 0.05f, 0.1f, c.farHills) }
        layer(0.2f) {
            drawForestEdge(horizon, h * 0.17f, c.forest, -w * 0.3f, w * 0.44f, 9)
            drawForestEdge(horizon, h * 0.13f, c.forest, w * 0.78f, w * 1.3f, 6)
        }
        drawRect(c.water, Offset(0f, horizon), Size(w, h - horizon))
        layer(0.5f) { drawReflection(sunX, horizon, sunRadius, c.sun, eased) }
        drawShimmer(horizon)
        layer(-0.12f) {
            val shore = Path().apply {
                moveTo(-w * 0.3f, h)
                quadraticTo(w * 0.04f, h * 0.8f, w * 0.34f, h)
                close()
            }
            drawPath(shore, c.nearForest)
            drawSpruce(w * 0.05f, h * 0.94f, h * 0.32f, c.nearForest)
            drawSpruce(w * 0.15f, h * 0.98f, h * 0.22f, c.nearForest)
        }
    }

    fun trail() = with(scope) {
        drawRect(Brush.verticalGradient(listOf(c.skyTop, c.skyBottom)))
        layer(0.5f) { drawSun(Offset(w * 0.2f, lerp(h * 0.34f, h * 0.22f, eased)), w * 0.06f, c.sun) }
        layer(0.35f) { drawHills(h * 0.52f, h * 0.05f, 0.3f, c.farHills) }
        layer(0.12f) {
            val hill = Path().apply {
                moveTo(-w * 0.5f, h)
                for (i in 0..SAMPLES) {
                    val x = -w * 0.5f + w * 2f * i / SAMPLES
                    lineTo(x, hillY(x))
                }
                lineTo(w * 1.5f, h)
                close()
            }
            drawPath(hill, c.hills)
            listOf(0.08f to 0.1f, 0.17f to 0.13f, 0.24f to 0.09f, 0.9f to 0.12f, 0.97f to 0.1f, 1.05f to 0.14f)
                .forEach { (x, height) -> drawSpruce(w * x, hillY(w * x) + h * 0.02f, h * height, c.forest) }
            drawTrailPath(stage(0.1f, 0.8f))
            drawFlag(stage(0.75f, 1f))
        }
        layer(-0.12f) {
            drawForestEdge(h * 1.02f, h * 0.28f, c.nearForest, -w * 0.3f, w * 0.26f, 4)
            drawForestEdge(h * 1.02f, h * 0.24f, c.nearForest, w * 0.86f, w * 1.3f, 3)
        }
    }

    /** The hill in [trail]: its summit at 62 % across and 30 % down. */
    private fun hillY(x: Float): Float {
        val distance = (x - w * 0.62f) / (w * 0.62f)
        return (h * 0.3f + h * 0.62f * distance * distance).coerceAtMost(h * 1.05f)
    }

    private fun DrawScope.drawTrailPath(drawn: Float) {
        if (drawn <= 0f) return
        val trail = Path().apply {
            moveTo(w * 0.42f, h * 1.02f)
            cubicTo(w * 0.75f, h * 0.88f, w * 0.3f, h * 0.7f, w * 0.55f, h * 0.58f)
            cubicTo(w * 0.75f, h * 0.48f, w * 0.5f, h * 0.4f, w * 0.62f, h * 0.31f)
        }
        val measure = PathMeasure().apply { setPath(trail, false) }
        val segment = Path()
        measure.getSegment(0f, measure.length * drawn, segment, true)
        drawPath(segment, c.trail, style = Stroke(w * 0.024f, cap = StrokeCap.Round))
    }

    private fun DrawScope.drawFlag(raised: Float) {
        if (raised <= 0f) return
        val foot = Offset(w * 0.62f, h * 0.31f)
        val top = Offset(foot.x, foot.y - h * 0.11f * raised)
        drawLine(c.roof, foot, top, 2.dp.toPx(), StrokeCap.Round)
        val flag = Path().apply {
            moveTo(top.x, top.y)
            lineTo(top.x + w * 0.07f * raised, top.y + h * 0.025f)
            lineTo(top.x, top.y + h * 0.05f)
            close()
        }
        drawPath(flag, c.sun)
    }

    fun night() = with(scope) {
        val horizon = h * 0.64f
        drawRect(Brush.verticalGradient(listOf(c.nightTop, c.nightBottom), 0f, horizon))
        layer(0.5f) {
            STARS.forEachIndexed { i, (x, y, radius) ->
                val lit = stage(0.05f + i * 0.035f, 0.25f + i * 0.035f)
                if (lit > 0f) drawCircle(c.star.copy(alpha = lit * 0.9f), radius.dp.toPx(), Offset(w * x, h * y))
            }
        }
        val moonX = w * 0.7f
        val moonRadius = w * 0.065f
        layer(0.45f) { drawMoon(Offset(moonX, lerp(h * 0.32f, h * 0.24f, eased)), moonRadius) }
        layer(0.35f) { drawHills(horizon - h * 0.05f, h * 0.04f, 0.55f, c.nightHills) }
        layer(0.2f) { drawForestEdge(horizon, h * 0.14f, c.nightForest, -w * 0.3f, w * 1.3f, 15) }
        drawRect(c.nightWater, Offset(0f, horizon), Size(w, h - horizon))
        layer(0.45f) { drawReflection(moonX, horizon, moonRadius, c.moon, eased) }
        drawShimmer(horizon)
        layer(-0.12f) {
            val shore = Path().apply {
                moveTo(w * 0.62f, h)
                quadraticTo(w * 0.95f, h * 0.82f, w * 1.3f, h * 0.86f)
                lineTo(w * 1.3f, h)
                close()
            }
            drawPath(shore, c.nightForest)
            drawSpruce(w * 0.86f, h * 0.92f, h * 0.3f, c.nightForest)
            drawSpruce(w * 0.96f, h * 0.9f, h * 0.38f, c.nightForest)
        }
    }

    private fun DrawScope.drawMoon(center: Offset, radius: Float) {
        drawCircle(
            Brush.radialGradient(
                listOf(c.moon.copy(alpha = 0.3f * eased), c.moon.copy(alpha = 0f)),
                center,
                radius * 4f,
            ),
            radius * 4f,
            center,
        )
        val full = Path().apply { addOval(Rect(center, radius)) }
        val shadow = Path().apply { addOval(Rect(center + Offset(-radius * 0.45f, -radius * 0.25f), radius * 0.92f)) }
        val crescent = Path().apply { op(full, shadow, PathOperation.Difference) }
        drawPath(crescent, c.moon.copy(alpha = eased))
    }

    fun cottage() = with(scope) {
        drawRect(Brush.verticalGradient(listOf(c.skyTop, c.skyBottom)))
        layer(0.5f) { drawSun(Offset(w * 0.82f, lerp(h * 0.3f, h * 0.2f, eased)), w * 0.065f, c.sun) }
        layer(0.35f) { drawHills(h * 0.5f, h * 0.05f, 0.6f, c.farHills) }
        layer(0.2f) { drawForestEdge(h * 0.6f, h * 0.13f, c.forest, -w * 0.3f, w * 1.3f, 14) }
        layer(0.08f) {
            drawHills(h * 0.63f, h * 0.02f, 0.2f, c.meadow)
            drawCottage(centerX = w * 0.44f, ground = h * 0.78f - (1f - eased) * h * 0.03f)
        }
        layer(-0.12f) {
            drawBush(Offset(w * 0.1f, h * 0.97f), w * 0.13f, berriesFrom = 0)
            drawBush(Offset(w * 0.88f, h * 0.99f), w * 0.15f, berriesFrom = 4)
        }
    }

    private fun DrawScope.drawCottage(centerX: Float, ground: Float) {
        val width = w * 0.34f
        val height = h * 0.17f
        val left = centerX - width / 2f
        val right = centerX + width / 2f
        val top = ground - height
        // Chimney first, so the roof covers its foot.
        drawRect(c.roof, Offset(right - width * 0.28f, top - height * 0.62f), Size(width * 0.07f, height * 0.4f))
        drawRect(c.cottage, Offset(left, top), Size(width, height))
        val corner = width * 0.035f
        drawRect(c.trim, Offset(left, top), Size(corner, height))
        drawRect(c.trim, Offset(right - corner, top), Size(corner, height))
        val roof = Path().apply {
            moveTo(left - width * 0.06f, top)
            lineTo(left + width * 0.12f, top - height * 0.5f)
            lineTo(right - width * 0.12f, top - height * 0.5f)
            lineTo(right + width * 0.06f, top)
            close()
        }
        drawPath(roof, c.roof)
        drawRect(c.trim, Offset(left - width * 0.02f, top), Size(width * 1.04f, height * 0.05f))
        // The door, white-framed in the middle.
        val doorWidth = width * 0.15f
        val doorHeight = height * 0.62f
        val doorLeft = centerX - doorWidth / 2f
        drawRect(c.trim, Offset(doorLeft, ground - doorHeight), Size(doorWidth, doorHeight))
        val frame = width * 0.018f
        drawRect(
            c.roof,
            Offset(doorLeft + frame, ground - doorHeight + frame),
            Size(doorWidth - 2 * frame, doorHeight - frame),
        )
        drawWindow(Offset(left + width * 0.11f, top + height * 0.24f), Size(width * 0.2f, height * 0.36f), frame)
        drawWindow(Offset(right - width * 0.31f, top + height * 0.24f), Size(width * 0.2f, height * 0.36f), frame)
    }

    private fun DrawScope.drawWindow(topLeft: Offset, size: Size, frame: Float) {
        drawRect(c.trim, topLeft, size)
        drawRect(c.pane, topLeft + Offset(frame, frame), Size(size.width - 2 * frame, size.height - 2 * frame))
        val middle = topLeft + Offset(size.width / 2f, size.height / 2f)
        drawLine(c.trim, Offset(middle.x, topLeft.y), Offset(middle.x, topLeft.y + size.height), frame)
        drawLine(c.trim, Offset(topLeft.x, middle.y), Offset(topLeft.x + size.width, middle.y), frame)
    }

    /** A rounded bush of leaves whose lingonberries ripen one after another, [berriesFrom] setting their turn. */
    private fun DrawScope.drawBush(base: Offset, radius: Float, berriesFrom: Int) {
        listOf(Triple(-0.55f, -0.35f, 0.62f), Triple(0.5f, -0.3f, 0.66f), Triple(0f, -0.62f, 0.72f))
            .forEach { (x, y, r) -> drawCircle(c.leaves, radius * r, base + Offset(radius * x, radius * y)) }
        listOf(-0.5f to -0.55f, 0.1f to -0.9f, 0.55f to -0.45f, -0.1f to -0.35f).forEachIndexed { i, (x, y) ->
            val ripe = stage(0.35f + (berriesFrom + i) * 0.06f, 0.55f + (berriesFrom + i) * 0.06f)
            if (ripe > 0f) drawCircle(c.berry, radius * 0.11f * ripe, base + Offset(radius * x, radius * y))
        }
    }

    fun steps() = with(scope) {
        val meadowTop = h * 0.57f
        drawRect(Brush.verticalGradient(listOf(c.skyTop, c.skyBottom), 0f, meadowTop))
        layer(0.5f) { drawSun(Offset(w * 0.78f, lerp(h * 0.3f, h * 0.2f, eased)), w * 0.06f, c.sun) }
        layer(0.35f) { drawHills(h * 0.48f, h * 0.05f, 0.85f, c.farHills) }
        layer(0.2f) { drawForestEdge(meadowTop, h * 0.12f, c.forest, -w * 0.3f, w * 1.3f, 15) }
        drawRect(c.meadow, Offset(0f, meadowTop), Size(w, h - meadowTop))
        val path = Path().apply {
            moveTo(w * 0.27f, h)
            quadraticTo(w * 0.42f, h * 0.76f, w * 0.49f, meadowTop)
            lineTo(w * 0.51f, meadowTop)
            quadraticTo(w * 0.6f, h * 0.76f, w * 0.73f, h)
            close()
        }
        drawPath(path, c.trail)
        repeat(FOOTPRINTS) { i ->
            val shown = stage(0.15f + i * 0.09f, 0.3f + i * 0.09f)
            if (shown > 0f) {
                val t = i / FOOTPRINTS.toFloat()
                val scale = lerp(1f, 0.35f, t)
                val side = if (i % 2 == 0) -1f else 1f
                val center = Offset(w * 0.5f + side * w * 0.035f * scale, lerp(h * 0.95f, h * 0.63f, t.pow(0.8f)))
                drawFootprint(center, w * 0.05f * scale, c.footprint.copy(alpha = shown))
            }
        }
        layer(-0.12f) {
            FLOWERS.forEachIndexed { i, (x, y) ->
                val open = stage(0.2f + i * 0.07f, 0.45f + i * 0.07f)
                if (open > 0f) {
                    drawCircle(if (i % 2 == 0) c.sun else c.berry, w * 0.013f * open, Offset(w * x, h * y))
                }
            }
        }
    }

    /** A bare footprint, [length] from toe to heel. */
    private fun DrawScope.drawFootprint(center: Offset, length: Float, color: Color) {
        val width = length * 0.42f
        drawOval(color, center - Offset(width / 2f, length * 0.5f), Size(width, length * 0.62f))
        drawOval(color, center + Offset(-width * 0.4f, length * 0.18f), Size(width * 0.8f, length * 0.34f))
    }

    // Shared layers.

    private fun DrawScope.drawSun(center: Offset, radius: Float, color: Color) {
        drawCircle(
            Brush.radialGradient(listOf(color.copy(alpha = 0.38f), color.copy(alpha = 0f)), center, radius * 4.5f),
            radius * 4.5f,
            center,
        )
        drawCircle(color, radius, center)
    }

    /** Rolling hills along [base], [amplitude] high, drawn wider than the scene so parallax never shows an edge. */
    private fun DrawScope.drawHills(base: Float, amplitude: Float, phase: Float, color: Color) {
        val left = -w * 0.5f
        val right = w * 1.5f
        val hills = Path().apply {
            moveTo(left, h)
            for (i in 0..SAMPLES) {
                val x = left + (right - left) * i / SAMPLES
                val t = x / w
                val wave = 0.6f * sin(2f * PI.toFloat() * (t * 0.9f + phase)) +
                    0.4f * sin(2f * PI.toFloat() * (t * 2.1f + phase * 1.7f))
                lineTo(x, base - amplitude * wave)
            }
            lineTo(right, h)
            close()
        }
        drawPath(hills, color)
    }

    /** A row of [count] spruces from [from] to [to] standing on [base], on a low band so they read as one edge. */
    private fun DrawScope.drawForestEdge(base: Float, height: Float, color: Color, from: Float, to: Float, count: Int) {
        drawRect(color, Offset(from, base - height * 0.1f), Size(to - from, height * 0.1f + 1f))
        val step = (to - from) / count
        repeat(count) { i ->
            val scale = TREE_HEIGHTS[i % TREE_HEIGHTS.size]
            val nudge = TREE_NUDGES[i % TREE_NUDGES.size]
            drawSpruce(from + step * (i + 0.5f + nudge), base, height * scale, color)
        }
    }

    /** One spruce of three tiers, like the one in the launcher icon. */
    private fun DrawScope.drawSpruce(x: Float, base: Float, height: Float, color: Color) {
        val half = height * 0.28f
        val tree = Path()
        for (tier in 0 until 3) {
            val top = base - height + tier * height * 0.22f
            val bottom = top + height * 0.45f
            val width = half * (0.55f + tier * 0.32f)
            tree.moveTo(x, top)
            tree.lineTo(x + width, bottom)
            tree.lineTo(x - width, bottom)
            tree.close()
        }
        drawPath(tree, color)
        val trunk = height * 0.05f
        drawRect(color, Offset(x - trunk / 2f, base - height * 0.13f), Size(trunk, height * 0.13f))
    }

    /** A sun or moon mirrored in the lake as broken lines that narrow with depth. */
    private fun DrawScope.drawReflection(x: Float, horizon: Float, radius: Float, color: Color, strength: Float) {
        val rows = 7
        val stroke = 2.dp.toPx()
        repeat(rows) { i ->
            val y = horizon + (i + 0.6f) * (h - horizon) / (rows + 1)
            val half = radius * (1.1f - i * 0.12f)
            val alpha = 0.6f * (1f - i / (rows + 1f)) * strength
            drawLine(color.copy(alpha = alpha), Offset(x - half, y), Offset(x + half, y), stroke, StrokeCap.Round)
        }
    }

    /** Light on the water, a few calm lines that brighten as the scene appears. */
    private fun DrawScope.drawShimmer(horizon: Float) {
        val stroke = 1.5f.dp.toPx()
        SHIMMER.forEach { (x, y, length) ->
            val at = Offset(w * x, horizon + (h - horizon) * y)
            drawLine(c.shine.copy(alpha = 0.35f * eased), at, at + Offset(w * length, 0f), stroke, StrokeCap.Round)
        }
    }

    private companion object {
        const val SAMPLES = 48
        const val FOOTPRINTS = 8

        /** Heights and offsets of trees in a forest edge, so the edge looks grown rather than planted. */
        val TREE_HEIGHTS = listOf(1f, 0.72f, 1.25f, 0.86f, 1.1f, 0.76f, 1.32f, 0.92f, 0.8f, 1.18f)
        val TREE_NUDGES = listOf(0f, 0.2f, -0.15f, 0.1f, -0.25f, 0.15f, -0.05f, 0.25f, -0.2f, 0.05f)

        /** Stars as (x, y, radius in dp), placed by hand away from the moon. */
        val STARS = listOf(
            Triple(0.08f, 0.1f, 1.4f),
            Triple(0.18f, 0.26f, 1f),
            Triple(0.27f, 0.08f, 1.8f),
            Triple(0.35f, 0.34f, 1.1f),
            Triple(0.44f, 0.16f, 1.3f),
            Triple(0.53f, 0.06f, 1f),
            Triple(0.57f, 0.38f, 1.4f),
            Triple(0.86f, 0.1f, 1.2f),
            Triple(0.93f, 0.3f, 1.6f),
            Triple(0.12f, 0.44f, 1.2f),
            Triple(0.4f, 0.48f, 0.9f),
            Triple(0.82f, 0.45f, 1f),
            Triple(0.22f, 0.15f, 0.9f),
            Triple(0.63f, 0.2f, 0.8f),
        )

        /** Lines of light on the water as (x, depth below the horizon, length), in fractions of the scene. */
        val SHIMMER = listOf(
            Triple(0.08f, 0.25f, 0.08f),
            Triple(0.28f, 0.45f, 0.12f),
            Triple(0.82f, 0.3f, 0.1f),
            Triple(0.74f, 0.7f, 0.14f),
            Triple(0.16f, 0.82f, 0.1f),
        )

        /** Flowers in the meadow as (x, y). */
        val FLOWERS = listOf(
            0.1f to 0.7f,
            0.86f to 0.68f,
            0.2f to 0.86f,
            0.93f to 0.84f,
            0.06f to 0.93f,
            0.8f to 0.94f,
        )
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float = start + (stop - start) * fraction
