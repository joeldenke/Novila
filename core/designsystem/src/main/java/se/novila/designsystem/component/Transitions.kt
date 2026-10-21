package se.novila.designsystem.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import se.novila.designsystem.theme.NovilaTheme

/**
 * Novila's motion between screens: calm and short, so a tap feels answered without the screen putting on a show.
 *
 * - Deeper into a tab (and back out), the screens share an axis: the new one drifts in a few percent from the side it
 *   comes from while the old one drifts the other way, and they cross-fade in the middle, so nothing slides a whole
 *   screen width.
 * - Between tabs, which are peers, the screens fade through each other without moving sideways.
 * - A way into a part (a tile under [NovilaEntries]) grows into the screen it opens and shrinks back into it, through
 *   [NovilaSharedBounds]; the app connects both ends.
 *
 * With Android's animations turned off every duration is zero, so screens simply change.
 */
object NovilaTransitions {
    /** How long a move between screens takes. */
    const val DURATION_MILLIS = 360

    /** The old screen is gone this soon, so the two never sit on top of each other for long. */
    private const val FADE_OUT_MILLIS = 120

    /** How far a screen drifts, as a share of the width: about 30 dp on a phone. */
    private const val DRIFT = 12

    /** How far a tab grows from as it fades in. */
    private const val TAB_SCALE = 0.97f

    /** The old screen when the user drags back from the edge: it shrinks a little and lets the one below through. */
    private const val PREDICTIVE_SCALE = 0.92f

    /** Starts quickly and settles slowly, as everything in Novila (Material's emphasized decelerate). */
    private val Settle: Easing = NovilaMotion.Settle

    /** A screen on its way out speeds up as it leaves (Material's emphasized accelerate). */
    private val Leave: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    /** Opening a screen within the tab: it drifts in from the end side while the one below drifts to the start. */
    val forward: ContentTransform
        get() = axis(towardsEnd = true)

    /** Going back within the tab: [forward] in reverse. */
    val back: ContentTransform
        get() = axis(towardsEnd = false)

    /** Switching tabs: the old tab fades out, then the new one fades in as it settles to full size. */
    val tab: ContentTransform
        get() {
            val enter = fadeIn(fadeInSpec()) + scaleIn(tween(DURATION_MILLIS, easing = Settle), TAB_SCALE)
            return enter togetherWith fadeOut(tween(FADE_OUT_MILLIS, easing = Leave))
        }

    /**
     * Dragging back from the edge, following the finger: the screen below fades in from where [back] brings it from
     * while the current one shrinks and fades, so letting go mid-way can go either way.
     */
    val predictiveBack: ContentTransform
        get() {
            val enter = slideInHorizontally(tween(DURATION_MILLIS, easing = LinearEasing)) { -it / DRIFT } +
                fadeIn(tween(DURATION_MILLIS, easing = LinearEasing))
            val exit = scaleOut(tween(DURATION_MILLIS, easing = LinearEasing), PREDICTIVE_SCALE) +
                fadeOut(tween(DURATION_MILLIS, easing = LinearEasing))
            return enter togetherWith exit
        }

    /** Both screens drift the same way along the width, the new one from [towardsEnd]'s side, and cross-fade. */
    private fun axis(towardsEnd: Boolean): ContentTransform {
        val sign = if (towardsEnd) 1 else -1
        val enter = slideInHorizontally(tween(DURATION_MILLIS, easing = Settle)) { sign * it / DRIFT } +
            fadeIn(fadeInSpec())
        val exit = slideOutHorizontally(tween(DURATION_MILLIS, easing = Settle)) { -sign * it / DRIFT } +
            fadeOut(tween(FADE_OUT_MILLIS, easing = Leave))
        return enter togetherWith exit
    }

    /** The new screen fades in once the old one is gone. */
    private fun fadeInSpec() = tween<Float>(DURATION_MILLIS - FADE_OUT_MILLIS, FADE_OUT_MILLIS, LinearEasing)

    /** How long a tile takes to grow into the screen it opens; a little longer than a move, since it travels more. */
    internal const val SHARED_MILLIS = 420

    /** The content of both ends cross-fades while the container grows: the old out first, the new in after. */
    internal const val SHARED_FADE_OUT_MILLIS = 150
}

/**
 * Where the shared transitions happen, set by the app around its navigation. Null in previews, screenshots and
 * tests, which then draw [NovilaSharedBounds] as a plain box.
 */
val LocalNovilaSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

/** The enter and exit of the screen being drawn, set for each screen by the app's navigation. */
val LocalNovilaAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * [content] as one end of a container that moves between two screens: give a tile and the screen it opens the same
 * [key], and the tile grows into the screen when it opens and the screen shrinks back into the tile when the user
 * goes back (following the finger with predictive back). The container keeps its [shape] on the way, and the two
 * contents cross-fade inside it. Without a match on the other screen, or outside the app's navigation (previews,
 * screenshots), it is a plain box. A null [key] is never shared.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NovilaSharedBounds(
    key: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = NovilaTheme.shapes.medium,
    content: @Composable () -> Unit,
) {
    val shared = LocalNovilaSharedTransitionScope.current
    val visibility = LocalNovilaAnimatedVisibilityScope.current
    if (key == null || shared == null || visibility == null) {
        Box(modifier, propagateMinConstraints = true) { content() }
        return
    }
    with(shared) {
        Box(
            modifier = modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key),
                animatedVisibilityScope = visibility,
                enter = fadeIn(
                    tween(
                        NovilaTransitions.SHARED_MILLIS - NovilaTransitions.SHARED_FADE_OUT_MILLIS,
                        NovilaTransitions.SHARED_FADE_OUT_MILLIS,
                        LinearEasing,
                    ),
                ),
                exit = fadeOut(tween(NovilaTransitions.SHARED_FADE_OUT_MILLIS, easing = LinearEasing)),
                boundsTransform = { _, _ -> tween(NovilaTransitions.SHARED_MILLIS, easing = NovilaMotion.Settle) },
                // The default resize mode scales each end to the moving bounds instead of measuring it again, so a
                // list is not laid out on every frame and no text rewraps on the way.
                clipInOverlayDuringTransition = OverlayClip(shape),
            ),
            propagateMinConstraints = true,
        ) {
            content()
        }
    }
}
