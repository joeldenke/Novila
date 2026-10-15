package se.novila.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

private val LocalNovilaPillars = staticCompositionLocalOf { NovilaLightColors.pillars() }
private val LocalNovilaAccent = staticCompositionLocalOf { NovilaLightAccent }
private val LocalNovilaHero = staticCompositionLocalOf { NovilaLightHero }
private val LocalNovilaSurfaces = staticCompositionLocalOf { NovilaLightSurfaces }

/**
 * Root theme for the app. Wraps [MaterialTheme] so Material 3 components pick up Novila
 * tokens, and adds tokens Material doesn't have (pillar colors, the sun accent, spacing).
 *
 * @param dynamicColor use the wallpaper colors on Android 12+ instead of the Skog & sol palette.
 *   Off by default: only the brand palette is checked against WCAG 2.2 (ColorContrastTest).
 */
@Composable
fun NovilaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> NovilaDarkColors

        else -> NovilaLightColors
    }
    val pillars = remember(colorScheme) { colorScheme.pillars() }
    CompositionLocalProvider(
        LocalNovilaSpacing provides NovilaSpacing(),
        LocalNovilaPillars provides pillars,
        LocalNovilaAccent provides if (darkTheme) NovilaDarkAccent else NovilaLightAccent,
        LocalNovilaHero provides if (darkTheme) NovilaDarkHero else NovilaLightHero,
        LocalNovilaSurfaces provides if (darkTheme) NovilaDarkSurfaces else NovilaLightSurfaces,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NovilaTypography,
            shapes = NovilaShapes,
            content = content,
        )
    }
}

/** Single entry point for design tokens in feature code. */
object NovilaTheme {
    val colors: ColorScheme
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.colorScheme

    /** Training (pine), sleep (lake blue) and nutrition (falu red), each a full color role. */
    val pillars: NovilaPillarColors
        @Composable @ReadOnlyComposable
        get() = LocalNovilaPillars.current

    /** The midsummer sun, for at most one main call to action per screen. */
    val accent: NovilaAccentColors
        @Composable @ReadOnlyComposable
        get() = LocalNovilaAccent.current

    /** The pine hero card with the sun, for the top of a dashboard. */
    val hero: NovilaHeroColors
        @Composable @ReadOnlyComposable
        get() = LocalNovilaHero.current

    /** Card surface and outline. */
    val surfaces: NovilaSurfaceColors
        @Composable @ReadOnlyComposable
        get() = LocalNovilaSurfaces.current

    val typography: Typography
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.typography

    val shapes: Shapes
        @Composable @ReadOnlyComposable
        get() = MaterialTheme.shapes

    val spacing: NovilaSpacing
        @Composable @ReadOnlyComposable
        get() = LocalNovilaSpacing.current
}
