package se.novila.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * A habit counted in whole weeks (Monday to Sunday), never in days, so one missed day never breaks anything. A week
 * counts when it has [days] days that count for the habit:
 * - [Sleep]: nights within half an hour of the sleep goal.
 * - [Training]: days with training, and as many days without it. Rest is part of training, so a week of training
 *   every day never counts, and more sessions than that are never worth more.
 * - [Nutrition]: days with a good nutrition score ([GOOD_NUTRITION_SCORE] or more).
 */
enum class Habit(val days: Int) {
    Sleep(4),
    Training(2),
    Nutrition(4),
}

/**
 * How a [habit] stands: [weeks] in a row that counted, up to this week, and the [best] run so far. [thisWeek] is how
 * many of the [Habit.days] this week has; this week adds to [weeks] once it counts, and never breaks the run while it
 * is still going. For training, [restDays] are this week's days without training so far.
 */
data class HabitWeeks(val habit: Habit, val weeks: Int, val best: Int, val thisWeek: Int, val restDays: Int = 0) {
    /** 0 to 1, how far this week has come. */
    val progress: Float get() = (thisWeek.toFloat() / habit.days).coerceIn(0f, 1f)
}

/** Something counted over all the logs, with the [steps] that are milestones on the way. */
enum class MilestoneKind(val steps: List<Int>) {
    /** Nights logged, or recorded by a watch. */
    Nights(listOf(1, 7, 30, 100, 365)),

    /** Days with training. */
    TrainingDays(listOf(1, 10, 25, 50, 100, 250)),

    /** Days with food logged. */
    FoodDays(listOf(1, 7, 30, 100, 365)),

    /** Days that reached the water goal. */
    WaterDays(listOf(7, 30, 100, 365)),
}

/** A milestone [step] that was reached [on] a day. */
data class ReachedMilestone(val step: Int, val on: LocalDate)

/** How far the logs have come towards a [kind] of milestone: the [total] so far, what is [reached] and the [next]. */
data class MilestoneTrack(val kind: MilestoneKind, val total: Int, val reached: List<ReachedMilestone>) {
    /** The next step, or null when every step is reached. */
    val next: Int? get() = kind.steps.firstOrNull { it > total }

    /** 0 to 1, from the last step reached to the next. */
    val progress: Float
        get() {
            val next = next ?: return 1f
            val from = reached.lastOrNull()?.step ?: 0
            return ((total - from).toFloat() / (next - from)).coerceIn(0f, 1f)
        }
}

/** The [habits] counted in weeks and the [milestones] over all the logs. */
data class Achievements(val habits: List<HabitWeeks>, val milestones: List<MilestoneTrack>)

/**
 * The weeks in a row and the milestones in [days] (every saved day, in any order) up to [today], measured against
 * today's [goals]. Only what the user logged (and a watch's nights) counts; a day without logs is unknown and simply
 * doesn't count, and nothing is ever taken away.
 */
fun achievements(days: List<DayEntry>, today: LocalDate, goals: Goals = Goals()): Achievements {
    val past = days.filter { !it.date.isAfter(today) }.sortedBy { it.date }
    val byDate = past.associateBy { it.date }
    return Achievements(
        habits = Habit.entries.map { habitWeeks(it, byDate, past.firstOrNull()?.date, today, goals) },
        milestones = MilestoneKind.entries.map { milestones(it, past, goals) },
    )
}

/** The night was within half an hour of the goal. */
private fun DayEntry.nearSleepGoal(goals: Goals) =
    sleepHours > 0f && sleepHours >= goals.sleepHours - SLEEP_GOAL_MARGIN

private fun DayEntry.ateWell(goals: Goals) = (nutritionScore(goals)?.total ?: 0) >= GOOD_NUTRITION_SCORE

/** Half an hour short of the sleep goal still counts as near it. */
private const val SLEEP_GOAL_MARGIN = 0.5f

private fun habitWeeks(
    habit: Habit,
    days: Map<LocalDate, DayEntry>,
    first: LocalDate?,
    today: LocalDate,
    goals: Goals,
): HabitWeeks {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    fun week(start: LocalDate, end: LocalDate): List<DayEntry> {
        val dates = generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }
        return dates.map { days[it] ?: DayEntry(it) }.toList()
    }

    fun counted(week: List<DayEntry>): Int = when (habit) {
        Habit.Sleep -> week.count { it.nearSleepGoal(goals) }
        Habit.Nutrition -> week.count { it.ateWell(goals) }
        Habit.Training -> week.count { it.trainingMinutes > 0 }
    }

    fun rest(week: List<DayEntry>): Int = week.count { it.trainingMinutes == 0 }

    fun counts(week: List<DayEntry>): Boolean =
        counted(week) >= habit.days && (habit != Habit.Training || rest(week) >= habit.days)

    val current = week(monday, today)
    val currentCounts = counts(current)
    // Every finished week from the first saved one, oldest first.
    val finished = if (first == null) {
        emptyList()
    } else {
        generateSequence(first.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) { it.plusWeeks(1) }
            .takeWhile { it.isBefore(monday) }
            .map { counts(week(it, it.plusDays(6))) }
            .toList()
    }
    val inARow = finished.takeLastWhile { it }.size + if (currentCounts) 1 else 0
    var best = 0
    var streak = 0
    (finished + currentCounts).forEach { counts ->
        streak = if (counts) streak + 1 else 0
        best = maxOf(best, streak)
    }
    return HabitWeeks(
        habit = habit,
        weeks = inARow,
        best = best,
        thisWeek = counted(current),
        restDays = if (habit == Habit.Training) rest(current) else 0,
    )
}

private fun milestones(kind: MilestoneKind, days: List<DayEntry>, goals: Goals): MilestoneTrack {
    val counted = days.filter { day ->
        when (kind) {
            MilestoneKind.Nights -> day.sleepHours > 0f
            MilestoneKind.TrainingDays -> day.trainingMinutes > 0
            MilestoneKind.FoodDays -> day.food != FoodLog()
            MilestoneKind.WaterDays -> day.drinks.isNotEmpty() && day.waterGlasses >= goals.waterGlasses
        }
    }
    return MilestoneTrack(
        kind = kind,
        total = counted.size,
        reached = kind.steps.filter { it <= counted.size }.map { ReachedMilestone(it, counted[it - 1].date) },
    )
}
