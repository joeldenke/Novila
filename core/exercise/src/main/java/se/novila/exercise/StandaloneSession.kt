package se.novila.exercise

import java.time.DayOfWeek
import kotlin.time.Instant

/**
 * Today's strength session when there is no program to read: on the watch, which has no phone data of its own.
 * It uses the same [StrengthSessions] and [Workout] steps as the phone's workout mode, so a session started on the
 * watch has the same exercises, sets, rests and timed holds. The first session of the week runs from Monday to
 * Wednesday, the second from Thursday, like the program's two strength days.
 */
object StandaloneSession {
    /** Whether the day's session is the week's second one (B), which varies the exercises. */
    fun second(day: DayOfWeek): Boolean = when (day) {
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY -> false
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY -> true
    }

    /** Roughly how long the session takes, including the coach's five minutes of warm-up and cool-down. */
    fun minutes(level: StrengthLevel): Int = when (level) {
        StrengthLevel.Beginner -> 20
        StrengthLevel.Intermediate -> 30
        StrengthLevel.Advanced -> 40
    }

    /** The day's planned session at [level], [second] unless the day picks the week's second session. */
    fun of(day: DayOfWeek, level: StrengthLevel, second: Boolean = second(day)): PlannedSession = PlannedSession(
        day = day,
        type = ExerciseType.Strength,
        minutes = minutes(level),
        exercises = StrengthSessions.session(level, second = second),
    )

    /** The day's session, ready to run step by step from [now]. */
    fun start(
        day: DayOfWeek,
        level: StrengthLevel,
        now: Instant,
        second: Boolean = second(day),
    ): Workout = Workout.start(of(day, level, second), now)
}
