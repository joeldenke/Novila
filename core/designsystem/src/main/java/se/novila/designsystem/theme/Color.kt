package se.novila.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

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
    val Falu17 = Color(0xFF2E1A16)
    val Falu20 = Color(0xFF5E1A10)
    val Falu25 = Color(0xFF43211A)
    val Falu40 = Color(0xFF9A3324)
    val Falu80 = Color(0xFFFFAA97)
    val Falu90 = Color(0xFFFFDAD1)
    val Falu95 = Color(0xFFFADDD6)

    // Lake tones for the anatomy figure's body.
    val Lake85 = Color(0xFFC2D4EC)
    val Lake22 = Color(0xFF22344D)
    val Lake16 = Color(0xFF1A283C)

    // Ember: the sun's warmth, deep enough to mark working muscles on the lake blue body.
    val Ember40 = Color(0xFFBC4C08)
    val Ember80 = Color(0xFFFF9A52)
    val Ember90 = Color(0xFFF8D3B4)
    val Ember20 = Color(0xFF5C3218)

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

/**
 * The neutral color roles UI code reads through [NovilaTheme.colors]: text, surfaces, outline and error. Novila's
 * own type, so nothing outside :core:designsystem sees Material 3. The theme picks them from the active
 * [ColorScheme] (also with dynamic color); add a role here when a screen needs one.
 */
@Immutable
data class NovilaColors(
    val surface: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val outline: Color,
    val error: Color,
)

internal fun ColorScheme.toNovilaColors() = NovilaColors(
    surface = surface,
    onSurface = onSurface,
    onSurfaceVariant = onSurfaceVariant,
    surfaceContainerLow = surfaceContainerLow,
    surfaceContainer = surfaceContainer,
    outline = outline,
    error = error,
)

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
    training = role(primary, onPrimary, primaryContainer, onPrimaryContainer),
    sleep = role(secondary, onSecondary, secondaryContainer, onSecondaryContainer),
    nutrition = role(tertiary, onTertiary, tertiaryContainer, onTertiaryContainer),
)

/**
 * The Skog & sol palette always passes ColorContrastTest, but wallpaper colors don't have to: a dark
 * dynamic scheme can give a light container where the pillar's (also light) icon and chart vanish.
 * Then the container becomes the pillar color as a soft tint on the surface, with readable text on it.
 */
private fun ColorScheme.role(color: Color, onColor: Color, container: Color, onContainer: Color): NovilaColorRole {
    if (contrast(color, container) >= MIN_ICON_CONTRAST) return NovilaColorRole(color, onColor, container, onContainer)
    val tinted = color.copy(alpha = TINT_ALPHA).compositeOver(surface)
    val text = if (contrast(onContainer, tinted) >= MIN_TEXT_CONTRAST) onContainer else onSurface
    return NovilaColorRole(color, onColor, tinted, text)
}

private const val MIN_ICON_CONTRAST = 3.0
private const val MIN_TEXT_CONTRAST = 4.5
private const val TINT_ALPHA = 0.16f

/** WCAG 2.x contrast ratio between two opaque colors. */
internal fun contrast(a: Color, b: Color): Double {
    val la = a.luminance().toDouble()
    val lb = b.luminance().toDouble()
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}

internal val NovilaLightAccent = NovilaAccentColors(sun = NovilaPalette.Sun, onSun = NovilaPalette.OnSun)
internal val NovilaDarkAccent = NovilaAccentColors(sun = NovilaPalette.SunDark, onSun = NovilaPalette.OnSun)

/**
 * The pine hero card at the top of a dashboard: a vertical-ish gradient from [start] to [end],
 * [content] for its text, the [sun] for its emblem and pillar-tinted progress segments on [track].
 * Sleep's hero panel is a night lake from [sleepStart] to [sleepEnd], Nutrition's falu red from [nutritionStart]
 * to [nutritionEnd], with the same content, track and sun.
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
    val sleepStart: Color,
    val sleepEnd: Color,
    val nutritionStart: Color,
    val nutritionEnd: Color,
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
        sleepStart = Lake10,
        sleepEnd = Lake40,
        nutritionStart = Falu10,
        nutritionEnd = Falu40,
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
        sleepStart = Lake16,
        sleepEnd = Lake25,
        nutritionStart = Falu17,
        nutritionEnd = Falu25,
    )
}

internal val NovilaLightSurfaces = with(NovilaPalette) { NovilaSurfaceColors(card = Birch100, cardOutline = Birch87) }
internal val NovilaDarkSurfaces = with(NovilaPalette) { NovilaSurfaceColors(card = Night11, cardOutline = Night14) }

/**
 * The anatomy figure in exercise illustrations: a calm lake blue [body] (parts on the far side in
 * [bodyShade]) with an [outline], and the muscles an exercise trains in warm ember. Main muscles are
 * filled with [musclePrimary]; supporting muscles get a [muscleSecondary] fill edged in [musclePrimary],
 * so the two read apart by shape as well as color. Equipment is drawn in quiet iron: handles and bars in
 * [gear], weight plates in the darker [weight], and resistance bands in pine ([band]).
 */
@Immutable
data class NovilaAnatomyColors(
    val body: Color,
    val bodyShade: Color,
    val outline: Color,
    val shadow: Color,
    val musclePrimary: Color,
    val muscleSecondary: Color,
    val gear: Color,
    val weight: Color,
    val band: Color,
)

internal val NovilaLightAnatomy = with(NovilaPalette) {
    NovilaAnatomyColors(
        body = Lake95,
        bodyShade = Lake85,
        outline = Lake40,
        shadow = Lake40.copy(alpha = 0.14f),
        musclePrimary = Ember40,
        muscleSecondary = Ember90,
        gear = Moss35,
        weight = Bark20,
        band = Pine45,
    )
}

internal val NovilaDarkAnatomy = with(NovilaPalette) {
    NovilaAnatomyColors(
        body = Lake22,
        bodyShade = Lake16,
        outline = Lake80,
        shadow = Lake80.copy(alpha = 0.12f),
        musclePrimary = Ember80,
        muscleSecondary = Ember20,
        gear = Night55,
        weight = Night72,
        band = Pine80,
    )
}
