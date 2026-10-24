package se.novila.model

import se.novila.science.Observation
import se.novila.science.adjustedTest
import se.novila.science.benjaminiHochberg
import se.novila.science.isNoticeable
import se.novila.science.kruskalWallis
import java.time.DayOfWeek
import kotlin.math.abs
import kotlin.math.sign

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
 * there are [MIN_NIGHTS] days on each side, the difference is one the user would notice, and it holds once the
 * weekend is accounted for and every comparison is adjusted together, so it is unlikely to be chance alone. On the
 * device and deterministic. [ranking] is how far past "worth telling" the finding is, so the strongest ranks first.
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
 * Like [analyzeSleep], a connection needs [MIN_NIGHTS] days on each side and a difference worth noticing, is compared
 * weekend with weekend and weekday with weekday, and every comparison is adjusted together (Benjamini–Hochberg) and
 * kept at [FALSE_DISCOVERY_RATE], or at [KEEP_DISCOVERY_RATE] when it was shown yesterday.
 */
fun connections(days: List<DayEntry>, goals: Goals = Goals(), limit: Int = 2): List<Connection> {
    val shownYesterday = tested(days.dropLast(1), goals)
        .filter { it.connection != null && it.q <= FALSE_DISCOVERY_RATE }
        .map { it.key }
        .toSet()
    return tested(days, goals)
        .filter { it.q <= FALSE_DISCOVERY_RATE || (it.key in shownYesterday && it.q <= KEEP_DISCOVERY_RATE) }
        .mapNotNull { it.connection }
        .sortedByDescending { it.ranking }
        .take(limit)
}

/** One comparison that was made: its [connection] when the difference is worth noticing, its [p] and later its [q]. */
private class Tested(val key: String, val connection: Connection?, val p: Double, val q: Double = 1.0)

/** Every comparison over the window, with the Benjamini–Hochberg q among them. */
private fun tested(days: List<DayEntry>, goals: Goals): List<Tested> {
    val window = days.takeLast(CONNECTION_WINDOW_DAYS)
    val tests = listOfNotNull(
        trainingAfterSleep(window, goals),
        wellbeing(window, WellbeingDriver.Nutrition, Wellbeing.Energy, goals),
        wellbeing(window, WellbeingDriver.Training, Wellbeing.Mood, goals),
        weekdayPattern(window, goals),
    )
    val q = benjaminiHochberg(tests.map { it.p })
    return tests.mapIndexed { i, test -> Tested(test.key, test.connection, test.p, q[i]) }
}

/**
 * Whether the day says anything about what the user did, so a missing training is a rest day and not unknown. Sleep
 * alone is not enough: a logged night doesn't say anything was trained. Mirrors the sleep analysis's idea of a day.
 */
private fun DayEntry.engaged() =
    food != FoodLog() || drinks.isNotEmpty() || mood > 0 || energy > 0 ||
        strengthMinutes > 0 || cardio.isNotEmpty()

/** A weekend day, when both sleep and training tend to differ from the week. */
private val DayEntry.weekend: Boolean
    get() = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

/**
 * The nights against the training the same day: a full night (at or above the goal) against a shorter one, among
 * days the user engaged with. Grouped by whether the night reached the goal, so it answers "do I train more on days
 * after a good night?".
 */
private fun trainingAfterSleep(days: List<DayEntry>, goals: Goals): Tested? {
    val observations = days.mapNotNull { day ->
        val night = day.sleepHours.takeIf { it > 0f } ?: return@mapNotNull null
        if (!day.engaged()) return@mapNotNull null
        Observation(day.trainingMinutes.toDouble(), exposed = night >= goals.sleepHours, stratum = day.weekend)
    }
    return compare("training-after-sleep", TRAINING_NOTICEABLE, observations) { a, b, t ->
        val with = observations.count { it.exposed }
        TrainingAfterSleep(a, b, with, observations.size - with, t)
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
): Tested? {
    val observations = days.mapNotNull { day ->
        val value = when (wellbeing) {
            Wellbeing.Energy -> day.energy.takeIf { it > 0 }?.toDouble()
            Wellbeing.Mood -> day.mood.takeIf { it > 0 }?.toDouble()
        } ?: return@mapNotNull null
        val had = when (driver) {
            WellbeingDriver.Nutrition -> day.nutritionScore(goals)?.total?.let { it >= GOOD_NUTRITION_SCORE }
            WellbeingDriver.Training -> if (day.engaged()) day.trainingMinutes >= TRAINED_MINUTES else null
        } ?: return@mapNotNull null
        Observation(value, exposed = had, stratum = day.weekend)
    }
    return compare("wellbeing-$driver-$wellbeing", WELLBEING_NOTICEABLE, observations) { a, b, t ->
        val with = observations.count { it.exposed }
        WellbeingConnection(driver, wellbeing, a, b, with, observations.size - with, t)
    }
}

/**
 * The weekday that stands out most across the parts, when one does: the widest best-to-worst gap of the three. The
 * best and worst of seven averages always differ somewhat, so the weekdays are first tested together
 * ([kruskalWallis]): only a metric whose weekdays really differ can have a weekday that stands out.
 */
private fun weekdayPattern(days: List<DayEntry>, goals: Goals): Tested? =
    WeekdayMetric.entries.mapNotNull { metric -> weekdayCandidate(days, metric, goals) }
        .maxByOrNull { (it.connection as? WeekdayPattern)?.ranking ?: 0.0 }

/**
 * [metric]'s best and worst weekday, when at least two weekdays have [MIN_WEEKDAY_DAYS] days: a test with the
 * Kruskal–Wallis p of the weekdays, and a pattern when the gap is worth noticing.
 */
private fun weekdayCandidate(days: List<DayEntry>, metric: WeekdayMetric, goals: Goals): Tested? {
    val byWeekday = mutableMapOf<DayOfWeek, MutableList<Double>>()
    days.forEach { day ->
        val value = metric.value(day, goals) ?: return@forEach
        byWeekday.getOrPut(day.date.dayOfWeek) { mutableListOf() } += value
    }
    val usable = byWeekday.filterValues { it.size >= MIN_WEEKDAY_DAYS }
    if (usable.size < 2) return null
    val test = kruskalWallis(usable.values.toList()) ?: return null
    val best = usable.maxBy { it.value.average() }
    val worst = usable.minBy { it.value.average() }
    val difference = best.value.average() - worst.value.average()
    val pattern = WeekdayPattern(
        metric = metric,
        best = best.key,
        bestAverage = best.value.average(),
        worst = worst.key,
        worstAverage = worst.value.average(),
        days = byWeekday.values.sumOf { it.size },
    ).takeIf { difference >= metric.noticeable }
    return Tested("weekday", pattern, test.p)
}

private fun WeekdayMetric.value(day: DayEntry, goals: Goals): Double? = when (this) {
    WeekdayMetric.TrainingMinutes -> if (day.engaged()) day.trainingMinutes.toDouble() else null
    WeekdayMetric.SleepHours -> day.sleepHours.takeIf { it > 0f }?.toDouble()
    WeekdayMetric.NutritionScore -> day.nutritionScore(goals)?.total?.toDouble()
}

/**
 * The exposed days against the others in [observations], weekend with weekend ([adjustedTest]): a test when there are
 * [MIN_NIGHTS] days on each side, and a connection when the difference, both as measured and adjusted, is at least
 * [noticeable].
 */
private fun <T : Connection> compare(
    key: String,
    noticeable: Double,
    observations: List<Observation>,
    build: (Double, Double, Double) -> T,
): Tested? {
    val a = observations.filter { it.exposed }.map { it.value }
    val b = observations.filterNot { it.exposed }.map { it.value }
    if (a.size < MIN_NIGHTS || b.size < MIN_NIGHTS) return null
    val test = adjustedTest(observations, MIN_STANDARD_ERROR) ?: return null
    val difference = a.average() - b.average()
    val agrees = abs(difference) >= noticeable && test.isNoticeable(noticeable) &&
        sign(difference) == sign(test.estimate)
    return Tested(key, if (agrees) build(a.average(), b.average(), test.t) else null, test.p)
}

private val WeekdayMetric.noticeable: Double
    get() = when (this) {
        WeekdayMetric.TrainingMinutes -> 15.0
        WeekdayMetric.SleepHours -> 0.75
        WeekdayMetric.NutritionScore -> 8.0
    }
