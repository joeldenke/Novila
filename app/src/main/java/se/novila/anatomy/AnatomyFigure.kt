package se.novila.anatomy

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import se.novila.designsystem.theme.NovilaAnatomyColors
import se.novila.designsystem.theme.NovilaTheme
import kotlin.math.max
import kotlin.math.min

// Line widths and margins in body heights, so the figure looks the same at any size.
private const val OUTLINE = 0.0055f
private const val MUSCLE_EDGE = 0.0045f
private const val MARGIN = 0.04f

/**
 * An exercise on Novila's anatomy figure: a lake blue body with the trained muscles in ember, seen
 * from the exercise's own best camera. Still at the hardest moment of the repetition by default, for
 * thumbnails. With [repetitions], it plays that many calm repetitions once and comes to rest; change
 * [replay] to play them again. Never loops, so screenshot tests and battery stay calm.
 */
@Composable
fun AnatomyFigure(
    motion: ExerciseMotion,
    contentDescription: String,
    modifier: Modifier = Modifier,
    repetitions: Int = 0,
    replay: Any? = null,
    shape: BodyShape = BodyShape.NEUTRAL,
) {
    val colors = NovilaTheme.anatomy
    val still = LocalInspectionMode.current || repetitions <= 0
    val progress = remember(motion, repetitions, replay) { Animatable(if (still) motion.peak else 0f) }
    LaunchedEffect(progress, still) {
        if (!still) {
            progress.animateTo(
                repetitions.toFloat(),
                tween(repetitions * motion.repetitionMillis, easing = LinearEasing),
            )
        }
    }
    val bounds = remember(motion, shape) { motion.bounds(shape) }
    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        drawScene(motion.sceneAt(progress.value, shape), bounds, colors)
    }
}

private fun DrawScope.drawScene(scene: Scene, bounds: Bounds, colors: NovilaAnatomyColors) {
    val room = Size(bounds.width + 2 * MARGIN, bounds.height + 2 * MARGIN)
    val scale = min(size.width / room.width, size.height / room.height)
    val origin = Offset(
        size.width / 2f - (bounds.left + bounds.width / 2f) * scale,
        size.height / 2f + (bounds.bottom + bounds.height / 2f) * scale,
    )
    fun Point2.toOffset() = Offset(origin.x + x * scale, origin.y - y * scale)
    fun path(points: List<Point2>) = Path().apply {
        points.forEachIndexed { i, point ->
            val offset = point.toOffset()
            if (i == 0) moveTo(offset.x, offset.y) else lineTo(offset.x, offset.y)
        }
        close()
    }
    val outline = Stroke(max(1f, OUTLINE * scale), cap = StrokeCap.Round, join = StrokeJoin.Round)
    val edge = Stroke(max(1f, MUSCLE_EDGE * scale), join = StrokeJoin.Round)

    scene.shadow?.let { shadow ->
        drawOval(
            colors.shadow,
            topLeft = Point2(shadow.center.x - shadow.radiusX, shadow.center.y + shadow.radiusY).toOffset(),
            size = Size(2 * shadow.radiusX * scale, 2 * shadow.radiusY * scale),
        )
    }
    // Each part's outline is drawn after its muscles, so they sit inside it, and before the next part.
    var pendingOutline: Path? = null
    fun flushOutline() {
        pendingOutline?.let { drawPath(it, colors.outline, style = outline) }
        pendingOutline = null
    }
    scene.layers.forEach { layer ->
        val shape = path(layer.outline)
        when (layer) {
            is Layer.Part -> {
                flushOutline()
                drawPath(shape, lerp(colors.body, colors.bodyShade, layer.shade))
                pendingOutline = shape
            }

            is Layer.Muscle -> when (layer.emphasis) {
                Emphasis.Primary -> drawPath(shape, colors.musclePrimary)

                Emphasis.Secondary -> {
                    drawPath(shape, colors.muscleSecondary)
                    drawPath(shape, colors.musclePrimary, style = edge)
                }
            }
        }
    }
    flushOutline()
}
