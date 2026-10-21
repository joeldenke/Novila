package se.novila.feature.training

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.launch
import se.novila.anatomy.BodyShape
import se.novila.anatomy.Bounds
import se.novila.anatomy.Emphasis
import se.novila.anatomy.ExerciseMotion
import se.novila.anatomy.Layer
import se.novila.anatomy.Point2
import se.novila.anatomy.PropKind
import se.novila.anatomy.Scene
import se.novila.designsystem.component.NovilaPlayButton
import se.novila.designsystem.component.NovilaSlider
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaAnatomyColors
import se.novila.designsystem.theme.NovilaTheme
import se.novila.feature.training.R
import kotlin.math.max
import kotlin.math.min

// Line widths and margins in body heights, so the figure looks the same at any size.
private const val OUTLINE = 0.0055f
private const val MUSCLE_EDGE = 0.0045f
private const val MARGIN = 0.04f
private const val PLATE_RIM = 0.009f
private const val PLATE_FACE_ALPHA = 0.25f

/**
 * An exercise on Novila's anatomy figure: a lake blue body with the trained muscles in ember and its equipment in
 * iron or pine, seen from the exercise's own best camera. Still at the hardest moment of the repetition by default, for
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
    AnatomyCanvas(motion, { progress.value }, contentDescription, modifier, shape)
}

/**
 * The figure in a soft frame at [aspectRatio], with controls like a video player: play and pause, and a slider to drag the movement forwards and
 * backwards through [repetitions] calm repetitions, to study a moment. It plays them once when shown and stops;
 * play again from the end starts over. Never loops. Still at the hardest moment in previews and screenshots.
 */
@Composable
fun AnatomyPlayer(
    motion: ExerciseMotion,
    contentDescription: String,
    repetitions: Int,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 1.2f,
    shape: BodyShape = BodyShape.NEUTRAL,
) {
    val still = LocalInspectionMode.current
    val end = repetitions.toFloat()
    val progress = remember(motion, repetitions) { Animatable(if (still) motion.peak else 0f) }
    var playing by remember(motion, repetitions) { mutableStateOf(!still) }
    LaunchedEffect(progress, playing) {
        if (!playing) return@LaunchedEffect
        if (progress.value >= end) progress.snapTo(0f)
        val left = ((end - progress.value) * motion.repetitionMillis).toInt()
        progress.animateTo(end, tween(left, easing = LinearEasing))
        playing = false
    }
    val scope = rememberCoroutineScope()
    val scrub = stringResource(R.string.exercise_scrub)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
        AnatomyCanvas(
            motion,
            { progress.value },
            contentDescription,
            Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .background(NovilaTheme.colors.surfaceContainerLow, NovilaTheme.shapes.small),
            shape,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.m),
        ) {
            NovilaPlayButton(
                playing = playing,
                onClick = { playing = !playing },
                description = stringResource(if (playing) R.string.exercise_pause else R.string.exercise_play),
                role = NovilaTheme.pillars.training,
            )
            Column(Modifier.weight(1f)) {
                NovilaSlider(
                    value = progress.value.coerceIn(0f, end),
                    onValueChange = { value ->
                        // Dragging takes over from playing.
                        playing = false
                        scope.launch { progress.snapTo(value) }
                    },
                    valueRange = 0f..end,
                    steps = 0,
                    role = NovilaTheme.pillars.training,
                    modifier = Modifier.semantics { this.contentDescription = scrub },
                )
                NovilaText(
                    stringResource(
                        R.string.exercise_repetition,
                        (progress.value.toInt() + 1).coerceAtMost(repetitions),
                        repetitions,
                    ),
                    style = NovilaTheme.typography.labelMedium,
                    color = NovilaTheme.colors.onSurfaceVariant,
                )
            }
        }
    }
}

/** Draws [motion] at the moment [progress] gives (repetitions from 0), read while drawing. */
@Composable
private fun AnatomyCanvas(
    motion: ExerciseMotion,
    progress: () -> Float,
    contentDescription: String,
    modifier: Modifier,
    shape: BodyShape,
) {
    val colors = NovilaTheme.anatomy
    val bounds = remember(motion, shape) { motion.bounds(shape) }
    Canvas(modifier.semantics { this.contentDescription = contentDescription }) {
        drawScene(motion.sceneAt(progress(), shape), bounds, colors)
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
    val rim = Stroke(max(1f, PLATE_RIM * scale), join = StrokeJoin.Round)

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

            is Layer.Gear -> {
                flushOutline()
                when (layer.kind) {
                    PropKind.Bar -> drawPath(shape, colors.gear)

                    PropKind.Band -> drawPath(shape, colors.band)

                    // A plate is a rim around a see-through face, so it never hides the head or the muscles.
                    PropKind.Weight -> {
                        drawPath(shape, colors.weight.copy(alpha = PLATE_FACE_ALPHA))
                        drawPath(shape, colors.weight, style = rim)
                    }
                }
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
