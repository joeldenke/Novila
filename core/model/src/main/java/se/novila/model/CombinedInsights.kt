package se.novila.model

import java.time.DayOfWeek
import kotlin.math.abs

/** How far back the connections look: about three months, like the sleep links. */
const val CONNECTION_WINDOW_DAYS = 90

/** The part of the day a wellbeing connection groups by. */
enum class WellbeingDriver {
    /** Days eaten well ([GOOD_NUTRITION_SCORE] or more) against days that weren't. */
    Nutrition,

    /** Days with at least [TRAINED_MINUTES] of training against days without. */
    Training,
}

/** How the user said they felt, on the days they said so. */
enum class Wellbeing {
    /** Full of energy against exhausted, 1–5. */
    Energy,

    /** Good mood against bad, 1–5. */
    Mood,
}

/** A part a weekday pattern can be about. */
enum class WeekdayMetric {
    /** Minutes of strength and cardio. */
    TrainingMinutes,

    /** Hours slept. */
    SleepHours,

    /** The nutrition score, 0–100. */
    NutritionScore,
}

/**
 * A connection between two parts of the app, found in the user's own logs. Like [analyzeSleep], it is only shown when
 * there are [MIN_NIGHTS] days on each side, the difference is one the user would notice, and Welch's t is at least
 * [MIN_STRENGTH], so it is unlikely to be chance alone. On the device and deterministic. [ranking] is how far past
 * "worth telling" the finding is, so the strongest ranks first.
 */
sealed interface Connection {
    val ranking: Double
}

/**
 * The night before against the training that day: the days after a night at or above the user's sleep goal
 * ([withAverage] minutes trained) against the days after a shorter night ([withoutAverage]). A positive
 * [difference] means more training after a full night; a negative one means less.
 */
data class TrainingAfterSleep(
    val withAverage: Double,
    val withoutAverage: Double,
    val withDays: Int,
    val withoutDays: Int,
    val strength: Double,
) : Connection {
    val difference: Double get() = withAverage - withoutAverage
    override val ranking: Double get() = abs(difference) / TRAINING_NOTICEABLE
}

/**
 * How the user felt ([wellbeing], 1–5) on days with [driver] against days without. A positive [difference] means they
 * felt better on the days with it.
 */
data class WellbeingConnection(
    val driver: WellbeingDriver,
    val wellbeing: Wellbeing,
    val withAverage: Double,
    val withoutAverage: Double,
    val withDays: Int,
    val withoutDays: Int,
    val strength: Double,
) : Connection {
    val difference: Double get() = withAverage - withoutAverage
    override val ranking: Double get() = abs(difference) / WELLBEING_NOTICEABLE
}

/**
 * A weekday that stands out for one part: [best] is the weekday with the highest average ([bestAverage]) and [worst]
 * the lowest ([worstAverage]), over [days] logged days. [difference] is best minus worst, in the metric's own unit.
 */
data class WeekdayPattern(
    val metric: WeekdayMetric,
    val best: DayOfWeek,
    val bestAverage: Double,
    val worst: DayOfWeek,
    val worstAverage: Double,
    val days: Int,
) : Connection {
    val difference: Double get() = bestAverage - worstAverage
    override val ranking: Double get() = difference / metric.noticeable
}

/** Minutes of training after a full night against a short one before it is worth telling. */
private const val TRAINING_NOTICEABLE = 10.0

/** Half a step on the 1–5 scale, like the sleep quality link. */
private const val WELLBEING_NOTICEABLE = 0.5

/** Weekday averages need this many days behind them on both the best and the worst day. */
private const val MIN_WEEKDAY_DAYS = 3

/**
 * Connections that join the app's parts, from [days] (oldest first, ending today) over the last
 * [CONNECTION_WINDOW_DAYS]: how the night before shows in the training that day, how food and training show in how
 * the user felt, and which weekday stands out for training, sleep or food. At most [limit], the strongest first, one
 * per family. Only days the user actually logged count; a day without logs is unknown, not a day without training.
 * Like [analyzeSleep], a connection needs [MIN_NIGHTS] days on each side, a difference worth noticing and |t| of at
 * least [MIN_STRENGTH].
 */
fun connections(days: List<DayEntry>, goals: Goals = Goals(), limit: Int = 2): List<Connection> =
    listOfNotNull(
        trainingAfterSleep(days, goals),
        wellbeing(days, WellbeingDriver.Nutrition, Wellbeing.Energy, goals),
        wellbeing(days, WellbeingDriver.Training, Wellbeing.Mood, goals),
        weekdayPattern(days, goals),
    ).sortedByDescending { it.ranking }.take(limit)

/**
 * Whether the day says anything about what the user did, so a missing training is a rest day and not unknown. Sleep
 * alone is not enough: a logged night doesn't say anything was trained. Mirrors the sleep analysis's idea of a day.
 */
private fun DayEntry.engaged() =
    food != FoodLog() || drinks.isNotEmpty() || mood > 0 || energy > 0 ||
        strengthMinutes > 0 || cardio.isNotEmpty()

private fun DayEntry.trainingMinutes() = strengthMinutes + cardioMinutes

/**
 * The nights against the training the same day: a full night (at or above the goal) against a shorter one, among
 * days the user engaged with. Grouped by whether the night reached the goal, so it answers "do I train more on days
 * after a good night?".
 */
private fun trainingAfterSleep(days: List<DayEntry>, goals: Goals): TrainingAfterSleep? {
    val with = mutableListOf<Double>()
    val without = mutableListOf<Double>()
    days.takeLast(CONNECTION_WINDOW_DAYS).forEach { day ->
        val night = day.sleepHours.takeIf { it > 0f } ?: return@forEach
        if (!day.engaged()) return@forEach
        (if (night >= goals.sleepHours) with else without) += day.trainingMinutes().toDouble()
    }
    return link(TRAINING_NOTICEABLE, with, without) { a, b, t ->
        TrainingAfterSleep(a, b, with.size, without.size, t)
    }
}

/**
 * The days with [driver] against the days without, by how the user felt ([wellbeing], 1–5). Food is known from the
 * nutrition score, training from an engaged day's minutes.
 */
private fun wellbeing(
    days: List<DayEntry>,
    driver: WellbeingDriver,
    wellbeing: Wellbeing,
    goals: Goals,
): WellbeingConnection? {
    val with = mutableListOf<Double>()
    val without = mutableListOf<Double>()
    days.takeLast(CONNECTION_WINDOW_DAYS).forEach { day ->
        val value = when (wellbeing) {
            Wellbeing.Energy -> day.energy.takeIf { it > 0 }?.toDouble()
            Wellbeing.Mood -> day.mood.takeIf { it > 0 }?.toDouble()
        } ?: return@forEach
        val had = when (driver) {
            WellbeingDriver.Nutrition -> day.nutritionScore(goals)?.total?.let { it >= GOOD_NUTRITION_SCORE }
            WellbeingDriver.Training -> if (day.engaged()) day.trainingMinutes() >= TRAINED_MINUTES else null
        } ?: return@forEach
        (if (had) with else without) += value
    }
    return link(WELLBEING_NOTICEABLE, with, without) { a, b, t ->
        WellbeingConnection(driver, wellbeing, a, b, with.size, without.size, t)
    }
}

/** The weekday that stands out most across the parts, when one does: the widest best-to-worst gap of the three. */
private fun weekdayPattern(days: List<DayEntry>, goals: Goals): WeekdayPattern? =
    WeekdayMetric.entries.mapNotNull { metric -> weekdayCandidate(days, metric, goals) }
        .maxByOrNull { it.ranking }

/** [metric]'s best and worst weekday, when both have [MIN_WEEKDAY_DAYS] days and the gap is worth noticing. */
private fun weekdayCandidate(days: List<DayEntry>, metric: WeekdayMetric, goals: Goals): WeekdayPattern? {
    val byWeekday = mutableMapOf<DayOfWeek, MutableList<Double>>()
    days.takeLast(CONNECTION_WINDOW_DAYS).forEach { day ->
        val value = metric.value(day, goals) ?: return@forEach
        byWeekday.getOrPut(day.date.dayOfWeek) { mutableListOf() } += value
    }
    val usable = byWeekday.filterValues { it.size >= MIN_WEEKDAY_DAYS }
    if (usable.size < 2) return null
    val best = usable.maxByOrNull { it.value.average() } ?: return null
    val worst = usable.minByOrNull { it.value.average() } ?: return null
    val difference = best.value.average() - worst.value.average()
    if (difference < metric.noticeable) return null
    return WeekdayPattern(
        metric = metric,
        best = best.key,
        bestAverage = best.value.average(),
        worst = worst.key,
        worstAverage = worst.value.average(),
        days = byWeekday.values.sumOf { it.size },
    )
}

private fun WeekdayMetric.value(day: DayEntry, goals: Goals): Double? = when (this) {
    WeekdayMetric.TrainingMinutes -> if (day.engaged()) day.trainingMinutes().toDouble() else null
    WeekdayMetric.SleepHours -> day.sleepHours.takeIf { it > 0f }?.toDouble()
    WeekdayMetric.NutritionScore -> day.nutritionScore(goals)?.total?.toDouble()
}

/** A comparison worth telling about, or null: [MIN_NIGHTS] each side, [noticeable] apart and |t| ≥ [MIN_STRENGTH]. */
private fun <T> link(
    noticeable: Double,
    a: List<Double>,
    b: List<Double>,
    build: (Double, Double, Double) -> T,
): T? {
    if (a.size < MIN_NIGHTS || b.size < MIN_NIGHTS) return null
    val difference = a.average() - b.average()
    if (abs(difference) < noticeable) return null
    val t = welchT(a, b)
    if (abs(t) < MIN_STRENGTH) return null
    return build(a.average(), b.average(), t)
}

private val WeekdayMetric.noticeable: Double
    get() = when (this) {
        WeekdayMetric.TrainingMinutes -> 15.0
        WeekdayMetric.SleepHours -> 0.75
        WeekdayMetric.NutritionScore -> 8.0
    }
