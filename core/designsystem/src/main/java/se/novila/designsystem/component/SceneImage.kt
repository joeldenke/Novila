package se.novila.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import se.novila.designsystem.theme.NovilaTheme

/**
 * A painted scene (a bitmap in the feature's `drawable-nodpi`) in the large rounded corners of a hero. When it appears
 * it settles once: it eases from a touch closer and dimmer into place while the light comes up. With [parallax] (how
 * far the scene is from the middle of a pager, in pages) the picture lags behind its card while swiping, so the pages
 * have depth; it is read while drawing, so swiping never recomposes. In dark mode the scene is dimmed a little to sit
 * calmly on the dark surface, unless it is already a night scene ([dimInDark] false). Decorative: screen readers skip
 * it, so the text beside it must say what matters.
 */
@Composable
fun NovilaSceneImage(
    painter: Painter,
    modifier: Modifier = Modifier,
    parallax: () -> Float = { 0f },
    dimInDark: Boolean = true,
) {
    val reveal = rememberReveal(durationMillis = 2400)
    val dark = NovilaTheme.colors.surface.luminance() < 0.5f
    val dim = if (dark && dimInDark) DARK_DIM else 0f
    Box(modifier.clip(NovilaTheme.shapes.extraLarge).clearAndSetSemantics {}) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().graphicsLayer {
                val settle = 1f - reveal.value
                val scale = PARALLAX_SCALE + settle * SETTLE_ZOOM
                scaleX = scale
                scaleY = scale
                // The zoom leaves a margin on each side; the picture shifts within it, never past its edge.
                translationX = -parallax().coerceIn(-1f, 1f) * size.width * (PARALLAX_SCALE - 1f) / 2f
            },
        )
        Spacer(
            Modifier.matchParentSize().drawBehind {
                val veil = dim + (1f - reveal.value) * REVEAL_DIM
                if (veil > 0f) drawRect(Color.Black.copy(alpha = veil))
            },
        )
    }
}

/** Drawn this much larger than its card, so it has room to move with the pager. */
private const val PARALLAX_SCALE = 1.12f

/** How much closer the scene starts before it settles. */
private const val SETTLE_ZOOM = 0.06f

/** How dark the scene starts before the light comes up. */
private const val REVEAL_DIM = 0.35f

/** How much day scenes are dimmed in dark mode. */
private const val DARK_DIM = 0.18f
