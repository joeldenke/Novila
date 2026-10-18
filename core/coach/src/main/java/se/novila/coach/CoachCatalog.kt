package se.novila.coach

import se.novila.exercise.Dosage
import se.novila.exercise.StrengthLevel

/** The movement an exercise trains. A balanced session covers the lower body, a push and the trunk. */
enum class MovementPattern { Legs, Hinge, Calves, Push, Back, Core, Conditioning }

/**
 * What the coach needs to know about an exercise in the anatomy library, besides its muscles: the
 * [pattern] it trains, how it is dosed, the lowest [level] it suits, and whether it strains the knees,
 * the back or is a jump. [rehabOnly] exercises belong to the rehab programs and never appear in an
 * ordinary strength session, where they would take a slot meant for a balanced push or trunk exercise.
 * Every exercise in ExerciseLibrary has an entry (checked by CoachPlannerTest).
 */
data class CoachExercise(
    val id: String,
    val pattern: MovementPattern,
    val dosage: Dosage = Dosage.Repetitions,
    val perSide: Boolean = false,
    val level: StrengthLevel = StrengthLevel.Beginner,
    val knees: Boolean = false,
    val back: Boolean = false,
    val jump: Boolean = false,
    val rehabOnly: Boolean = false,
) {
    /** Whether the exercise suits the user's [considerations]. */
    fun suits(considerations: Set<Consideration>): Boolean =
        !(knees && Consideration.Knees in considerations) &&
            !(back && Consideration.Back in considerations) &&
            !(jump && Consideration.Quiet in considerations)
}

object CoachCatalog {
    val exercises: List<CoachExercise> = listOf(
        CoachExercise("squat", MovementPattern.Legs),
        CoachExercise("split_squat", MovementPattern.Legs, perSide = true, knees = true),
        CoachExercise(
            "reverse_lunge",
            MovementPattern.Legs,
            perSide = true,
            level = StrengthLevel.Intermediate,
            knees = true,
        ),
        CoachExercise("glute_bridge", MovementPattern.Hinge),
        CoachExercise("good_morning", MovementPattern.Hinge, back = true),
        CoachExercise("calf_raise", MovementPattern.Calves),
        CoachExercise("push_up", MovementPattern.Push),
        CoachExercise("superman", MovementPattern.Back, back = true),
        CoachExercise("bird_dog", MovementPattern.Back, perSide = true),
        CoachExercise("plank", MovementPattern.Core, Dosage.Seconds),
        CoachExercise("crunch", MovementPattern.Core, back = true),
        CoachExercise("arm_raise", MovementPattern.Push, rehabOnly = true),
        CoachExercise("neck_nod", MovementPattern.Back, rehabOnly = true),
        CoachExercise(
            "mountain_climber",
            MovementPattern.Conditioning,
            Dosage.Seconds,
            level = StrengthLevel.Intermediate,
        ),
        CoachExercise("jumping_jack", MovementPattern.Conditioning, Dosage.Seconds, knees = true, jump = true),
    )

    fun byId(id: String): CoachExercise? = exercises.firstOrNull { it.id == id }
}
