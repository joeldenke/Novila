package se.novila.exercise

import java.time.DayOfWeek
import kotlin.math.ceil

/** One step of a guided strength session. */
sealed interface WorkoutStep {
    /** Set [set] of [exercise]'s sets (from 1), the [exerciseNumber]th exercise of the session (from 1). */
    data class Work(val exercise: PlannedExercise, val exerciseNumber: Int, val set: Int) : WorkoutStep

    /** A pause before [next]. */
    data class Rest(val seconds: Int, val next: Work) : WorkoutStep
}

/**
 * A strength session in progress, step by step: every set, then a rest before the next set or the next
 * exercise, but none after the last set. Timed steps (rests and holds) end on their own at [endsAtMillis];
 * the user ends repetition sets. Times are milliseconds since the epoch, passed in, so this is plain
 * Kotlin and unit tested without a clock.
 */
data class Workout(
    val day: DayOfWeek,
    val exerciseCount: Int,
    val steps: List<WorkoutStep>,
    val index: Int,
    val startedAtMillis: Long,
    val endsAtMillis: Long?,
    val finishedAtMillis: Long? = null,
) {
    val step: WorkoutStep? get() = steps.getOrNull(index)

    val isFinished: Boolean get() = index >= steps.size

    /** Share of the steps done, for a progress bar. */
    val progress: Float get() = if (steps.isEmpty()) 1f else index.toFloat() / steps.size

    /** Whole seconds left of a timed step, rounded up so the countdown never shows 0 while running. */
    fun secondsLeft(nowMillis: Long): Int? =
        endsAtMillis?.let { ceil((it - nowMillis).coerceAtLeast(0) / 1000.0).toInt() }

    /** Sets done so far. */
    val setsDone: Int get() = steps.take(index).count { it is WorkoutStep.Work }

    /** Whole minutes from the start to the end, at least one. */
    fun minutes(nowMillis: Long): Int =
        (((finishedAtMillis ?: nowMillis) - startedAtMillis) / 60_000L).toInt().coerceAtLeast(1)

    /** On to the next step: after the user finishes a set or skips a rest, or when a timed step runs out. */
    fun next(nowMillis: Long): Workout {
        if (isFinished) return this
        val nextIndex = index + 1
        val nextStep = steps.getOrNull(nextIndex)
        return copy(
            index = nextIndex,
            endsAtMillis = nextStep?.let { endOf(it, nowMillis) },
            finishedAtMillis = if (nextStep == null) nowMillis else null,
        )
    }

    companion object {
        fun start(session: PlannedSession, nowMillis: Long): Workout {
            val steps = steps(session.exercises)
            return Workout(
                day = session.day,
                exerciseCount = session.exercises.size,
                steps = steps,
                index = 0,
                startedAtMillis = nowMillis,
                endsAtMillis = steps.firstOrNull()?.let { endOf(it, nowMillis) },
                finishedAtMillis = if (steps.isEmpty()) nowMillis else null,
            )
        }

        fun steps(exercises: List<PlannedExercise>): List<WorkoutStep> {
            val work = exercises.flatMapIndexed { i, exercise ->
                (1..exercise.sets).map { set -> WorkoutStep.Work(exercise, exerciseNumber = i + 1, set = set) }
            }
            return work.flatMapIndexed { i, step ->
                val next = work.getOrNull(i + 1)
                if (next == null) listOf(step) else listOf(step, WorkoutStep.Rest(step.exercise.restSeconds, next))
            }
        }

        /** Rests and holds run on a timer; repetition sets wait for the user. */
        private fun endOf(step: WorkoutStep, nowMillis: Long): Long? = when (step) {
            is WorkoutStep.Rest -> nowMillis + step.seconds * 1000L

            is WorkoutStep.Work -> step.exercise.takeIf { it.dosage == Dosage.Seconds }?.let {
                nowMillis +
                    it.amount * 1000L
            }
        }
    }
}
