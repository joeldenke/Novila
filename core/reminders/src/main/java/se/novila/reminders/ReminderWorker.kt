package se.novila.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import se.novila.data.CoachStore
import se.novila.data.HealthStore
import se.novila.data.RemindersStore
import se.novila.data.trainingWeek
import se.novila.model.REMINDER_LATE_LIMIT
import se.novila.model.Reminder
import se.novila.model.isNeeded
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

/**
 * One reminder at its time: reads the saved day, shows the notification only if there is still something to log
 * (and, for training, a session planned today), and plans the next day. A few milliseconds once a day per reminder;
 * nothing else runs in the background. Created by [ReminderWorkerFactory].
 *
 * Always succeeds: the next day is appended to this work, and a failed run would cancel it.
 */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
    private val health: HealthStore,
    private val coach: CoachStore,
    private val reminders: RemindersStore,
    private val scheduler: ReminderScheduler,
    private val notifications: ReminderNotifications,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val reminder = Reminder.entries.firstOrNull { it.name == inputData.getString(KEY_REMINDER) }
            ?: return Result.success()
        val setting = reminders.reminders.filterNotNull().first()[reminder]
        // Turned off meanwhile: the work was cancelled too, so this is only a run that had already started.
        if (!setting.on) return Result.success()
        val now = LocalDateTime.now()
        if (isTimely(now)) notifyIfNeeded(reminder, now.toLocalDate())
        scheduler.scheduleNext(reminder, setting.time, now)
        return Result.success()
    }

    /** Not hours late, as when the phone was off at the time: a morning reminder at lunch helps no one. */
    private fun isTimely(now: LocalDateTime): Boolean {
        val planned = try {
            inputData.getString(KEY_AT)?.let(LocalDateTime::parse)
        } catch (e: DateTimeParseException) {
            null
        } ?: return true
        return Duration.between(planned, now) <= REMINDER_LATE_LIMIT
    }

    private suspend fun notifyIfNeeded(reminder: Reminder, today: LocalDate) {
        val days = health.days.filterNotNull().first()
        // Health Connect is only read in the foreground, so only the app's own sessions count here.
        val trainingDay = reminder == Reminder.Training &&
            trainingWeek(days, coach.answers.filterNotNull().first(), emptyList(), today).hasSession(today)
        if (reminder.isNeeded(days.day(today), trainingDay)) notifications.show(reminder)
    }

    companion object {
        const val KEY_REMINDER = "reminder"

        /** When the reminder was planned for, to skip it when it runs too late. */
        const val KEY_AT = "at"
    }
}
