package se.novila.feature.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import com.github.takahirom.roborazzi.captureRoboImage
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import se.novila.designsystem.glance.NovilaGlanceTheme
import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.ui.preview.PreviewData

/**
 * The widget as the launcher shows it: composed to RemoteViews by Glance, applied to real views and captured over a
 * wallpaper. Glance has no `@Preview` that Roborazzi's scanner finds, so these are the widget's screenshot tests.
 */
@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class WidgetScreenshotTest {
    private fun capture(name: String, size: DpSize, day: DayEntry = PreviewData.today, hour: Int = 20) {
        val context = RuntimeEnvironment.getApplication()
        val views = runBlocking {
            GlanceRemoteViews().compose(context, size) {
                NovilaGlanceTheme { WidgetContent(day, Goals(), hour) }
            }.remoteViews
        }
        captureRoboImage("src/test/screenshots/se.novila.feature.widget.NovilaWidget.$name.png") {
            Box(Modifier.background(Brush.verticalGradient(Wallpaper)).padding(16.dp)) {
                AndroidView(
                    factory = { views.apply(it, null) },
                    modifier = Modifier.size(size.width, size.height),
                )
            }
        }
    }

    @Test fun large() = capture("Large", DpSize(340.dp, 260.dp))

    @Config(qualifiers = "+night")
    @Test
    fun largeNight() = capture("LargeNight", DpSize(340.dp, 260.dp))

    @Test fun morning() = capture("Morning", DpSize(340.dp, 260.dp), hour = 7)

    @Config(qualifiers = "+night")
    @Test
    fun empty() = capture("Empty", DpSize(340.dp, 260.dp), day = DayEntry(PreviewData.date))

    @Test fun low() = capture("Low", DpSize(340.dp, 160.dp))

    @Test fun small() = capture("Small", DpSize(180.dp, 180.dp))

    private companion object {
        /** A forest and lake wallpaper, so the panel is seen over something like a home screen. */
        val Wallpaper = listOf(Color(0xFF5E6B5A), Color(0xFF2F4A35), Color(0xFF1F6B5C))
    }
}
