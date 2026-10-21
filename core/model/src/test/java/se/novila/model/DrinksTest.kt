package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DrinksTest {
    private val date = LocalDate.of(2026, 10, 14)

    @Test fun `water, coffee, tea and milk count towards the water goal, sweet drinks don't`() {
        val day = DayEntry(date)
            .withDrink(Drink(DrinkKind.Water, 500))
            .withDrink(Drink(DrinkKind.Coffee, 150))
            .withDrink(Drink(DrinkKind.Milk, 200))
            .withDrink(Drink(DrinkKind.SoftDrink, 330))
            .withDrink(Drink(DrinkKind.Juice, 200))
        assertEquals(850, day.waterMl)
        assertEquals(1380, day.drunkMl)
        assertEquals(4, day.waterGlasses)
    }

    @Test fun `a drink stays within its range and a day holds at most so many`() {
        assertEquals(MIN_DRINK_ML, DayEntry(date).withDrink(Drink(DrinkKind.Water, 0)).drunkMl)
        assertEquals(MAX_DRINK_ML, DayEntry(date).withDrink(Drink(DrinkKind.Water, 10_000)).drunkMl)
        val full = DayEntry(date, drinks = glassesOfWater(MAX_DRINKS))
        assertEquals(full, full.withDrink(Drink(DrinkKind.Tea, 250)))
    }

    @Test fun `a glass of water is the usual serving at the time it is drunk, and counts as a glass`() {
        val day = DayEntry(date).withGlassOfWater(LocalTime.of(9, 30))
        assertEquals(listOf(Drink(DrinkKind.Water, Goals.GLASS_ML, LocalTime.of(9, 30))), day.drinks)
        assertEquals(1, day.waterGlasses)
    }

    @Test fun `the last drink can be taken back`() {
        val day = DayEntry(date).withDrink(Drink(DrinkKind.Water, 500)).withDrink(Drink(DrinkKind.Coffee, 150))
        assertEquals(listOf(Drink(DrinkKind.Water, 500)), day.withoutLastDrink().drinks)
        assertEquals(DayEntry(date), DayEntry(date).withoutLastDrink())
    }

    @Test fun `water portions are glasses of water, and fewer takes back the last glasses only`() {
        val day = DayEntry(date).withDrink(Drink(DrinkKind.Coffee, 150)).withPortions(FoodGroup.Water, +2)
        assertEquals(2, day.portions(FoodGroup.Water))
        val fewer = day.withPortions(FoodGroup.Water, -3)
        assertEquals(listOf(Drink(DrinkKind.Coffee, 150)), fewer.drinks)
        assertEquals(0, fewer.portions(FoodGroup.Water))
    }

    @Test fun `a bottle counts for the score like the glasses it holds`() {
        val day = DayEntry(date).withDrink(Drink(DrinkKind.Water, 1600))
        assertEquals(8, day.waterGlasses)
        assertNull(day.nextPortion()?.takeIf { it.group == FoodGroup.Water })
    }

    @Test fun `caffeine from two in the afternoon is late`() {
        val morning = DayEntry(date).withDrink(Drink(DrinkKind.Coffee, 150, LocalTime.of(8, 0)))
        assertFalse(morning.lateCaffeine()!!)
        assertTrue(morning.withDrink(Drink(DrinkKind.Tea, 250, LocalTime.of(LATE_CAFFEINE_HOUR, 0))).lateCaffeine()!!)
        assertFalse(morning.withDrink(Drink(DrinkKind.HerbalTea, 250, LocalTime.of(21, 0))).lateCaffeine()!!)
        assertTrue(DayEntry(date).withDrink(Drink(DrinkKind.EnergyDrink, 250, LocalTime.of(16, 0))).lateCaffeine()!!)
    }

    @Test fun `caffeine is unknown without drinks or without a time`() {
        assertNull(DayEntry(date).lateCaffeine())
        assertNull(DayEntry(date).withDrink(Drink(DrinkKind.Coffee, 150)).lateCaffeine())
        assertFalse(DayEntry(date, drinks = glassesOfWater(2)).lateCaffeine()!!)
    }
}
