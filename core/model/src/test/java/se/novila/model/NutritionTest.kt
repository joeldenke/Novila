package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
        val food = FoodLog(5, meals = 3, protein = 3, wholeGrain = 3)
        val day = DayEntry(date, drinks = glassesOfWater(WATER_GOAL), food = food)
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

    @Test fun `red meat counts as half a protein portion and costs points beyond one a day`() {
        // Three protein portions of fish, beans or eggs fill the group; three of red meat count as one and a half.
        val fish = DayEntry(date, food = FoodLog(protein = 3)).nutritionScore()!!
        val meat = DayEntry(date, food = FoodLog(redMeat = 3)).nutritionScore()!!
        assertEquals(20, fish.total)
        assertEquals(10, meat.parts.single { it.group == FoodGroup.Protein }.points.toInt())
        // Two portions beyond the free one cost five points each.
        assertEquals(10, meat.redMeatPenalty)
        assertEquals(0, meat.total)
        // One a day costs nothing.
        val one = DayEntry(date, food = FoodLog(protein = 2, redMeat = 1)).nutritionScore()!!
        assertEquals(0, one.redMeatPenalty)
        assertEquals(17, one.total)
        // Red meat alone counts as something logged.
        assertEquals(0, DayEntry(date, food = FoodLog(redMeat = 1)).nutritionScore()?.redMeatPenalty)
    }

    @Test fun `fish counts as protein in full and is counted over the week`() {
        val fish = DayEntry(date, food = FoodLog(fish = 3)).nutritionScore()!!
        assertEquals(20, fish.total)
        // With the protein filled by fish, the next step is another group.
        val fed = DayEntry(date, food = FoodLog(meals = 3, fish = 3))
        assertEquals(FoodGroup.Vegetables, fed.nextPortion()?.group)
        // Fish on two of the last seven days; the one eight days ago doesn't count.
        val days = listOf(0L, 3L, 8L).map { DayEntry(date.minusDays(it), food = FoodLog(fish = 1)) } +
            DayEntry(date.minusDays(1), food = FoodLog(protein = 2))
        assertEquals(2, fishDaysThisWeek(days, date))
        assertTrue(fishDaysThisWeek(days, date) >= FISH_TIMES_A_WEEK)
    }
}
