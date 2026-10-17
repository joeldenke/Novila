package se.novila.feature.training

import se.novila.anatomy.ExerciseMotion
import se.novila.coach.BodyArea
import java.util.Locale

/**
 * Whether every word of [query] is found in one of [texts] (an exercise's name and its muscles), ignoring case, so
 * "ben knä" and "Squat" both find what they should. An empty query matches everything.
 */
internal fun matchesSearch(query: String, texts: List<String>, locale: Locale): Boolean {
    val words = query.trim().lowercase(locale).split(Regex("\\s+")).filter { it.isNotEmpty() }
    val haystack = texts.map { it.lowercase(locale) }
    return words.all { word -> haystack.any { it.contains(word) } }
}

/** Whether [motion] mainly trains [area]; every exercise is in "all" (null). */
internal fun trains(motion: ExerciseMotion, area: BodyArea?): Boolean =
    area == null || motion.primary.any { it in area.muscles }
