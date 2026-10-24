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

    /**
     * Fish, beans and lentils, eggs or poultry. Red meat and charcuterie are logged on their own ([FoodLog.redMeat])
     * and count for less ([RED_MEAT_CREDIT]), since the advice is to eat less of them.
     */
    Protein(goal = 3, points = 20),

    /** Whole grain bread, pasta, rice, oats. */
    WholeGrain(goal = 3, points = 15),

    /** Glasses of water, about 2 dl: the drinks that count towards the water goal ([DrinkKind.hydrates]). */
    Water(goal = WATER_GOAL, points = 15),
}

/** Sweets, pastries, crisps and soft drinks: one a day is free, every one after that costs points. */
const val FREE_TREATS = 1
const val TREAT_PENALTY = 5
const val MAX_TREAT_PENALTY = 20

/**
 * Red meat (beef, pork, lamb, game) and charcuterie: the dietary advice is at most 350 g a week, of which as little
 * charcuterie as possible, and to choose fish, beans and lentils, eggs or poultry more often. A portion counts as
 * [RED_MEAT_CREDIT] of a protein portion, and each portion beyond [FREE_RED_MEAT] a day costs [RED_MEAT_PENALTY]
 * points, at most [MAX_RED_MEAT_PENALTY].
 */
const val RED_MEAT_CREDIT = 0.5f
const val FREE_RED_MEAT = 1
const val RED_MEAT_PENALTY = 5
const val MAX_RED_MEAT_PENALTY = 10

/** A nutrition score from this many points counts as a good day. */
const val GOOD_NUTRITION_SCORE = 70

/**
 * Portions logged per day. Water is logged as drinks ([DayEntry.drinks]). [protein] is fish, beans and lentils, eggs
 * and poultry; [redMeat] is red meat and charcuterie, counted apart because the advice is to eat less of it.
 */
data class FoodLog(
    val vegetables: Int = 0,
    val meals: Int = 0,
    val protein: Int = 0,
    val wholeGrain: Int = 0,
    val treats: Int = 0,
    val redMeat: Int = 0,
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

/**
 * [delta] portions more or fewer of [group], never below zero. For water, more is glasses of water and fewer takes back
 * the last glasses of water logged.
 */
fun DayEntry.withPortions(group: FoodGroup, delta: Int): DayEntry {
    val count = (portions(group) + delta).coerceAtLeast(0)
    return when (group) {
        FoodGroup.Vegetables -> copy(food = food.copy(vegetables = count))
        FoodGroup.Meals -> copy(food = food.copy(meals = count))
        FoodGroup.Protein -> copy(food = food.copy(protein = count))
        FoodGroup.WholeGrain -> copy(food = food.copy(wholeGrain = count))
        FoodGroup.Water -> withWater(delta)
    }
}

private fun DayEntry.withWater(delta: Int): DayEntry {
    if (delta >= 0) return glassesOfWater(delta).fold(this) { day, glass -> day.withDrink(glass) }
    val glasses = drinks.withIndex().filter { it.value.kind == DrinkKind.Water }.takeLast(-delta).map { it.index }
    return copy(drinks = drinks.filterIndexed { index, _ -> index !in glasses })
}

fun DayEntry.withTreats(delta: Int): DayEntry = copy(food = food.copy(treats = (food.treats + delta).coerceAtLeast(0)))

fun DayEntry.withRedMeat(delta: Int): DayEntry =
    copy(food = food.copy(redMeat = (food.redMeat + delta).coerceAtLeast(0)))

/** One food group's part of the score. */
data class ScorePart(val group: FoodGroup, val portions: Int, val points: Float) {
    val progress: Float get() = points / group.points
}

/** Today's nutrition: the [total] from 0 to 100, its [parts] and what treats and red meat cost. */
data class NutritionScore(
    val total: Int,
    val parts: List<ScorePart>,
    val treatPenalty: Int,
    val redMeatPenalty: Int = 0,
) {
    val progress: Float get() = total / 100f
}

/**
 * Each food group earns points in proportion to how close it is to its daily goal, capped at the
 * goal: five portions of vegetables is the advice, a sixth earns nothing more, so no single group
 * can make up for a missing one. Red meat counts as [RED_MEAT_CREDIT] of a protein portion. Treats beyond
 * [FREE_TREATS] and red meat beyond [FREE_RED_MEAT] subtract points. Null before anything
 * is logged, so an empty day reads as "not logged" rather than as a bad score. Water counts towards the user's
 * own goal in [goals].
 */
fun DayEntry.nutritionScore(goals: Goals = Goals()): NutritionScore? {
    if (FoodGroup.entries.all { portions(it) == 0 } && food.treats == 0 && food.redMeat == 0) return null
    val parts = FoodGroup.entries.map { group ->
        val count = portions(group)
        val counted = count + if (group == FoodGroup.Protein) food.redMeat * RED_MEAT_CREDIT else 0f
        val goal = group.dailyGoal(goals)
        ScorePart(group, count, group.points * (counted / goal).coerceAtMost(1f))
    }
    val penalty = ((food.treats - FREE_TREATS).coerceAtLeast(0) * TREAT_PENALTY).coerceAtMost(MAX_TREAT_PENALTY)
    val redMeatPenalty = ((food.redMeat - FREE_RED_MEAT).coerceAtLeast(0) * RED_MEAT_PENALTY)
        .coerceAtMost(MAX_RED_MEAT_PENALTY)
    val total = (parts.sumOf { it.points.toDouble() }.roundToInt() - penalty - redMeatPenalty).coerceIn(0, 100)
    return NutritionScore(total, parts, penalty, redMeatPenalty)
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
