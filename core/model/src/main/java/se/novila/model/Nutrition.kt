package se.novila.model

import kotlin.math.roundToInt

/**
 * What the nutrition score is made of, from the Swedish Food Agency's dietary advice
 * (Livsmedelsverkets kostråd): plenty of vegetables and fruit, protein and whole grain at
 * regular meals, and water to drink. [goal] is portions per day; [points] is the group's share
 * of the 100-point score, weighted by how much it matters for a healthy diet. Water's goal is the user's own
 * ([Goals.waterGlasses], see [dailyGoal]); [Water]'s [goal] is the default.
 */
enum class FoodGroup(val goal: Int, val points: Int) {
    /** Vegetables, fruit and berries, about 100 g a portion: 500 g a day. */
    Vegetables(goal = 5, points = 30),

    /** Regular meals: breakfast, lunch and dinner. */
    Meals(goal = 3, points = 20),

    /** Fish, beans and lentils, eggs, poultry or meat. */
    Protein(goal = 3, points = 20),

    /** Whole grain bread, pasta, rice, oats. */
    WholeGrain(goal = 3, points = 15),

    /** Glasses of water. */
    Water(goal = WATER_GOAL, points = 15),
}

/** Sweets, pastries, crisps and soft drinks: one a day is free, every one after that costs points. */
const val FREE_TREATS = 1
const val TREAT_PENALTY = 5
const val MAX_TREAT_PENALTY = 20

/** A nutrition score from this many points counts as a good day. */
const val GOOD_NUTRITION_SCORE = 70

/** Portions logged per day. Water is logged as [DayEntry.waterGlasses]. */
data class FoodLog(
    val vegetables: Int = 0,
    val meals: Int = 0,
    val protein: Int = 0,
    val wholeGrain: Int = 0,
    val treats: Int = 0,
)

/** Portions a day for this group: the dietary advice, or for water the user's own goal. */
fun FoodGroup.dailyGoal(goals: Goals = Goals()): Int = if (this == FoodGroup.Water) goals.waterGlasses else goal

fun DayEntry.portions(group: FoodGroup): Int = when (group) {
    FoodGroup.Vegetables -> food.vegetables
    FoodGroup.Meals -> food.meals
    FoodGroup.Protein -> food.protein
    FoodGroup.WholeGrain -> food.wholeGrain
    FoodGroup.Water -> waterGlasses
}

fun DayEntry.withPortions(group: FoodGroup, delta: Int): DayEntry {
    val count = (portions(group) + delta).coerceAtLeast(0)
    return when (group) {
        FoodGroup.Vegetables -> copy(food = food.copy(vegetables = count))
        FoodGroup.Meals -> copy(food = food.copy(meals = count))
        FoodGroup.Protein -> copy(food = food.copy(protein = count))
        FoodGroup.WholeGrain -> copy(food = food.copy(wholeGrain = count))
        FoodGroup.Water -> copy(waterGlasses = count)
    }
}

fun DayEntry.withTreats(delta: Int): DayEntry = copy(food = food.copy(treats = (food.treats + delta).coerceAtLeast(0)))

/** One food group's part of the score. */
data class ScorePart(val group: FoodGroup, val portions: Int, val points: Float) {
    val progress: Float get() = points / group.points
}

/** Today's nutrition: the [total] from 0 to 100, its [parts] and what treats cost. */
data class NutritionScore(val total: Int, val parts: List<ScorePart>, val treatPenalty: Int) {
    val progress: Float get() = total / 100f
}

/**
 * Each food group earns points in proportion to how close it is to its daily goal, capped at the
 * goal: five portions of vegetables is the advice, a sixth earns nothing more, so no single group
 * can make up for a missing one. Treats beyond [FREE_TREATS] subtract points. Null before anything
 * is logged, so an empty day reads as "not logged" rather than as a bad score. Water counts towards the user's
 * own goal in [goals].
 */
fun DayEntry.nutritionScore(goals: Goals = Goals()): NutritionScore? {
    if (FoodGroup.entries.all { portions(it) == 0 } && food.treats == 0) return null
    val parts = FoodGroup.entries.map { group ->
        val count = portions(group)
        val goal = group.dailyGoal(goals)
        ScorePart(group, count, group.points * (count.toFloat() / goal).coerceAtMost(1f))
    }
    val penalty = ((food.treats - FREE_TREATS).coerceAtLeast(0) * TREAT_PENALTY).coerceAtMost(MAX_TREAT_PENALTY)
    val total = (parts.sumOf { it.points.toDouble() }.roundToInt() - penalty).coerceIn(0, 100)
    return NutritionScore(total, parts, penalty)
}

/** The next portion that raises the score the most, and by how many points. */
data class NextPortion(val group: FoodGroup, val points: Int)

/**
 * The smartest next step for today's nutrition: the group where one more portion is worth the
 * most points. Null when every group is at its goal.
 */
fun DayEntry.nextPortion(goals: Goals = Goals()): NextPortion? = FoodGroup.entries
    .filter { portions(it) < it.dailyGoal(goals) }
    .maxByOrNull { it.points.toFloat() / it.dailyGoal(goals) }
    ?.let { NextPortion(it, (it.points.toFloat() / it.dailyGoal(goals)).roundToInt()) }
