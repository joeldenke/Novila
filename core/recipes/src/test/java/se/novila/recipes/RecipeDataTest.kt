package se.novila.recipes

import org.junit.Assert.assertTrue
import org.junit.Test
import se.novila.model.FoodGroup

/** The recipe library itself: coherent data, and enough of it to suggest alternatives for every diet and slot. */
class RecipeDataTest {
    /** At least five recipes the user can eat in each slot, so suggestions have something to rotate between. */
    private val minimumPerSlot = 5

    private val animalFamilies = setOf(
        FoodKind.FishSeafood,
        FoodKind.Meat,
        FoodKind.Poultry,
        FoodKind.Dairy,
        FoodKind.Eggs,
    )

    @Test fun `the library is large enough for alternatives in every slot and diet`() {
        assertTrue("only ${Recipe.entries.size} recipes", Recipe.entries.size >= 40)
        MealSlot.entries.forEach { slot ->
            Diet.entries.forEach { diet ->
                val allowed = Recipe.entries.count { it.slot == slot && FoodPreferences(diet).allows(it) }
                assertTrue("$slot allows only $allowed recipes for $diet", allowed >= minimumPerSlot)
            }
        }
        Diet.entries.forEach { diet ->
            assertTrue("no recipe of its own diet $diet", Recipe.entries.any { it.diet == diet })
        }
    }

    @Test fun `every ingredient family and every time bucket is covered`() {
        FoodKind.entries.forEach { kind ->
            assertTrue("no recipe contains $kind", Recipe.entries.any { kind in it.kinds })
        }
        RecipeTime.entries.forEach { time ->
            assertTrue("no $time recipe", Recipe.entries.any { it.time == time })
        }
    }

    @Test fun `every recipe has coherent portions, time and ingredient families`() {
        Recipe.entries.forEach { recipe ->
            val name = recipe.name
            assertTrue("$name has no time", recipe.minutes > 0)
            assertTrue("$name has too many vegetables", recipe.vegetables in 0..3)
            assertTrue("$name has too many protein portions", recipe.protein in 0..2)
            assertTrue("$name has too many whole grain portions", recipe.wholeGrain in 0..2)
            assertTrue("$name fills nothing", FoodGroup.entries.any { recipe.fills(it) })
            assertTrue("$name has no ingredient families", recipe.kinds.isNotEmpty())
            assertTrue("$name gives water", !recipe.fills(FoodGroup.Water))
            assertTrue(
                "$name is a snack but gives a meal",
                recipe.slot != MealSlot.Snack || !recipe.fills(FoodGroup.Meals),
            )
            assertTrue(
                "$name is a meal but gives no meal",
                recipe.slot == MealSlot.Snack || recipe.fills(FoodGroup.Meals),
            )
        }
    }

    @Test fun `a diet never contains an animal family it does not allow`() {
        val fishAndMeat = setOf(FoodKind.FishSeafood, FoodKind.Meat, FoodKind.Poultry)
        val meat = setOf(FoodKind.Meat, FoodKind.Poultry)
        val allowed = mapOf(
            Diet.Vegan to FoodKind.entries.filterNot { it in animalFamilies }.toSet(),
            Diet.Vegetarian to FoodKind.entries.filterNot { it in fishAndMeat }.toSet(),
            Diet.Pescetarian to FoodKind.entries.filterNot { it in meat }.toSet(),
            Diet.Everything to FoodKind.entries.toSet(),
        )
        Diet.entries.forEach { diet ->
            Recipe.entries.filter { it.diet == diet }.forEach { recipe ->
                assertTrue(
                    "${recipe.name} is $diet but contains ${recipe.kinds - allowed.getValue(diet)}",
                    recipe.kinds.all { it in allowed.getValue(diet) },
                )
            }
        }
    }

    @Test fun `each allergen belongs to a family the recipe lists`() {
        val requiredFamily = mapOf(
            Allergen.Gluten to FoodKind.Grains,
            Allergen.Fish to FoodKind.FishSeafood,
            Allergen.Crustaceans to FoodKind.FishSeafood,
            Allergen.Molluscs to FoodKind.FishSeafood,
            Allergen.Milk to FoodKind.Dairy,
            Allergen.Eggs to FoodKind.Eggs,
            Allergen.Peanuts to FoodKind.NutsSeeds,
            Allergen.TreeNuts to FoodKind.NutsSeeds,
            Allergen.Sesame to FoodKind.NutsSeeds,
        )
        Recipe.entries.forEach { recipe ->
            for (allergen in recipe.allergens) {
                val family = requiredFamily[allergen] ?: continue
                assertTrue("${recipe.name} lists $allergen without $family", family in recipe.kinds)
            }
        }
    }

    @Test fun `quick, medium and long recipes are not all the same slot`() {
        RecipeTime.entries.forEach { time ->
            assertTrue("$time has no main meal", Recipe.entries.any { it.time == time && it.slot == MealSlot.Main })
        }
    }
}
