package se.novila.model

import kotlin.math.roundToInt

const val SLEEP_GOAL_HOURS = 8f
const val STEPS_GOAL = 8000L

val DayEntry.sleepProgress: Float get() = (sleepHours / SLEEP_GOAL_HOURS).coerceIn(0f, 1f)

fun stepsProgress(steps: Long): Float = (steps.toFloat() / STEPS_GOAL).coerceIn(0f, 1f)

/**
 * Today's balance from 0 to 100: sleep, nutrition and (when known) movement, weighted equally.
 * Without a movement source the balance is sleep and nutrition only, so it never punishes a
 * user for not connecting one.
 */
fun DayEntry.balance(steps: Long?): Int {
    val parts = listOfNotNull(sleepProgress, nutritionScore()?.progress ?: 0f, steps?.let(::stepsProgress))
    return (parts.average() * 100).roundToInt()
}

/** The one thing to do next, shown under the balance. */
sealed interface DayTip {
    data object LogSleep : DayTip

    /** One more portion of [NextPortion.group] lifts the nutrition score the most. */
    data class Eat(val next: NextPortion) : DayTip

    data object GoalsMet : DayTip
}

fun DayEntry.tip(): DayTip {
    if (sleepHours <= 0f) return DayTip.LogSleep
    return nextPortion()?.let(DayTip::Eat) ?: DayTip.GoalsMet
}

enum class Greeting { Morning, Afternoon, Evening }

/** Morning 05–11, afternoon 12–17, evening otherwise. */
fun greetingFor(hour: Int): Greeting = when (hour) {
    in 5..11 -> Greeting.Morning
    in 12..17 -> Greeting.Afternoon
    else -> Greeting.Evening
}
