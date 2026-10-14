package se.novila.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * Root theme for the app. Wraps [MaterialTheme] so Material 3 components pick up Novila
 * tokens, and adds tokens Material doesn't have (spacing).
 *
 * Brand colors are fixed on purpose (no dynamic color) to keep a consistent identity.
 */
@Composable
fun NovilaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalNovilaSpacing provides NovilaSpacing()) {
        MaterialTheme(
            colorScheme = if (darkTheme) NovilaDarkColors else NovilaLightColors,
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
