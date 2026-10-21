package se.novila.wear

import android.app.Application
import dev.zacsweers.metro.createGraphFactory

/** Holds the watch app's Metro graph, created on first use. */
class WearApplication : Application() {
    val graph: WearGraph by lazy { createGraphFactory<WearGraph.Factory>().create() }
}
