package se.novila.designsystem.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProvider
import androidx.glance.color.ColorProviders
import androidx.glance.unit.ColorProvider
import se.novila.designsystem.R
import se.novila.designsystem.theme.NovilaDarkAccent
import se.novila.designsystem.theme.NovilaDarkColors
import se.novila.designsystem.theme.NovilaLightAccent
import se.novila.designsystem.theme.NovilaLightColors
import se.novila.designsystem.theme.NovilaPalette
import se.novila.designsystem.theme.NovilaSpacing
import androidx.glance.material3.ColorProviders as Material3ColorProviders

/**
 * Novila's look for home-screen widgets. Glance draws with RemoteViews, so the Compose components and NovilaTheme
 * can't be used there; these are the same colors and tokens in Glance's form. The pillars are the color roles, as in
 * the app: training = `primary`, sleep = `secondary`, nutrition = `tertiary`. Light and dark follow the phone.
 */
object NovilaGlance {
    /** The app's light and dark color schemes. */
    val colors: ColorProviders = Material3ColorProviders(light = NovilaLightColors, dark = NovilaDarkColors)

    /**
     * The widget card: a frosted surface that lets a hint of the wallpaper through. RemoteViews can't blur what is
     * behind them, so this is a semi-transparent surface, not a real blur. Kept fairly opaque (birch in light, night
     * forest in dark) so text stays legible over any wallpaper.
     */
    val cardBackground: ColorProvider = ColorProvider(
        day = NovilaPalette.Birch97.copy(alpha = 0.86f),
        night = NovilaPalette.Night6.copy(alpha = 0.80f),
    )

    /** A hairline for the card, matching the app's outline. */
    val cardOutline: ColorProvider = ColorProvider(
        day = NovilaPalette.Birch80,
        night = NovilaPalette.Night26,
    )

    /** The midsummer sun, the one strong accent, for the widget's emblem. */
    val accentSun: ColorProvider = ColorProvider(day = NovilaLightAccent.sun, night = NovilaDarkAccent.sun)

    /** The sun emblem, exposed here so a widget never reads another module's resources. */
    val sunDrawable: Int = R.drawable.ic_novila_sun

    val spacing: NovilaSpacing = NovilaSpacing()

    /** The widget's own corners, like the hero card (18 dp). */
    val widgetCornerRadius: Dp = 18.dp

    /** Buttons and chips, the 14 dp base. */
    val cornerRadius: Dp = 14.dp

    /** Text sizes, matching the app's headline, title, body and label styles. */
    val headlineSize: TextUnit = 28.sp
    val titleSize: TextUnit = 16.sp
    val bodySize: TextUnit = 14.sp
    val labelSize: TextUnit = 12.sp
}

/** [GlanceTheme] with Novila's colors. */
@Composable
fun NovilaGlanceTheme(content: @Composable () -> Unit) = GlanceTheme(colors = NovilaGlance.colors, content = content)
