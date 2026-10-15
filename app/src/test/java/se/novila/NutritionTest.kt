package se.novila

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class NutritionTest {
    private val date = LocalDate.of(2026, 10, 15)

    @Test fun `weights add up to 100`() {
        assertEquals(100, FoodGroup.entries.sumOf { it.points })
    }

    @Test fun `nothing logged has no score`() {
        assertNull(DayEntry(date).nutritionScore())
    }

    @Test fun `every group at its goal scores 100`() {
        val day = DayEntry(date, waterGlasses = WATER_GOAL, food = FoodLog(5, meals = 3, protein = 3, wholeGrain = 3))
        assertEquals(100, day.nutritionScore()?.total)
    }

    @Test fun `groups earn points in proportion to their goal`() {
        // 3/5 vegetables = 18, 2/3 meals = 13.3, nothing else.
        val day = DayEntry(date, food = FoodLog(vegetables = 3, meals = 2))
        assertEquals(31, day.nutritionScore()?.total)
    }

    @Test fun `more than the goal earns nothing extra`() {
        val five = DayEntry(date, food = FoodLog(vegetables = 5)).nutritionScore()?.total
        val ten = DayEntry(date, food = FoodLog(vegetables = 10)).nutritionScore()?.total
        assertEquals(30, five)
        assertEquals(five, ten)
    }

    @Test fun `one treat is free, then each costs points up to a cap`() {
        val base = FoodLog(vegetables = 5, meals = 3)
        assertEquals(50, DayEntry(date, food = base.copy(treats = 1)).nutritionScore()?.total)
        assertEquals(40, DayEntry(date, food = base.copy(treats = 3)).nutritionScore()?.total)
        assertEquals(30, DayEntry(date, food = base.copy(treats = 20)).nutritionScore()?.total)
        assertEquals(0, DayEntry(date, food = FoodLog(treats = 3)).nutritionScore()?.total)
    }

    @Test fun `next portion is the one worth the most points`() {
        assertEquals(NextPortion(FoodGroup.Meals, 7), DayEntry(date).nextPortion())
        val fed = DayEntry(date, food = FoodLog(meals = 3, protein = 3))
        assertEquals(NextPortion(FoodGroup.Vegetables, 6), fed.nextPortion())
        val almost = DayEntry(date, food = FoodLog(5, meals = 3, protein = 3, wholeGrain = 3))
        assertEquals(NextPortion(FoodGroup.Water, 2), almost.nextPortion())
        assertNull(almost.withPortions(FoodGroup.Water, WATER_GOAL).nextPortion())
    }
}
