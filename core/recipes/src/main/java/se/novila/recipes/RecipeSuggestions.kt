package se.novila.recipes

import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.Goals
import se.novila.model.nutritionScore

/** A recipe to suggest, and how many [points] one serving would add to the day's nutrition score. */
data class RecipeSuggestion(val recipe: Recipe, val points: Int)

/** One of the user's own recipes to suggest, and how many [points] one serving would add to the day's score. */
data class OwnRecipeSuggestion(val recipe: OwnRecipe, val points: Int)

/**
 * The [slot] suggested for now and the recipes for it, best first: Novila's [recipes] and the user's [own]. Empty when
 * no recipe fits the preferences.
 */
data class RecipeSuggestions(
    val slot: MealSlot,
    val recipes: List<RecipeSuggestion>,
    val own: List<OwnRecipeSuggestion> = emptyList(),
)

/** Breakfast is suggested until this hour, as long as no meal is logged yet. */
const val BREAKFAST_UNTIL_HOUR = 10

/** Recipes suggested at a time. */
const val SUGGESTED_RECIPES = 3

/** The user's own recipes suggested at a time, besides Novila's. */
const val SUGGESTED_OWN_RECIPES = 2

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
 * one recipe; then the ones cooked on more of the last days ([RecipeBook.cookedDays]) go first within the group. The
 * user's own recipes in [book] are suggested alongside, by points and then by how often they were cooked. Always the
 * same result for the same input.
 */
fun suggestRecipes(
    day: DayEntry,
    hour: Int,
    preferences: FoodPreferences,
    goals: Goals = Goals(),
    book: RecipeBook = RecipeBook(),
): RecipeSuggestions {
    val slot = mealSlotAt(day, hour)
    val before = day.nutritionScore(goals)?.total ?: 0
    fun points(after: DayEntry) = (after.nutritionScore(goals)?.total ?: 0) - before
    val candidates = Recipe.entries.filter { it.slot == slot && preferences.allows(it) }
    val recipes = candidates
        .map { RecipeSuggestion(it, points(day.withRecipe(it))) }
        .groupBy { it.points }
        .entries
        .sortedByDescending { it.key }
        .flatMap { (_, samePoints) ->
            // Rotate the whole tie group, one step a day: an affine shift of every key would not change their order.
            val offset = Math.floorMod(day.date.toEpochDay(), samePoints.size.toLong()).toInt()
            (samePoints.drop(offset) + samePoints.take(offset))
                .sortedByDescending { book.cookedDays(it.recipe.key, day.date) }
        }
        .take(SUGGESTED_RECIPES)
    val own = book.recipes
        .filter { it.slot == slot && preferences.allows(it) }
        .map { OwnRecipeSuggestion(it, points(day.withRecipe(it))) }
        .sortedWith(
            compareByDescending<OwnRecipeSuggestion> { it.points }
                .thenByDescending { book.cookedDays(it.recipe.key, day.date) }
                .thenBy { it.recipe.id },
        )
        .take(SUGGESTED_OWN_RECIPES)
    return RecipeSuggestions(slot, recipes, own)
}
