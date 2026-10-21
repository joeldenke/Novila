package se.novila.model

import kotlin.math.abs
import kotlin.math.roundToInt

/** A discovery the logs will unlock: what the insights can show once there are enough days behind them. */
enum class Discovery {
    /** What may affect the user's sleep ([analyzeSleep]), from logged nights. */
    SleepLinks,

    /** How the night before shows in training ([connections]), from training days after a logged night. */
    TrainingAfterSleep,

    /** The weekday that stands out ([connections]), from days with something logged. */
    Weekdays,
}

/**
 * How close a [discovery] is: [logged] of the [needed] days it waits for. A rough guide to when it can show, not a
 * promise: the discovery also needs a difference worth telling about, so it may come later.
 */
data class DiscoveryProgress(val discovery: Discovery, val logged: Int, val needed: Int) {
    val unlocked: Boolean get() = logged >= needed

    /** 0 to 1, for a bar. */
    val progress: Float get() = (logged.toFloat() / needed).coerceIn(0f, 1f)
}

/**
 * How far the [days] (the connection window, oldest first) are from each [Discovery]. A sleep link needs
 * [MIN_NIGHTS] nights on each side, so at least twice that; the same for training days after a logged night; a
 * weekday pattern needs three of each weekday it compares, so three weeks of days with something logged.
 */
fun discoveryProgress(days: List<DayEntry>): List<DiscoveryProgress> {
    val nights = days.count { it.sleepHours > 0f }
    val trainingAfterNight = days.zipWithNext().count { (before, day) ->
        before.sleepHours > 0f && day.trainingMinutes > 0
    }
    val logged = days.count { it.hasLog }
    return listOf(
        DiscoveryProgress(Discovery.SleepLinks, nights, needed = MIN_NIGHTS * 2),
        DiscoveryProgress(Discovery.TrainingAfterSleep, trainingAfterNight, needed = MIN_NIGHTS * 2),
        DiscoveryProgress(Discovery.Weekdays, logged, needed = WEEKDAY_DAYS),
    )
}

/** Three of every weekday. */
private const val WEEKDAY_DAYS = 21

/** Strength and cardio minutes of the day, both logged in the app. */
val DayEntry.trainingMinutes: Int get() = strengthMinutes + cardioMinutes

/** Anything at all is logged for the day: a night, food, a drink or training. */
val DayEntry.hasLog: Boolean get() = sleepHours > 0f || food != FoodLog() || drinks.isNotEmpty() || trainingMinutes > 0

/**
 * Something the trends say over a [History]'s range against the range before, across sleep, food and training.
 * Worked out on the phone, deterministic and only from the user's own logs; never a diagnosis.
 */
sealed interface TrendFeedback {
    /** Both sleep and the nutrition score went up. */
    data class BothImproving(val sleepHours: Float, val nutritionPoints: Int) : TrendFeedback

    /** More training, but less sleep: a reminder that rest is part of training. */
    data class MoreTrainingLessSleep(val trainingMinutes: Int, val sleepHours: Float) : TrendFeedback

    /** Average sleep per night changed by [hours], positive is more. */
    data class Sleep(val hours: Float) : TrendFeedback

    /** The average nutrition score changed by [points]. */
    data class Nutrition(val points: Int) : TrendFeedback

    /** Training minutes in all changed by [minutes]. */
    data class Training(val minutes: Int) : TrendFeedback

    /** [reached] of the [logged] nights reached the sleep goal. */
    data class SleepGoal(val reached: Int, val logged: Int) : TrendFeedback

    /** [reached] of the [logged] days with drinks reached the water goal. */
    data class WaterGoal(val reached: Int, val logged: Int) : TrendFeedback
}

/** A change in the nutrition score smaller than this is noise. */
const val NUTRITION_CHANGE_POINTS = 3

/** A change in training minutes over the range smaller than this is noise. */
const val TRAINING_CHANGE_MINUTES = 30

/** A goal count needs at least this many logged days to say anything. */
const val GOAL_MIN_DAYS = 3

/**
 * At most [limit] things the [history] says, the ones that join parts first, then each part's change against the
 * range before, then how often the goals were reached. A change only counts with logs on both sides; when two parts
 * move together, the joined feedback replaces both.
 */
fun trendFeedback(history: History, goals: Goals = Goals(), limit: Int = 4): List<TrendFeedback> {
    val sleep = history.sleepTrend()?.takeIf { it != 0f }
    val nutrition = history.nutritionTrend(goals)?.takeIf { abs(it) >= NUTRITION_CHANGE_POINTS }
    val training = trainingTrend(history)
    val joined = mutableListOf<TrendFeedback>()
    var showSleep = sleep != null
    var showNutrition = nutrition != null
    var showTraining = training != null
    if (sleep != null && nutrition != null && sleep > 0f && nutrition > 0) {
        joined += TrendFeedback.BothImproving(sleep, nutrition)
        showSleep = false
        showNutrition = false
    }
    if (training != null && training > 0 && sleep != null && sleep < 0f) {
        joined += TrendFeedback.MoreTrainingLessSleep(training, sleep)
        showSleep = false
        showTraining = false
    }
    val days = history.days
    val nights = days.filter { it.sleepHours > 0f }
    val drinking = days.filter { it.drinks.isNotEmpty() }
    return listOfNotNull(
        *joined.toTypedArray(),
        sleep?.takeIf { showSleep }?.let(TrendFeedback::Sleep),
        nutrition?.takeIf { showNutrition }?.let(TrendFeedback::Nutrition),
        training?.takeIf { showTraining }?.let(TrendFeedback::Training),
        TrendFeedback.SleepGoal(nights.count { it.sleepHours >= goals.sleepHours }, nights.size)
            .takeIf { nights.size >= GOAL_MIN_DAYS },
        TrendFeedback.WaterGoal(drinking.count { it.waterGlasses >= goals.waterGlasses }, drinking.size)
            .takeIf { drinking.size >= GOAL_MIN_DAYS },
    ).take(limit)
}

/** Training minutes in the range against the range before, when both have training logged and it moved enough. */
private fun trainingTrend(history: History): Int? {
    val now = history.days.sumOf { it.trainingMinutes }
    val before = history.previous.sumOf { it.trainingMinutes }
    if (now == 0 || before == 0) return null
    return (now - before).takeIf { abs(it) >= TRAINING_CHANGE_MINUTES }
}

/** Training minutes per day on average over [days] with training, or null without any. */
fun List<DayEntry>.averageTrainingMinutes(): Int? =
    map { it.trainingMinutes }.filter { it > 0 }.takeIf { it.isNotEmpty() }?.average()?.roundToInt()
