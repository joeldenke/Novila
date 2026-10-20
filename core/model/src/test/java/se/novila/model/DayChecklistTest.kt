package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class DayChecklistTest {
    private val date = LocalDate.of(2026, 10, 18)
    private val training = TrainingToday(plannedMinutes = 30)

    @Test fun `a new day has everything to do, sleep first`() {
        val checklist = dayChecklist(DayEntry(date), Goals(), training)
        assertEquals(0, checklist.doneCount)
        assertEquals(DayTask.Sleep(0f), checklist.next)
    }

    @Test fun `a full day is all done`() {
        val day = DayEntry(
            date,
            sleepHours = 7.5f,
            drinks = glassesOfWater(8),
            food = FoodLog(vegetables = 5, meals = 3, protein = 3, wholeGrain = 3),
        )
        val checklist = dayChecklist(day, Goals(), TrainingToday(plannedMinutes = 30, doneMinutes = 30))
        assertTrue(checklist.allDone)
        assertNull(checklist.next)
    }

    @Test fun `a rest day needs no training`() {
        assertTrue(DayTask.Training(TrainingToday()).done)
        assertTrue(DayTask.Training(TrainingToday(doneMinutes = 20)).done)
    }

    @Test fun `training is done when as many minutes as planned are trained`() {
        assertFalse(DayTask.Training(TrainingToday(plannedMinutes = 30, doneMinutes = 20)).done)
        assertTrue(DayTask.Training(TrainingToday(plannedMinutes = 30, doneMinutes = 45)).done)
    }

    @Test fun `food is done at a good day's score`() {
        val good = DayEntry(date, drinks = glassesOfWater(8), food = FoodLog(vegetables = 5, meals = 3, protein = 2))
        val nutrition = dayChecklist(good, Goals(), training).tasks.filterIsInstance<DayTask.Nutrition>().single()
        assertTrue(nutrition.done)
        assertFalse(dayChecklist(DayEntry(date), Goals(), training).tasks[2].done)
    }

    @Test fun `water counts towards the user's own goal`() {
        val day = DayEntry(date, drinks = glassesOfWater(6))
        assertTrue(dayChecklist(day, Goals(waterGlasses = 6), training).tasks.any { it is DayTask.Water && it.done })
        assertFalse(dayChecklist(day, Goals(), training).tasks.any { it is DayTask.Water && it.done })
    }

    @Test fun `the next task skips what is done`() {
        val day = DayEntry(date, sleepHours = 8f)
        assertEquals(DayTask.Training(training), dayChecklist(day, Goals(), training).next)
    }
}
