package se.novila.wear

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import kotlin.time.Clock

/**
 * The watch app's Metro graph. It is separate from the phone's [se.novila.di.AppGraph] because the watch app is its
 * own app; the only binding is the clock the workout timer counts with, so a test can step time instead of waiting.
 */
@DependencyGraph(AppScope::class)
interface WearGraph : ViewModelGraph {
    @Provides
    fun clock(): Clock = Clock.System

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(): WearGraph
    }
}
