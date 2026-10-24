package se.novila.model

import se.novila.science.Observation
import se.novila.science.TTest
import se.novila.science.adjustedTest
import se.novila.science.benjaminiHochberg
import se.novila.science.isNoticeable
import se.novila.science.standardDeviation
import java.time.DayOfWeek
import kotlin.math.abs
import kotlin.math.sign

/**
 * Something that may affect a night's sleep: what the user did the day before (from that day's logs), or what they
 * said about the evening when logging the night ([SleepFactor]). [expected] is the direction research suggests: +1
 * helps sleep, -1 hurts it. The user's own logs decide what is shown; [expected] only orders a week's changes before
 * there are enough nights to tell.
 */
enum class SleepDriver(val expected: Int) {
    /** A good nutrition score ([GOOD_NUTRITION_SCORE] or more) the day before. */
    AteWell(+1),

    /** The water goal reached the day before. */
    DrankEnough(+1),

    /** At least [TRAINED_MINUTES] of strength or cardio the day before. */
    Trained(+1),

    /** [TREATS_PORTIONS] or more portions of sweets and snacks the day before. */
    Treats(-1),

    /** Caffeine late the day before: said about the evening, or a drink logged late ([DayEntry.lateCaffeine]). */
    LateCaffeine(-1),
    Screens(-1),
    Alcohol(-1),
    Stress(-1),
    LateMeal(-1),
    LateExercise(-1),

    /** A period that night, said when logging it. */
    Period(-1),
}

/** What a night is measured by: how long it was, or how rested the user felt (1–5). */
enum class SleepOutcome(
    /** The smallest difference worth telling the user about: 20 minutes, or half a step on the 1–5 scale. */
    val noticeable: Double,
) {
    Hours(1.0 / 3),
    Quality(0.5),
}

/**
 * Nights after [driver] against nights without it, in the user's own logs: [withAverage] over [withNights] nights and
 * [withoutAverage] over [withoutNights]. [difference] is with minus without, in hours or quality steps. [strength] is
 * the t of the difference once nights before free days are accounted for, so a larger, steadier difference over more
 * nights ranks first.
 */
data class SleepLink(
    val driver: SleepDriver,
    val outcome: SleepOutcome,
    val withAverage: Double,
    val withoutAverage: Double,
    val withNights: Int,
    val withoutNights: Int,
    val strength: Double,
) {
    val difference: Double get() = withAverage - withoutAverage
}

/** How many of the last [DAYS_PER_WEEK] days had [driver], against the week before. */
data class DriverChange(val driver: SleepDriver, val before: Int, val now: Int)

/**
 * The last seven nights against the seven before: [outcome] averaged [now] against [before] (hours or quality steps),
 * and these [drivers] moved with it in the direction that explains it. [confirmed] when the user's longer history
 * shows the same link for at least one of them, so it is more than one week's coincidence.
 */
data class SleepChange(
    val outcome: SleepOutcome,
    val now: Double,
    val before: Double,
    val drivers: List<DriverChange>,
    val confirmed: Boolean,
) {
    val change: Double get() = now - before
}

/** Bedtimes over the last two weeks vary by [spreadMinutes] (standard deviation) over [nights] nights. */
data class BedtimeSpread(val spreadMinutes: Int, val nights: Int)

/**
 * What the user's logs say about their sleep: this week's [change] and what came with it, the strongest [links]
 * between what they do and how they sleep, and how regular their bedtime is ([bedtimes]). [nightsLogged] is how many
 * nights in the window have sleep logged, so the app can say how far from the first findings the user is.
 */
data class SleepAnalysis(
    val change: SleepChange?,
    val links: List<SleepLink>,
    val bedtimes: BedtimeSpread?,
    val nightsLogged: Int,
) {
    val isEmpty: Boolean get() = change == null && links.isEmpty() && bedtimes == null
}

const val TRAINED_MINUTES = 20
const val TREATS_PORTIONS = 2

/** Nights needed on each side of a comparison before it says anything. */
const val MIN_NIGHTS = 4

/** How far back the links look: about three months of nights. */
const val SLEEP_WINDOW_NIGHTS = 90

/** Bedtimes that vary by an hour or more (standard deviation) are worth a word. */
const val IRREGULAR_BEDTIME_MINUTES = 60

/**
 * The false discovery rate the links are kept at (Benjamini–Hochberg): every habit is compared with both how long and
 * how well the user slept, about twenty comparisons at once, so on average at most one link in ten shown is chance.
 */
const val FALSE_DISCOVERY_RATE = 0.10

/**
 * A link that was shown yesterday stays while its q is at most this, so a link near the threshold doesn't come and go
 * from one day to the next (hysteresis): it takes more to show a link than to keep it.
 */
const val KEEP_DISCOVERY_RATE = 0.20

private const val DAYS_PER_WEEK = 7

/** A week's change in a driver counts from two days more (or fewer) out of seven. */
private const val MIN_DRIVER_CHANGE = 2

/**
 * Analyses [days] (oldest first, ending today) against the user's [goals]. Each
 * night is paired with the day before it: what was eaten, drunk and trained that day, and what the user said about
 * the evening when logging the night. Only nights and days the user actually logged count; a day without logs is
 * unknown, not a day without training. At most [limit] links, the strongest first and one per driver.
 */
fun analyzeSleep(days: List<DayEntry>, goals: Goals = Goals(), limit: Int = 3): SleepAnalysis {
    val nights = nightsOf(days)
    // Yesterday's window, so a link that was shown then needs less to stay (KEEP_DISCOVERY_RATE).
    val shownYesterday = candidates(nightsOf(days.dropLast(1)), goals)
        .filter { it.q <= FALSE_DISCOVERY_RATE }
        .map { it.link.driver to it.link.outcome }
        .toSet()
    val allLinks = candidates(nights, goals).filter { candidate ->
        val key = candidate.link.driver to candidate.link.outcome
        candidate.q <= FALSE_DISCOVERY_RATE || (key in shownYesterday && candidate.q <= KEEP_DISCOVERY_RATE)
    }.map { it.link }
    return SleepAnalysis(
        change = change(nights, allLinks, goals),
        // The strongest link per driver, so one habit doesn't take two places (hours and quality).
        links = allLinks.groupBy { it.driver }.values.map { links -> links.maxBy { abs(it.strength) } }
            .sortedByDescending { abs(it.strength) }
            .take(limit),
        bedtimes = bedtimeSpread(nights),
        nightsLogged = nights.count { it.night.sleepHours > 0f },
    )
}

/** Each candidate link's q over [days], by driver and outcome, so the thresholds can be tested. */
internal fun linkQValues(days: List<DayEntry>, goals: Goals = Goals()): Map<Pair<SleepDriver, SleepOutcome>, Double> =
    candidates(nightsOf(days), goals).associate { (it.link.driver to it.link.outcome) to it.q }

private fun nightsOf(days: List<DayEntry>): List<Night> =
    days.zipWithNext { before, night -> Night(night, before) }.takeLast(SLEEP_WINDOW_NIGHTS)

private class Night(val night: DayEntry, val dayBefore: DayEntry) {
    /** A night before a free day (Saturday or Sunday morning), when no alarm cuts it short. */
    val beforeFreeDay: Boolean get() = night.date.dayOfWeek in FREE_MORNINGS
}

private val FREE_MORNINGS = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

/** The night's value for [outcome], or null when it wasn't logged. */
private fun Night.value(outcome: SleepOutcome): Double? = when (outcome) {
    SleepOutcome.Hours -> night.sleepHours.takeIf { it > 0f }?.toDouble()
    SleepOutcome.Quality -> night.sleepQuality.takeIf { it > 0 }?.toDouble()
}

/** Whether [driver] was there for this night, or null when the logs don't say. */
private fun Night.has(driver: SleepDriver, goals: Goals): Boolean? {
    val day = dayBefore
    val ateLogged = day.food != FoodLog()
    val anyLogged = ateLogged || day.drinks.isNotEmpty() || day.mood > 0 || day.strengthMinutes > 0 ||
        day.cardio.isNotEmpty()
    // Factors are only known for nights where the user filled in the details: quality or any factor.
    val detailed = night.sleepQuality > 0 || night.sleepFactors.isNotEmpty()
    return when (driver) {
        SleepDriver.AteWell -> day.nutritionScore(goals)?.let { it.total >= GOOD_NUTRITION_SCORE }
        SleepDriver.DrankEnough -> if (anyLogged) day.waterGlasses >= goals.waterGlasses else null
        SleepDriver.Trained -> if (anyLogged) day.strengthMinutes + day.cardioMinutes >= TRAINED_MINUTES else null
        SleepDriver.Treats -> if (ateLogged) day.food.treats >= TREATS_PORTIONS else null
        SleepDriver.LateCaffeine -> lateCaffeine(detailed)
        SleepDriver.Screens -> factor(SleepFactor.Screens, detailed)
        SleepDriver.Alcohol -> factor(SleepFactor.Alcohol, detailed)
        SleepDriver.Stress -> factor(SleepFactor.Stress, detailed)
        SleepDriver.LateMeal -> factor(SleepFactor.LateMeal, detailed)
        SleepDriver.LateExercise -> factor(SleepFactor.LateExercise, detailed)
        SleepDriver.Period -> factor(SleepFactor.Period, detailed)
    }
}

/** Said about the evening, or a coffee, tea or energy drink logged late the day before. */
private fun Night.lateCaffeine(detailed: Boolean): Boolean? = when {
    dayBefore.lateCaffeine() == true -> true
    detailed -> SleepFactor.LateCaffeine in night.sleepFactors
    else -> dayBefore.lateCaffeine()
}

private fun Night.factor(factor: SleepFactor, detailed: Boolean): Boolean? =
    if (detailed) factor in night.sleepFactors else null

/** A possible link and its Benjamini–Hochberg [q] among every comparison made over the same nights. */
private class Candidate(val link: SleepLink, val q: Double)

/**
 * Every driver against every outcome over [nights]. Each comparison with [MIN_NIGHTS] on each side is a test, and
 * all of them are adjusted together ([benjaminiHochberg]), since twenty habits tested at once will always turn up a
 * few by chance. Of those, the candidates are the ones with a difference the user would notice, both as measured and
 * once the weekend is accounted for.
 */
private fun candidates(nights: List<Night>, goals: Goals): List<Candidate> {
    val tests = SleepOutcome.entries.flatMap { outcome ->
        SleepDriver.entries.mapNotNull { driver -> compare(nights, driver, outcome, goals) }
    }
    val q = benjaminiHochberg(tests.map { it.second.p })
    return tests.indices.mapNotNull { i ->
        val (link, test) = tests[i]
        val noticeable = link.outcome.noticeable
        val agrees = abs(link.difference) >= noticeable && test.isNoticeable(noticeable) &&
            sign(link.difference) == sign(test.estimate)
        if (agrees) Candidate(link, q[i]) else null
    }
}

/**
 * Nights with [driver] against nights without, adjusted for the nights before free days ([adjustedTest]): Friday's
 * alcohol and Saturday's lie-in would otherwise read as alcohol making the night longer. Needs [MIN_NIGHTS] on each
 * side. The link's averages are the nights as they were; its [SleepLink.strength] is the adjusted t.
 */
private fun compare(
    nights: List<Night>,
    driver: SleepDriver,
    outcome: SleepOutcome,
    goals: Goals,
): Pair<SleepLink, TTest>? {
    val observations = nights.mapNotNull { night ->
        val value = night.value(outcome) ?: return@mapNotNull null
        val has = night.has(driver, goals) ?: return@mapNotNull null
        Observation(value, exposed = has, stratum = night.beforeFreeDay)
    }
    val with = observations.filter { it.exposed }.map { it.value }
    val without = observations.filterNot { it.exposed }.map { it.value }
    if (with.size < MIN_NIGHTS || without.size < MIN_NIGHTS) return null
    val test = adjustedTest(observations, MIN_STANDARD_ERROR) ?: return null
    return SleepLink(driver, outcome, with.average(), without.average(), with.size, without.size, test.t) to test
}

/** Groups that don't vary at all still differ: their spread is taken as a quarter of the smallest noticeable step. */
internal const val MIN_STANDARD_ERROR = 0.05

/**
 * The last seven nights against the seven before, for each outcome with [MIN_NIGHTS] logged nights in both weeks.
 * When it changed noticeably, the drivers that moved with it (two days or more out of seven, in the direction that
 * explains the change: the user's own link where there is one, otherwise [SleepDriver.expected]). Only a change with
 * at least one such driver is returned: a change alone is already in the history chart. Hours go before quality.
 */
private fun change(nights: List<Night>, links: List<SleepLink>, goals: Goals): SleepChange? {
    if (nights.size < 2 * DAYS_PER_WEEK) return null
    val now = nights.takeLast(DAYS_PER_WEEK)
    val before = nights.dropLast(DAYS_PER_WEEK).takeLast(DAYS_PER_WEEK)
    return SleepOutcome.entries.firstNotNullOfOrNull { outcome ->
        val nowValues = now.mapNotNull { it.value(outcome) }
        val beforeValues = before.mapNotNull { it.value(outcome) }
        if (nowValues.size < MIN_NIGHTS || beforeValues.size < MIN_NIGHTS) return@firstNotNullOfOrNull null
        val change = nowValues.average() - beforeValues.average()
        if (abs(change) < outcome.noticeable) return@firstNotNullOfOrNull null
        val direction = if (change > 0) 1 else -1
        val drivers = SleepDriver.entries.mapNotNull { driver ->
            val beforeCount = before.count { it.has(driver, goals) == true }
            val nowCount = now.count { it.has(driver, goals) == true }
            val effect = links.firstOrNull { it.driver == driver && it.outcome == outcome }
                ?.let { if (it.difference > 0) 1 else -1 }
                ?: driver.expected
            val moved = nowCount - beforeCount
            DriverChange(driver, beforeCount, nowCount)
                .takeIf { abs(moved) >= MIN_DRIVER_CHANGE && moved * effect * direction > 0 }
        }.sortedByDescending { abs(it.now - it.before) }.take(2)
        if (drivers.isEmpty()) return@firstNotNullOfOrNull null
        val confirmed = drivers.any { changed ->
            links.any { it.driver == changed.driver && it.outcome == outcome }
        }
        SleepChange(outcome, nowValues.average(), beforeValues.average(), drivers, confirmed)
    }
}

/**
 * How much bedtimes varied over the last two weeks, when five or more are logged and they vary by an hour or more.
 * Times are counted from noon, so 23:30 and 00:30 are an hour apart, not 23.
 */
private fun bedtimeSpread(nights: List<Night>): BedtimeSpread? {
    val minutes = nights.takeLast(2 * DAYS_PER_WEEK).mapNotNull { night ->
        night.night.bedtime?.let { (it.hour * 60 + it.minute - NOON + MINUTES_PER_DAY) % MINUTES_PER_DAY }
    }.map { it.toDouble() }
    if (minutes.size < 5) return null
    val spread = standardDeviation(minutes).toInt()
    return BedtimeSpread(spread, minutes.size).takeIf { spread >= IRREGULAR_BEDTIME_MINUTES }
}

private const val NOON = 12 * 60
private const val MINUTES_PER_DAY = 24 * 60
