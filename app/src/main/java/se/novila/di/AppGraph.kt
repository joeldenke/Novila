package se.novila.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import se.novila.healthconnect.HealthConnectSource
import se.novila.movement.InMemoryMovementCache
import se.novila.movement.MovementRepository
import kotlin.time.Clock

/**
 * The app's Metro graph. Modules contribute to [AppScope]: the stores in `:core:data` (`@Inject`, `@SingleIn`), the
 * coach's language model (`@ContributesBinding`) and each feature's ViewModels (`@ViewModelKey
 * @ContributesIntoMap`), which [ViewModelGraph] collects into the factory that `metroViewModel()` uses.
 *
 * Data sources are chosen here and nowhere else.
 */
@DependencyGraph(AppScope::class)
interface AppGraph : ViewModelGraph {
    /**
     * Movement for Today and Training, one repository for the app. Swap [HealthConnectSource] for another
     * MovementSource, or the cache for a persistent one, here.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun movementRepository(context: Context): MovementRepository = MovementRepository(
        source = HealthConnectSource(context),
        cache = InMemoryMovementCache(),
    )

    /** The time, for ViewModels that count it (the workout timer). Tests can pass their own [Clock]. */
    @Provides
    fun clock(): Clock = Clock.System

    @DependencyGraph.Factory
    fun interface Factory {
        /** [context] is the application context. */
        fun create(@Provides context: Context): AppGraph
    }
}
