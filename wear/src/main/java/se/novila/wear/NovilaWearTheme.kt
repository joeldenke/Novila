package se.novila.wear

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
 * Always Novila's dark palette: Wear OS has no light theme, and a light screen on an OLED watch is glaring and costs
 * battery. Many watches report the light ui mode, so following the system would show the phone's white cards.
 */
@Composable
fun NovilaWearTheme(content: @Composable () -> Unit) {
    NovilaTheme(darkTheme = true) {
        val colors = NovilaTheme.colors
        val typography = NovilaTheme.typography
        val shapes = NovilaTheme.shapes
        val pillars = NovilaTheme.pillars
        val wearColors = WearColorScheme(
            primary = pillars.training.color,
            primaryDim = pillars.training.container,
            primaryContainer = pillars.training.container,
            onPrimary = pillars.training.onColor,
            onPrimaryContainer = pillars.training.onContainer,
            secondary = pillars.sleep.color,
            secondaryDim = pillars.sleep.container,
            secondaryContainer = pillars.sleep.container,
            onSecondary = pillars.sleep.onColor,
            onSecondaryContainer = pillars.sleep.onContainer,
            tertiary = pillars.nutrition.color,
            tertiaryDim = pillars.nutrition.container,
            tertiaryContainer = pillars.nutrition.container,
            onTertiary = pillars.nutrition.onColor,
            onTertiaryContainer = pillars.nutrition.onContainer,
            surfaceContainerLow = colors.surfaceContainerLow,
            surfaceContainer = colors.surfaceContainer,
            surfaceContainerHigh = colors.surfaceContainer,
            onSurface = colors.onSurface,
            onSurfaceVariant = colors.onSurfaceVariant,
            outline = colors.outline,
            outlineVariant = colors.outline,
            background = colors.surface,
            onBackground = colors.onSurface,
            error = colors.error,
            errorDim = colors.error,
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
