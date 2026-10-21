package se.novila.exercise

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Whether an exercise is counted in repetitions or held for a number of seconds. */
enum class Dosage { Repetitions, Seconds }

/** How much strength training the user already does, from their recent workouts. */
enum class StrengthLevel(val sets: Int, val repetitions: Int, val seconds: Int, val rest: Duration) {
    Beginner(sets = 2, repetitions = 8, seconds = 20, rest = 90.seconds),
    Intermediate(sets = 3, repetitions = 10, seconds = 30, rest = 75.seconds),
    Advanced(sets = 3, repetitions = 12, seconds = 40, rest = 60.seconds),
}

/**
 * One exercise in a strength session: [sets] of [amount] repetitions or seconds (per side when
 * [perSide]), with [rest] between sets. [exerciseId] is an exercise in the anatomy library. [previous] is the target
 * the last time the user did it, when [Progression] moved the amount on from there.
 */
data class PlannedExercise(
    val exerciseId: String,
    val sets: Int,
    val amount: Int,
    val dosage: Dosage,
    val perSide: Boolean,
    val rest: Duration,
    val previous: Int? = null,
)

/**
 * Builds the two full-body strength sessions of the week from bodyweight exercises. Each session
 * has one exercise per slot (legs, push, hip hinge, core, back, conditioning), so every large muscle
 * group is trained on both strength days, with at least 48 hours in between (Monday and Thursday).
 * Beginners get the four basic slots with fewer sets and longer rests; more experienced users get more
 * exercises, more repetitions and shorter rests. Session B varies the exercises so the week isn't
 * the same session twice.
 */
object StrengthSessions {
    private class Move(val id: String, val dosage: Dosage = Dosage.Repetitions, val perSide: Boolean = false)

    private enum class Slot { Legs, Push, Hinge, Core, Back, Conditioning }

    private val SESSION_A = mapOf(
        Slot.Legs to Move("squat"),
        Slot.Push to Move("push_up"),
        Slot.Hinge to Move("glute_bridge"),
        Slot.Core to Move("plank", Dosage.Seconds),
        Slot.Back to Move("superman"),
        Slot.Conditioning to Move("mountain_climber", Dosage.Seconds),
    )

    private val SESSION_B = mapOf(
        Slot.Legs to Move("reverse_lunge", perSide = true),
        Slot.Push to Move("push_up"),
        Slot.Hinge to Move("good_morning"),
        Slot.Core to Move("crunch"),
        Slot.Back to Move("bird_dog", perSide = true),
        Slot.Conditioning to Move("jumping_jack", Dosage.Seconds),
    )

    /** A beginner lunges in place before stepping back into it. */
    private val BEGINNER_LEGS_B = Move("split_squat", perSide = true)

    /** Every exercise id the sessions can use. */
    val exerciseIds: Set<String> = (SESSION_A.values + SESSION_B.values + BEGINNER_LEGS_B).map { it.id }.toSet()

    fun level(strengthSessionsPerWeek: Double): StrengthLevel = when {
        strengthSessionsPerWeek >= 2 -> StrengthLevel.Advanced
        strengthSessionsPerWeek >= 1 -> StrengthLevel.Intermediate
        else -> StrengthLevel.Beginner
    }

    /** The first ([second] false) or second strength session of the week. */
    fun session(level: StrengthLevel, second: Boolean): List<PlannedExercise> {
        val slots = when (level) {
            StrengthLevel.Beginner -> listOf(Slot.Legs, Slot.Push, Slot.Hinge, Slot.Core)
            StrengthLevel.Intermediate -> listOf(Slot.Legs, Slot.Push, Slot.Hinge, Slot.Back, Slot.Core)
            StrengthLevel.Advanced -> listOf(Slot.Legs, Slot.Push, Slot.Hinge, Slot.Back, Slot.Core, Slot.Conditioning)
        }
        return slots.map { slot ->
            val move = when {
                !second -> SESSION_A.getValue(slot)
                slot == Slot.Legs && level == StrengthLevel.Beginner -> BEGINNER_LEGS_B
                else -> SESSION_B.getValue(slot)
            }
            PlannedExercise(
                exerciseId = move.id,
                sets = level.sets,
                amount = if (move.dosage == Dosage.Seconds) level.seconds else level.repetitions,
                dosage = move.dosage,
                perSide = move.perSide,
                rest = level.rest,
            )
        }
    }
}
