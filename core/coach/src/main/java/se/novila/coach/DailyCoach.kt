package se.novila.coach

import se.novila.model.DayEntry
import se.novila.model.Goals
import se.novila.model.NextPortion
import se.novila.model.TrainingToday
import se.novila.model.nextPortion
import se.novila.recovery.RecoveryStatus
import se.novila.sleep.SleepCoachPreferences
import java.time.LocalTime

/**
 * What the day is about, once sleep, training and nutrition are weighed together. The screen picks the card's icon
 * and colour from it.
 */
enum class CoachFocus {
    /** A short night or a body that is still recovering: the day should be gentler. */
    Recover,

    /** Today's planned session is the day's main thing. */
    Train,

    /** The one food or drink step that raises the day most. */
    Nutrition,

    /** Everything the day asks for is already done. */
    Balance,
}

/**
 * One thing the coach says today, as data, so the screen can put it in the user's own words. The pieces are meant to
 * be read in order: together they are one connected suggestion, not a list of separate tips.
 */
sealed interface CoachAdvice {
    /**
     * Today's readiness asks for a calm day, because of these [reasons] (at least two of last night, recovery, the
     * training load and cycle symptoms). Said first, instead of the short night or the recovery alone.
     */
    data class CalmDay(val reasons: List<ReadinessSignal>) : CoachAdvice

    /** The body seems ready for today's session, because of these [reasons]. */
    data class ReadyDay(val reasons: List<ReadinessSignal>) : CoachAdvice

    /** Last night was clearly shorter than the user's sleep goal. */
    data class ShortNight(val hours: Float) : CoachAdvice

    /** The watch says the body is still recovering, so the day should be gentler. */
    data object LowRecovery : CoachAdvice

    /** Today's planned session, made lighter because the night was short or the body is recovering. */
    data class LighterSession(val minutes: Int) : CoachAdvice

    /** Today's planned session, as it stands. */
    data class PlannedSession(val minutes: Int) : CoachAdvice

    /** A rest day: no session is planned. */
    data object RestDay : CoachAdvice

    /** Turn in at this time tonight, to repay the short night. */
    data class EarlierBedtime(val time: LocalTime) : CoachAdvice

    /** The one food group a portion of which raises the day most. */
    data class AddPortion(val next: NextPortion) : CoachAdvice

    /** Everything the day asks for is done. */
    data object WellDone : CoachAdvice
}

/**
 * The coach's one suggestion for today: what the day is about ([focus]) and the connected [advice] behind it, in the
 * order it is said. The advice is never empty; the first piece is the day's main point.
 */
data class DailyCoach(val focus: CoachFocus, val advice: List<CoachAdvice>) {
    /** The day's main point, the first piece of advice. */
    val main: CoachAdvice get() = advice.first()
}

/** A night this much shorter than the goal is worth a gentler day. */
const val SHORT_NIGHT_MARGIN = 0.5f

/**
 * Novila's one coach for all parts: the day's single suggestion, worked out from the user's own [day], their [goals],
 * today's [training] from the week's program, how the body has [recovered][recovery], today's [readiness] and the
 * [bedtimeGoal] the sleep coach runs on. It connects the parts instead of advising on each one alone: a short night,
 * a body that is still recovering or a readiness that asks for a calm day (said with its reasons) makes today's
 * session lighter and points to an earlier bedtime, a ready body is said with its reasons before the planned session,
 * a normal day points to the planned session and one food step, and when the day is done it simply says so.
 * Deterministic, entirely on the device, and never a diagnosis: the same day always gives the same suggestion, so it
 * is unit tested.
 */
fun dailyCoach(
    day: DayEntry,
    goals: Goals = Goals(),
    training: TrainingToday = TrainingToday(),
    recovery: RecoveryStatus = RecoveryStatus.Unknown,
    bedtimeGoal: LocalTime = SleepCoachPreferences.DEFAULT_BEDTIME,
    readiness: Readiness? = null,
): DailyCoach {
    val shortNight = day.sleepHours > 0f && day.sleepHours < goals.sleepHours - SHORT_NIGHT_MARGIN
    val lowRecovery = recovery == RecoveryStatus.Low
    // Only today's remaining minutes count: whatever is already trained is behind us.
    val remaining = (training.plannedMinutes - training.doneMinutes).coerceAtLeast(0)
    val trainingOpen = !training.isRest && remaining > 0
    val next = day.nextPortion(goals)

    val calm = readiness?.takeIf { it.level == ReadinessLevel.Calm }
    val ready = readiness?.takeIf { it.level == ReadinessLevel.Ready }
    if (shortNight || lowRecovery || calm != null) {
        return DailyCoach(
            focus = CoachFocus.Recover,
            advice = buildList {
                // A calm day says all its reasons at once, the short night and the recovery among them.
                if (calm != null) {
                    add(CoachAdvice.CalmDay(calm.reasons))
                } else {
                    if (shortNight) add(CoachAdvice.ShortNight(day.sleepHours))
                    if (lowRecovery) add(CoachAdvice.LowRecovery)
                }
                when {
                    trainingOpen -> add(CoachAdvice.LighterSession(remaining))
                    training.isRest -> add(CoachAdvice.RestDay)
                }
                // The earlier bedtime only answers a short night; a low recovery wants rest, not just sleep.
                if (shortNight) add(CoachAdvice.EarlierBedtime(bedtimeGoal))
            },
        )
    }
    if (trainingOpen) {
        return DailyCoach(
            focus = CoachFocus.Train,
            advice = buildList {
                ready?.let { add(CoachAdvice.ReadyDay(it.reasons)) }
                add(CoachAdvice.PlannedSession(remaining))
                next?.let { add(CoachAdvice.AddPortion(it)) }
            },
        )
    }
    if (next != null) {
        return DailyCoach(CoachFocus.Nutrition, listOf(CoachAdvice.AddPortion(next)))
    }
    return DailyCoach(CoachFocus.Balance, listOf(CoachAdvice.WellDone))
}
