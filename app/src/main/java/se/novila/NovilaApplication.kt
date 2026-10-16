package se.novila

import android.app.Application
import dev.zacsweers.metro.createGraphFactory
import se.novila.di.AppGraph

/** Holds the app's Metro graph, created on first use. */
class NovilaApplication : Application() {
    val graph: AppGraph by lazy { createGraphFactory<AppGraph.Factory>().create(applicationContext) }
}
