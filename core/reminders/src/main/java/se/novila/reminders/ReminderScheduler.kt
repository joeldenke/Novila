package se.novila.reminders

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dev.zacsweers.metro.Inject
import se.novila.model.Reminder
import se.novila.model.ReminderSetting
import se.novila.model.nextReminderAt
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Plans each reminder as one piece of WorkManager work at its next time, so nothing runs in between and the plan
 * survives a restart of the phone (WorkManager keeps it). After running, a reminder plans its next day itself
 * ([scheduleNext]), at the same clock time even across daylight saving time.
 */
@Inject
class ReminderScheduler(private val context: Context) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Turns [reminder] on at [setting]'s time, replacing what was planned, or off. */
    fun apply(reminder: Reminder, setting: ReminderSetting, now: LocalDateTime = LocalDateTime.now()) {
        if (setting.on) {
            enqueue(reminder, setting.time, now, ExistingWorkPolicy.REPLACE)
        } else {
            workManager.cancelUniqueWork(workName(reminder))
        }
    }

    /**
     * From the reminder's own work, while it runs: the next time after [now]. Appended, so it starts once the running
     * work has finished instead of cancelling it.
     */
    internal fun scheduleNext(reminder: Reminder, time: LocalTime, now: LocalDateTime) =
        enqueue(reminder, time, now, ExistingWorkPolicy.APPEND_OR_REPLACE)

    /** Cancels every planned reminder, as when the user deletes all data. */
    fun cancelAll() = workManager.cancelAllWorkByTag(TAG)

    private fun enqueue(reminder: Reminder, time: LocalTime, now: LocalDateTime, policy: ExistingWorkPolicy) {
        val at = nextReminderAt(now, time)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(Duration.between(now, at))
            .setInputData(workDataOf(ReminderWorker.KEY_REMINDER to reminder.name, ReminderWorker.KEY_AT to "$at"))
            .addTag(TAG)
            .build()
        workManager.enqueueUniqueWork(workName(reminder), policy, request)
    }

    companion object {
        /** Every reminder's work carries this tag. */
        const val TAG = "reminder"

        /** The one piece of work planned for [reminder]. */
        fun workName(reminder: Reminder) = "reminder-${reminder.name.lowercase()}"
    }
}
