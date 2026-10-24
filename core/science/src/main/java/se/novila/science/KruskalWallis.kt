package se.novila.science

/** The Kruskal–Wallis [h] statistic over some groups, its degrees of freedom [df] (groups − 1) and the [p]-value. */
data class KruskalWallis(val h: Double, val df: Int) {
    val p: Double get() = chiSquaredUpperP(h, df.toDouble())
}

/**
 * The Kruskal–Wallis test: whether some of the [groups] tend to be higher than others, from the ranks of all values
 * together, so it needs no normal distribution and isn't swayed by one odd day. Ties share their mean rank and H is
 * corrected for them. The p-value is the chi-squared approximation. Null with fewer than two non-empty groups, or when
 * every value is the same.
 */
fun kruskalWallis(groups: List<List<Double>>): KruskalWallis? {
    val used = groups.filter { it.isNotEmpty() }
    if (used.size < 2) return null
    val all = used.flatMapIndexed { group, values -> values.map { group to it } }.sortedBy { it.second }
    val n = all.size
    val ranks = DoubleArray(n)
    var ties = 0.0
    var start = 0
    while (start < n) {
        var end = start
        while (end + 1 < n && all[end + 1].second == all[start].second) end++
        val rank = (start + end) / 2.0 + 1
        for (i in start..end) ranks[i] = rank
        val tied = (end - start + 1).toDouble()
        ties += tied * tied * tied - tied
        start = end + 1
    }
    val correction = 1 - ties / (n.toDouble() * n * n - n)
    if (correction <= 0) return null
    val rankSums = DoubleArray(used.size)
    all.forEachIndexed { i, (group, _) -> rankSums[group] += ranks[i] }
    val h = 12.0 / (n * (n + 1.0)) * used.indices.sumOf { rankSums[it] * rankSums[it] / used[it].size } - 3 * (n + 1.0)
    return KruskalWallis(h / correction, used.size - 1)
}
