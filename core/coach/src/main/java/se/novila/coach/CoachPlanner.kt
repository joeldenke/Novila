package se.novila.coach

import se.novila.anatomy.Equipment
import se.novila.anatomy.ExerciseLibrary
import se.novila.anatomy.MuscleGroup
import se.novila.exercise.Dosage
import se.novila.exercise.Exercise
import se.novila.exercise.ExerciseKind
import se.novila.exercise.ExerciseType
import se.novila.exercise.PlannedExercise
import se.novila.exercise.PlannedSession
import se.novila.exercise.StrengthLevel
import se.novila.exercise.TrainingProgram
import se.novila.exercise.TrainingProgramGenerator
import se.novila.exercise.Workout
import se.novila.recovery.RecoveryStatus
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

    /** One strength day a week: a good start, and WHO advises two when there is room. */
    data object OneStrengthDay : CoachNote
    data class Focus(val areas: Set<BodyArea>) : CoachNote
    data class Considered(val consideration: Consideration) : CoachNote

    /** The sessions use the user's own equipment, and pick it over bodyweight when it trains the same muscles. */
    data class UsesEquipment(val equipment: Set<Equipment>) : CoachNote

    /** The week's cardio is below WHO's 150 minutes: everyday walks make up for it. */
    data class CardioBelowGuideline(val minutes: Int) : CoachNote
    data object CardioProgression : CoachNote

    /** Which rehab program and phase, and when to move on to the next one. */
    data class RehabPhase(val area: RehabArea, val phase: Int) : CoachNote

    /** How much pain is fine during a rehab session, and what to do when it is more. */
    data object PainGuide : CoachNote

    /**
     * The dose follows the latest sessions: how hard they felt ([EffortCheck.effort], RPE) or that the latest one hurt
     * ([EffortCheck.pain]).
     */
    data class Effort(val check: EffortCheck) : CoachNote

    /** The coach's suggestion for the rehab phase, from the pain after the latest sessions. */
    data class Rehab(val check: RehabCheck) : CoachNote

    /** The watch says the body is still recovering (resting heart rate up, HRV down): today's session is lighter. */
    data object Recovery : CoachNote

    /** The user chose a lighter session on a day with cycle symptoms: today's session is lighter. */
    data object Lighter : CoachNote
}

/**
 * A program from the coach and the [notes] that explain it. [suggestedPhase] is the rehab phase the coach suggests
 * moving to, or null when the current one is right; the user decides.
 */
data class CoachPlan(val program: TrainingProgram, val notes: List<CoachNote>, val suggestedPhase: Int? = null)

/**
 * Novila's training coach. It builds a week from the user's [CoachPreferences], their recent workouts
 * and the exercises in the anatomy library, entirely on the device:
 * - The days are spread over the week, with the strength days as far apart as possible (at least 48 hours
 *   when there is room), cardio on the others and mobility last. The goal decides the split.
 * - Cardio follows TrainingProgramGenerator: at most a tenth more a week than the recent average.
 * - Each strength session first gets a lower-body exercise, a push and a trunk exercise, then fills the
 *   time with the exercises that best train the muscles still lacking, weighted towards the focus areas,
 *   varied between the week's sessions. Exercises that don't suit the considerations, or need equipment the
 *   user doesn't have, are never used; equipment they have is preferred, since it can be made heavier.
 * - Sets, repetitions and rest follow the strength level and the goal; the time estimate includes the warm-up
 *   and the cool-down that [Workout] runs around the sets.
 * - The user's feedback after the latest sessions adjusts the dose, and suggests the next rehab phase
 *   ([CoachFeedback]). When the watch says the body is still recovering ([RecoveryStatus.Low]), or the
 *   user chose a lighter session on a day with cycle symptoms (`lighter`), today's session alone is lighter: one
 *   set less and a lighter cardio. Rehab stays as it is — it is already
 *   managed by pain, not by numbers.
 * Plain Kotlin and deterministic: the same answers give the same program until [CoachPreferences.variation]
 * changes, so it is unit tested.
 */
object CoachPlanner {
    /** The minutes the workout mode spends warming up and cooling down around the sets ([Workout]). */
    private val WARM_UP = Workout.WARM_UP + Workout.COOL_DOWN
    private val TIME_PER_REPETITION = 3.seconds
    private val CHANGE_OVER = 30.seconds
    private const val MIN_EXERCISES = 3
    private const val MAX_EXERCISES = 7
    private const val MIN_CARDIO_MINUTES = 10
    private const val MOBILITY_MINUTES = 15
    private const val MAX_SETS = 4

    fun plan(
        preferences: CoachPreferences,
        history: List<Exercise>,
        today: LocalDate,
        feedback: List<SessionFeedback> = emptyList(),
        recovery: RecoveryStatus = RecoveryStatus.Unknown,
        lighter: Boolean = false,
    ): CoachPlan {
        val weekStart = TrainingProgramGenerator.weekStart(today)
        val recent = TrainingProgramGenerator.summarize(history, weekStart)
        val days = preferences.trainingDays
        if (preferences.goal == CoachGoal.Rehab) {
            val check = CoachFeedback.rehab(feedback, preferences.rehabPhase, today)
            return rehab(preferences, days, weekStart, check)
        }
        val effort = CoachFeedback.effort(feedback, today)

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

        // Another suggestion steers away from the exercises the one before it had, wherever there are others. The
        // suggestions before are worked out the same way, a few back, so each one differs from the one shown last.
        val first = (preferences.variation - SUGGESTIONS_BACK).coerceAtLeast(0)
        val before = (first until preferences.variation).fold(emptyList<Set<String>>()) { avoid, variation ->
            strengthSessions(preferences.copy(variation = variation), strengthDays, recent.level, effort, avoid)
                .map { session -> session.ids() }
        }
        val strength = strengthSessions(preferences, strengthDays, recent.level, effort, before)
        val sessions = strength +
            cardioDays.map { PlannedSession(it, recent.cardioType, cardioMinutes) } +
            mobilityDays.map { PlannedSession(it, ExerciseType.Mobility, mobilityMinutes) }
        val weeklyCardio = cardioMinutes * cardioDays.size
        val adjusted = if (recovery == RecoveryStatus.Low || lighter) {
            sessions.map { it.loweredFor(today) }
        } else {
            sessions
        }

        return CoachPlan(
            program = TrainingProgram(weekStart, weeklyCardio, adjusted.sortedBy { it.day }),
            notes = notes(preferences, recent.level, strengthCount, weeklyCardio, effort) +
                when {
                    adjusted == sessions -> emptyList()
                    recovery == RecoveryStatus.Low -> listOf(CoachNote.Recovery)
                    else -> listOf(CoachNote.Lighter)
                },
        )
    }

    /**
     * A strength session on each of [days], each varied from the one before it. [avoid] are exercises to steer away
     * from in each session, by its place in the week.
     */
    private fun strengthSessions(
        preferences: CoachPreferences,
        days: List<DayOfWeek>,
        level: StrengthLevel,
        effort: EffortCheck?,
        avoid: List<Set<String>> = emptyList(),
    ): List<PlannedSession> {
        val sessions = mutableListOf<PlannedSession>()
        days.forEachIndexed { index, day ->
            val previous = sessions.lastOrNull()?.ids().orEmpty()
            val skip = avoid.getOrNull(index).orEmpty()
            val exercises = session(preferences, level, index, previous, effort?.advice, skip)
            sessions += PlannedSession(day, ExerciseType.Strength, sessionMinutes(exercises), exercises)
        }
        return sessions
    }

    private fun PlannedSession.ids(): Set<String> = exercises.map { it.exerciseId }.toSet()

    /** [this] as today's session when the body is still recovering: one set less, and a lighter cardio. */
    private fun PlannedSession.loweredFor(today: LocalDate): PlannedSession {
        if (day != today.dayOfWeek) return this
        return when (type.kind) {
            ExerciseKind.Strength -> {
                val fewer = exercises.map { it.copy(sets = (it.sets - 1).coerceAtLeast(1)) }
                copy(exercises = fewer, minutes = sessionMinutes(fewer))
            }

            ExerciseKind.Cardio -> copy(
                minutes = TrainingProgramGenerator.roundTo5(minutes * RECOVERY_CARDIO_FACTOR)
                    .coerceAtLeast(RECOVERY_MIN_CARDIO_MINUTES),
            )

            ExerciseKind.Mobility -> this
        }
    }

    /**
     * A rehab week: three calm sessions every other day with the phase's exercises, and walks on the days in
     * between. The exercises are chosen by hand per area and phase (see [REHAB]) rather than by score, so the
     * program is predictable; the considerations still leave out what doesn't suit.
     */
    private fun rehab(
        preferences: CoachPreferences,
        days: List<DayOfWeek>,
        weekStart: LocalDate,
        check: RehabCheck?,
    ): CoachPlan {
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
                check?.let { add(CoachNote.Rehab(it)) }
                add(CoachNote.RestBetweenStrengthDays)
                Consideration.entries.filter {
                    it in preferences.considerations
                }.forEach { add(CoachNote.Considered(it)) }
            },
            suggestedPhase = check?.phase?.takeIf { it != phase },
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
        (if (preferences.goal == CoachGoal.Strength && preferences.daysPerWeek >= 4) 3 else 2)
            .coerceAtMost(preferences.daysPerWeek)

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
        effort: EffortAdvice?,
        avoid: Set<String> = emptySet(),
    ): List<PlannedExercise> {
        val candidates = CoachCatalog.exercises.filter {
            it.suits(preferences.considerations) &&
                it.canDoWith(preferences.equipment) &&
                it.level <= level &&
                !it.rehabOnly &&
                ExerciseLibrary.byId(it.id) != null
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
                (if (exercise.equipment != null) EQUIPMENT_BONUS else 0.0) +
                jitter.getValue(exercise.id) -
                (if (chosen.any { it.pattern == exercise.pattern }) SAME_PATTERN_PENALTY else 0.0) -
                (if (exercise.id in previous) REPEAT_PENALTY else 0.0) -
                (if (exercise.id in avoid) AVOID_PENALTY else 0.0)
        }

        fun pick(exercise: CoachExercise) {
            chosen += exercise
            val motion = ExerciseLibrary.byId(exercise.id)!!
            motion.primary.forEach { need[it] = need.getValue(it) * PRIMARY_DONE }
            motion.secondary.forEach { need[it] = need.getValue(it) * SECONDARY_DONE }
        }

        fun dosed(exercise: CoachExercise) = dose(exercise, level, preferences.goal, preferences.focus, effort)

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
     * [focus] area gets one set more. [effort] after the latest sessions takes a set away, or adds repetitions.
     */
    internal fun dose(
        exercise: CoachExercise,
        level: StrengthLevel,
        goal: CoachGoal,
        focus: Set<BodyArea> = emptySet(),
        effort: EffortAdvice? = null,
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
        val fewerSets = if (effort == EffortAdvice.Easier) 1 else 0
        val harder = effort == EffortAdvice.Harder
        return PlannedExercise(
            exerciseId = exercise.id,
            sets = (min(sets + focusSet, MAX_SETS) - fewerSets).coerceAtLeast(1),
            amount = if (exercise.dosage == Dosage.Seconds) {
                level.seconds + extraSeconds + (if (harder) HARDER_SECONDS else 0)
            } else {
                level.repetitions + extraReps + (if (harder) HARDER_REPETITIONS else 0)
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

    /** The session's length with the warm-up and cool-down, rounded to five minutes. */
    private fun sessionMinutes(exercises: List<PlannedExercise>): Int =
        TrainingProgramGenerator.roundTo5((WARM_UP + exercises.total()).toDouble(DurationUnit.MINUTES))
            .coerceAtLeast(CoachPreferences.MIN_MINUTES)

    private fun notes(
        preferences: CoachPreferences,
        level: StrengthLevel,
        strengthDays: Int,
        weeklyCardio: Int,
        effort: EffortCheck?,
    ): List<CoachNote> = buildList {
        add(CoachNote.Goal(preferences.goal))
        add(CoachNote.Level(level))
        effort?.let { add(CoachNote.Effort(it)) }
        if (strengthDays > 1) add(CoachNote.RestBetweenStrengthDays) else add(CoachNote.OneStrengthDay)
        if (preferences.focus.isNotEmpty()) add(CoachNote.Focus(preferences.focus))
        Consideration.entries.filter { it in preferences.considerations }.forEach { add(CoachNote.Considered(it)) }
        if (preferences.equipment.isNotEmpty()) add(CoachNote.UsesEquipment(preferences.equipment))
        if (weeklyCardio < TrainingProgramGenerator.GUIDELINE_CARDIO_MINUTES) {
            add(CoachNote.CardioBelowGuideline(weeklyCardio))
        }
        if (weeklyCardio > 0) add(CoachNote.CardioProgression)
    }

    /**
     * Rehab exercises per area, phase 1 to 3. The knee starts with the hips and calves and adds knee bends
     * once they are easy; the lower back starts with holding the trunk still and adds hinging last. The
     * shoulder starts without load through the arm and adds pushing; the neck begins with a slow nod and
     * keeps the posture work around it; the hip starts with bridges and adds hinging and split stances.
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
        RehabArea.Shoulder to listOf(
            listOf("arm_raise", "bird_dog", "superman"),
            listOf("arm_raise", "push_up", "plank", "superman"),
            listOf("push_up", "arm_raise", "plank", "bird_dog", "superman"),
        ),
        RehabArea.Neck to listOf(
            listOf("neck_nod", "bird_dog", "superman"),
            listOf("neck_nod", "plank", "superman", "bird_dog"),
            listOf("neck_nod", "arm_raise", "plank", "superman"),
        ),
        RehabArea.Hip to listOf(
            listOf("glute_bridge", "bird_dog", "plank"),
            listOf("glute_bridge", "squat", "bird_dog", "plank"),
            listOf("squat", "split_squat", "good_morning", "glute_bridge", "plank"),
        ),
    )
    private const val REHAB_DAYS = 3
    private const val REHAB_WALK_MINUTES = 20
    private val REHAB_REST = 60.seconds

    /** Today's cardio is lighter while the body recovers, but never under ten minutes. */
    private const val RECOVERY_CARDIO_FACTOR = 0.8
    private const val RECOVERY_MIN_CARDIO_MINUTES = 10

    /** Each session gets one of each before anything else, in this order. */
    private val REQUIRED = listOf(
        setOf(MovementPattern.Legs, MovementPattern.Hinge),
        setOf(MovementPattern.Push),
        setOf(MovementPattern.Core, MovementPattern.Back),
    )
    private const val HARDER_REPETITIONS = 2
    private const val HARDER_SECONDS = 10
    private const val FOCUS_WEIGHT = 2.5
    private const val SECONDARY_SHARE = 0.5
    private const val PRIMARY_DONE = 0.25
    private const val SECONDARY_DONE = 0.6
    private const val SAME_PATTERN_PENALTY = 1.5
    private const val REPEAT_PENALTY = 0.8

    /** Enough to pick another exercise for the same pattern when there is one, for another suggestion. */
    private const val AVOID_PENALTY = 2.0

    /** How many suggestions back are worked out to know what the one shown last had. */
    private const val SUGGESTIONS_BACK = 3

    /** Enough to pick a loaded exercise over a bodyweight one for the same muscles, never over a missing muscle. */
    private const val EQUIPMENT_BONUS = 0.4
}
