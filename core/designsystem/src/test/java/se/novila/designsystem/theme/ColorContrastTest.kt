package se.novila.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG 2.2 contrast for every color role pair the app relies on, in light and dark:
 * 4.5:1 for text (1.4.3) and 3:1 for UI components and icons (1.4.11).
 * Plain JVM test: no Android or Robolectric needed.
 */
class ColorContrastTest {
    private val schemes = mapOf(
        "light" to (NovilaLightColors to NovilaLightAccent),
        "dark" to (NovilaDarkColors to NovilaDarkAccent),
    )

    @Test fun `text on its role meets 4,5 to 1`() = assertAll(TEXT) { scheme, accent ->
        listOf(
            "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
            "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
            "onSecondary/secondary" to (scheme.onSecondary to scheme.secondary),
            "onSecondaryContainer/secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
            "onTertiary/tertiary" to (scheme.onTertiary to scheme.tertiary),
            "onTertiaryContainer/tertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
            "onError/error" to (scheme.onError to scheme.error),
            "onErrorContainer/errorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
            "onBackground/background" to (scheme.onBackground to scheme.background),
            "inverseOnSurface/inverseSurface" to (scheme.inverseOnSurface to scheme.inverseSurface),
            "inversePrimary/inverseSurface" to (scheme.inversePrimary to scheme.inverseSurface),
            "onSun/sun" to (accent.onSun to accent.sun),
        ) + scheme.surfaces().flatMap { (name, surface) ->
            listOf(
                "onSurface/$name" to (scheme.onSurface to surface),
                "onSurfaceVariant/$name" to (scheme.onSurfaceVariant to surface),
            )
        }
    }

    @Test fun `accents and outlines on surfaces meet 3 to 1`() = assertAll(NON_TEXT) { scheme, _ ->
        scheme.surfaces().flatMap { (name, surface) ->
            listOf(
                "outline/$name" to (scheme.outline to surface),
                "primary/$name" to (scheme.primary to surface),
                "secondary/$name" to (scheme.secondary to surface),
                "tertiary/$name" to (scheme.tertiary to surface),
                "error/$name" to (scheme.error to surface),
            )
        }
    }

    @Test fun `pillar icons stand out on their containers`() = assertAll(NON_TEXT) { scheme, _ ->
        val pillars = scheme.pillars()
        listOf(
            "training" to (pillars.training.color to pillars.training.container),
            "sleep" to (pillars.sleep.color to pillars.sleep.container),
            "nutrition" to (pillars.nutrition.color to pillars.nutrition.container),
        )
    }

    private fun assertAll(
        minimum: Double,
        pairs: (ColorScheme, NovilaAccentColors) -> List<Pair<String, Pair<Color, Color>>>,
    ) {
        val failures = schemes.flatMap { (mode, theme) ->
            pairs(theme.first, theme.second).mapNotNull { (name, colors) ->
                val ratio = contrast(colors.first, colors.second)
                if (ratio < minimum) "$mode $name: ${"%.2f".format(ratio)}:1" else null
            }
        }
        assertTrue("Below $minimum:1 -> $failures", failures.isEmpty())
    }

    private fun ColorScheme.surfaces() = listOf(
        "surface" to surface,
        "surfaceContainerLowest" to surfaceContainerLowest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
    )

    private companion object {
        const val TEXT = 4.5
        const val NON_TEXT = 3.0

        /** WCAG 2.x contrast ratio from sRGB relative luminance. */
        fun contrast(a: Color, b: Color): Double {
            val la = luminance(a)
            val lb = luminance(b)
            return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
        }

        fun luminance(color: Color): Double {
            fun channel(c: Float): Double = c.toDouble().let {
                if (it <=
                    0.04045
                ) {
                    it / 12.92
                } else {
                    ((it + 0.055) / 1.055).pow(2.4)
                }
            }
            return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
        }
    }
}
