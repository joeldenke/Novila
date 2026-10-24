package se.novila.model

import se.novila.science.powerMean
import kotlin.math.roundToInt

const val SLEEP_GOAL_HOURS = 8f
const val STEPS_GOAL = 8000L

fun DayEntry.sleepProgress(goals: Goals = Goals()): Float = (sleepHours / goals.sleepHours).coerceIn(0f, 1f)

fun stepsProgress(steps: Long, goals: Goals = Goals()): Float = (steps.toFloat() / goals.steps).coerceIn(0f, 1f)

/**
 * Today's balance from 0 to 100: sleep, nutrition and (when known) movement, weighted equally and each measured
 * against the user's [goals]. It is a balance, so one part can't make up for another: the parts are averaged as a
 * power mean with exponent [BALANCE_EXPONENT] rather than the plain mean, the way composite indices such as the UN's
 * Human Development Index reward parts that are met together. A full night and nothing eaten is a quarter of the way,
 * half of each is half, and all of them is everything. Without a movement source the balance is sleep and nutrition
 * only, so it never punishes a user for not connecting one.
 */
fun DayEntry.balance(steps: Long?, goals: Goals = Goals()): Int {
    val parts = listOfNotNull(
        sleepProgress(goals),
        nutritionScore(goals)?.progress ?: 0f,
        steps?.let { stepsProgress(it, goals) },
    )
    return (powerMean(parts.map { it.toDouble() }, BALANCE_EXPONENT) * 100).roundToInt()
}

/** Between the plain mean (1) and the geometric mean (0): an unmet part pulls the balance down, but never to zero. */
const val BALANCE_EXPONENT = 0.5
enum class Greeting { Morning, Afternoon, Evening }

/** Morning 05–11, afternoon 12–17, evening otherwise. */
fun greetingFor(hour: Int): Greeting = when (hour) {
    in 5..11 -> Greeting.Morning
    in 12..17 -> Greeting.Afternoon
    else -> Greeting.Evening
}
