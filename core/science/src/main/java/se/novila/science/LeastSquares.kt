package se.novila.science

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * An ordinary least squares fit: one [coefficients] per column of the design, each with its [standardErrors], and
 * [df], the residual degrees of freedom (observations minus coefficients).
 */
class LeastSquaresFit(val coefficients: List<Double>, val standardErrors: List<Double>, val df: Int)

/**
 * Fits [y] to the columns of [rows] (one row per observation, the same number of columns in each) by ordinary least
 * squares: β = (XᵀX)⁻¹Xᵀy, with standard errors from the residual variance. Null when the columns can't be told apart
 * (one is a combination of the others, such as an exposure that only ever happens on weekends next to a weekend
 * column) or there are no residual degrees of freedom left.
 */
fun leastSquares(rows: List<DoubleArray>, y: List<Double>): LeastSquaresFit? {
    require(rows.size == y.size) { "One row per observation" }
    val k = rows.firstOrNull()?.size ?: return null
    val df = rows.size - k
    if (df < 1) return null
    val xtx = Array(k) { i -> DoubleArray(k) { j -> rows.sumOf { it[i] * it[j] } } }
    val xty = DoubleArray(k) { i -> rows.indices.sumOf { rows[it][i] * y[it] } }
    val inverse = invert(xtx) ?: return null
    val beta = List(k) { i -> (0 until k).sumOf { inverse[i][it] * xty[it] } }
    val residuals = rows.indices.sumOf { n ->
        val fitted = (0 until k).sumOf { rows[n][it] * beta[it] }
        (y[n] - fitted) * (y[n] - fitted)
    }
    val variance = residuals / df
    return LeastSquaresFit(beta, List(k) { sqrt(variance * inverse[it][it].coerceAtLeast(0.0)) }, df)
}

/** A least squares line: [slope] per unit of x, the [intercept] at x = 0 and the slope's [standardError]. */
data class LinearTrend(val slope: Double, val intercept: Double, val standardError: Double)

/**
 * The least squares line through the points ([x], [y]), with the slope's standard error (0 with only two points).
 * Null with fewer than two points or when all x are the same.
 */
fun linearTrend(x: List<Double>, y: List<Double>): LinearTrend? {
    require(x.size == y.size) { "One x per y" }
    if (x.size < 2) return null
    val meanX = x.average()
    val meanY = y.average()
    val sxx = x.sumOf { (it - meanX) * (it - meanX) }
    if (sxx == 0.0) return null
    val slope = x.indices.sumOf { (x[it] - meanX) * (y[it] - meanY) } / sxx
    val intercept = meanY - slope * meanX
    val residuals = x.indices.sumOf {
        val error = y[it] - intercept - slope * x[it]
        error * error
    }
    val error = if (x.size > 2) sqrt(residuals / (x.size - 2) / sxx) else 0.0
    return LinearTrend(slope, intercept, error)
}

/** The inverse of the square [matrix] by Gauss–Jordan elimination with partial pivoting, or null when singular. */
private fun invert(matrix: Array<DoubleArray>): Array<DoubleArray>? {
    val n = matrix.size
    val a = Array(n) { i ->
        DoubleArray(2 * n) { j ->
            when {
                j < n -> matrix[i][j]
                j - n == i -> 1.0
                else -> 0.0
            }
        }
    }
    val scale = matrix.maxOf { row -> row.maxOf { abs(it) } }.takeIf { it > 0 } ?: return null
    for (column in 0 until n) {
        val pivot = (column until n).maxBy { abs(a[it][column]) }
        if (abs(a[pivot][column]) < SINGULAR * scale) return null
        a[column] = a[pivot].also { a[pivot] = a[column] }
        val divisor = a[column][column]
        for (j in 0 until 2 * n) a[column][j] /= divisor
        for (row in 0 until n) {
            if (row == column) continue
            val factor = a[row][column]
            if (factor != 0.0) for (j in 0 until 2 * n) a[row][j] -= factor * a[column][j]
        }
    }
    return Array(n) { i -> DoubleArray(n) { j -> a[i][n + j] } }
}

private const val SINGULAR = 1e-9
