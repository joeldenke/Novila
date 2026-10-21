package se.novila.exercise

import java.time.LocalDate

/**
 * What the user did of one exercise in one session on [date]: the [planned] repetitions or seconds per set, and what
 * they [done] in each set, in order.
 */
data class ExerciseResult(val exerciseId: String, val date: LocalDate, val planned: Int, val done: List<Int>) {
    /** Every set reached what was planned. */
    val reached: Boolean get() = done.isNotEmpty() && done.all { it >= planned }

    /** A set fell clearly short of what was planned. */
    val missed: Boolean get() = done.any { it <= planned - Progression.MISSED_BY }
}

/** The results the user has saved, oldest first, at most [MAX_RESULTS] from the last [Progression.MEMORY_DAYS] days. */
data class ExerciseLog(val results: List<ExerciseResult> = emptyList()) {
    /** With [session]'s results added, a session that day replacing an earlier one of the same exercise. */
    fun withSession(session: List<ExerciseResult>): ExerciseLog {
        val today = session.maxOfOrNull { it.date } ?: return this
        val since = today.minusDays(Progression.MEMORY_DAYS)
        val kept = results.filter { old ->
            old.date > since && session.none { it.exerciseId == old.exerciseId && it.date == old.date }
        }
        return ExerciseLog((kept + session).sortedBy { it.date }.takeLast(MAX_RESULTS))
    }

    /** The latest result of [exerciseId] before [today], or null when there is none in memory. */
    fun latest(exerciseId: String, today: LocalDate): ExerciseResult? = results.lastOrNull {
        it.exerciseId == exerciseId && it.date < today && it.date > today.minusDays(Progression.MEMORY_DAYS)
    }

    companion object {
        const val MAX_RESULTS = 400
    }
}

/**
 * Raises each exercise step by step from what the user did last time (double progression): when every set of the
 * latest session reached the target, the next one has a repetition more (five seconds for a hold); when a set fell
 * clearly short, a step less; otherwise the same. The dose the coach or the program plans is the starting point, and
 * the progression stays within [MAX_STEPS_UP] steps above it and [MAX_STEPS_DOWN] below. Rehab sessions are left as
 * they are: pain decides their dose, not repetitions. Plain Kotlin and deterministic, so it is unit tested.
 */
object Progression {
    const val STEP_REPETITIONS = 1
    const val STEP_SECONDS = 5
    const val MAX_STEPS_UP = 6
    const val MAX_STEPS_DOWN = 2

    /** A set this much short of the target counts as missed. */
    const val MISSED_BY = 2

    /** How long a result is remembered: a break longer than this starts from the planned dose again. */
    const val MEMORY_DAYS = 42L

    /** [program]'s strength sessions with every exercise dosed from [log]; other sessions as they are. */
    fun apply(program: TrainingProgram, log: ExerciseLog, today: LocalDate): TrainingProgram {
        if (log.results.isEmpty()) return program
        return program.copy(
            sessions = program.sessions.map { session ->
                if (session.type != ExerciseType.Strength) {
                    session
                } else {
                    session.copy(exercises = session.exercises.map { dose(it, log, today) })
                }
            },
        )
    }

    /** [planned] with its amount moved on from the latest result of the same exercise. */
    fun dose(planned: PlannedExercise, log: ExerciseLog, today: LocalDate): PlannedExercise {
        val latest = log.latest(planned.exerciseId, today) ?: return planned
        val step = step(planned.dosage)
        val amount = next(latest, step).coerceIn(
            (planned.amount - MAX_STEPS_DOWN * step).coerceAtLeast(1),
            planned.amount + MAX_STEPS_UP * step,
        )
        return planned.copy(amount = amount, previous = latest.planned)
    }

    /** The target after [result]: a [step] more when every set was reached, a step less when one was missed. */
    fun next(result: ExerciseResult, step: Int): Int = when {
        result.reached -> result.planned + step
        result.missed -> (result.planned - step).coerceAtLeast(1)
        else -> result.planned
    }

    fun step(dosage: Dosage): Int = when (dosage) {
        Dosage.Repetitions -> STEP_REPETITIONS
        Dosage.Seconds -> STEP_SECONDS
    }
}

/**
 * The [ExerciseLog] as text: "p1" on the first line, then "date|exerciseId|planned|done,done,…" per result. Unreadable
 * lines are skipped, and anything not starting with a known version decodes to an empty log.
 */
object ExerciseLogCodec {
    private const val VERSION = "p1"
    private const val FIELDS = 4

    fun encode(log: ExerciseLog): String = (
        listOf(VERSION) +
            log.results.map { "${it.date}|${it.exerciseId}|${it.planned}|${it.done.joinToString(",")}" }
        ).joinToString("\n")

    fun decode(raw: String?): ExerciseLog {
        val lines = raw?.split('\n') ?: return ExerciseLog()
        if (lines.first() != VERSION) return ExerciseLog()
        val results = lines.drop(1).mapNotNull { line ->
            val fields = line.split('|')
            if (fields.size != FIELDS) return@mapNotNull null
            val date = runCatching { LocalDate.parse(fields[0]) }.getOrNull() ?: return@mapNotNull null
            val planned = fields[2].toIntOrNull()?.takeIf { it > 0 } ?: return@mapNotNull null
            val done = fields[3].split(',').map { it.toIntOrNull() ?: return@mapNotNull null }
            ExerciseResult(fields[1], date, planned, done.map { it.coerceAtLeast(0) }).takeIf { fields[1].isNotEmpty() }
        }
        return ExerciseLog(results.sortedBy { it.date }.takeLast(ExerciseLog.MAX_RESULTS))
    }
}
