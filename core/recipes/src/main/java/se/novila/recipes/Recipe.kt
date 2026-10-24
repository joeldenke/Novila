package se.novila.recipes

import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.withFish
import se.novila.model.withPortions
import se.novila.model.withRedMeat

/** When in the day a recipe is eaten. Breakfast and main meals count as a meal; a snack does not. */
enum class MealSlot(val isMeal: Boolean) {
    Breakfast(isMeal = true),
    Main(isMeal = true),
    Snack(isMeal = false),
}

/**
 * The ingredient families a recipe belongs to, independent of the nutrition score's [FoodGroup]s. They are only used
 * to describe and test the library (so every family is covered, and a diet never contains an animal family it does
 * not allow); the score still counts [FoodGroup] portions.
 */
enum class FoodKind {
    Vegetables,
    Fruit,
    FishSeafood,
    Meat,
    Poultry,
    Dairy,
    Eggs,
    Grains,
    Potatoes,
    Legumes,
    NutsSeeds,
    Fats,
}

/** How long a recipe takes, for coverage and tests: quick is under 15 minutes, long is over 30. */
enum class RecipeTime {
    Quick,
    Medium,
    Long,
    ;

    companion object {
        fun of(minutes: Int): RecipeTime = when {
            minutes < 15 -> Quick
            minutes <= 30 -> Medium
            else -> Long
        }
    }
}

/**
 * Novila's own recipes, one serving each, and the single source of the recipe library.
 *
 * Every recipe says:
 * - [slot]: when it is eaten (breakfast, main meal or snack).
 * - [diet]: the least strict diet that may eat it. A recipe fits a user when its diet is the same as or stricter than
 *   the user's ([Diet] is ordered from most to least inclusive), so a vegan recipe fits everyone.
 * - [minutes]: preparation and cooking time, used for [RecipeTime] and the user's time limit.
 * - [redMeat]: portions of red meat or charcuterie (any recipe with [FoodKind.Meat]), counted apart from [protein].
 * - [fish]: portions of fish or seafood, counted apart from [protein] so the week's fish can be told; a recipe with
 *   only a little fish ([FoodKind.FishSeafood] as a topping) may give none.
 * - [vegetables], [protein] and [wholeGrain]: portions one serving gives in the nutrition score's groups (vegetables
 *   at about 100 g, potatoes are not vegetables in the dietary advice). [portions] adds meals from [slot]; water is
 *   always 0.
 * - [allergens]: the 14 labelled allergens the ingredients contain as written.
 * - [kinds]: the ingredient families it contains, including ones the score does not count separately (fruit, fish,
 *   meat, dairy, potatoes, fats).
 *
 * Names, ingredients and steps live in `:feature:nutrition`'s `strings.xml` in English and Swedish, keyed by the enum
 * name (`recipe_overnight_oats`, `recipe_overnight_oats_ingredients`, `recipe_overnight_oats_steps`) and mapped in
 * `RecipeText.kt`. The allergens are those of the ingredients as written; ready-made products such as stock or spice
 * mixes vary, so the app always asks the user to check the packaging.
 *
 * Adding a recipe: add an entry here with all fields, add the three string resources in both languages, and add its
 * line to `RecipeText.kt`. `RecipeDataTest` checks that the entry is coherent. The target is at least 40 recipes, with
 * at least five the user's diet allows in every meal slot (so suggestions have alternatives to rotate between), every
 * [FoodKind] represented and all three [RecipeTime] buckets covered.
 */
enum class Recipe(
    val slot: MealSlot,
    val diet: Diet,
    val minutes: Int,
    val vegetables: Int,
    val protein: Int,
    val wholeGrain: Int,
    val allergens: Set<Allergen>,
    val kinds: Set<FoodKind>,
    val redMeat: Int = 0,
    val fish: Int = 0,
) {
    OvernightOats(
        MealSlot.Breakfast,
        Diet.Vegetarian,
        minutes = 5,
        vegetables = 2,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Milk),
        kinds = setOf(FoodKind.Grains, FoodKind.Dairy, FoodKind.Fruit),
    ),
    EggSandwich(
        MealSlot.Breakfast,
        Diet.Vegetarian,
        minutes = 10,
        vegetables = 1,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Eggs),
        kinds = setOf(FoodKind.Grains, FoodKind.Eggs, FoodKind.Vegetables),
    ),
    BerrySmoothie(
        MealSlot.Breakfast,
        Diet.Vegan,
        minutes = 5,
        vegetables = 2,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten),
        kinds = setOf(FoodKind.Fruit, FoodKind.Vegetables, FoodKind.Grains),
    ),
    RyePorridgeWithLingonberries(
        MealSlot.Breakfast,
        Diet.Vegan,
        minutes = 10,
        vegetables = 1,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten),
        kinds = setOf(FoodKind.Grains, FoodKind.Fruit),
    ),
    AvocadoToast(
        MealSlot.Breakfast,
        Diet.Vegan,
        minutes = 10,
        vegetables = 2,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten),
        kinds = setOf(FoodKind.Grains, FoodKind.Vegetables, FoodKind.Fats),
    ),
    ScrambledEggsWithSpinach(
        MealSlot.Breakfast,
        Diet.Vegetarian,
        minutes = 10,
        vegetables = 1,
        protein = 1,
        wholeGrain = 0,
        allergens = setOf(Allergen.Eggs, Allergen.Milk),
        kinds = setOf(FoodKind.Eggs, FoodKind.Vegetables, FoodKind.Dairy, FoodKind.Fats),
    ),
    SkyrWithMuesli(
        MealSlot.Breakfast,
        Diet.Vegetarian,
        minutes = 5,
        vegetables = 1,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Milk, Allergen.TreeNuts),
        kinds = setOf(FoodKind.Dairy, FoodKind.Grains, FoodKind.Fruit, FoodKind.NutsSeeds),
    ),
    SmokedSalmonRye(
        MealSlot.Breakfast,
        Diet.Pescetarian,
        minutes = 10,
        vegetables = 2,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Fish),
        kinds = setOf(FoodKind.FishSeafood, FoodKind.Grains, FoodKind.Vegetables),
    ),
    BananaOatPancakes(
        MealSlot.Breakfast,
        Diet.Vegan,
        minutes = 15,
        vegetables = 1,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten),
        kinds = setOf(FoodKind.Fruit, FoodKind.Grains, FoodKind.Fats),
    ),
    MangoSmoothieBowl(
        MealSlot.Breakfast,
        Diet.Vegan,
        minutes = 10,
        vegetables = 2,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten),
        kinds = setOf(FoodKind.Fruit, FoodKind.Grains, FoodKind.Vegetables),
    ),
    TofuScrambleOnRye(
        MealSlot.Breakfast,
        Diet.Vegan,
        minutes = 15,
        vegetables = 1,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Soy),
        kinds = setOf(FoodKind.Legumes, FoodKind.Grains, FoodKind.Vegetables, FoodKind.Fats),
    ),
    BreakfastBurrito(
        MealSlot.Breakfast,
        Diet.Everything,
        minutes = 15,
        vegetables = 1,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Eggs, Allergen.Milk),
        kinds = setOf(FoodKind.Eggs, FoodKind.Grains, FoodKind.Vegetables, FoodKind.Dairy),
    ),
    QuickBeanWrap(
        MealSlot.Main,
        Diet.Vegan,
        minutes = 10,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten),
        kinds = setOf(FoodKind.Grains, FoodKind.Legumes, FoodKind.Vegetables, FoodKind.Fats),
    ),
    LentilBolognese(
        MealSlot.Main,
        Diet.Vegan,
        minutes = 30,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten),
        kinds = setOf(FoodKind.Legumes, FoodKind.Vegetables, FoodKind.Grains, FoodKind.Fats),
    ),
    BakedSalmon(
        MealSlot.Main,
        Diet.Pescetarian,
        minutes = 30,
        vegetables = 2,
        protein = 0,
        wholeGrain = 0,
        allergens = setOf(Allergen.Fish),
        kinds = setOf(FoodKind.FishSeafood, FoodKind.Vegetables, FoodKind.Potatoes, FoodKind.Fats),
        fish = 1,
    ),
    ChickpeaCurry(
        MealSlot.Main,
        Diet.Vegan,
        minutes = 30,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = emptySet(),
        kinds = setOf(FoodKind.Legumes, FoodKind.Vegetables, FoodKind.Grains, FoodKind.Fats),
    ),
    ChickenStirFry(
        MealSlot.Main,
        Diet.Everything,
        minutes = 30,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Soy),
        kinds = setOf(FoodKind.Poultry, FoodKind.Vegetables, FoodKind.Grains, FoodKind.Fats),
    ),
    VegetableOmelette(
        MealSlot.Main,
        Diet.Vegetarian,
        minutes = 15,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Eggs),
        kinds = setOf(FoodKind.Eggs, FoodKind.Vegetables, FoodKind.Grains, FoodKind.Fats),
    ),
    TunaWheatSalad(
        MealSlot.Main,
        Diet.Pescetarian,
        minutes = 20,
        vegetables = 2,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Fish),
        kinds = setOf(FoodKind.FishSeafood, FoodKind.Grains, FoodKind.Vegetables, FoodKind.Fats),
        fish = 1,
    ),
    BeanSoup(
        MealSlot.Main,
        Diet.Vegan,
        minutes = 30,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Celery),
        kinds = setOf(FoodKind.Legumes, FoodKind.Vegetables, FoodKind.Grains, FoodKind.Fats),
    ),
    ChickenBulgurSalad(
        MealSlot.Main,
        Diet.Everything,
        minutes = 25,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Milk),
        kinds = setOf(FoodKind.Poultry, FoodKind.Grains, FoodKind.Vegetables, FoodKind.Dairy),
    ),
    SwedishMeatballs(
        MealSlot.Main,
        Diet.Everything,
        minutes = 30,
        vegetables = 2,
        protein = 0,
        wholeGrain = 0,
        allergens = setOf(Allergen.Gluten, Allergen.Eggs, Allergen.Milk),
        kinds = setOf(FoodKind.Meat, FoodKind.Potatoes, FoodKind.Dairy, FoodKind.Eggs, FoodKind.Grains),
        redMeat = 1,
    ),
    FalafelWithCouscous(
        MealSlot.Main,
        Diet.Vegan,
        minutes = 30,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Sesame),
        kinds = setOf(FoodKind.Legumes, FoodKind.Grains, FoodKind.Vegetables, FoodKind.NutsSeeds, FoodKind.Fats),
    ),
    SalmonSoup(
        MealSlot.Main,
        Diet.Pescetarian,
        minutes = 30,
        vegetables = 2,
        protein = 0,
        wholeGrain = 0,
        allergens = setOf(Allergen.Fish, Allergen.Milk),
        kinds = setOf(FoodKind.FishSeafood, FoodKind.Vegetables, FoodKind.Potatoes, FoodKind.Dairy),
        fish = 1,
    ),
    ShrimpPasta(
        MealSlot.Main,
        Diet.Pescetarian,
        minutes = 15,
        vegetables = 2,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Crustaceans),
        kinds = setOf(FoodKind.FishSeafood, FoodKind.Grains, FoodKind.Vegetables, FoodKind.Fats),
        fish = 1,
    ),
    TofuVegetableCurry(
        MealSlot.Main,
        Diet.Vegan,
        minutes = 30,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Soy),
        kinds = setOf(FoodKind.Legumes, FoodKind.Vegetables, FoodKind.Grains, FoodKind.Fats),
    ),
    SwedishPeaSoup(
        MealSlot.Main,
        Diet.Everything,
        minutes = 40,
        vegetables = 1,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Mustard),
        kinds = setOf(FoodKind.Legumes, FoodKind.Meat, FoodKind.Vegetables, FoodKind.Grains, FoodKind.Fats),
        redMeat = 1,
    ),
    RootVegetableLentilSoup(
        MealSlot.Main,
        Diet.Vegan,
        minutes = 30,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Celery),
        kinds = setOf(FoodKind.Legumes, FoodKind.Vegetables, FoodKind.Grains, FoodKind.Fats),
    ),
    ChickenTikkaWithRice(
        MealSlot.Main,
        Diet.Everything,
        minutes = 40,
        vegetables = 1,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Milk),
        kinds = setOf(FoodKind.Poultry, FoodKind.Dairy, FoodKind.Grains, FoodKind.Vegetables),
    ),
    CodWithDill(
        MealSlot.Main,
        Diet.Pescetarian,
        minutes = 25,
        vegetables = 1,
        protein = 0,
        wholeGrain = 0,
        allergens = setOf(Allergen.Fish, Allergen.Milk),
        kinds = setOf(FoodKind.FishSeafood, FoodKind.Potatoes, FoodKind.Vegetables, FoodKind.Dairy, FoodKind.Fats),
        fish = 1,
    ),
    HalloumiSkewers(
        MealSlot.Main,
        Diet.Vegetarian,
        minutes = 20,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Milk),
        kinds = setOf(FoodKind.Dairy, FoodKind.Vegetables, FoodKind.Grains, FoodKind.Fats),
    ),
    JacketPotatoWithBeans(
        MealSlot.Main,
        Diet.Vegetarian,
        minutes = 15,
        vegetables = 1,
        protein = 1,
        wholeGrain = 0,
        allergens = setOf(Allergen.Milk),
        kinds = setOf(FoodKind.Potatoes, FoodKind.Legumes, FoodKind.Dairy, FoodKind.Vegetables),
    ),
    BuckwheatSalad(
        MealSlot.Main,
        Diet.Vegan,
        minutes = 25,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = emptySet(),
        kinds = setOf(FoodKind.Grains, FoodKind.Vegetables, FoodKind.Legumes, FoodKind.Fats),
    ),
    FishCakesWithPotatoes(
        MealSlot.Main,
        Diet.Pescetarian,
        minutes = 30,
        vegetables = 1,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Fish, Allergen.Eggs, Allergen.Milk),
        kinds = setOf(FoodKind.FishSeafood, FoodKind.Potatoes, FoodKind.Grains, FoodKind.Eggs, FoodKind.Dairy),
        fish = 1,
    ),
    SpinachRicottaLasagna(
        MealSlot.Main,
        Diet.Vegetarian,
        minutes = 45,
        vegetables = 2,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Milk, Allergen.Eggs),
        kinds = setOf(FoodKind.Dairy, FoodKind.Eggs, FoodKind.Grains, FoodKind.Vegetables, FoodKind.Fats),
    ),
    BeefAndRootVegetableStew(
        MealSlot.Main,
        Diet.Everything,
        minutes = 45,
        vegetables = 2,
        protein = 0,
        wholeGrain = 0,
        allergens = setOf(Allergen.Celery),
        kinds = setOf(FoodKind.Meat, FoodKind.Vegetables, FoodKind.Potatoes, FoodKind.Fats),
        redMeat = 1,
    ),
    MinuteSteakWithTomatoSalad(
        MealSlot.Main,
        Diet.Everything,
        minutes = 15,
        vegetables = 2,
        protein = 0,
        wholeGrain = 0,
        allergens = emptySet(),
        kinds = setOf(FoodKind.Meat, FoodKind.Vegetables, FoodKind.Fats),
        redMeat = 1,
    ),
    RiceNoodlesWithPrawns(
        MealSlot.Main,
        Diet.Pescetarian,
        minutes = 20,
        vegetables = 2,
        protein = 0,
        wholeGrain = 1,
        // Soy sauce is brewed with wheat.
        allergens = setOf(Allergen.Crustaceans, Allergen.Soy, Allergen.Gluten),
        kinds = setOf(FoodKind.FishSeafood, FoodKind.Grains, FoodKind.Vegetables, FoodKind.Fats),
        fish = 1,
    ),
    ChanaMasala(
        MealSlot.Main,
        Diet.Vegan,
        minutes = 30,
        vegetables = 1,
        protein = 1,
        wholeGrain = 1,
        allergens = emptySet(),
        kinds = setOf(FoodKind.Legumes, FoodKind.Vegetables, FoodKind.Grains, FoodKind.Fats),
    ),
    AppleAndNuts(
        MealSlot.Snack,
        Diet.Vegan,
        minutes = 2,
        vegetables = 1,
        protein = 0,
        wholeGrain = 0,
        allergens = setOf(Allergen.TreeNuts),
        kinds = setOf(FoodKind.Fruit, FoodKind.NutsSeeds),
    ),
    CarrotsAndHummus(
        MealSlot.Snack,
        Diet.Vegan,
        minutes = 5,
        vegetables = 1,
        protein = 0,
        wholeGrain = 0,
        allergens = setOf(Allergen.Sesame),
        kinds = setOf(FoodKind.Vegetables, FoodKind.Legumes, FoodKind.NutsSeeds, FoodKind.Fats),
    ),
    YogurtWithBerries(
        MealSlot.Snack,
        Diet.Vegetarian,
        minutes = 3,
        vegetables = 1,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Milk),
        kinds = setOf(FoodKind.Dairy, FoodKind.Fruit, FoodKind.Grains),
    ),
    BananaWithPeanutButter(
        MealSlot.Snack,
        Diet.Vegan,
        minutes = 2,
        vegetables = 1,
        protein = 0,
        wholeGrain = 0,
        allergens = setOf(Allergen.Peanuts),
        kinds = setOf(FoodKind.Fruit, FoodKind.NutsSeeds, FoodKind.Fats),
    ),
    Edamame(
        MealSlot.Snack,
        Diet.Vegan,
        minutes = 10,
        vegetables = 1,
        protein = 1,
        wholeGrain = 0,
        allergens = setOf(Allergen.Soy),
        kinds = setOf(FoodKind.Legumes),
    ),
    RoastedChickpeas(
        MealSlot.Snack,
        Diet.Vegan,
        minutes = 15,
        vegetables = 1,
        protein = 1,
        wholeGrain = 0,
        allergens = emptySet(),
        kinds = setOf(FoodKind.Legumes, FoodKind.Fats),
    ),
    CottageCheeseWithTomato(
        MealSlot.Snack,
        Diet.Vegetarian,
        minutes = 3,
        vegetables = 1,
        protein = 1,
        wholeGrain = 0,
        allergens = setOf(Allergen.Milk),
        kinds = setOf(FoodKind.Dairy, FoodKind.Vegetables),
    ),
    BoiledEggWithCrispbread(
        MealSlot.Snack,
        Diet.Vegetarian,
        minutes = 10,
        vegetables = 1,
        protein = 1,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Eggs),
        kinds = setOf(FoodKind.Eggs, FoodKind.Grains, FoodKind.Vegetables),
    ),
    TunaRyeCrispbread(
        MealSlot.Snack,
        Diet.Pescetarian,
        minutes = 5,
        vegetables = 1,
        protein = 0,
        wholeGrain = 1,
        allergens = setOf(Allergen.Gluten, Allergen.Fish, Allergen.Milk),
        kinds = setOf(FoodKind.FishSeafood, FoodKind.Grains, FoodKind.Dairy, FoodKind.Vegetables),
        fish = 1,
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

    /** Whether one serving fills any of [group]. */
    fun fills(group: FoodGroup): Boolean = portions(group) > 0

    /** How quick the recipe is, from its [minutes]. */
    val time: RecipeTime get() = RecipeTime.of(minutes)
}

/** The day with one serving of [recipe] logged: its portions added to each food group, its red meat and its fish. */
fun DayEntry.withRecipe(recipe: Recipe): DayEntry =
    FoodGroup.entries.fold(this) { day, group -> day.withPortions(group, recipe.portions(group)) }
        .withRedMeat(recipe.redMeat)
        .withFish(recipe.fish)
