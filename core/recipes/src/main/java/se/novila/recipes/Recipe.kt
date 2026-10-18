package se.novila.recipes

import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.withPortions

/** When in the day a recipe is eaten. Breakfast and main meals count as a meal; a snack does not. */
enum class MealSlot(val isMeal: Boolean) {
    Breakfast(isMeal = true),
    Main(isMeal = true),
    Snack(isMeal = false),
}

/**
 * Novila's own recipes, one serving each. Every recipe says what one serving gives in the nutrition score's food
 * groups ([vegetables] at about 100 g a portion, [protein], [wholeGrain]; potatoes are not vegetables in the dietary
 * advice), which [diet] it fits, which of the labelled [allergens] its ingredients contain and how many [minutes] it
 * takes. Names, ingredients and steps are in `:feature:nutrition`'s strings, in English and Swedish.
 *
 * The allergens are those of the ingredients as written; ready-made products such as stock or spice mixes vary,
 * so the app always asks the user to check the packaging.
 */
enum class Recipe(
    val slot: MealSlot,
    val diet: Diet,
    val minutes: Int,
    val vegetables: Int,
    val protein: Int,
    val wholeGrain: Int,
    val allergens: Set<Allergen>,
) {
    OvernightOats(
        MealSlot.Breakfast,
        Diet.Vegetarian,
        minutes = 5,
        vegetables = 2,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Milk),
    ),
    EggSandwich(
        MealSlot.Breakfast,
        Diet.Vegetarian,
        minutes = 10,
        vegetables = 1,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Eggs),
    ),
    BerrySmoothie(
        MealSlot.Breakfast,
        Diet.Vegan,
        minutes = 5,
        vegetables = 2,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten),
    ),
    LentilBolognese(
        MealSlot.Main,
        Diet.Vegan,
        minutes = 30,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten),
    ),
    BakedSalmon(
        MealSlot.Main,
        Diet.Pescetarian,
        minutes = 30,
        vegetables = 2,
        protein = 1,
        wholeGrain = 0,
        allergens = setOf(Allergen.Fish),
    ),
    ChickpeaCurry(
        MealSlot.Main,
        Diet.Vegan,
        minutes = 30,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = emptySet(),
    ),
    ChickenStirFry(
        MealSlot.Main,
        Diet.Everything,
        minutes = 30,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Soy),
    ),
    VegetableOmelette(
        MealSlot.Main,
        Diet.Vegetarian,
        minutes = 15,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Eggs),
    ),
    TunaWheatSalad(
        MealSlot.Main,
        Diet.Pescetarian,
        minutes = 20,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Fish),
    ),
    BeanSoup(
        MealSlot.Main,
        Diet.Vegan,
        minutes = 30,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Celery),
    ),
    ChickenBulgurSalad(
        MealSlot.Main,
        Diet.Everything,
        minutes = 25,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Milk),
    ),
    AppleAndNuts(
        MealSlot.Snack,
        Diet.Vegan,
        minutes = 2,
        vegetables = 1,
        protein = 0,
        wholeGrain = 0,
        allergens = setOf(Allergen.TreeNuts),
    ),
    CarrotsAndHummus(
        MealSlot.Snack,
        Diet.Vegan,
        minutes = 5,
        vegetables = 1,
        protein = 0,
        wholeGrain = 0,
        allergens = setOf(Allergen.Sesame),
    ),
    YogurtWithBerries(
        MealSlot.Snack,
        Diet.Vegetarian,
        minutes = 3,
        vegetables = 1,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Milk),
    ),
    ;

    /** Portions of [group] in one serving. A breakfast or main meal is one of the day's meals. */
    fun portions(group: FoodGroup): Int = when (group) {
        FoodGroup.Vegetables -> vegetables
        FoodGroup.Protein -> protein
        FoodGroup.WholeGrain -> wholeGrain
        FoodGroup.Meals -> if (slot.isMeal) 1 else 0
        FoodGroup.Water -> 0
    }
}

/** The day with one serving of [recipe] logged: its portions added to each food group. */
fun DayEntry.withRecipe(recipe: Recipe): DayEntry =
    FoodGroup.entries.fold(this) { day, group -> day.withPortions(group, recipe.portions(group)) }
