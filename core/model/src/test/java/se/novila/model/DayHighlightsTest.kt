package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DayHighlightsTest {
    private val june = LocalDate.of(2026, 6, 10)
    private val november = LocalDate.of(2026, 11, 10)
    private val walk = CardioLog(CardioType.Walking, 30)

    @Test fun `a workout is the latest log before food, water and the night`() {
        val day = DayEntry(june, sleepHours = 7f, drinks = glassesOfWater(2))
            .withPortions(FoodGroup.Vegetables, 2)
            .withCardio(CardioLog(CardioType.Cycling, 20))
            .withCardio(walk)
        assertEquals(LatestLog.Cardio(walk), day.latestLog())
        assertEquals(LatestLog.Strength(25), DayEntry(june, sleepHours = 7f).withStrengthSession(25).latestLog())
        val ate = DayEntry(june, drinks = glassesOfWater(3)).withPortions(FoodGroup.Protein, 2)
        assertEquals(LatestLog.Food(2), ate.latestLog())
        assertEquals(LatestLog.Water(3), DayEntry(june, sleepHours = 7f, drinks = glassesOfWater(3)).latestLog())
        assertEquals(LatestLog.Sleep(7f), DayEntry(june, sleepHours = 7f).latestLog())
        assertNull(DayEntry(june).latestLog())
    }

    @Test fun `a walk in summer mentions vitamin D, in winter the daylight`() {
        assertEquals(DayCheer.OutdoorsSummer(walk), DayEntry(june).withCardio(walk).cheers().first())
        assertEquals(DayCheer.OutdoorsDaylight(walk), DayEntry(november).withCardio(walk).cheers().first())
    }

    @Test fun `swimming is its own cheer, not an outdoor one`() {
        val day = DayEntry(june).withCardio(CardioLog(CardioType.Swimming, 40))
        assertEquals(listOf(DayCheer.Swim(40)), day.cheers())
    }

    @Test fun `the night is measured against the goal`() {
        val goals = Goals(sleepHours = 8f)
        assertEquals(listOf(DayCheer.RestedNight(8f)), DayEntry(june, sleepHours = 8f).cheers(goals))
        assertEquals(listOf(DayCheer.ShortNight(6f)), DayEntry(june, sleepHours = 6f).cheers(goals))
        // A little short is neither: nothing else to say, so the day starts.
        assertEquals(listOf(DayCheer.Start), DayEntry(june, sleepHours = 7f).cheers(goals))
    }

    @Test fun `what the user did comes first, then the night, then food and water`() {
        val day = DayEntry(june, sleepHours = 8f, drinks = glassesOfWater(8))
            .withStrengthSession(30)
            .withPortions(FoodGroup.Vegetables, 5)
        assertEquals(
            listOf(DayCheer.Strength(30), DayCheer.RestedNight(8f), DayCheer.Vegetables, DayCheer.Water),
            day.cheers(Goals(sleepHours = 8f, waterGlasses = 8)).take(4),
        )
    }

    @Test fun `nothing logged starts the day`() {
        assertEquals(listOf(DayCheer.Start), DayEntry(june).cheers())
    }
}
