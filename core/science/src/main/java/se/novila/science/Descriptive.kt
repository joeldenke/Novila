package se.novila.science

import kotlin.math.pow
import kotlin.math.sqrt

/** The sample variance (n − 1) of [values], or 0 with fewer than two. */
fun sampleVariance(values: List<Double>): Double {
    if (values.size < 2) return 0.0
    val mean = values.average()
    return values.sumOf { (it - mean) * (it - mean) } / (values.size - 1)
}

/** The sample standard deviation (n − 1) of [values], or 0 with fewer than two. */
fun standardDeviation(values: List<Double>): Double = sqrt(sampleVariance(values))

/** The population standard deviation (n) of [values], as Foster's monotony uses it, or 0 when empty. */
fun populationStandardDeviation(values: List<Double>): Double {
    if (values.isEmpty()) return 0.0
    val mean = values.average()
    return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
}

/** The median of [values]: the middle one, or the mean of the two middle ones for an even count. */
fun median(values: List<Double>): Double {
    require(values.isNotEmpty()) { "The median of nothing is undefined" }
    val sorted = values.sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}

/**
 * The power mean of [values] (all 0 or more) with exponent [p]: (Σ xᵖ / n)^(1/p). p = 1 is the ordinary mean, and the
 * lower p is, the less a high value can make up for a low one; p → 0 is the geometric mean (an unmet part pulls the
 * whole to zero) and p → −∞ the minimum. Composite indices that should reward balance between their parts use a p
 * below 1 for that reason (the UN's Human Development Index uses the geometric mean).
 */
fun powerMean(values: List<Double>, p: Double): Double {
    require(values.isNotEmpty()) { "The mean of nothing is undefined" }
    require(values.all { it >= 0 }) { "The power mean is for values of 0 or more" }
    require(p > 0) { "Only positive exponents are supported: $p" }
    return values.sumOf { it.pow(p) }.div(values.size).pow(1 / p)
}
