package se.novila.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import se.novila.designsystem.theme.NovilaTheme
import kotlin.math.absoluteValue

/**
 * A painted scene (a bitmap in the feature's `drawable-nodpi`) in the large rounded corners of a hero. When it appears
 * it settles once: it eases from a touch closer and dimmer into place while the light comes up. With [parallax] (how
 * far the scene is from the middle of a pager, in pages) the picture lags behind its card while swiping, and the card
 * steps back a little and fades as it leaves, so the pages have depth; it is read while drawing, so swiping never
 * recomposes. In dark mode a day scene turns to dusk (less color and light, not a grey veil) and its lower edge melts
 * into the dark surface, so it never glares; a night scene ([dimInDark] false) keeps its own light and only melts.
 * Decorative: screen readers skip it, so the text beside it must say what matters.
 */
@Composable
fun NovilaSceneImage(
    painter: Painter,
    modifier: Modifier = Modifier,
    parallax: () -> Float = { 0f },
    dimInDark: Boolean = true,
) {
    val reveal = rememberReveal(durationMillis = 2400)
    val surface = NovilaTheme.colors.surface
    val dark = surface.luminance() < 0.5f
    val dusk = remember(dark, dimInDark) { if (dark && dimInDark) duskFilter() else null }
    Box(
        modifier
            .graphicsLayer {
                val away = parallax().absoluteValue.coerceAtMost(1f)
                val scale = 1f - away * STEP_BACK
                scaleX = scale
                scaleY = scale
                alpha = 1f - away * FADE_AWAY
            }
            .clip(NovilaTheme.shapes.extraLarge)
            .clearAndSetSemantics {},
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            colorFilter = dusk,
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
                if (dark) {
                    drawRect(Brush.verticalGradient(MELT_FROM to Color.Transparent, 1f to surface))
                }
                val veil = (1f - reveal.value) * REVEAL_DIM
                if (veil > 0f) drawRect(Color.Black.copy(alpha = veil))
            },
        )
    }
}

/** Dusk for a day scene in dark mode: colors a little quieter and the light lower, as after sunset. */
private fun duskFilter(): ColorFilter {
    val dusk = ColorMatrix().apply { setToSaturation(DUSK_SATURATION) }
    dusk *= ColorMatrix().apply { setToScale(DUSK_LIGHT, DUSK_LIGHT, DUSK_LIGHT * DUSK_BLUE, 1f) }
    return ColorFilter.colorMatrix(dusk)
}

/** Drawn this much larger than its card, so it has room to move with the pager. */
private const val PARALLAX_SCALE = 1.12f

/** How much closer the scene starts before it settles. */
private const val SETTLE_ZOOM = 0.06f

/** How dark the scene starts before the light comes up. */
private const val REVEAL_DIM = 0.35f

/** How much smaller the card is a whole page away, as if it steps back. */
private const val STEP_BACK = 0.08f

/** How much the card fades a whole page away. */
private const val FADE_AWAY = 0.5f

/** Saturation of a day scene in dark mode (1 is unchanged). */
private const val DUSK_SATURATION = 0.8f

/** Light of a day scene in dark mode (1 is unchanged). */
private const val DUSK_LIGHT = 0.8f

/** Blue kept a touch higher than red and green, for the cool of dusk. */
private const val DUSK_BLUE = 1.06f

/** Where, from the top, the scene starts melting into the dark surface. */
private const val MELT_FROM = 0.62f
