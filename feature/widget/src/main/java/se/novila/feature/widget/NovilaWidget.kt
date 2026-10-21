package se.novila.feature.widget

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import se.novila.designsystem.glance.NovilaGlanceTheme
import java.time.LocalDate
import java.time.LocalTime

/**
 * Today on the home screen: the app's hero card with the day's balance as rings, a value per part and the day's
 * encouragement, laid out for the widget's exact size. Reads the same stores as the app, while the widget is shown; nothing is read from Health
 * Connect here (it only allows reads while the app is open), so the balance is sleep and food, like Today without a
 * movement source.
 */
class NovilaWidget : GlanceAppWidget() {
    // The panel and rings are drawn for the size the launcher gives the widget, so they are never stretched.
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = context.widgetGraph
        provideContent {
            val days by graph.healthStore.days.collectAsState()
            val goals by graph.goalsStore.goals.collectAsState()
            NovilaGlanceTheme {
                // Nothing but the panel until the stores are read, a moment after the phone starts. The sky follows
                // the hour; the system's hourly update moves it on.
                WidgetContent(day = days?.day(LocalDate.now()), goals = goals, hour = LocalTime.now().hour)
            }
        }
    }
}

/** Gives the system the widget. Registered in this module's manifest. */
class NovilaWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NovilaWidget()
}
