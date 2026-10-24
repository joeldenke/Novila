package se.novila.science

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * A difference between two groups and how sure it is: the [estimate] (the first group minus the second), its
 * [standardError], [t] = estimate / standard error, the degrees of freedom [df] and the two-sided [p]: the chance of a
 * difference at least this large if there were none.
 */
data class TTest(val estimate: Double, val standardError: Double, val t: Double, val df: Double) {
    val p: Double get() = studentTTwoSidedP(t, df)
}

/**
 * Welch's t-test for the difference between the means of [a] and [b], which doesn't assume the groups vary equally,
 * with the Welch–Satterthwaite degrees of freedom. The standard error is never below [minStandardError], so groups
 * that don't vary at all (the same hours every night) still compare as a large, not an infinite, difference. Null
 * with fewer than two values in either group.
 */
fun welchTest(a: List<Double>, b: List<Double>, minStandardError: Double = 0.0): TTest? {
    if (a.size < 2 || b.size < 2) return null
    val va = sampleVariance(a) / a.size
    val vb = sampleVariance(b) / b.size
    val error = sqrt(va + vb).coerceAtLeast(minStandardError)
    val df = if (va + vb == 0.0) {
        (a.size + b.size - 2).toDouble()
    } else {
        (va + vb) * (va + vb) / (va * va / (a.size - 1) + vb * vb / (b.size - 1))
    }
    val estimate = a.average() - b.average()
    return TTest(estimate, error, estimate / error, df)
}

/**
 * One observation for [adjustedTest]: its [value], whether it was [exposed] (the habit happened) and its [stratum]
 * (a second thing that could explain the value, such as a weekend).
 */
data class Observation(val value: Double, val exposed: Boolean, val stratum: Boolean)

/**
 * The difference between exposed and unexposed observations once the [Observation.stratum] is accounted for: the
 * exposure's coefficient in the least squares fit value = β₀ + β₁·exposed + β₂·stratum. A habit that mostly happens on
 * weekends, when sleep is longer anyway, is then compared weekend with weekend and weekday with weekday, so the
 * weekend isn't credited to the habit. When the stratum never varies there is nothing to adjust for, and it is
 * [welchTest]. Null with fewer than two observations on either side, or when the exposure can't be told apart from
 * the stratum (every exposed observation is in one stratum and every unexposed one in the other).
 */
fun adjustedTest(observations: List<Observation>, minStandardError: Double = 0.0): TTest? {
    val (exposed, unexposed) = observations.partition { it.exposed }
    if (exposed.size < 2 || unexposed.size < 2) return null
    if (observations.all { it.stratum } || observations.none { it.stratum }) {
        return welchTest(exposed.map { it.value }, unexposed.map { it.value }, minStandardError)
    }
    val fit = leastSquares(
        rows = observations.map { doubleArrayOf(1.0, if (it.exposed) 1.0 else 0.0, if (it.stratum) 1.0 else 0.0) },
        y = observations.map { it.value },
    ) ?: return null
    val estimate = fit.coefficients[1]
    val error = fit.standardErrors[1].coerceAtLeast(minStandardError)
    return TTest(estimate, error, estimate / error, fit.df.toDouble())
}

/** Whether [test] tells of a difference at least [noticeable] in size, either way. */
fun TTest.isNoticeable(noticeable: Double): Boolean = abs(estimate) >= noticeable
