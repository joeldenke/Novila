package se.novila.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Raw brand palette, "Skog & sol": the Swedish countryside on a summer morning, in Nordic calm.
 * Pine (training), lake blue (sleep), falu red (nutrition), birch neutrals and the midsummer sun
 * as the one strong accent.
 *
 * Only the theme maps these to roles; UI code reads roles via [NovilaTheme.colors],
 * [NovilaTheme.pillars] and [NovilaTheme.accent], never these values directly.
 * Every role pair is checked against WCAG 2.2 by ColorContrastTest.
 */
internal object NovilaPalette {
    // Pine
    val Pine10 = Color(0xFF0D3220)
    val Pine15 = Color(0xFF1B3F2C)
    val Pine17 = Color(0xFF1A2A1F)
    val Pine20 = Color(0xFF163724)
    val Pine25 = Color(0xFF253A2C)
    val Pine40 = Color(0xFF24533B)
    val Pine45 = Color(0xFF3D6B3A)
    val Pine80 = Color(0xFF9BD1AE)
    val Pine90 = Color(0xFFCDEBD6)
    val Pine95 = Color(0xFFDDEBD9)

    // Lake
    val Lake10 = Color(0xFF0F2E4D)
    val Lake20 = Color(0xFF173556)
    val Lake25 = Color(0xFF1F3047)
    val Lake40 = Color(0xFF2F5885)
    val Lake80 = Color(0xFFA9C7EE)
    val Lake90 = Color(0xFFD6E5F8)
    val Lake95 = Color(0xFFDCE7F5)

    // Falu red
    val Falu10 = Color(0xFF5A1A0E)
    val Falu20 = Color(0xFF5E1A10)
    val Falu25 = Color(0xFF43211A)
    val Falu40 = Color(0xFF9A3324)
    val Falu80 = Color(0xFFFFAA97)
    val Falu90 = Color(0xFFFFDAD1)
    val Falu95 = Color(0xFFFADDD6)

    // Midsummer sun: a fill with dark text, never a text color on light surfaces (1.8:1 on white).
    val Sun = Color(0xFFF2B705)
    val SunDark = Color(0xFFF7C948)
    val OnSun = Color(0xFF2B2100)

    // Error stays a true red so it never reads as nutrition's falu red.
    val Red10 = Color(0xFF410002)
    val Red20 = Color(0xFF690005)
    val Red30 = Color(0xFF93000A)
    val Red40 = Color(0xFFBA1A1A)
    val Red80 = Color(0xFFFFB4AB)
    val Red90 = Color(0xFFFFDAD6)

    // Birch (light) and night forest (dark) neutrals
    val Birch100 = Color(0xFFFFFFFF)
    val Birch97 = Color(0xFFF7F4EC)
    val Birch95 = Color(0xFFF3F0E7)
    val Birch93 = Color(0xFFEEEBE1)
    val Birch91 = Color(0xFFE9E5DA)
    val Birch89 = Color(0xFFE3DFD3)
    val Birch87 = Color(0xFFE6E1D4)
    val Birch80 = Color(0xFFCFCABD)
    val Moss50 = Color(0xFF787D72)
    val Moss35 = Color(0xFF545A4C)
    val Bark20 = Color(0xFF30332B)
    val Bark10 = Color(0xFF1B1E16)

    val Night4 = Color(0xFF0C0E0B)
    val Night6 = Color(0xFF111410)
    val Night9 = Color(0xFF171A15)
    val Night11 = Color(0xFF1A1E18)
    val Night14 = Color(0xFF232720)
    val Night18 = Color(0xFF2D322A)
    val Night20 = Color(0xFF2E3329)
    val Night26 = Color(0xFF3E4439)
    val Night55 = Color(0xFF878D80)
    val Night72 = Color(0xFFB1B6A8)
    val Night90 = Color(0xFFE3E4DC)
    val Night93 = Color(0xFFECEDE6)
    val Night95 = Color(0xFFF1EFE7)
    val Night19 = Color(0xFF2E312A)
}

internal val NovilaLightColors: ColorScheme = with(NovilaPalette) {
    lightColorScheme(
        primary = Pine40,
        onPrimary = Birch100,
        primaryContainer = Pine95,
        onPrimaryContainer = Pine20,
        inversePrimary = Pine80,
        secondary = Lake40,
        onSecondary = Birch100,
        secondaryContainer = Lake95,
        onSecondaryContainer = Lake20,
        tertiary = Falu40,
        onTertiary = Birch100,
        tertiaryContainer = Falu95,
        onTertiaryContainer = Falu20,
        error = Red40,
        onError = Birch100,
        errorContainer = Red90,
        onErrorContainer = Red10,
        background = Birch97,
        onBackground = Bark10,
        surface = Birch97,
        onSurface = Bark10,
        surfaceVariant = Birch87,
        onSurfaceVariant = Moss35,
        surfaceTint = Pine40,
        surfaceContainerLowest = Birch100,
        surfaceContainerLow = Birch95,
        surfaceContainer = Birch93,
        surfaceContainerHigh = Birch91,
        surfaceContainerHighest = Birch89,
        inverseSurface = Bark20,
        inverseOnSurface = Night95,
        outline = Moss50,
        outlineVariant = Birch80,
    )
}

internal val NovilaDarkColors: ColorScheme = with(NovilaPalette) {
    darkColorScheme(
        primary = Pine80,
        onPrimary = Pine10,
        primaryContainer = Pine25,
        onPrimaryContainer = Pine90,
        inversePrimary = Pine40,
        secondary = Lake80,
        onSecondary = Lake10,
        secondaryContainer = Lake25,
        onSecondaryContainer = Lake90,
        tertiary = Falu80,
        onTertiary = Falu10,
        tertiaryContainer = Falu25,
        onTertiaryContainer = Falu90,
        error = Red80,
        onError = Red20,
        errorContainer = Red30,
        onErrorContainer = Red90,
        background = Night6,
        onBackground = Night93,
        surface = Night6,
        onSurface = Night93,
        surfaceVariant = Night20,
        onSurfaceVariant = Night72,
        surfaceTint = Pine80,
        surfaceContainerLowest = Night4,
        surfaceContainerLow = Night9,
        surfaceContainer = Night11,
        surfaceContainerHigh = Night14,
        surfaceContainerHighest = Night18,
        inverseSurface = Night90,
        inverseOnSurface = Night19,
        outline = Night55,
        outlineVariant = Night26,
    )
}

/** A color with its content color and a softer container pair, like a Material 3 color role. */
@Immutable
data class NovilaColorRole(
    val color: Color,
    val onColor: Color,
    val container: Color,
    val onContainer: Color,
)

/** Each part of the app has its own color: training is pine, sleep is lake blue, nutrition is falu red. */
@Immutable
data class NovilaPillarColors(
    val training: NovilaColorRole,
    val sleep: NovilaColorRole,
    val nutrition: NovilaColorRole,
)

/** The midsummer sun: the one strong accent, for the main call to action on a screen. */
@Immutable
data class NovilaAccentColors(
    val sun: Color,
    val onSun: Color,
)

/** Pillars follow the scheme's roles, so they also adapt when dynamic color is on. */
internal fun ColorScheme.pillars() = NovilaPillarColors(
    training = NovilaColorRole(primary, onPrimary, primaryContainer, onPrimaryContainer),
    sleep = NovilaColorRole(secondary, onSecondary, secondaryContainer, onSecondaryContainer),
    nutrition = NovilaColorRole(tertiary, onTertiary, tertiaryContainer, onTertiaryContainer),
)

internal val NovilaLightAccent = NovilaAccentColors(sun = NovilaPalette.Sun, onSun = NovilaPalette.OnSun)
internal val NovilaDarkAccent = NovilaAccentColors(sun = NovilaPalette.SunDark, onSun = NovilaPalette.OnSun)

/**
 * The pine hero card at the top of a dashboard: a vertical-ish gradient from [start] to [end],
 * [content] for its text, the [sun] for its emblem and pillar-tinted progress segments on [track].
 */
@Immutable
data class NovilaHeroColors(
    val start: Color,
    val end: Color,
    val content: Color,
    val track: Color,
    val sun: Color,
    val training: Color,
    val sleep: Color,
    val nutrition: Color,
)

/** Cards sit on the birch background as a lighter surface with a hairline outline. */
@Immutable
data class NovilaSurfaceColors(
    val card: Color,
    val cardOutline: Color,
)

internal val NovilaLightHero = with(NovilaPalette) {
    NovilaHeroColors(
        start = Pine15,
        end = Pine45,
        content = Birch100,
        track = Birch100.copy(alpha = 0.25f),
        sun = Sun,
        training = Pine80,
        sleep = Lake80,
        nutrition = Falu80,
    )
}

internal val NovilaDarkHero = with(NovilaPalette) {
    NovilaHeroColors(
        start = Pine17,
        end = Pine25,
        content = Night93,
        track = Night93.copy(alpha = 0.18f),
        sun = SunDark,
        training = Pine80,
        sleep = Lake80,
        nutrition = Falu80,
    )
}

internal val NovilaLightSurfaces = with(NovilaPalette) { NovilaSurfaceColors(card = Birch100, cardOutline = Birch87) }
internal val NovilaDarkSurfaces = with(NovilaPalette) { NovilaSurfaceColors(card = Night11, cardOutline = Night14) }
