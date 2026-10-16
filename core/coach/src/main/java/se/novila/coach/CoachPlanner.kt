package se.novila.coach

import se.novila.anatomy.ExerciseLibrary
import se.novila.anatomy.MuscleGroup
import se.novila.exercise.Dosage
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseType
import se.novila.exercise.PlannedExercise
import se.novila.exercise.PlannedSession
import se.novila.exercise.StrengthLevel
import se.novila.exercise.TrainingProgram
import se.novila.exercise.TrainingProgramGenerator
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/** Why the coach built the program the way it did. The UI turns each note into a sentence. */
sealed interface CoachNote {
    data class Level(val level: StrengthLevel) : CoachNote
    data class Goal(val goal: CoachGoal) : CoachNote
    data object RestBetweenStrengthDays : CoachNote
    data class Focus(val areas: Set<BodyArea>) : CoachNote
    data class Considered(val consideration: Consideration) : CoachNote

    /** The week's cardio is below WHO's 150 minutes: everyday walks make up for it. */
    data class CardioBelowGuideline(val minutes: Int) : CoachNote
    data object CardioProgression : CoachNote

    /** Which rehab program and phase, and when to move on to the next one. */
    data class RehabPhase(val area: RehabArea, val phase: Int) : CoachNote

    /** How much pain is fine during a rehab session, and what to do when it is more. */
    data object PainGuide : CoachNote
}

/** A program from the coach and the [notes] that explain it. */
data class CoachPlan(val program: TrainingProgram, val notes: List<CoachNote>)

/**
 * Novila's training coach. It builds a week from the user's [CoachPreferences], their recent workouts
 * and the exercises in the anatomy library, entirely on the device:
 * - The days are spread over the week, with the strength days as far apart as possible (at least 48 hours
 *   when there is room), cardio on the others and mobility last. The goal decides the split.
 * - Cardio follows TrainingProgramGenerator: at most a tenth more a week than the recent average.
 * - Each strength session first gets a lower-body exercise, a push and a trunk exercise, then fills the
 *   time with the exercises that best train the muscles still lacking, weighted towards the focus areas,
 *   varied between the week's sessions. Exercises that don't suit the considerations are never used.
 * - Sets, repetitions and rest follow the strength level and the goal; the time estimate includes a warm-up.
 * Plain Kotlin and deterministic: the same answers give the same program until [CoachPreferences.variation]
 * changes, so it is unit tested.
 */
object CoachPlanner {
    private val WARM_UP = 5.minutes
    private val TIME_PER_REPETITION = 3.seconds
    private val CHANGE_OVER = 30.seconds
    private const val MIN_EXERCISES = 3
    private const val MAX_EXERCISES = 7
    private const val MIN_CARDIO_MINUTES = 10
    private const val MOBILITY_MINUTES = 15
    private const val MAX_SETS = 4

    /** The training days for each number of days a week, spread out over the week. */
    private val DAYS: Map<Int, List<DayOfWeek>> = mapOf(
        3 to listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
        4 to listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.SATURDAY),
        5 to listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SUNDAY),
        6 to DayOfWeek.entries.take(6),
    )

    fun plan(preferences: CoachPreferences, history: List<Exercise>, today: LocalDate): CoachPlan {
        val weekStart = TrainingProgramGenerator.weekStart(today)
        val recent = TrainingProgramGenerator.summarize(history, weekStart)
        val days = DAYS.getValue(preferences.daysPerWeek.coerceIn(CoachPreferences.MIN_DAYS, CoachPreferences.MAX_DAYS))
        if (preferences.goal == CoachGoal.Rehab) return rehab(preferences, days, weekStart)

        val strengthCount = strengthDays(preferences)
        val strengthDays = spreadOut(days, strengthCount)
        val others = days - strengthDays.toSet()
        val mobilityDays = others.takeLast(min(mobilityDays(preferences), others.size))
        val cardioDays = others - mobilityDays.toSet()

        val cardioMinutes = if (cardioDays.isEmpty()) {
            0
        } else {
            TrainingProgramGenerator.roundTo5(recent.cardioTarget / cardioDays.size.toDouble())
                .coerceIn(MIN_CARDIO_MINUTES, max(MIN_CARDIO_MINUTES, preferences.sessionMinutes))
        }
        val mobilityMinutes = if (preferences.goal == CoachGoal.Mobility) {
            preferences.sessionMinutes
        } else {
            min(MOBILITY_MINUTES, preferences.sessionMinutes)
        }

        val strengthSessions = mutableListOf<PlannedSession>()
        strengthDays.forEachIndexed { index, day ->
            val previous = strengthSessions.lastOrNull()?.exercises.orEmpty().map { it.exerciseId }.toSet()
            val exercises = session(preferences, recent.level, index, previous)
            strengthSessions += PlannedSession(day, ExerciseType.Strength, sessionMinutes(exercises), exercises)
        }
        val sessions = strengthSessions +
            cardioDays.map { PlannedSession(it, recent.cardioType, cardioMinutes) } +
            mobilityDays.map { PlannedSession(it, ExerciseType.Mobility, mobilityMinutes) }
        val weeklyCardio = cardioMinutes * cardioDays.size

        return CoachPlan(
            program = TrainingProgram(weekStart, weeklyCardio, sessions.sortedBy { it.day }),
            notes = notes(preferences, recent.level, strengthCount, weeklyCardio),
        )
    }

    /**
     * A rehab week: three calm sessions every other day with the phase's exercises, and walks on the days in
     * between. The exercises are chosen by hand per area and phase (see [REHAB]) rather than by score, so the
     * program is predictable; the considerations still leave out what doesn't suit.
     */
    private fun rehab(preferences: CoachPreferences, days: List<DayOfWeek>, weekStart: LocalDate): CoachPlan {
        val phase = preferences.rehabPhase.coerceIn(1, CoachPreferences.MAX_REHAB_PHASE)
        val rehabDays = spreadOut(days, REHAB_DAYS)
        val walkDays = days - rehabDays.toSet()
        val walkMinutes =
            min(REHAB_WALK_MINUTES + (phase - 1) * CoachPreferences.MINUTES_STEP, preferences.sessionMinutes)
        val exercises = rehabExercises(preferences.rehabArea, phase)
            .mapNotNull(CoachCatalog::byId)
            .filter { it.suits(preferences.considerations) }
            .map { rehabDose(it, phase) }
        val sessions = rehabDays.map { PlannedSession(it, ExerciseType.Rehab, sessionMinutes(exercises), exercises) } +
            walkDays.map { PlannedSession(it, ExerciseType.Walking, walkMinutes) }
        return CoachPlan(
            program = TrainingProgram(weekStart, walkMinutes * walkDays.size, sessions.sortedBy { it.day }),
            notes = buildList {
                add(CoachNote.Goal(CoachGoal.Rehab))
                add(CoachNote.RehabPhase(preferences.rehabArea, phase))
                add(CoachNote.PainGuide)
                add(CoachNote.RestBetweenStrengthDays)
                Consideration.entries.filter {
                    it in preferences.considerations
                }.forEach { add(CoachNote.Considered(it)) }
            },
        )
    }

    /** The exercises of a rehab [phase] for [area], in the order they are done. */
    internal fun rehabExercises(area: RehabArea, phase: Int): List<String> =
        REHAB.getValue(area)[phase.coerceIn(1, CoachPreferences.MAX_REHAB_PHASE) - 1]

    /** Slow and controlled: more sets, repetitions and longer holds with each phase, always a full minute of rest. */
    internal fun rehabDose(exercise: CoachExercise, phase: Int): PlannedExercise = PlannedExercise(
        exerciseId = exercise.id,
        sets = if (phase == 1) 2 else 3,
        amount = if (exercise.dosage == Dosage.Seconds) 10 + phase * 5 else 6 + phase * 2,
        dosage = exercise.dosage,
        perSide = exercise.perSide,
        rest = REHAB_REST,
    )

    /** WHO's two strength days; three when strength is the goal and there is time for it. */
    private fun strengthDays(preferences: CoachPreferences): Int =
        if (preferences.goal == CoachGoal.Strength && preferences.daysPerWeek >= 4) 3 else 2

    private fun mobilityDays(preferences: CoachPreferences): Int = when {
        preferences.goal == CoachGoal.Mobility -> if (preferences.daysPerWeek >= 4) 2 else 1
        preferences.goal == CoachGoal.Endurance -> if (preferences.daysPerWeek >= 6) 1 else 0
        preferences.daysPerWeek >= 5 -> 1
        else -> 0
    }

    /** The [count] days of [days] that leave the most days between them, counted around the week. */
    internal fun spreadOut(days: List<DayOfWeek>, count: Int): List<DayOfWeek> {
        var best = days.take(count)
        var bestGap = -1
        fun search(start: Int, chosen: List<DayOfWeek>) {
            if (chosen.size == count) {
                val gap = minGap(chosen)
                if (gap > bestGap) {
                    best = chosen
                    bestGap = gap
                }
                return
            }
            for (i in start until days.size) search(i + 1, chosen + days[i])
        }
        search(0, emptyList())
        return best
    }

    /** The fewest days from one chosen day to the next, wrapping from Sunday to Monday. */
    internal fun minGap(days: List<DayOfWeek>): Int = days.indices.minOf { i ->
        val next = days[(i + 1) % days.size]
        (next.value - days[i].value + 7).mod(7).let { if (it == 0) 7 else it }
    }

    private fun session(
        preferences: CoachPreferences,
        level: StrengthLevel,
        index: Int,
        previous: Set<String>,
    ): List<PlannedExercise> {
        val candidates = CoachCatalog.exercises.filter {
            it.suits(preferences.considerations) && it.level <= level && ExerciseLibrary.byId(it.id) != null
        }
        val jitter = Random(preferences.variation * 1_000 + index).let { random ->
            candidates.associate { it.id to if (preferences.variation == 0) 0.0 else random.nextDouble() * 0.9 }
        }
        val need = MuscleGroup.entries.associateWith { muscle ->
            if (preferences.focus.any { muscle in it.muscles }) FOCUS_WEIGHT else 1.0
        }.toMutableMap()
        val chosen = mutableListOf<CoachExercise>()
        val budget = preferences.sessionMinutes.minutes - WARM_UP

        fun score(exercise: CoachExercise): Double {
            val motion = ExerciseLibrary.byId(exercise.id)!!
            return motion.primary.sumOf { need.getValue(it) } +
                SECONDARY_SHARE * motion.secondary.sumOf { need.getValue(it) } +
                goalBonus(preferences.goal, exercise.pattern) +
                jitter.getValue(exercise.id) -
                (if (chosen.any { it.pattern == exercise.pattern }) SAME_PATTERN_PENALTY else 0.0) -
                (if (exercise.id in previous) REPEAT_PENALTY else 0.0)
        }

        fun pick(exercise: CoachExercise) {
            chosen += exercise
            val motion = ExerciseLibrary.byId(exercise.id)!!
            motion.primary.forEach { need[it] = need.getValue(it) * PRIMARY_DONE }
            motion.secondary.forEach { need[it] = need.getValue(it) * SECONDARY_DONE }
        }

        fun dosed(exercise: CoachExercise) = dose(exercise, level, preferences.goal, preferences.focus)

        fun timeWith(exercise: CoachExercise) = (chosen + exercise).map(::dosed).total()

        // A balanced base first: lower body, a push and the trunk.
        REQUIRED.forEach { patterns ->
            candidates.filter { it.pattern in patterns && it !in chosen }.maxByOrNull(::score)?.let(::pick)
        }
        // Then the exercises that add the most, while there is time.
        while (chosen.size < MAX_EXERCISES) {
            val next = candidates.filter { it !in chosen }.maxByOrNull(::score) ?: break
            if (chosen.size >= MIN_EXERCISES && timeWith(next) > budget) break
            pick(next)
        }
        return chosen.sortedBy { it.pattern }.map(::dosed)
    }

    private fun goalBonus(goal: CoachGoal, pattern: MovementPattern): Double = when (goal) {
        CoachGoal.Endurance -> if (pattern == MovementPattern.Conditioning) 1.0 else 0.0

        CoachGoal.Mobility -> when (pattern) {
            MovementPattern.Back -> 0.8
            MovementPattern.Hinge -> 0.5
            else -> 0.0
        }

        CoachGoal.Strength -> if (pattern == MovementPattern.Conditioning) -0.5 else 0.0

        CoachGoal.Health, CoachGoal.Rehab -> 0.0
    }

    /**
     * Sets, repetitions or seconds and rest from the level, adjusted for the goal. An exercise for a
     * [focus] area gets one set more.
     */
    internal fun dose(
        exercise: CoachExercise,
        level: StrengthLevel,
        goal: CoachGoal,
        focus: Set<BodyArea> = emptySet(),
    ): PlannedExercise {
        val primary = ExerciseLibrary.byId(exercise.id)?.primary.orEmpty()
        val focusSet = if (focus.any { area -> primary.any { it in area.muscles } }) 1 else 0
        val (sets, extraReps, extraSeconds) = when (goal) {
            CoachGoal.Strength -> listOf(min(level.sets + 1, MAX_SETS), 0, 0)
            CoachGoal.Endurance -> listOf(level.sets, 5, 15)
            CoachGoal.Health, CoachGoal.Mobility, CoachGoal.Rehab -> listOf(level.sets, 0, 0)
        }
        val rest = when (goal) {
            CoachGoal.Strength -> level.rest + 30.seconds
            CoachGoal.Endurance -> maxOf(30.seconds, level.rest - 30.seconds)
            CoachGoal.Health, CoachGoal.Mobility, CoachGoal.Rehab -> level.rest
        }
        return PlannedExercise(
            exerciseId = exercise.id,
            sets = min(sets + focusSet, MAX_SETS),
            amount = if (exercise.dosage == Dosage.Seconds) {
                level.seconds + extraSeconds
            } else {
                level.repetitions + extraReps
            },
            dosage = exercise.dosage,
            perSide = exercise.perSide,
            rest = rest,
        )
    }

    /** Roughly how long an exercise takes: its sets, the rests between them and moving on to the next. */
    internal fun duration(planned: PlannedExercise): Duration {
        val work = when (planned.dosage) {
            Dosage.Seconds -> planned.amount.seconds
            Dosage.Repetitions -> TIME_PER_REPETITION * planned.amount
        }
        val perSet = work * (if (planned.perSide) 2 else 1)
        return perSet * planned.sets + planned.rest * (planned.sets - 1) + CHANGE_OVER
    }

    private fun List<PlannedExercise>.total(): Duration = fold(Duration.ZERO) { sum, it -> sum + duration(it) }

    /** The session's length with a warm-up, rounded to five minutes. */
    private fun sessionMinutes(exercises: List<PlannedExercise>): Int =
        TrainingProgramGenerator.roundTo5((WARM_UP + exercises.total()).toDouble(DurationUnit.MINUTES))
            .coerceAtLeast(CoachPreferences.MIN_MINUTES)

    private fun notes(
        preferences: CoachPreferences,
        level: StrengthLevel,
        strengthDays: Int,
        weeklyCardio: Int,
    ): List<CoachNote> = buildList {
        add(CoachNote.Goal(preferences.goal))
        add(CoachNote.Level(level))
        if (strengthDays > 1) add(CoachNote.RestBetweenStrengthDays)
        if (preferences.focus.isNotEmpty()) add(CoachNote.Focus(preferences.focus))
        Consideration.entries.filter { it in preferences.considerations }.forEach { add(CoachNote.Considered(it)) }
        if (weeklyCardio < TrainingProgramGenerator.GUIDELINE_CARDIO_MINUTES) {
            add(CoachNote.CardioBelowGuideline(weeklyCardio))
        }
        if (weeklyCardio > 0) add(CoachNote.CardioProgression)
    }

    /**
     * Rehab exercises per area, phase 1 to 3. The knee starts with the hips and calves and adds knee bends
     * once they are easy; the lower back starts with holding the trunk still and adds hinging last.
     */
    private val REHAB: Map<RehabArea, List<List<String>>> = mapOf(
        RehabArea.Knee to listOf(
            listOf("glute_bridge", "calf_raise", "bird_dog"),
            listOf("glute_bridge", "squat", "calf_raise", "plank"),
            listOf("squat", "split_squat", "glute_bridge", "calf_raise", "plank"),
        ),
        RehabArea.LowerBack to listOf(
            listOf("bird_dog", "glute_bridge", "plank"),
            listOf("bird_dog", "glute_bridge", "squat", "plank"),
            listOf("squat", "good_morning", "glute_bridge", "bird_dog", "plank"),
        ),
    )
    private const val REHAB_DAYS = 3
    private const val REHAB_WALK_MINUTES = 20
    private val REHAB_REST = 60.seconds

    /** Each session gets one of each before anything else, in this order. */
    private val REQUIRED = listOf(
        setOf(MovementPattern.Legs, MovementPattern.Hinge),
        setOf(MovementPattern.Push),
        setOf(MovementPattern.Core, MovementPattern.Back),
    )
    private const val FOCUS_WEIGHT = 2.5
    private const val SECONDARY_SHARE = 0.5
    private const val PRIMARY_DONE = 0.25
    private const val SECONDARY_DONE = 0.6
    private const val SAME_PATTERN_PENALTY = 1.5
    private const val REPEAT_PENALTY = 0.8
}
