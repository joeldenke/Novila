package se.novila.recipes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.FoodLog
import se.novila.model.nutritionScore
import java.time.LocalDate

class RecipeSuggestionsTest {
    private val date = LocalDate.of(2026, 10, 18)
    private val empty = DayEntry(date)

    @Test fun `breakfast in the morning, a main meal later and a snack when the meals are done`() {
        assertEquals(MealSlot.Breakfast, mealSlotAt(empty, hour = 7))
        assertEquals(MealSlot.Main, mealSlotAt(empty, hour = 12))
        assertEquals(MealSlot.Main, mealSlotAt(DayEntry(date, food = FoodLog(meals = 1)), hour = 8))
        assertEquals(MealSlot.Snack, mealSlotAt(DayEntry(date, food = FoodLog(meals = 3)), hour = 15))
    }

    @Test fun `every slot has recipes for every diet`() {
        MealSlot.entries.forEach { slot ->
            Diet.entries.forEach { diet ->
                assertTrue("$slot $diet", Recipe.entries.any { it.slot == slot && FoodPreferences(diet).allows(it) })
            }
        }
    }

    @Test fun `a diet gets recipes of its own diet and stricter ones`() {
        val vegetarian = FoodPreferences(Diet.Vegetarian)
        assertTrue(vegetarian.allows(Recipe.VegetableOmelette))
        assertTrue(vegetarian.allows(Recipe.ChickpeaCurry))
        assertFalse(vegetarian.allows(Recipe.BakedSalmon))
        assertFalse(vegetarian.allows(Recipe.ChickenStirFry))
        assertTrue(FoodPreferences(Diet.Pescetarian).allows(Recipe.BakedSalmon))
        assertFalse(FoodPreferences(Diet.Vegan).allows(Recipe.OvernightOats))
        assertTrue(Recipe.entries.all { FoodPreferences().allows(it) })
    }

    @Test fun `no suggestion contains an allergen the user avoids`() {
        Allergen.entries.forEach { allergen ->
            val preferences = FoodPreferences(allergens = setOf(allergen))
            listOf(7, 12, 20).forEach { hour ->
                val suggested = suggestRecipes(empty, hour, preferences).recipes
                assertTrue("$allergen", suggested.none { allergen in it.recipe.allergens })
            }
        }
    }

    @Test fun `recipes that take too long are left out`() {
        val quick = FoodPreferences(maxMinutes = 15)
        val suggested = suggestRecipes(empty, hour = 12, preferences = quick).recipes
        assertTrue(suggested.isNotEmpty())
        assertTrue(suggested.all { it.recipe.minutes <= 15 })
    }

    @Test fun `nothing is suggested when no recipe fits`() {
        val strict = FoodPreferences(Diet.Vegan, allergens = setOf(Allergen.Gluten, Allergen.Celery), maxMinutes = 15)
        assertEquals(emptyList<RecipeSuggestion>(), suggestRecipes(empty, hour = 12, preferences = strict).recipes)
    }

    @Test fun `the recipe that fills the most comes first`() {
        // Vegetables are done, protein and whole grain are not: a main without whole grain is worth less.
        val day = DayEntry(date, food = FoodLog(vegetables = 5, meals = 1))
        val suggested = suggestRecipes(day, hour = 12, preferences = FoodPreferences(Diet.Pescetarian)).recipes
        assertEquals(SUGGESTED_RECIPES, suggested.size)
        assertTrue(suggested.zipWithNext().all { (a, b) -> a.points >= b.points })
        assertTrue(suggested.none { it.recipe == Recipe.BakedSalmon })
    }

    @Test fun `the points are what one serving adds to the score`() {
        val day = DayEntry(date, food = FoodLog(vegetables = 1, meals = 1))
        suggestRecipes(day, hour = 12, preferences = FoodPreferences()).recipes.forEach {
            val after = day.withRecipe(it.recipe).nutritionScore()?.total ?: 0
            assertEquals(after - (day.nutritionScore()?.total ?: 0), it.points)
        }
    }

    @Test fun `the same day gives the same suggestions, and ties rotate between days`() {
        val preferences = FoodPreferences()
        assertEquals(suggestRecipes(empty, 12, preferences), suggestRecipes(empty, 12, preferences))
        val tomorrow = DayEntry(date.plusDays(1))
        assertNotEquals(
            suggestRecipes(empty, 12, preferences).recipes.first().recipe,
            suggestRecipes(tomorrow, 12, preferences).recipes.first().recipe,
        )
    }

    @Test fun `logging a recipe adds its portions and a meal`() {
        val day = empty.withRecipe(Recipe.LentilBolognese)
        assertEquals(FoodLog(vegetables = 2, meals = 1, protein = 1, wholeGrain = 1), day.food)
        assertEquals(0, empty.withRecipe(Recipe.AppleAndNuts).food.meals)
        assertEquals(0, Recipe.entries.sumOf { it.portions(FoodGroup.Water) })
    }

    @Test fun `preferences survive encoding and unknown formats are ignored`() {
        val preferences = FoodPreferences(Diet.Vegan, setOf(Allergen.Sesame, Allergen.Gluten), maxMinutes = 30)
        assertEquals(preferences, FoodPreferencesCodec.decode(FoodPreferencesCodec.encode(preferences)))
        assertEquals(FoodPreferences(), FoodPreferencesCodec.decode(FoodPreferencesCodec.encode(FoodPreferences())))
        assertNull(FoodPreferencesCodec.decode(null))
        assertNull(FoodPreferencesCodec.decode("f9;Vegan;;"))
        assertEquals(FoodPreferences(maxMinutes = null), FoodPreferencesCodec.decode("f1;Everything;Nope;45"))
    }
}
