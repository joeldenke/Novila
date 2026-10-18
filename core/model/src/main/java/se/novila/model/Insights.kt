package se.novila.model

import kotlin.math.abs
import kotlin.math.roundToInt

/** Days in the order a chart shows them: oldest first, ending with today. */
fun weekOf(today: DayEntry, history: List<DayEntry>): List<DayEntry> = history.reversed() + today

/** Average hours over the nights that were logged, or null when none were. */
fun List<DayEntry>.averageSleep(): Float? =
    map { it.sleepHours }.filter { it > 0f }.takeIf { it.isNotEmpty() }?.average()?.toFloat()

/** Average nutrition score over the days with food logged, or null when none were. */
fun List<DayEntry>.averageNutrition(goals: Goals = Goals()): Int? =
    mapNotNull { it.nutritionScore(goals)?.total }.takeIf { it.isNotEmpty() }?.average()?.roundToInt()

/**
 * Something worth knowing about the week, worked out on the phone from the user's own logs.
 * Each one needs enough days behind it to be more than chance.
 */
sealed interface Insight {
    /** Mood is clearly better on days after at least [hours] of sleep. */
    data class MoodFollowsSleep(val hours: Float) : Insight

    /** Last night against the week's average, in hours: positive is more. */
    data class SleepVsAverage(val hours: Float) : Insight

    /** The nutrition score in the last few days against earlier in the week, in points. */
    data class NutritionTrend(val points: Int) : Insight

    /** Days in a row with a good nutrition score. */
    data class NutritionStreak(val days: Int) : Insight

    /** Today's step goal of [steps] is reached. */
    data class StepsGoalReached(val steps: Int) : Insight

    /** Every logged night this week within an hour of the others. */
    data object SteadySleep : Insight
}

const val MOOD_SLEEP_HOURS = 7f

/**
 * [week] runs from oldest to today (see [weekOf]). At most [limit] insights, the most telling first. Steps and
 * nutrition are measured against the user's [goals].
 */
fun weekInsights(week: List<DayEntry>, steps: Long?, limit: Int = 3, goals: Goals = Goals()): List<Insight> =
    listOfNotNull(
        moodFollowsSleep(week),
        sleepVsAverage(week),
        nutritionStreak(week, goals),
        nutritionTrend(week, goals),
        Insight.StepsGoalReached(goals.steps).takeIf { steps != null && steps >= goals.steps },
        steadySleep(week),
    ).take(limit)

/** At least two days on each side of [MOOD_SLEEP_HOURS], and almost a whole mood step between them. */
private fun moodFollowsSleep(week: List<DayEntry>): Insight? {
    val (rested, short) = week.filter { it.mood > 0 && it.sleepHours > 0f }
        .partition { it.sleepHours >= MOOD_SLEEP_HOURS }
    if (rested.size < 2 || short.size < 2) return null
    val difference = rested.map { it.mood }.average() - short.map { it.mood }.average()
    return Insight.MoodFollowsSleep(MOOD_SLEEP_HOURS).takeIf { difference >= 0.75 }
}

/** Last night against at least three other nights, when it differs by 45 minutes or more. */
private fun sleepVsAverage(week: List<DayEntry>): Insight? {
    val lastNight = week.lastOrNull()?.sleepHours?.takeIf { it > 0f } ?: return null
    val others = week.dropLast(1).filter { it.sleepHours > 0f }
    if (others.size < 3) return null
    val difference = lastNight - others.map { it.sleepHours }.average().toFloat()
    // Rounded to the half hour, like the hours the user logs.
    val rounded = difference.roundToHalfHour()
    return Insight.SleepVsAverage(rounded).takeIf { abs(difference) >= 0.75f }
}

/** Three or more good days in a row, ending today or, while today is still going, yesterday. */
private fun nutritionStreak(week: List<DayEntry>, goals: Goals): Insight? {
    fun good(day: DayEntry) = (day.nutritionScore(goals)?.total ?: 0) >= GOOD_NUTRITION_SCORE
    val days = if (week.lastOrNull()?.let(::good) == true) week else week.dropLast(1)
    val streak = days.asReversed().takeWhile(::good).size
    return Insight.NutritionStreak(streak).takeIf { streak >= 3 }
}

/** The last three full days against the ones before, when the score moved 8 points or more. */
private fun nutritionTrend(week: List<DayEntry>, goals: Goals): Insight? {
    // Today is still in progress, so it would always look worse.
    val scores = week.dropLast(1).mapNotNull { it.nutritionScore(goals)?.total }
    if (scores.size < 5) return null
    val recent = scores.takeLast(3).average()
    val earlier = scores.dropLast(3).average()
    val difference = (recent - earlier).roundToInt()
    return Insight.NutritionTrend(difference).takeIf { abs(difference) >= 8 }
}

private fun steadySleep(week: List<DayEntry>): Insight? {
    val nights = week.map { it.sleepHours }.filter { it > 0f }
    if (nights.size < 4) return null
    return Insight.SteadySleep.takeIf { nights.max() - nights.min() <= 1f }
}
