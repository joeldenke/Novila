package se.novila.coach

import se.novila.anatomy.Equipment
import se.novila.anatomy.ExerciseLibrary
import se.novila.exercise.Dosage
import se.novila.exercise.StrengthLevel

/**
 * The movement an exercise trains. A balanced session covers the lower body, a push and the trunk. Sessions run in
 * this order, so a balance exercise comes first, while the legs are fresh.
 */
enum class MovementPattern { Balance, Legs, Hinge, Calves, Push, Back, Core, Conditioning }

/**
 * What the coach needs to know about an exercise in the anatomy library, besides its muscles: the
 * [pattern] it trains, how it is dosed, the lowest [level] it suits, and whether it strains the knees,
 * the back or is a jump. [rehabOnly] exercises belong to the rehab programs and never appear in an
 * ordinary strength session, where they would take a slot meant for a balanced push or trunk exercise.
 * The [equipment] comes from the anatomy library, so an exercise drawn with dumbbells always needs them.
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
    /** What the exercise is done with, or null for bodyweight. */
    val equipment: Equipment? get() = ExerciseLibrary.byId(id)?.equipment

    /** Whether the user can do the exercise with the [available] equipment. */
    fun canDoWith(available: Set<Equipment>): Boolean = equipment.let { it == null || it in available }

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
        // Bodyweight variations, after the exercises they vary so those win a tie.
        CoachExercise("knee_push_up", MovementPattern.Push),
        CoachExercise("single_leg_bridge", MovementPattern.Hinge, perSide = true, level = StrengthLevel.Intermediate),
        CoachExercise("dead_bug", MovementPattern.Core, perSide = true),
        CoachExercise(
            "high_knees",
            MovementPattern.Conditioning,
            Dosage.Seconds,
            level = StrengthLevel.Intermediate,
            jump = true,
        ),
        // Dumbbells.
        CoachExercise("goblet_squat", MovementPattern.Legs),
        CoachExercise("dumbbell_deadlift", MovementPattern.Hinge, back = true),
        CoachExercise("dumbbell_row", MovementPattern.Back, back = true),
        CoachExercise("dumbbell_press", MovementPattern.Push),
        CoachExercise("biceps_curl", MovementPattern.Back),
        // Resistance band.
        CoachExercise("band_row", MovementPattern.Back),
        CoachExercise("band_pulldown", MovementPattern.Back),
        CoachExercise("band_chest_press", MovementPattern.Push),
        // Barbell, at the gym. Heavier, so not for beginners.
        CoachExercise("front_squat", MovementPattern.Legs, level = StrengthLevel.Intermediate, knees = true),
        CoachExercise("deadlift", MovementPattern.Hinge, level = StrengthLevel.Intermediate, back = true),
        CoachExercise("barbell_row", MovementPattern.Back, level = StrengthLevel.Intermediate, back = true),
        // Balance, only from CoachPlanner.BALANCE_AGE, when WHO advises it.
        CoachExercise("single_leg_stand", MovementPattern.Balance, Dosage.Seconds, perSide = true),
        CoachExercise("single_leg_hinge", MovementPattern.Balance, perSide = true, level = StrengthLevel.Intermediate),
    )

    fun byId(id: String): CoachExercise? = exercises.firstOrNull { it.id == id }
}
