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
    private val heroes = mapOf("light" to NovilaLightHero, "dark" to NovilaDarkHero)
    private val anatomy = mapOf(
        "light" to (NovilaLightSurfaces to NovilaLightAnatomy),
        "dark" to (NovilaDarkSurfaces to NovilaDarkAnatomy),
    )
    private val surfaces = mapOf(
        "light" to (NovilaLightColors to NovilaLightSurfaces),
        "dark" to (NovilaDarkColors to NovilaDarkSurfaces),
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

    @Test fun `pillars stay readable when dynamic color gives a light container in dark mode`() {
        // Like a wallpaper scheme on Android 16: light cyan tertiary container under a light tertiary.
        val scheme = NovilaDarkColors.copy(
            tertiary = Color(0xFF80D4DE),
            tertiaryContainer = Color(0xFFADE3EA),
            onTertiaryContainer = Color(0xFF00363D),
        )
        val nutrition = scheme.pillars().nutrition
        val failures = listOf(
            "icon" to contrast(nutrition.color, nutrition.container) to NON_TEXT,
            "text" to contrast(nutrition.onContainer, nutrition.container) to TEXT,
        ).filter { (pair, minimum) -> pair.second < minimum }
        assertTrue("Below minimum -> $failures", failures.isEmpty())
    }

    @Test fun `text on cards meets 4,5 to 1`() = assertPairs(TEXT, surfaces) { (scheme, card) ->
        listOf(
            "onSurface/card" to (scheme.onSurface to card.card),
            "onSurfaceVariant/card" to (scheme.onSurfaceVariant to card.card),
        )
    }

    @Test fun `hero text meets 4,5 to 1 across its gradient`() = assertPairs(TEXT, heroes) { hero ->
        listOf(
            "content/start" to (hero.content to hero.start),
            "content/end" to (hero.content to hero.end),
            "content/sleepStart" to (hero.content to hero.sleepStart),
            "content/sleepEnd" to (hero.content to hero.sleepEnd),
            "content/nutritionStart" to (hero.content to hero.nutritionStart),
            "content/nutritionEnd" to (hero.content to hero.nutritionEnd),
        )
    }

    @Test fun `hero sun and progress stand out across its gradient`() = assertPairs(NON_TEXT, heroes) { hero ->
        listOf(hero.start, hero.end).flatMap { ground ->
            listOf(
                "sun" to (hero.sun to ground),
                "training" to (hero.training to ground),
                "sleep" to (hero.sleep to ground),
                "nutrition" to (hero.nutrition to ground),
            )
        } + ("check/training" to (hero.start to hero.training)) // A done day in NovilaWeekStrip.
    }

    // Sleep's and Nutrition's panels: the part's own bars and ring, and the sun for the shown day.
    @Test fun `sleep and nutrition heroes keep their progress and sun apart`() = assertPairs(NON_TEXT, heroes) { hero ->
        listOf(hero.sleepStart, hero.sleepEnd).flatMap { ground ->
            listOf("sleep" to (hero.sleep to ground), "sun/sleep" to (hero.sun to ground))
        } + listOf(hero.nutritionStart, hero.nutritionEnd).flatMap { ground ->
            listOf("nutrition" to (hero.nutrition to ground), "sun/nutrition" to (hero.sun to ground))
        }
    }

    @Test fun `anatomy muscles and outline stand out`() = assertPairs(NON_TEXT, anatomy) { (card, figure) ->
        listOf(
            "musclePrimary/body" to (figure.musclePrimary to figure.body),
            "musclePrimary/bodyShade" to (figure.musclePrimary to figure.bodyShade),
            "musclePrimary/muscleSecondary" to (figure.musclePrimary to figure.muscleSecondary),
            "outline/body" to (figure.outline to figure.body),
            "outline/bodyShade" to (figure.outline to figure.bodyShade),
            "outline/card" to (figure.outline to card.card),
            "gear/card" to (figure.gear to card.card),
            "gear/body" to (figure.gear to figure.body),
            "weight/card" to (figure.weight to card.card),
            "weight/body" to (figure.weight to figure.body),
            "band/card" to (figure.band to card.card),
            "band/body" to (figure.band to figure.body),
        )
    }

    private fun <T> assertPairs(
        minimum: Double,
        themes: Map<String, T>,
        pairs: (T) -> List<Pair<String, Pair<Color, Color>>>,
    ) {
        val failures = themes.flatMap { (mode, theme) ->
            pairs(theme).mapNotNull { (name, colors) ->
                val ratio = contrast(colors.first, colors.second)
                if (ratio < minimum) "$mode $name: ${"%.2f".format(ratio)}:1" else null
            }
        }
        assertTrue("Below $minimum:1 -> $failures", failures.isEmpty())
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
