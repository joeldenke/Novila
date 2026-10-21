package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TrendsTest {
    private val today = LocalDate.of(2026, 10, 17)

    private fun week(previous: (LocalDate) -> DayEntry, now: (LocalDate) -> DayEntry): History {
        val start = today.minusDays(6)
        return historyOf(HistoryRange.Week, today) { date -> if (date < start) previous(date) else now(date) }
    }

    private fun food(score: Int) = FoodLog(vegetables = score, meals = score, protein = score, wholeGrain = score)

    @Test fun `nothing logged says nothing`() {
        assertEquals(emptyList<TrendFeedback>(), trendFeedback(week(::DayEntry, ::DayEntry)))
    }

    @Test fun `sleep and food going up together are told as one`() {
        val history = week(
            previous = { DayEntry(it, sleepHours = 6f, food = food(1)) },
            now = { DayEntry(it, sleepHours = 7.5f, food = food(3)) },
        )
        val feedback = trendFeedback(history)
        val both = feedback.first() as TrendFeedback.BothImproving
        assertEquals(1.5f, both.sleepHours)
        assertTrue(both.nutritionPoints > 0)
        assertFalse(feedback.any { it is TrendFeedback.Sleep || it is TrendFeedback.Nutrition })
    }

    @Test fun `more training with less sleep is a reminder to rest`() {
        val history = week(
            previous = { DayEntry(it, sleepHours = 8f, strengthMinutes = 20) },
            now = { DayEntry(it, sleepHours = 6.5f, strengthMinutes = 40) },
        )
        val feedback = trendFeedback(history)
        assertEquals(TrendFeedback.MoreTrainingLessSleep(trainingMinutes = 140, sleepHours = -1.5f), feedback.first())
        assertFalse(feedback.any { it is TrendFeedback.Sleep || it is TrendFeedback.Training })
    }

    @Test fun `training needs logs on both sides and a change worth telling`() {
        val small = week({ DayEntry(it, strengthMinutes = 30) }, { DayEntry(it, strengthMinutes = 33) })
        assertFalse(trendFeedback(small).any { it is TrendFeedback.Training })
        val onlyNow = week(::DayEntry) { DayEntry(it, strengthMinutes = 30) }
        assertFalse(trendFeedback(onlyNow).any { it is TrendFeedback.Training })
        val more = week({ DayEntry(it, strengthMinutes = 20) }, { DayEntry(it, strengthMinutes = 30) })
        assertTrue(TrendFeedback.Training(70) in trendFeedback(more))
    }

    @Test fun `goal counts need three logged days`() {
        val two = week(::DayEntry) { if (it >= today.minusDays(1)) DayEntry(it, sleepHours = 8f) else DayEntry(it) }
        assertFalse(trendFeedback(two).any { it is TrendFeedback.SleepGoal })
        val three = week(::DayEntry) {
            when {
                it == today -> DayEntry(it, sleepHours = 6f)
                it >= today.minusDays(2) -> DayEntry(it, sleepHours = 8f)
                else -> DayEntry(it)
            }
        }
        assertTrue(TrendFeedback.SleepGoal(reached = 2, logged = 3) in trendFeedback(three, Goals(sleepHours = 7.5f)))
    }

    @Test fun `the feedback is limited`() {
        val history = week(
            previous = { DayEntry(it, sleepHours = 6f, food = food(1), strengthMinutes = 10) },
            now = {
                DayEntry(
                    it,
                    sleepHours = 8f,
                    food = food(3),
                    strengthMinutes = 30,
                    drinks = List(8) { Drink(DrinkKind.Water, Goals.GLASS_ML) },
                )
            },
        )
        assertEquals(2, trendFeedback(history, limit = 2).size)
    }

    @Test fun `discoveries count the days they wait for`() {
        val days = (0L..9L).map { offset ->
            val date = today.minusDays(9 - offset)
            val night = offset % 2 == 0L
            DayEntry(date, sleepHours = if (night) 7f else 0f, strengthMinutes = if (night) 0 else 30)
        }
        val progress = discoveryProgress(days).associateBy { it.discovery }
        assertEquals(5, progress.getValue(Discovery.SleepLinks).logged)
        assertEquals(MIN_NIGHTS * 2, progress.getValue(Discovery.SleepLinks).needed)
        // Every training day follows a logged night.
        assertEquals(5, progress.getValue(Discovery.TrainingAfterSleep).logged)
        assertEquals(10, progress.getValue(Discovery.Weekdays).logged)
        assertFalse(progress.getValue(Discovery.Weekdays).unlocked)
        assertEquals(10f / 21f, progress.getValue(Discovery.Weekdays).progress)
    }

    @Test fun `a discovery with enough days is unlocked`() {
        val days = (0L..20L).map { DayEntry(today.minusDays(20 - it), sleepHours = 7f) }
        assertTrue(discoveryProgress(days).first { it.discovery == Discovery.Weekdays }.unlocked)
        assertEquals(1f, discoveryProgress(days).first { it.discovery == Discovery.SleepLinks }.progress)
    }
}
