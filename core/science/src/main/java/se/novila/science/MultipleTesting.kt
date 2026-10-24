package se.novila.science

/**
 * Benjamini–Hochberg adjusted p-values (q-values) for [pValues], in the same order. Testing many habits at once makes
 * some look linked by chance alone; a finding is kept when its q is at most the false discovery rate one accepts, so
 * that at most that share of the findings kept are expected to be chance (Benjamini & Hochberg 1995). Each q is
 * min over the larger p's of p·m / rank, and never above 1.
 */
fun benjaminiHochberg(pValues: List<Double>): List<Double> {
    val m = pValues.size
    if (m == 0) return emptyList()
    val order = pValues.indices.sortedBy { pValues[it] }
    val adjusted = DoubleArray(m)
    var smallest = 1.0
    for (rank in m downTo 1) {
        val index = order[rank - 1]
        smallest = minOf(smallest, pValues[index] * m / rank)
        adjusted[index] = smallest
    }
    return adjusted.toList()
}
