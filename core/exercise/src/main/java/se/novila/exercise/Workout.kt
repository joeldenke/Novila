package se.novila.exercise

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

/** One step of a guided strength session. */
sealed interface WorkoutStep {
    /** A step that runs on a clock instead of waiting for the user: a rest, the warm-up or the cool-down. */
    sealed interface Timed : WorkoutStep {
        val duration: Duration
    }

    /** Set [set] of [exercise]'s sets (from 1), the [exerciseNumber]th exercise of the session (from 1). */
    data class Work(val exercise: PlannedExercise, val exerciseNumber: Int, val set: Int) : WorkoutStep

    /** A pause of [duration] before [next]. */
    data class Rest(override val duration: Duration, val next: Work) : Timed

    /** The easy [duration] before the first set: get warm, then do [next]. */
    data class WarmUp(override val duration: Duration, val next: Work) : Timed

    /** The [duration] of slowing down after the last set, before the session is saved. */
    data class CoolDown(override val duration: Duration) : Timed
}

/**
 * A strength session in progress, step by step: a warm-up, then every set with a rest before the next set or
 * the next exercise, and a cool-down after the last set. Timed steps (rests, the warm-up, the cool-down and
 * holds) end on their own at [endsAt]; the user ends repetition sets. What the user [done] in each set is kept by
 * step index, so the next session can build on it ([results], [Progression]). The current time is passed in, so this
 * is plain Kotlin and unit tested without a clock.
 */
data class Workout(
    val day: DayOfWeek,
    val exerciseCount: Int,
    val steps: List<WorkoutStep>,
    val index: Int,
    val startedAt: Instant,
    val endsAt: Instant?,
    val finishedAt: Instant? = null,
    val done: Map<Int, Int> = emptyMap(),
) {
    val step: WorkoutStep? get() = steps.getOrNull(index)

    val isFinished: Boolean get() = index >= steps.size

    /** Share of the steps done, for a progress bar. */
    val progress: Float get() = if (steps.isEmpty()) 1f else index.toFloat() / steps.size

    /** Whole seconds left of a timed step, rounded up so the countdown never shows 0 while running. */
    fun secondsLeft(now: Instant): Int? =
        endsAt?.let { ceil((it - now).coerceAtLeast(Duration.ZERO).toDouble(DurationUnit.SECONDS)).toInt() }

    /** Sets done so far. */
    val setsDone: Int get() = steps.take(index).count { it is WorkoutStep.Work }

    /** Time from the start to the end, or to [now] while the session runs. */
    fun elapsed(now: Instant): Duration = (finishedAt ?: now) - startedAt

    /** Whole minutes from the start to the end, at least one. */
    fun minutes(now: Instant): Int = elapsed(now).inWholeMinutes.toInt().coerceAtLeast(1)

    /**
     * What the user did in the current set so far: the repetitions they said, or the planned amount until they
     * change it. Null when the current step isn't a set.
     */
    val doneInSet: Int? get() = (step as? WorkoutStep.Work)?.let { done[index] ?: it.exercise.amount }

    /** The user did [amount] repetitions in the current set, instead of the planned amount. */
    fun withDone(amount: Int): Workout {
        if (step !is WorkoutStep.Work) return this
        return copy(done = done + (index to amount.coerceIn(0, MAX_DONE)))
    }

    /**
     * On to the next step: after the user finishes a set or skips a rest, or when a timed step runs out. A hold the
     * user ends early counts the seconds held.
     */
    fun next(now: Instant): Workout {
        if (isFinished) return this
        val nextIndex = index + 1
        val nextStep = steps.getOrNull(nextIndex)
        val work = step as? WorkoutStep.Work
        val held = work?.takeIf { it.exercise.dosage == Dosage.Seconds && index !in done }?.let {
            (it.exercise.amount - (secondsLeft(now) ?: 0)).coerceAtLeast(0)
        }
        return copy(
            index = nextIndex,
            endsAt = nextStep?.let { endOf(it, now) },
            finishedAt = if (nextStep == null) now else null,
            done = if (held != null) done + (index to held) else done,
        )
    }

    /** What the user did of each exercise in the sets done so far, as results on [date]. */
    fun results(date: LocalDate): List<ExerciseResult> =
        doneSets().map { (exercise, sets) -> ExerciseResult(exercise.exerciseId, date, exercise.amount, sets) }

    /**
     * Each exercise done so far and how [Progression] moves its target for the next time: a step up (positive), the
     * same (0) or a step back (negative), in repetitions or seconds.
     */
    fun nextChanges(): List<Pair<PlannedExercise, Int>> = results(LocalDate.MIN).zip(doneSets()) { result, (exercise) ->
        exercise to Progression.next(result, Progression.step(exercise.dosage)) - exercise.amount
    }

    /** Each exercise with the sets done so far, in the order they were done. */
    private fun doneSets(): List<Pair<PlannedExercise, List<Int>>> = steps.take(index)
        .withIndex()
        .filter { it.value is WorkoutStep.Work }
        .groupBy { (it.value as WorkoutStep.Work).exerciseNumber }
        .values
        .map { sets ->
            val exercise = (sets.first().value as WorkoutStep.Work).exercise
            exercise to sets.map { done[it.index] ?: exercise.amount }
        }

    companion object {
        /** The most repetitions a set can be said to have. */
        const val MAX_DONE = 999

        /** The timed phases around the sets. The coach budgets their five minutes into the session's minutes. */
        val WARM_UP = 3.minutes
        val COOL_DOWN = 2.minutes

        fun start(session: PlannedSession, now: Instant): Workout {
            val steps = steps(session.exercises)
            return Workout(
                day = session.day,
                exerciseCount = session.exercises.size,
                steps = steps,
                index = 0,
                startedAt = now,
                endsAt = steps.firstOrNull()?.let { endOf(it, now) },
                finishedAt = if (steps.isEmpty()) now else null,
            )
        }

        fun steps(exercises: List<PlannedExercise>): List<WorkoutStep> {
            val work = exercises.flatMapIndexed { i, exercise ->
                (1..exercise.sets).map { set -> WorkoutStep.Work(exercise, exerciseNumber = i + 1, set = set) }
            }
            if (work.isEmpty()) return emptyList()
            val sets = work.flatMapIndexed { i, step ->
                val next = work.getOrNull(i + 1)
                if (next == null) listOf(step) else listOf(step, WorkoutStep.Rest(step.exercise.rest, next))
            }
            return listOf(WorkoutStep.WarmUp(WARM_UP, work.first())) + sets + WorkoutStep.CoolDown(COOL_DOWN)
        }

        /** Rests, the warm-up, the cool-down and holds run on a timer; repetition sets wait for the user. */
        private fun endOf(step: WorkoutStep, now: Instant): Instant? = when (step) {
            is WorkoutStep.Timed -> now + step.duration
            is WorkoutStep.Work -> step.exercise.takeIf { it.dosage == Dosage.Seconds }?.let { now + it.amount.seconds }
        }
    }
}
