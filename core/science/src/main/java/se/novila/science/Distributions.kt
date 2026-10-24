package se.novila.science

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/**
 * The two-sided p-value of Student's t with [df] degrees of freedom: the chance of a |t| at least this large when
 * there is no difference. From the regularized incomplete beta function, I(df / (df + t²); df / 2, 1 / 2).
 */
fun studentTTwoSidedP(t: Double, df: Double): Double {
    require(df > 0) { "Degrees of freedom must be positive: $df" }
    if (t.isInfinite()) return 0.0
    if (t.isNaN()) return 1.0
    return regularizedBeta(df / (df + t * t), df / 2, 0.5).coerceIn(0.0, 1.0)
}

/** The chance that a chi-squared variable with [df] degrees of freedom is at least [x]: Q(df / 2, x / 2). */
fun chiSquaredUpperP(x: Double, df: Double): Double {
    require(df > 0) { "Degrees of freedom must be positive: $df" }
    if (x <= 0) return 1.0
    return regularizedGammaQ(df / 2, x / 2).coerceIn(0.0, 1.0)
}

/** ln Γ([x]) for x > 0, by the Lanczos approximation (g = 7, nine terms), accurate to about 15 digits. */
internal fun lnGamma(x: Double): Double {
    if (x < 0.5) return ln(PI / sin(PI * x)) - lnGamma(1 - x)
    val z = x - 1
    var sum = LANCZOS[0]
    for (i in 1 until LANCZOS.size) sum += LANCZOS[i] / (z + i)
    val t = z + LANCZOS_G + 0.5
    return 0.5 * ln(2 * PI) + (z + 0.5) * ln(t) - t + ln(sum)
}

/** The regularized incomplete beta function I_x(a, b), by Lentz's continued fraction (Numerical Recipes 6.4). */
internal fun regularizedBeta(x: Double, a: Double, b: Double): Double {
    if (x <= 0) return 0.0
    if (x >= 1) return 1.0
    val front = exp(lnGamma(a + b) - lnGamma(a) - lnGamma(b) + a * ln(x) + b * ln(1 - x))
    return if (x < (a + 1) / (a + b + 2)) {
        front * betaFraction(x, a, b) / a
    } else {
        1 - front * betaFraction(1 - x, b, a) / b
    }
}

private fun betaFraction(x: Double, a: Double, b: Double): Double {
    var c = 1.0
    var d = nonZero(1 - (a + b) * x / (a + 1))
    d = 1 / d
    var h = d
    for (m in 1..MAX_ITERATIONS) {
        val m2 = 2 * m
        var aa = m * (b - m) * x / ((a - 1 + m2) * (a + m2))
        d = 1 / nonZero(1 + aa * d)
        c = nonZero(1 + aa / c)
        h *= d * c
        aa = -(a + m) * (a + b + m) * x / ((a + m2) * (a + 1 + m2))
        d = 1 / nonZero(1 + aa * d)
        c = nonZero(1 + aa / c)
        val step = d * c
        h *= step
        if (abs(step - 1) < EPSILON) break
    }
    return h
}

/** The regularized upper incomplete gamma function Q(a, x), by series or continued fraction (Numerical Recipes 6.2). */
internal fun regularizedGammaQ(a: Double, x: Double): Double {
    if (x <= 0) return 1.0
    val front = exp(-x + a * ln(x) - lnGamma(a))
    if (x < a + 1) {
        var term = 1 / a
        var sum = term
        var n = a
        for (i in 1..MAX_ITERATIONS) {
            n += 1
            term *= x / n
            sum += term
            if (abs(term) < abs(sum) * EPSILON) break
        }
        return 1 - sum * front
    }
    var b = x + 1 - a
    var c = 1 / TINY
    var d = 1 / b
    var h = d
    for (i in 1..MAX_ITERATIONS) {
        val an = -i * (i - a)
        b += 2
        d = 1 / nonZero(an * d + b)
        c = nonZero(b + an / c)
        val step = d * c
        h *= step
        if (abs(step - 1) < EPSILON) break
    }
    return front * h
}

private fun nonZero(value: Double): Double = if (abs(value) < TINY) TINY else value

private const val MAX_ITERATIONS = 300
private const val EPSILON = 1e-14
private const val TINY = 1e-300
private const val LANCZOS_G = 7.0
private val LANCZOS = doubleArrayOf(
    0.99999999999980993,
    676.5203681218851,
    -1259.1392167224028,
    771.32342877765313,
    -176.61502916214059,
    12.507343278686905,
    -0.13857109526572012,
    9.9843695780195716e-6,
    1.5056327351493116e-7,
)
