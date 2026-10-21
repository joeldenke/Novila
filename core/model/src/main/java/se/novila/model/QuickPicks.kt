package se.novila.model

import java.time.LocalDate

/** A meal the user wrote in their own words and logged on [date], kept so it can be logged again with one tap. */
data class LoggedMeal(val text: String, val date: LocalDate)

/** A meal offered as a quick pick: its [text] as the user last wrote it, and whether they starred it ([favorite]). */
data class MealPick(val text: String, val favorite: Boolean)

/**
 * The meals to log again with one tap and the drinks to log again with one tap, as the Nutrition tab's sheets show
 * them: [meals] are the favorites first, then the meals logged most often lately ([MealMemory.picks]); [drinks] are
 * the kinds and amounts drunk most often lately ([recentDrinks]).
 */
data class QuickPicks(val meals: List<MealPick> = emptyList(), val drinks: List<Drink> = emptyList())

/**
 * The meals the user has written in their own words: the [recent] ones (at most [MAX_RECENT_MEALS], newest last), and
 * the [favorites] they starred (at most [MAX_FAVORITE_MEALS], in the order they were starred). Two spellings of the
 * same meal ("Gröt, bär" and "gröt,  bär.") are one meal ([mealKey]).
 */
data class MealMemory(val recent: List<LoggedMeal> = emptyList(), val favorites: List<String> = emptyList()) {
    /**
     * Remembers [text] as logged on [date]. Meals older than [QUICK_PICK_DAYS] before it are forgotten, and only the
     * last [MAX_RECENT_MEALS] are kept. Blank text changes nothing.
     */
    fun withLogged(text: String, date: LocalDate): MealMemory {
        val line = mealLine(text)
        if (line.isEmpty()) return this
        val since = date.minusDays(QUICK_PICK_DAYS.toLong())
        val kept = recent.filter { it.date > since } + LoggedMeal(line, date)
        return copy(recent = kept.takeLast(MAX_RECENT_MEALS))
    }

    /** Whether [text] is one of the [favorites]. */
    fun isFavorite(text: String): Boolean = favorites.any { mealKey(it) == mealKey(text) }

    /**
     * Stars [text], or takes the star away when it already has one. A new favorite goes last; when there are already
     * [MAX_FAVORITE_MEALS], nothing changes, so the user takes one away first.
     */
    fun toggledFavorite(text: String): MealMemory {
        val line = mealLine(text)
        if (line.isEmpty()) return this
        return when {
            isFavorite(line) -> copy(favorites = favorites.filterNot { mealKey(it) == mealKey(line) })
            favorites.size >= MAX_FAVORITE_MEALS -> this
            else -> copy(favorites = favorites + line)
        }
    }

    /**
     * The meals to offer on [today]: every favorite first, in the order they were starred, then up to [limit] other
     * meals from the last [QUICK_PICK_DAYS] days, the ones logged on most days first and the latest first among
     * those. Each is shown as the user wrote it last time. Deterministic, so the picks only move when the logs do.
     */
    fun picks(today: LocalDate, limit: Int = MAX_MEAL_PICKS): List<MealPick> {
        val since = today.minusDays(QUICK_PICK_DAYS.toLong())
        val starred = favorites.map { MealPick(it, favorite = true) }
        val others = recent
            .filter { it.date > since && it.date <= today && !isFavorite(it.text) }
            .groupBy { mealKey(it.text) }
            .values
            .sortedWith(
                compareByDescending<List<LoggedMeal>> { meals -> meals.distinctBy { it.date }.size }
                    .thenByDescending { meals -> meals.maxOf { it.date } }
                    .thenByDescending { meals -> recent.lastIndexOf(meals.last()) },
            )
            .take(limit)
            .map { MealPick(it.last().text, favorite = false) }
        return starred + others
    }
}

/**
 * The drinks to log again with one tap: the kinds and amounts drunk on most of [days] (the last [QUICK_PICK_DAYS]),
 * the latest first among those, at most [limit]. A glass of water is left out, since the tab already logs it with one
 * tap.
 */
fun recentDrinks(days: List<DayEntry>, limit: Int = MAX_DRINK_PICKS): List<Drink> {
    val glass = DrinkKind.Water to DrinkKind.Water.defaultMl
    return days
        .flatMap { day -> day.drinks.map { (it.kind to it.ml) to day.date } }
        .filter { (drink, _) -> drink != glass }
        .groupBy({ it.first }, { it.second })
        .entries
        .sortedWith(
            compareByDescending<Map.Entry<Pair<DrinkKind, Int>, List<LocalDate>>> { it.value.distinct().size }
                .thenByDescending { it.value.max() }
                .thenBy { it.key.first.ordinal }
                .thenBy { it.key.second },
        )
        .take(limit)
        .map { (drink, _) -> Drink(drink.first, drink.second) }
}

/** How many days back the quick picks look. */
const val QUICK_PICK_DAYS = 30

/** How many meals are remembered, newest kept. */
const val MAX_RECENT_MEALS = 60

/** How many meals can be starred. */
const val MAX_FAVORITE_MEALS = 8

/** How many meals besides the favorites are offered. */
const val MAX_MEAL_PICKS = 4

/** How many drinks are offered. */
const val MAX_DRINK_PICKS = 3

/** How long a remembered meal may be: one line, like the diary. */
const val MAX_MEAL_LENGTH = 140

/** A meal as it is remembered: trimmed, one line, at most [MAX_MEAL_LENGTH] characters. */
fun mealLine(text: String): String =
    text.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').trim().take(MAX_MEAL_LENGTH).trim()

/** What makes two spellings the same meal: case, extra spaces and a full stop at the end don't count. */
fun mealKey(text: String): String = mealLine(text).lowercase().replace(WHITESPACE, " ").trimEnd('.', '!', ' ')

private val WHITESPACE = Regex("""\s+""")

/**
 * The [MealMemory] as text: "v1" on the first line, then one line per meal, "R|date|text" for a recent one (in order)
 * and "F|text" for a favorite. A meal is one line ([mealLine]), so its text may hold anything else, a "|" too.
 * Unreadable lines are skipped, and anything not starting with a known version decodes to nothing remembered.
 */
object MealMemoryCodec {
    private const val VERSION = "v1"

    fun encode(memory: MealMemory): String {
        val recent = memory.recent.map { "R|${it.date}|${mealLine(it.text)}" }
        val favorites = memory.favorites.map { "F|${mealLine(it)}" }
        return (listOf(VERSION) + recent + favorites).joinToString("\n")
    }

    fun decode(raw: String?): MealMemory {
        val lines = raw?.split('\n') ?: return MealMemory()
        if (lines.first() != VERSION) return MealMemory()
        val recent = lines.drop(1).filter { it.startsWith("R|") }.mapNotNull { line ->
            val fields = line.split('|', limit = 3)
            val date = fields.getOrNull(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val text = mealLine(fields.getOrNull(2).orEmpty())
            if (date != null && text.isNotEmpty()) LoggedMeal(text, date) else null
        }
        val favorites = lines.drop(1).filter { it.startsWith("F|") }
            .map { mealLine(it.removePrefix("F|")) }
            .filter { it.isNotEmpty() }
            .distinctBy(::mealKey)
        return MealMemory(recent.takeLast(MAX_RECENT_MEALS), favorites.take(MAX_FAVORITE_MEALS))
    }
}
