package se.novila.model

import java.time.LocalTime

/**
 * What the user drank. [hydrates] counts towards the water goal: water, coffee, tea and milk, which all add to the
 * drink a day besides food ([Goals.DRINK_ML_PER_KG]). Juice, soft drinks and energy drinks are logged but don't count,
 * since the advice is water as the everyday drink. [caffeine] marks the drinks that usually have caffeine, for the
 * sleep analysis ([lateCaffeine]); no amount is estimated, since it varies too much. [defaultMl] is a usual serving.
 */
enum class DrinkKind(val hydrates: Boolean, val caffeine: Boolean, val defaultMl: Int) {
    Water(hydrates = true, caffeine = false, defaultMl = 200),
    Coffee(hydrates = true, caffeine = true, defaultMl = 150),
    Tea(hydrates = true, caffeine = true, defaultMl = 250),
    HerbalTea(hydrates = true, caffeine = false, defaultMl = 250),
    Milk(hydrates = true, caffeine = false, defaultMl = 200),
    Juice(hydrates = false, caffeine = false, defaultMl = 200),
    SoftDrink(hydrates = false, caffeine = false, defaultMl = 330),
    EnergyDrink(hydrates = false, caffeine = true, defaultMl = 250),
}

/** One drink: [ml] of [kind], drunk at [time] (null when logged afterwards for an earlier day). */
data class Drink(val kind: DrinkKind, val ml: Int, val time: LocalTime? = null)

/** The sizes offered when logging a drink: glass, mug, can, bottle and large bottle. Any amount in between works. */
val DRINK_SIZES_ML = listOf(150, 200, 250, 330, 500, 750)

/** A drink is logged in steps of this many millilitres, between [MIN_DRINK_ML] and [MAX_DRINK_ML]. */
const val DRINK_STEP_ML = 50
const val MIN_DRINK_ML = 50
const val MAX_DRINK_ML = 2000

/** At most this many drinks a day, so a stuck finger can't fill the storage. */
const val MAX_DRINKS = 60

/**
 * Caffeine from this hour counts as late: about six hours or more before a usual bedtime, the common advice for the
 * last cup. The user's own nights decide whether it matters for them ([analyzeSleep]).
 */
const val LATE_CAFFEINE_HOUR = 14

/** Millilitres that count towards the water goal. */
val DayEntry.waterMl: Int get() = drinks.filter { it.kind.hydrates }.sumOf { it.ml }

/** Everything drunk, in millilitres, whether it counts towards the water goal or not. */
val DayEntry.drunkMl: Int get() = drinks.sumOf { it.ml }

/** [count] glasses of water ([Goals.GLASS_ML] each), without a time: how water was logged before drinks. */
fun glassesOfWater(count: Int): List<Drink> = List(count.coerceAtLeast(0)) { Drink(DrinkKind.Water, Goals.GLASS_ML) }

/** Adds [drink], its amount kept within [MIN_DRINK_ML]–[MAX_DRINK_ML]. A day holds at most [MAX_DRINKS]. */
fun DayEntry.withDrink(drink: Drink): DayEntry {
    if (drinks.size >= MAX_DRINKS) return this
    return copy(drinks = drinks + drink.copy(ml = drink.ml.coerceIn(MIN_DRINK_ML, MAX_DRINK_ML)))
}

/** Takes back the last drink logged. */
fun DayEntry.withoutLastDrink(): DayEntry = copy(drinks = drinks.dropLast(1))

/**
 * Whether the day had caffeine late ([LATE_CAFFEINE_HOUR] or later): true when a coffee, tea or energy drink was
 * logged then, false when drinks were logged and every one with caffeine was earlier, and null when the logs don't
 * say (nothing drunk logged, or caffeine without a time).
 */
fun DayEntry.lateCaffeine(): Boolean? {
    val caffeinated = drinks.filter { it.kind.caffeine }
    return when {
        caffeinated.any { it.time != null && it.time.hour >= LATE_CAFFEINE_HOUR } -> true
        drinks.isEmpty() || caffeinated.any { it.time == null } -> null
        else -> false
    }
}
