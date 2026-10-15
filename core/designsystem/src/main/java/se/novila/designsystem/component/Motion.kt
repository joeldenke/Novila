package se.novila.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp

/**
 * Novila's motion: calm and unhurried, like a still lake. Everything eases out softly and nothing
 * bounces or loops forever. Animations only play when content first appears; previews and
 * screenshots show the final state.
 */
internal object NovilaMotion {
    /** Starts briskly and settles slowly. */
    val Settle: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val ENTER_MILLIS = 700
    const val DRAW_MILLIS = 1200
    const val STAGGER_MILLIS = 70
}

/** Progress from 0f to 1f that plays once when the caller enters composition. */
@Composable
internal fun rememberReveal(
    durationMillis: Int = NovilaMotion.DRAW_MILLIS,
    delayMillis: Int = 0,
): State<Float> {
    val still = LocalInspectionMode.current
    val progress = remember { Animatable(if (still) 1f else 0f) }
    LaunchedEffect(progress) {
        progress.animateTo(1f, tween(durationMillis, delayMillis, NovilaMotion.Settle))
    }
    return progress.asState()
}

/**
 * Fades [content] in while it rises a few dp into place. Give consecutive blocks on a screen
 * increasing [order]s so they arrive one after another.
 */
@Composable
fun NovilaReveal(
    order: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val progress = rememberReveal(NovilaMotion.ENTER_MILLIS, order * NovilaMotion.STAGGER_MILLIS)
    val rise = with(LocalDensity.current) { 16.dp.toPx() }
    Box(
        modifier.graphicsLayer {
            val p = progress.value
            alpha = p
            translationY = (1f - p) * rise
        },
    ) {
        content()
    }
}
