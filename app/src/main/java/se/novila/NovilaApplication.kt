package se.novila

import android.app.Application
import androidx.work.Configuration
import dev.zacsweers.metro.createGraphFactory
import se.novila.di.AppGraph
import se.novila.feature.widget.WidgetGraph

/**
 * Holds the app's Metro graph, created on first use, gives it to the home-screen widget and sets up WorkManager with
 * the reminders' worker factory from it. WorkManager starts on demand (its default initializer is removed in the
 * app's manifest), so an app start that schedules nothing doesn't touch it.
 */
class NovilaApplication :
    Application(),
    Configuration.Provider,
    WidgetGraph.Holder {
    val graph: AppGraph by lazy { createGraphFactory<AppGraph.Factory>().create(applicationContext) }

    override val widgetGraph: WidgetGraph get() = graph

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(graph.reminderWorkerFactory).build()
}
