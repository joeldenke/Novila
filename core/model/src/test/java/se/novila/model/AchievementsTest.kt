package se.novila.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class AchievementsTest {
    /** A Thursday: this week has Monday to Thursday so far. */
    private val today = LocalDate.of(2026, 10, 22)
    private val monday = LocalDate.of(2026, 10, 19)

    private fun habit(result: Achievements, habit: Habit) = result.habits.first { it.habit == habit }

    private fun track(result: Achievements, kind: MilestoneKind) = result.milestones.first { it.kind == kind }

    /** The days of the week starting [start], each made by [day] from its index 0 (Monday) to 6 (Sunday). */
    private fun week(start: LocalDate, day: (Int, LocalDate) -> DayEntry) =
        (0..6).map { day(it, start.plusDays(it.toLong())) }

    private val goodFood = FoodLog(vegetables = 10, meals = 10, protein = 10, wholeGrain = 10)

    private fun ateWell(date: LocalDate) = DayEntry(date, food = goodFood, drinks = glassesOfWater(WATER_GOAL))

    @Test fun `nothing logged has no weeks and no milestones`() {
        val result = achievements(emptyList(), today)
        result.habits.forEach {
            assertEquals(0, it.weeks)
            assertEquals(0, it.best)
            assertEquals(0, it.thisWeek)
        }
        result.milestones.forEach {
            assertEquals(0, it.total)
            assertEquals(emptyList<ReachedMilestone>(), it.reached)
            assertEquals(it.kind.steps.first(), it.next)
        }
    }

    @Test fun `weeks with four nights near the goal count in a row`() {
        val days = (1..3).flatMap { back ->
            week(monday.minusWeeks(back.toLong())) { i, date -> DayEntry(date, sleepHours = if (i < 4) 7.5f else 0f) }
        }
        val sleep = habit(achievements(days, today), Habit.Sleep)
        assertEquals(3, sleep.weeks)
        assertEquals(3, sleep.best)
    }

    @Test fun `a night more than half an hour short of the goal does not count`() {
        val days = week(monday.minusWeeks(1)) { _, date -> DayEntry(date, sleepHours = 7f) }
        assertEquals(0, habit(achievements(days, today), Habit.Sleep).weeks)
    }

    @Test fun `this week adds to the run once it counts, and never breaks it while it is going`() {
        val last = week(monday.minusWeeks(1)) { _, date -> DayEntry(date, sleepHours = 8f) }
        val going = (0..1).map { DayEntry(monday.plusDays(it.toLong()), sleepHours = 8f) }
        val running = habit(achievements(last + going, today), Habit.Sleep)
        assertEquals(1, running.weeks)
        assertEquals(2, running.thisWeek)

        val done = (0..3).map { DayEntry(monday.plusDays(it.toLong()), sleepHours = 8f) }
        assertEquals(2, habit(achievements(last + done, today), Habit.Sleep).weeks)
    }

    @Test fun `a week that did not count ends the run, but the best run stays`() {
        val good = { start: LocalDate -> week(start) { _, date -> DayEntry(date, sleepHours = 8f) } }
        val days = good(monday.minusWeeks(4)) + good(monday.minusWeeks(3)) + good(monday.minusWeeks(1))
        val sleep = habit(achievements(days, today), Habit.Sleep)
        assertEquals(1, sleep.weeks)
        assertEquals(2, sleep.best)
    }

    @Test fun `a training week needs two days of training and two of rest`() {
        val twice = week(monday.minusWeeks(1)) { i, date -> DayEntry(date, strengthMinutes = if (i < 2) 30 else 0) }
        assertEquals(1, habit(achievements(twice, today), Habit.Training).weeks)
    }

    @Test fun `training every day never counts`() {
        val daily = week(monday.minusWeeks(1)) { _, date -> DayEntry(date, strengthMinutes = 30) }
        assertEquals(0, habit(achievements(daily, today), Habit.Training).weeks)
    }

    @Test fun `this training week says its sessions and its rest so far`() {
        val days = listOf(
            DayEntry(monday, strengthMinutes = 30),
            DayEntry(monday.plusDays(2), cardio = listOf(CardioLog(CardioType.Walking, 40))),
        )
        val training = habit(achievements(days, today), Habit.Training)
        assertEquals(2, training.thisWeek)
        assertEquals(2, training.restDays)
        assertEquals(1, training.weeks)
        assertEquals(1f, training.progress)
    }

    @Test fun `weeks eaten well count four good days`() {
        val days = week(monday.minusWeeks(1)) { i, date -> if (i < 4) ateWell(date) else DayEntry(date) }
        assertEquals(1, habit(achievements(days, today), Habit.Nutrition).weeks)
    }

    @Test fun `milestones are reached on the day the count gets there`() {
        val nights = (0 until 8).map { DayEntry(today.minusDays(7L - it), sleepHours = 7f) }
        val track = track(achievements(nights, today), MilestoneKind.Nights)
        assertEquals(8, track.total)
        val reached = listOf(ReachedMilestone(1, today.minusDays(7)), ReachedMilestone(7, today.minusDays(1)))
        assertEquals(reached, track.reached)
        assertEquals(30, track.next)
        assertEquals(1f / 23f, track.progress)
    }

    @Test fun `every step reached has no next`() {
        val days = (0 until 400).map { DayEntry(today.minusDays(it.toLong()), sleepHours = 7f) }
        val track = track(achievements(days, today), MilestoneKind.Nights)
        assertNull(track.next)
        assertEquals(1f, track.progress)
    }

    @Test fun `water days count only the goal reached`() {
        val days = (0 until 7).map {
            DayEntry(today.minusDays(it.toLong()), drinks = glassesOfWater(if (it == 0) 2 else WATER_GOAL))
        }
        assertEquals(6, track(achievements(days, today), MilestoneKind.WaterDays).total)
    }

    @Test fun `days after today are left out`() {
        val days = listOf(DayEntry(today.plusDays(1), sleepHours = 8f))
        assertEquals(0, track(achievements(days, today), MilestoneKind.Nights).total)
    }
}
