package se.novila.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale. Use these instead of raw dp values in feature code. */
@Immutable
data class NovilaSpacing(
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val s: Dp = 8.dp,
    val m: Dp = 12.dp,
    val l: Dp = 16.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    /** Horizontal margin between screen edge and content. */
    val screenMargin: Dp = 16.dp,
)

internal val LocalNovilaSpacing = staticCompositionLocalOf { NovilaSpacing() }

/**
 * Corner radii from one base of 14 dp, restrained for Nordic calm: chips and small controls
 * 10 dp, cards and buttons 14 dp, hero cards 18 dp, sheets and dialogs 26 dp.
 */
internal val NovilaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(26.dp),
)
