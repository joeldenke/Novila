package se.novila.wear

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import se.novila.designsystem.theme.HankenGrotesk
import se.novila.designsystem.theme.NovilaTheme
import androidx.wear.compose.material3.ColorScheme as WearColorScheme
import androidx.wear.compose.material3.MaterialTheme as WearMaterialTheme
import androidx.wear.compose.material3.Shapes as WearShapes
import androidx.wear.compose.material3.Typography as WearTypography

/**
 * The watch's theme: Novila's colors, Hanken Grotesk and corner radii, mapped onto Compose for Wear OS Material 3.
 * [NovilaTheme] stays the single source of the tokens, so the watch looks like the phone app rather than the Wear
 * default palette; the Wear roles the watch shows (buttons, countdown numerals, body text) are mapped below.
 */
@Composable
fun NovilaWearTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    NovilaTheme(darkTheme = darkTheme) {
        val colors = NovilaTheme.colors
        val typography = NovilaTheme.typography
        val shapes = NovilaTheme.shapes
        val wearColors = WearColorScheme(
            primary = colors.primary,
            primaryDim = colors.primaryContainer,
            primaryContainer = colors.primaryContainer,
            onPrimary = colors.onPrimary,
            onPrimaryContainer = colors.onPrimaryContainer,
            secondary = colors.secondary,
            secondaryDim = colors.secondaryContainer,
            secondaryContainer = colors.secondaryContainer,
            onSecondary = colors.onSecondary,
            onSecondaryContainer = colors.onSecondaryContainer,
            tertiary = colors.tertiary,
            tertiaryDim = colors.tertiaryContainer,
            tertiaryContainer = colors.tertiaryContainer,
            onTertiary = colors.onTertiary,
            onTertiaryContainer = colors.onTertiaryContainer,
            surfaceContainerLow = colors.surfaceContainerLow,
            surfaceContainer = colors.surfaceContainer,
            surfaceContainerHigh = colors.surfaceContainerHigh,
            onSurface = colors.onSurface,
            onSurfaceVariant = colors.onSurfaceVariant,
            outline = colors.outline,
            outlineVariant = colors.outlineVariant,
            background = colors.background,
            onBackground = colors.onBackground,
            error = colors.error,
            errorDim = colors.errorContainer,
            errorContainer = colors.errorContainer,
            onError = colors.onError,
            onErrorContainer = colors.onErrorContainer,
        )
        val wearTypography = WearTypography(
            defaultFontFamily = HankenGrotesk,
            displayLarge = typography.displayLarge,
            displayMedium = typography.displayMedium,
            displaySmall = typography.displaySmall,
            titleLarge = typography.titleLarge,
            titleMedium = typography.titleMedium,
            titleSmall = typography.titleSmall,
            labelLarge = typography.labelLarge,
            labelMedium = typography.labelMedium,
            labelSmall = typography.labelSmall,
            bodyLarge = typography.bodyLarge,
            bodyMedium = typography.bodyMedium,
            bodySmall = typography.bodySmall,
            numeralExtraLarge = numeral(typography.displayLarge),
            numeralLarge = numeral(typography.displayMedium),
            numeralMedium = numeral(typography.headlineMedium),
            numeralSmall = numeral(typography.titleMedium),
            numeralExtraSmall = numeral(typography.titleSmall),
        )
        val wearShapes = WearShapes(
            extraSmall = shapes.extraSmall,
            small = shapes.small,
            medium = shapes.medium,
            large = shapes.large,
            extraLarge = shapes.extraLarge,
        )
        WearMaterialTheme(colorScheme = wearColors, typography = wearTypography, shapes = wearShapes) {
            content()
        }
    }
}

/** Tabular digits, so a countdown does not jump as the seconds change. */
private fun numeral(style: TextStyle): TextStyle = style.copy(fontFeatureSettings = "tnum")
