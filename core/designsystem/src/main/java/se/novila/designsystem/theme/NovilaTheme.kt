package se.novila.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import se.novila.designsystem.component.LocalNovilaEntryLayout
import se.novila.designsystem.component.NovilaEntryLayout

private val LocalNovilaColors = staticCompositionLocalOf { NovilaLightColors.toNovilaColors() }
private val LocalNovilaTypography = staticCompositionLocalOf { NovilaTextStyles }
private val LocalNovilaShapes = staticCompositionLocalOf { NovilaCorners }
private val LocalNovilaPillars = staticCompositionLocalOf { NovilaLightColors.pillars() }
private val LocalNovilaAccent = staticCompositionLocalOf { NovilaLightAccent }
private val LocalNovilaHero = staticCompositionLocalOf { NovilaLightHero }
private val LocalNovilaSurfaces = staticCompositionLocalOf { NovilaLightSurfaces }
private val LocalNovilaAnatomy = staticCompositionLocalOf { NovilaLightAnatomy }

/**
 * Root theme for the app. Provides Novila's own tokens (colors, type, shapes, pillars, the sun accent, spacing) and
 * wraps [MaterialTheme] with the same values, so the Material 3 components inside :core:designsystem pick them up.
 * Material 3 is an implementation detail of this module: no public type is a Material type, so it can be replaced
 * component by component.
 *
 * @param dynamicColor use the wallpaper colors on Android 12+ instead of the Skog & sol palette.
 *   Off by default: only the brand palette is checked against WCAG 2.2 (ColorContrastTest).
 * @param entryLayout how tabs show the ways into their parts (`NovilaEntries`), tiles or rows, as chosen in Settings.
 */
@Composable
fun NovilaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    entryLayout: NovilaEntryLayout = NovilaEntryLayout.Grid,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        darkTheme -> NovilaDarkColors

        else -> NovilaLightColors
    }
    val colors = remember(colorScheme) { colorScheme.toNovilaColors() }
    val pillars = remember(colorScheme) { colorScheme.pillars() }
    CompositionLocalProvider(
        LocalNovilaColors provides colors,
        LocalNovilaTypography provides NovilaTextStyles,
        LocalNovilaShapes provides NovilaCorners,
        LocalNovilaSpacing provides NovilaSpacing(),
        LocalNovilaPillars provides pillars,
        LocalNovilaAccent provides if (darkTheme) NovilaDarkAccent else NovilaLightAccent,
        LocalNovilaHero provides if (darkTheme) NovilaDarkHero else NovilaLightHero,
        LocalNovilaSurfaces provides if (darkTheme) NovilaDarkSurfaces else NovilaLightSurfaces,
        LocalNovilaAnatomy provides if (darkTheme) NovilaDarkAnatomy else NovilaLightAnatomy,
        LocalNovilaEntryLayout provides entryLayout,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTypography,
            shapes = MaterialShapes,
            content = content,
        )
    }
}

private val MaterialTypography = NovilaTextStyles.toMaterial()
private val MaterialShapes = NovilaCorners.toMaterial()

/** Single entry point for design tokens in feature code. */
object NovilaTheme {
    /** Text, surfaces, outline and error. */
    val colors: NovilaColors
        @Composable @ReadOnlyComposable
        get() = LocalNovilaColors.current

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

    /** The anatomy figure in exercise illustrations: lake blue body, ember muscles. */
    val anatomy: NovilaAnatomyColors
        @Composable @ReadOnlyComposable
        get() = LocalNovilaAnatomy.current

    val typography: NovilaTypography
        @Composable @ReadOnlyComposable
        get() = LocalNovilaTypography.current

    val shapes: NovilaShapes
        @Composable @ReadOnlyComposable
        get() = LocalNovilaShapes.current

    val spacing: NovilaSpacing
        @Composable @ReadOnlyComposable
        get() = LocalNovilaSpacing.current

    /** Tiles or rows for the ways into a tab's parts, as the user chose in Settings. */
    val entryLayout: NovilaEntryLayout
        @Composable @ReadOnlyComposable
        get() = LocalNovilaEntryLayout.current
}
