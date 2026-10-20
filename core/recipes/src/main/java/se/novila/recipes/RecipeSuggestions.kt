package se.novila.recipes

import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.nutritionScore

/** A recipe to suggest, and how many [points] one serving would add to the day's nutrition score. */
data class RecipeSuggestion(val recipe: Recipe, val points: Int)

/** The [slot] suggested for now and the recipes for it, best first. Empty when no recipe fits the preferences. */
data class RecipeSuggestions(val slot: MealSlot, val recipes: List<RecipeSuggestion>)

/** Breakfast is suggested until this hour, as long as no meal is logged yet. */
const val BREAKFAST_UNTIL_HOUR = 10

/** Recipes suggested at a time. */
const val SUGGESTED_RECIPES = 3

/**
 * Breakfast in the morning before the first meal is logged, a snack once the day's meals are all logged, otherwise a
 * main meal.
 */
fun mealSlotAt(day: DayEntry, hour: Int): MealSlot {
    val meals = day.food.meals
    return when {
        meals >= FoodGroup.Meals.goal -> MealSlot.Snack
        meals == 0 && hour < BREAKFAST_UNTIL_HOUR -> MealSlot.Breakfast
        else -> MealSlot.Main
    }
}

/**
 * Recipes for what is missing from [day] at [hour]: of the recipes for the [mealSlotAt] that [preferences] allow,
 * those that would raise the nutrition score the most, measured against [goals]. Recipes worth as much are rotated
 * by the day within their points group, so the same ties don't come first every day as long as the group has more than
 * one recipe. Always the same result for the same input.
 */
fun suggestRecipes(day: DayEntry, hour: Int, preferences: FoodPreferences, goals: Goals = Goals()): RecipeSuggestions {
    val slot = mealSlotAt(day, hour)
    val before = day.nutritionScore(goals)?.total ?: 0
    val candidates = Recipe.entries.filter { it.slot == slot && preferences.allows(it) }
    val recipes = candidates
        .map { RecipeSuggestion(it, (day.withRecipe(it).nutritionScore(goals)?.total ?: 0) - before) }
        .groupBy { it.points }
        .entries
        .sortedByDescending { it.key }
        .flatMap { (_, samePoints) ->
            // Rotate the whole tie group, one step a day: an affine shift of every key would not change their order.
            val offset = Math.floorMod(day.date.toEpochDay(), samePoints.size.toLong()).toInt()
            samePoints.drop(offset) + samePoints.take(offset)
        }
        .take(SUGGESTED_RECIPES)
    return RecipeSuggestions(slot, recipes)
}
