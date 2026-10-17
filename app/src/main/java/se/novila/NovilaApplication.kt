package se.novila

import android.app.Application
import androidx.work.Configuration
import dev.zacsweers.metro.createGraphFactory
import se.novila.di.AppGraph

/**
 * Holds the app's Metro graph, created on first use, and sets up WorkManager with the reminders' worker factory from
 * it. WorkManager starts on demand (its default initializer is removed in `:core:reminders`), so an app start that
 * schedules nothing doesn't touch it.
 */
class NovilaApplication :
    Application(),
    Configuration.Provider {
    val graph: AppGraph by lazy { createGraphFactory<AppGraph.Factory>().create(applicationContext) }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(graph.reminderWorkerFactory).build()
}
