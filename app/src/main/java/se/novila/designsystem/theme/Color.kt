package se.novila.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Raw brand palette. Only the theme maps these to roles; UI code reads roles via
 * [NovilaTheme.colors], never these values directly.
 *
 * Nordic palette: fjord (primary), moss (secondary), amber (tertiary).
 */
internal object NovilaPalette {
    val Fjord10 = Color(0xFF001F2A)
    val Fjord20 = Color(0xFF003546)
    val Fjord30 = Color(0xFF004D65)
    val Fjord40 = Color(0xFF006685)
    val Fjord80 = Color(0xFF6BD3FA)
    val Fjord90 = Color(0xFFBFE9FF)

    val Moss10 = Color(0xFF0D200F)
    val Moss20 = Color(0xFF223523)
    val Moss30 = Color(0xFF384B38)
    val Moss40 = Color(0xFF4F634F)
    val Moss80 = Color(0xFFB6CDB4)
    val Moss90 = Color(0xFFD2E9CF)

    val Amber10 = Color(0xFF2A1800)
    val Amber20 = Color(0xFF462A00)
    val Amber30 = Color(0xFF643F00)
    val Amber40 = Color(0xFF835400)
    val Amber80 = Color(0xFFFFB95C)
    val Amber90 = Color(0xFFFFDDB6)

    val Red10 = Color(0xFF410002)
    val Red20 = Color(0xFF690005)
    val Red30 = Color(0xFF93000A)
    val Red40 = Color(0xFFBA1A1A)
    val Red80 = Color(0xFFFFB4AB)
    val Red90 = Color(0xFFFFDAD6)

    val Neutral4 = Color(0xFF0B0F11)
    val Neutral6 = Color(0xFF101416)
    val Neutral10 = Color(0xFF191C1E)
    val Neutral12 = Color(0xFF1D2022)
    val Neutral17 = Color(0xFF272A2D)
    val Neutral22 = Color(0xFF323538)
    val Neutral24 = Color(0xFF363A3C)
    val Neutral87 = Color(0xFFD8DADD)
    val Neutral90 = Color(0xFFE1E3E5)
    val Neutral92 = Color(0xFFE7E8EA)
    val Neutral94 = Color(0xFFECEEF0)
    val Neutral96 = Color(0xFFF2F4F6)
    val Neutral98 = Color(0xFFF8F9FB)
    val Neutral100 = Color(0xFFFFFFFF)

    val NeutralVariant30 = Color(0xFF40484C)
    val NeutralVariant50 = Color(0xFF70787D)
    val NeutralVariant60 = Color(0xFF8A9297)
    val NeutralVariant80 = Color(0xFFC0C8CD)
    val NeutralVariant90 = Color(0xFFDCE4E9)
}

internal val NovilaLightColors: ColorScheme = with(NovilaPalette) {
    lightColorScheme(
        primary = Fjord40,
        onPrimary = Neutral100,
        primaryContainer = Fjord90,
        onPrimaryContainer = Fjord10,
        secondary = Moss40,
        onSecondary = Neutral100,
        secondaryContainer = Moss90,
        onSecondaryContainer = Moss10,
        tertiary = Amber40,
        onTertiary = Neutral100,
        tertiaryContainer = Amber90,
        onTertiaryContainer = Amber10,
        error = Red40,
        onError = Neutral100,
        errorContainer = Red90,
        onErrorContainer = Red10,
        background = Neutral98,
        onBackground = Neutral10,
        surface = Neutral98,
        onSurface = Neutral10,
        surfaceVariant = NeutralVariant90,
        onSurfaceVariant = NeutralVariant30,
        surfaceContainerLowest = Neutral100,
        surfaceContainerLow = Neutral96,
        surfaceContainer = Neutral94,
        surfaceContainerHigh = Neutral92,
        surfaceContainerHighest = Neutral90,
        outline = NeutralVariant50,
        outlineVariant = NeutralVariant80,
    )
}

internal val NovilaDarkColors: ColorScheme = with(NovilaPalette) {
    darkColorScheme(
        primary = Fjord80,
        onPrimary = Fjord20,
        primaryContainer = Fjord30,
        onPrimaryContainer = Fjord90,
        secondary = Moss80,
        onSecondary = Moss20,
        secondaryContainer = Moss30,
        onSecondaryContainer = Moss90,
        tertiary = Amber80,
        onTertiary = Amber20,
        tertiaryContainer = Amber30,
        onTertiaryContainer = Amber90,
        error = Red80,
        onError = Red20,
        errorContainer = Red30,
        onErrorContainer = Red90,
        background = Neutral6,
        onBackground = Neutral90,
        surface = Neutral6,
        onSurface = Neutral90,
        surfaceVariant = NeutralVariant30,
        onSurfaceVariant = NeutralVariant80,
        surfaceContainerLowest = Neutral4,
        surfaceContainerLow = Neutral10,
        surfaceContainer = Neutral12,
        surfaceContainerHigh = Neutral17,
        surfaceContainerHighest = Neutral22,
        outline = NeutralVariant60,
        outlineVariant = NeutralVariant30,
    )
}
