package se.novila.science

import kotlin.math.pow

/**
 * The smoothing factor of an exponentially weighted moving average that spans [days] days, 2 / (days + 1), as
 * Williams et al. (2017) use for the training load: the latest day weighs most and older days fade, instead of every
 * day in a window counting the same and a day dropping out all at once.
 */
fun ewmaFactor(days: Int): Double {
    require(days > 0) { "The span must be at least a day: $days" }
    return 2.0 / (days + 1)
}

/**
 * The exponentially weighted moving average of [values] (oldest first) with the smoothing [factor]: each value moves
 * the average that share of the way towards it. It starts from [seed], by default the plain mean of the values, so a
 * first day that happens to be a rest day doesn't pull the whole average down. Null when there are no values.
 */
fun ewma(values: List<Double>, factor: Double, seed: Double? = null): Double? {
    require(factor in 0.0..1.0) { "The smoothing factor is a share: $factor" }
    if (values.isEmpty()) return null
    return values.fold(seed ?: values.average()) { average, value -> average + factor * (value - average) }
}

/** How much something [age] days old counts when its weight halves every [halfLife] days: 0.5^(age / halfLife). */
fun decayWeight(age: Double, halfLife: Double): Double {
    require(halfLife > 0) { "The half-life must be positive: $halfLife" }
    return 0.5.pow(age / halfLife)
}
