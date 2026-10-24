package se.novila.science

/**
 * A person's own normal for a measurement: the [mean] and [spread] (standard deviation) of their recent values, over
 * [count] of them. A new value is judged against this rather than a fixed number, since what is normal varies a lot
 * between people (heart rate variability easily by a factor of three).
 */
data class Baseline(val mean: Double, val spread: Double, val count: Int) {
    /**
     * Whether [value] is below the normal range: more than [spreads] of the spread under the mean, with a spread of
     * at least [minSpread], so a baseline that hardly varied doesn't call every small dip unusual.
     */
    fun isBelow(value: Double, spreads: Double = 1.0, minSpread: Double = 0.0): Boolean =
        value < mean - spreads * maxOf(spread, minSpread)

    /** Whether [value] is above the normal range, as [isBelow] the other way. */
    fun isAbove(value: Double, spreads: Double = 1.0, minSpread: Double = 0.0): Boolean =
        value > mean + spreads * maxOf(spread, minSpread)
}

/** The baseline of [values], or null with fewer than [minCount] of them (at least two, for a spread). */
fun baseline(values: List<Double>, minCount: Int = 2): Baseline? {
    if (values.size < maxOf(minCount, 2)) return null
    return Baseline(values.average(), standardDeviation(values), values.size)
}
