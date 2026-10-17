package se.novila.designsystem.glance

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceTheme
import androidx.glance.color.ColorProviders
import se.novila.designsystem.theme.NovilaDarkColors
import se.novila.designsystem.theme.NovilaLightColors
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
