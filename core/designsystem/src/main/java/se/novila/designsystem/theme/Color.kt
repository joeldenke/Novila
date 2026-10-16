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

/**
 * The anatomy figure in exercise illustrations: a calm lake blue [body] (parts on the far side in
 * [bodyShade]) with an [outline], and the muscles an exercise trains in warm ember. Main muscles are
 * filled with [musclePrimary]; supporting muscles get a [muscleSecondary] fill edged in [musclePrimary],
 * so the two read apart by shape as well as color.
 */
@Immutable
data class NovilaAnatomyColors(
    val body: Color,
    val bodyShade: Color,
    val outline: Color,
    val shadow: Color,
    val musclePrimary: Color,
    val muscleSecondary: Color,
)

internal val NovilaLightAnatomy = with(NovilaPalette) {
    NovilaAnatomyColors(
        body = Lake95,
        bodyShade = Lake85,
        outline = Lake40,
        shadow = Lake40.copy(alpha = 0.14f),
        musclePrimary = Ember40,
        muscleSecondary = Ember90,
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
    )
}

/**
 * The drawn countryside in [se.novila.designsystem.component.NovilaIllustration]: skies by day, at dawn and at
 * night, hills and forest from far to near, the lake, a meadow with its trail, and a falu red cottage. Decorative only,
 * with no text on top, so these are not contrast pairs. Dark mode turns the same scenes towards dusk.
 */
@Immutable
internal data class NovilaSceneryColors(
    val skyTop: Color,
    val skyBottom: Color,
    val dawnTop: Color,
    val dawnBottom: Color,
    val nightTop: Color,
    val nightBottom: Color,
    val sun: Color,
    val moon: Color,
    val star: Color,
    val farHills: Color,
    val hills: Color,
    val forest: Color,
    val nearForest: Color,
    val nightHills: Color,
    val nightForest: Color,
    val meadow: Color,
    val water: Color,
    val nightWater: Color,
    val shine: Color,
    val trail: Color,
    val footprint: Color,
    val cottage: Color,
    val trim: Color,
    val roof: Color,
    val pane: Color,
    val leaves: Color,
    val berry: Color,
)

internal val NovilaLightScenery = with(NovilaPalette) {
    NovilaSceneryColors(
        skyTop = Color(0xFFCFE2F3),
        skyBottom = Color(0xFFF6F1E4),
        dawnTop = Color(0xFFBFD6EA),
        dawnBottom = Color(0xFFFBE3BC),
        nightTop = Color(0xFF0B1A33),
        nightBottom = Lake40,
        sun = Sun,
        moon = Color(0xFFF6EED2),
        star = Birch100,
        farHills = Color(0xFFB4CBBF),
        hills = Color(0xFF6E9C7E),
        forest = Pine40,
        nearForest = Pine15,
        nightHills = Color(0xFF1F3550),
        nightForest = Color(0xFF0E2236),
        meadow = Color(0xFFA9CB8F),
        water = Color(0xFF9DBEDD),
        nightWater = Lake20,
        shine = Birch100,
        trail = Color(0xFFE8D7AE),
        footprint = Color(0xFF8A6B3F),
        cottage = Falu40,
        trim = Birch100,
        roof = Bark20,
        pane = Lake40,
        leaves = Pine45,
        berry = Color(0xFFC8331F),
    )
}

internal val NovilaDarkScenery = with(NovilaPalette) {
    NovilaSceneryColors(
        skyTop = Color(0xFF1B2A3A),
        skyBottom = Color(0xFF3B4250),
        dawnTop = Color(0xFF14233A),
        dawnBottom = Color(0xFF5A4630),
        nightTop = Color(0xFF050B16),
        nightBottom = Lake25,
        sun = SunDark,
        moon = Color(0xFFEDE3C4),
        star = Night90,
        farHills = Color(0xFF2C3D3C),
        hills = Color(0xFF27402F),
        forest = Color(0xFF17301F),
        nearForest = Color(0xFF0C1A11),
        nightHills = Color(0xFF14243A),
        nightForest = Color(0xFF070F1A),
        meadow = Color(0xFF2E4A2A),
        water = Color(0xFF22364F),
        nightWater = Color(0xFF101E30),
        shine = Night90,
        trail = Color(0xFF6B5B3C),
        footprint = Color(0xFF2F2616),
        cottage = Color(0xFF8A2E20),
        trim = Night90,
        roof = Color(0xFF141712),
        // Lit windows at dusk.
        pane = SunDark,
        leaves = Color(0xFF2F5530),
        berry = Color(0xFFE0573F),
    )
}
