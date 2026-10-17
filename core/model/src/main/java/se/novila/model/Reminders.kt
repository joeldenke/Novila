package se.novila.model

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/** What Novila can remind the user about. Each is off until the user turns it on, at a time of their choice. */
enum class Reminder(val defaultTime: LocalTime) {
    /** In the morning: log last night's sleep. */
    Sleep(LocalTime.of(8, 30)),

    /** On days the week's program has a session: time to train. */
    Training(LocalTime.of(17, 30)),

    /** In the evening: log what was eaten today. */
    Nutrition(LocalTime.of(19, 30)),
}

/** One reminder: whether it is [on], and the [time] of day it comes. The time is kept while it is off. */
data class ReminderSetting(val on: Boolean, val time: LocalTime)

/** The user's reminders, all off until turned on. Saved by `RemindersStore` in `:core:data`. */
data class Reminders(
    private val settings: Map<Reminder, ReminderSetting> = Reminder.entries.associateWith {
        ReminderSetting(on = false, time = it.defaultTime)
    },
) {
    operator fun get(reminder: Reminder): ReminderSetting =
        settings[reminder] ?: ReminderSetting(on = false, time = reminder.defaultTime)

    fun with(reminder: Reminder, setting: ReminderSetting): Reminders =
        copy(settings = settings + (reminder to setting))

    /** Whether any reminder is on, so the app knows to ask for permission to notify. */
    val anyOn: Boolean get() = Reminder.entries.any { get(it).on }
}

/**
 * Whether the reminder is still worth sending on [day]: nothing to remind about once the user has logged it.
 * Training needs a planned session [trainingDay] and nothing trained yet; a workout only Health Connect knows about
 * isn't counted, since the app reads Health Connect in the foreground only.
 */
fun Reminder.isNeeded(day: DayEntry, trainingDay: Boolean): Boolean = when (this) {
    Reminder.Sleep -> day.sleepHours == 0f
    Reminder.Training -> trainingDay && day.strengthMinutes == 0 && day.cardio.isEmpty()
    Reminder.Nutrition -> day.food == FoodLog() && day.waterGlasses == 0
}

/** A reminder that comes this much later than planned (the phone was asleep, say) is no longer timely. */
val REMINDER_LATE_LIMIT: Duration = Duration.ofHours(2)

/** The next time [time] comes after [now]: later today, or tomorrow when it has passed. */
fun nextReminderAt(now: LocalDateTime, time: LocalTime): LocalDateTime {
    val today = now.toLocalDate().atTime(time)
    return if (today.isAfter(now)) today else today.plusDays(1)
}
