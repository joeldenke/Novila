package se.novila

import kotlin.math.roundToInt

const val SLEEP_GOAL_HOURS = 8f

val DayEntry.sleepProgress: Float get() = (sleepHours / SLEEP_GOAL_HOURS).coerceIn(0f, 1f)

/** Today's balance from 0 to 100: water and sleep against their goals, weighted equally. */
val DayEntry.balance: Int get() = ((waterProgress + sleepProgress) / 2 * 100).roundToInt()

/** The one thing to do next, shown under the balance. */
sealed interface DayTip {
    data object LogSleep : DayTip

    data class DrinkWater(val glassesLeft: Int) : DayTip

    data object GoalsMet : DayTip
}

fun DayEntry.tip(): DayTip = when {
    sleepHours <= 0f -> DayTip.LogSleep
    waterGlasses < WATER_GOAL -> DayTip.DrinkWater(WATER_GOAL - waterGlasses)
    else -> DayTip.GoalsMet
}

enum class Greeting { Morning, Afternoon, Evening }

/** Morning 05–11, afternoon 12–17, evening otherwise. */
fun greetingFor(hour: Int): Greeting = when (hour) {
    in 5..11 -> Greeting.Morning
    in 12..17 -> Greeting.Afternoon
    else -> Greeting.Evening
}
