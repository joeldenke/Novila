package se.novila.sleep

import se.novila.model.DayEntry
import se.novila.model.MAX_SLEEP_HOURS
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * A night a watch or another app recorded, on the [date] the user woke up (the day a [DayEntry] logs it on): in bed
 * from [bedtime] to [wakeTime], and asleep for [asleepMinutes] of that.
 */
data class Night(
    val date: LocalDate,
    val bedtime: LocalTime,
    val wakeTime: LocalTime,
    val asleepMinutes: Int,
)

/** A recorded sleep session in the user's local time; [awakeMinutes] are the stages the source marked as awake. */
data class SleepSession(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val awakeMinutes: Int = 0,
)

/** What the app can show about recorded nights, whatever the source is. */
sealed interface SleepState {
    /** The source isn't on this device, or not set up. Sleep is what the user logs. */
    data object Unavailable : SleepState

    /**
     * Not connected yet. [denied] after the user said no: the source then stops asking, so access
     * can only be given in its own settings.
     */
    data class NeedsPermission(val denied: Boolean = false) : SleepState

    /** Nights that ended on the asked day or later, oldest first. */
    data class Connected(val nights: List<Night>) : SleepState
}

/** The recorded nights by the day the user woke up, empty when not connected. */
val SleepState.nightsByDate: Map<LocalDate, Night>
    get() = (this as? SleepState.Connected)?.nights?.associateBy { it.date }.orEmpty()

/**
 * Where nights come from (Health Connect in the app). Like ExerciseSource, implementations never throw: a temporary
 * problem is a failed [Result], so the repository keeps what it showed. Everything else is a [SleepState].
 */
fun interface SleepSource {
    /** Nights that ended on [from] or later, oldest first. */
    suspend fun read(from: LocalDate): Result<SleepState>
}

/** A session shorter than this is a nap, never the night. */
const val MIN_NIGHT_MINUTES = 180L

/**
 * The night per day from recorded [sessions]: the longest session that ended that day, if it is long enough to be a
 * night ([MIN_NIGHT_MINUTES]). Naps and short sessions don't count. Oldest first.
 */
fun nightsOf(sessions: List<SleepSession>): List<Night> = sessions
    .filter { ChronoUnit.MINUTES.between(it.start, it.end) >= MIN_NIGHT_MINUTES }
    .groupBy { it.end.toLocalDate() }
    .map { (date, sessions) ->
        val longest = sessions.maxBy { Duration.between(it.start, it.end) }
        val inBed = ChronoUnit.MINUTES.between(longest.start, longest.end)
        Night(
            date = date,
            bedtime = longest.start.toLocalTime().truncatedTo(ChronoUnit.MINUTES),
            wakeTime = longest.end.toLocalTime().truncatedTo(ChronoUnit.MINUTES),
            asleepMinutes = (inBed - longest.awakeMinutes).coerceIn(0, inBed).toInt(),
        )
    }
    .sortedBy { it.date }

/** Whether the user logged the night themselves: hours or times. Only quality or causes don't count. */
val DayEntry.hasOwnSleep: Boolean get() = sleepHours > 0f || bedtime != null

/**
 * The day with the recorded [night] as its sleep when the user hasn't logged one; their own log always wins. Only for
 * showing: the night is never saved, so it is gone as soon as access is.
 */
fun DayEntry.withNight(night: Night?): DayEntry {
    if (night == null || hasOwnSleep) return this
    return withSleepTimes(night.bedtime, night.wakeTime)
        .copy(sleepHours = (night.asleepMinutes / 60f).coerceAtMost(MAX_SLEEP_HOURS))
}
