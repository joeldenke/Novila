package se.novila.model

import se.novila.science.linearTrend
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow

/** Which way the trend has moved over the last weeks, in a neutral word: never a goal. */
enum class WeightDirection { Steady, Lower, Higher }

/**
 * The weight's trend: a running average of the weigh-ins, so the day-to-day swings of fluid and food don't steer the
 * picture. [points] are the trend on each day the user weighed in, oldest first. [weeklyChangeKg] is how much the
 * trend has moved a week, on average, over the last [weeks] weeks, or null until weigh-ins reach back
 * [MIN_TREND_DAYS] days.
 */
data class WeightTrend(
    val points: List<Pair<LocalDate, Double>>,
    val weeklyChangeKg: Double?,
    val weeks: Int,
) {
    val latestKg: Double get() = points.last().second

    val direction: WeightDirection?
        get() = weeklyChangeKg?.let {
            when {
                abs(it) < STEADY_KG_PER_WEEK -> WeightDirection.Steady
                it < 0 -> WeightDirection.Lower
                else -> WeightDirection.Higher
            }
        }
}

/**
 * The trend of the weights in [entries] (any order), or null with fewer than two weigh-ins. Each weigh-in moves the
 * trend a tenth of the way towards it per day since the last one, so a gap counts as the days it was, however
 * often the user steps on the scale (an exponentially weighted moving average, as in The Hacker's Diet). The weekly
 * change is the slope of the least squares line through the weigh-ins of the last [TREND_DAYS] days, once they span
 * at least [MIN_TREND_DAYS]: a moving average lags behind, so the difference between two of its points understates a
 * change that has just begun, while the line through the weigh-ins themselves doesn't, and every weigh-in counts, so
 * one day moves it the least.
 * Deterministic, so it is unit tested.
 */
fun weightTrend(entries: Iterable<MeasurementEntry>): WeightTrend? {
    val weights = entries.mapNotNull { entry -> entry.measurements.weightKg?.let { entry.date to it } }
        .sortedBy { it.first }
    if (weights.size < 2) return null
    var trend = weights.first().second.toDouble()
    var previous = weights.first().first
    val points = weights.map { (date, kg) ->
        val days = ChronoUnit.DAYS.between(previous, date).toDouble()
        trend += (1 - (1 - DAILY_SMOOTHING).pow(days)) * (kg - trend)
        previous = date
        date to trend
    }
    val lastDate = weights.last().first
    val recent = weights.filter { (date, _) -> ChronoUnit.DAYS.between(date, lastDate) <= TREND_DAYS }
    val days = ChronoUnit.DAYS.between(recent.first().first, lastDate).takeIf { it >= MIN_TREND_DAYS }
    val line = days?.let {
        linearTrend(
            x = recent.map { (date, _) -> ChronoUnit.DAYS.between(lastDate, date).toDouble() },
            y = recent.map { (_, kg) -> kg.toDouble() },
        )
    }
    return WeightTrend(
        points = points,
        weeklyChangeKg = line?.let { it.slope * DAYS_PER_WEEK },
        weeks = ((days ?: 0L) / DAYS_PER_WEEK).toInt(),
    )
}

/** How far the trend moves towards a weigh-in per day since the one before. */
const val DAILY_SMOOTHING = 0.1

/** Weigh-ins this far apart, at the least, before the trend says which way it moves. */
const val MIN_TREND_DAYS = 14L

/** The weeks the trend's change looks back over, at the most. */
const val TREND_DAYS = 28L

/** Less than 0.1 kg a week is about the same: a scale's own wobble. */
const val STEADY_KG_PER_WEEK = 0.1

private const val DAYS_PER_WEEK = 7L
