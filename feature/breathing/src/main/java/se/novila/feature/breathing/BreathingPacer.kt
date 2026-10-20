package se.novila.feature.breathing

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import se.novila.breathing.BreathPhase
import se.novila.breathing.breathScale
import se.novila.designsystem.component.rememberNovilaReducedMotion
import se.novila.designsystem.theme.NovilaTheme
import kotlin.math.min

/** The circle at rest, and the fraction of it the breath reaches, so the ring never fills the whole canvas. */
private const val RESTING_SCALE = 0.62f

/**
 * The breathing pacer, in the Skog & sol style: a lake-blue breath circle with the sun's glow inside, over calm
 * water rings. It grows on the in-breath, holds, and shrinks on the out-breath ([phase] and the step's [progress]).
 *
 * The circle is the picture of the phase, not the only way to read it: the screen shows the phase and the count in
 * text (large-text friendly). With reduced motion the circle holds still at rest while that text keeps counting.
 */
@Composable
fun BreathingPacer(phase: BreathPhase, progress: Float, modifier: Modifier = Modifier) {
    val sleep = NovilaTheme.pillars.sleep
    val accent = NovilaTheme.accent
    val still = rememberNovilaReducedMotion()
    val scale = if (still) RESTING_SCALE else breathScale(phase, progress)
    Canvas(modifier.clearAndSetSemantics { }) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = min(size.width, size.height) / 2f
        val lineWidth = outer * 0.03f

        // The water: fixed rings that stay put while the breath moves.
        drawCircle(sleep.container, radius = outer * 0.98f, center = center)
        drawCircle(
            sleep.color.copy(alpha = 0.16f),
            radius = outer * 0.98f,
            center = center,
            style = Stroke(lineWidth, cap = StrokeCap.Round),
        )
        drawCircle(sleep.color.copy(alpha = 0.12f), radius = outer * 0.74f, center = center, style = Stroke(lineWidth))
        drawCircle(sleep.color.copy(alpha = 0.08f), radius = outer * 0.52f, center = center, style = Stroke(lineWidth))

        // The breath: a soft sun glow around a lake-blue circle with the sun at its heart.
        val radius = outer * scale
        drawCircle(accent.sun.copy(alpha = 0.18f), radius = radius * 1.16f, center = center)
        drawCircle(sleep.container, radius = radius, center = center)
        drawCircle(
            sleep.color,
            radius = radius,
            center = center,
            style = Stroke(lineWidth * 1.4f, cap = StrokeCap.Round),
        )
        drawCircle(accent.sun, radius = radius * 0.32f, center = center)
    }
}
