package se.novila.reminders

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import dev.zacsweers.metro.Inject
import se.novila.data.CoachStore
import se.novila.data.HealthStore
import se.novila.data.RemindersStore

/**
 * Creates the reminders' work with the app's stores from the Metro graph. `NovilaApplication` gives it to
 * WorkManager's configuration; work it doesn't know gets null, and WorkManager's default.
 */
@Inject
class ReminderWorkerFactory(
    private val health: HealthStore,
    private val coach: CoachStore,
    private val reminders: RemindersStore,
    private val scheduler: ReminderScheduler,
    private val notifications: ReminderNotifications,
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker? = when (workerClassName) {
        ReminderWorker::class.java.name -> ReminderWorker(
            appContext,
            workerParameters,
            health,
            coach,
            reminders,
            scheduler,
            notifications,
        )

        else -> null
    }
}
