package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MealLogTest {
    private val date = LocalDate.of(2026, 10, 15)
    private val day = DayEntry(date)

    @Test fun `the example from the roadmap fills in three groups`() {
        val meal = parseMeal("Frukost: gröt, bär")

        assertEquals(
            mapOf(FoodGroup.Meals to 1, FoodGroup.WholeGrain to 1, FoodGroup.Vegetables to 1),
            meal.portions,
        )
        assertEquals(0, meal.treats)
        assertTrue(meal.unknown.isEmpty())
    }

    @Test fun `english meal names and foods work the same`() {
        assertEquals(parseMeal("Frukost: gröt, bär").portions, parseMeal("Breakfast: porridge, berries").portions)
    }

    @Test fun `a meal name on its own is still a meal`() {
        assertEquals(mapOf(FoodGroup.Meals to 1), parseMeal("Lunch:").portions)
    }

    @Test fun `words outside the dictionary are reported, not guessed at`() {
        val meal = parseMeal("Frukost: yoghurt, gröt")

        assertEquals(listOf("yoghurt"), meal.unknown)
        assertEquals(mapOf(FoodGroup.Meals to 1, FoodGroup.WholeGrain to 1), meal.portions)
    }

    @Test fun `a name that is not a meal is food`() {
        val meal = parseMeal("Smoothie: banan")

        assertEquals(listOf("smoothie"), meal.unknown)
        assertEquals(mapOf(FoodGroup.Vegetables to 1), meal.portions)
    }

    @Test fun `sweets become treats`() {
        val meal = parseMeal("Godis och choklad, läsk")

        assertEquals(3, meal.treats)
        assertTrue(meal.portions.isEmpty())
    }

    @Test fun `the same food twice is one portion`() {
        assertEquals(mapOf(FoodGroup.WholeGrain to 1), parseMeal("gröt, gröt").portions)
    }

    @Test fun `separators case and connecting words do not matter`() {
        val meal = parseMeal("LUNCH: Kyckling och Ris, Sallad; vatten")

        assertEquals(
            mapOf(
                FoodGroup.Meals to 1,
                FoodGroup.Protein to 1,
                FoodGroup.WholeGrain to 1,
                FoodGroup.Vegetables to 1,
                FoodGroup.Water to 1,
            ),
            meal.portions,
        )
    }

    @Test fun `two portions of the same group are counted`() {
        assertEquals(mapOf(FoodGroup.Meals to 1, FoodGroup.Protein to 2), parseMeal("Lunch: kyckling, lax").portions)
    }

    @Test fun `a blank meal adds nothing`() {
        assertTrue(parseMeal("   ").isEmpty)
        assertEquals(day, day.withMeal(parseMeal("")))
    }

    @Test fun `a meal adds to what is already logged`() {
        val logged = day.withPortions(FoodGroup.Vegetables, 2).withTreats(1)

        val added = logged.withMeal(parseMeal("Middag: lax, bär, glass"))

        assertEquals(3, added.portions(FoodGroup.Vegetables))
        assertEquals(1, added.portions(FoodGroup.Protein))
        assertEquals(1, added.portions(FoodGroup.Meals))
        assertEquals(2, added.food.treats)
        assertEquals(logged.date, added.date)
    }
}
