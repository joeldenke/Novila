package se.novila.di

import android.content.Context
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.viewmodel.ViewModelGraph
import se.novila.data.SettingsStore
import se.novila.exercise.ExerciseRepository
import se.novila.feature.widget.WidgetGraph
import se.novila.healthconnect.HealthConnectExerciseSource
import se.novila.healthconnect.HealthConnectSource
import se.novila.movement.InMemoryMovementCache
import se.novila.movement.MovementRepository
import se.novila.reminders.ReminderWorkerFactory
import kotlin.time.Clock

/**
 * The app's Metro graph. Modules contribute to [AppScope]: the stores in `:core:data` (`@Inject`, `@SingleIn`), the
 * coach's language model (`@ContributesBinding`) and each feature's ViewModels (`@ViewModelKey
 * @ContributesIntoMap`), which [ViewModelGraph] collects into the factory that `metroViewModel()` uses.
 *
 * Data sources are chosen here and nowhere else. The home-screen widget reaches the stores through [WidgetGraph].
 */
@DependencyGraph(AppScope::class)
interface AppGraph :
    ViewModelGraph,
    WidgetGraph {
    /** The app's settings, which [se.novila.MainActivity] reads for the theme before any screen is shown. */
    val settingsStore: SettingsStore

    /** Creates the reminders' work with the stores from this graph, for WorkManager's configuration. */
    val reminderWorkerFactory: ReminderWorkerFactory

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

    /**
     * Workouts recorded by a watch or another app, for the training program. In memory only. Swap
     * [HealthConnectExerciseSource] for another ExerciseSource (such as the Google Health API) here.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun exerciseRepository(context: Context): ExerciseRepository =
        ExerciseRepository(HealthConnectExerciseSource(context))

    /** The time, for ViewModels that count it (the workout timer). Tests can pass their own [Clock]. */
    @Provides
    fun clock(): Clock = Clock.System

    @DependencyGraph.Factory
    fun interface Factory {
        /** [context] is the application context. */
        fun create(@Provides context: Context): AppGraph
    }
}
