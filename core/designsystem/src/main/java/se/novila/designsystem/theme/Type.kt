package se.novila.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import se.novila.designsystem.R

/**
 * Hanken Grotesk, a Google Font (SIL Open Font License, see assets/licenses), bundled rather than
 * downloaded: it works offline, renders the same in Robolectric screenshots as on devices, and
 * keeps the app free of network requests. Static weights cut from the variable font.
 */
val HankenGrotesk = FontFamily(
    Font(R.font.hanken_grotesk_regular, FontWeight.Normal),
    Font(R.font.hanken_grotesk_medium, FontWeight.Medium),
    Font(R.font.hanken_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.hanken_grotesk_bold, FontWeight.Bold),
)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    letterSpacing: Double,
) = TextStyle(
    fontFamily = HankenGrotesk,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
)

/**
 * All 15 Material 3 type roles in Hanken Grotesk, tuned for Nordic calm: Material's sizes, lighter
 * headings (SemiBold, never ExtraBold) with slightly tighter tracking, regular body text and
 * SemiBold labels so buttons and chips read clearly.
 */
internal val NovilaTypography = Typography(
    displayLarge = style(size = 57, lineHeight = 64, weight = FontWeight.SemiBold, letterSpacing = -0.5),
    displayMedium = style(size = 45, lineHeight = 52, weight = FontWeight.SemiBold, letterSpacing = -0.25),
    displaySmall = style(size = 36, lineHeight = 44, weight = FontWeight.SemiBold, letterSpacing = -0.25),
    headlineLarge = style(size = 32, lineHeight = 40, weight = FontWeight.SemiBold, letterSpacing = -0.25),
    headlineMedium = style(size = 28, lineHeight = 36, weight = FontWeight.SemiBold, letterSpacing = -0.2),
    headlineSmall = style(size = 24, lineHeight = 32, weight = FontWeight.SemiBold, letterSpacing = -0.1),
    titleLarge = style(size = 22, lineHeight = 28, weight = FontWeight.SemiBold, letterSpacing = 0.0),
    titleMedium = style(size = 16, lineHeight = 24, weight = FontWeight.SemiBold, letterSpacing = 0.1),
    titleSmall = style(size = 14, lineHeight = 20, weight = FontWeight.SemiBold, letterSpacing = 0.1),
    bodyLarge = style(size = 16, lineHeight = 24, weight = FontWeight.Normal, letterSpacing = 0.15),
    bodyMedium = style(size = 14, lineHeight = 20, weight = FontWeight.Normal, letterSpacing = 0.2),
    bodySmall = style(size = 12, lineHeight = 16, weight = FontWeight.Normal, letterSpacing = 0.3),
    labelLarge = style(size = 14, lineHeight = 20, weight = FontWeight.SemiBold, letterSpacing = 0.1),
    labelMedium = style(size = 12, lineHeight = 16, weight = FontWeight.Medium, letterSpacing = 0.4),
    labelSmall = style(size = 11, lineHeight = 16, weight = FontWeight.Medium, letterSpacing = 0.5),
)
