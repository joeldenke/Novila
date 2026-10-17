package se.novila.model

import java.time.Month

/**
 * What the user logged today, one kind at a time. Logs have no time of day, so the most telling kind is shown:
 * a workout before food and water, and those before the night.
 */
sealed interface LatestLog {
    /** The last cardio session logged today. */
    data class Cardio(val log: CardioLog) : LatestLog

    data class Strength(val minutes: Int) : LatestLog

    /** Every food portion logged today, water not counted. */
    data class Food(val portions: Int) : LatestLog

    data class Water(val glasses: Int) : LatestLog

    data class Sleep(val hours: Float) : LatestLog
}

/** The most telling thing logged today, or null when nothing is. */
fun DayEntry.latestLog(): LatestLog? {
    val foodPortions = FoodGroup.entries.filter { it != FoodGroup.Water }.sumOf { portions(it) }
    return when {
        cardio.isNotEmpty() -> LatestLog.Cardio(cardio.last())
        strengthMinutes > 0 -> LatestLog.Strength(strengthMinutes)
        foodPortions > 0 -> LatestLog.Food(foodPortions)
        waterGlasses > 0 -> LatestLog.Water(waterGlasses)
        sleepHours > 0f -> LatestLog.Sleep(sleepHours)
        else -> null
    }
}

/**
 * Something encouraging about what the user did today, true whatever the weather: the app doesn't know whether the
 * sun was out, only the season. Each follows public advice (Livsmedelsverket on vitamin D, WHO on activity).
 */
sealed interface DayCheer {
    /** Walked, ran or cycled in the summer half of the year, when midday sun makes vitamin D in the skin. */
    data class OutdoorsSummer(val log: CardioLog) : DayCheer

    /** Walked, ran or cycled in the dark half of the year: daylight still sets the body clock, which helps sleep. */
    data class OutdoorsDaylight(val log: CardioLog) : DayCheer

    /** Swimming trains the whole body and is gentle on the joints. */
    data class Swim(val minutes: Int) : DayCheer

    /** A strength session: muscles get stronger while resting, so tonight's sleep counts. */
    data class Strength(val minutes: Int) : DayCheer

    /** Slept at least the goal. */
    data class RestedNight(val hours: Float) : DayCheer

    /** Slept clearly less than the goal ([SHORT_NIGHT_HOURS] short or more): go easy and go to bed early. */
    data class ShortNight(val hours: Float) : DayCheer

    /** The vegetables of the day's guideline are eaten. */
    data object Vegetables : DayCheer

    /** The water goal is reached. */
    data object Water : DayCheer

    /** A good nutrition score ([GOOD_NUTRITION_SCORE] or more). */
    data class GoodFood(val score: Int) : DayCheer

    /** Nothing logged yet: logging the night is where the day starts. */
    data object Start : DayCheer
}

/** Hours under the sleep goal that make a night short. */
const val SHORT_NIGHT_HOURS = 1.5f

/** When midday sun in Sweden is strong enough for the skin to make vitamin D (Livsmedelsverket). */
val VitaminDMonths: Set<Month> = setOf(Month.APRIL, Month.MAY, Month.JUNE, Month.JULY, Month.AUGUST, Month.SEPTEMBER)

/**
 * Every cheer that fits the day, the most telling first: what the user did, then the night, then food and water.
 * [DayCheer.Start] only when nothing else fits. Sleep and food are measured against the user's [goals].
 */
fun DayEntry.cheers(goals: Goals = Goals()): List<DayCheer> {
    val outdoors = cardio.lastOrNull { it.type != CardioType.Swimming }
    val swim = cardio.filter { it.type == CardioType.Swimming }.sumOf { it.minutes }
    val score = nutritionScore(goals)?.total
    val found = listOfNotNull(
        outdoors?.let {
            if (date.month in VitaminDMonths) DayCheer.OutdoorsSummer(it) else DayCheer.OutdoorsDaylight(it)
        },
        DayCheer.Swim(swim).takeIf { swim > 0 },
        DayCheer.Strength(strengthMinutes).takeIf { strengthMinutes > 0 },
        when {
            sleepHours <= 0f -> null
            sleepHours >= goals.sleepHours -> DayCheer.RestedNight(sleepHours)
            sleepHours <= goals.sleepHours - SHORT_NIGHT_HOURS -> DayCheer.ShortNight(sleepHours)
            else -> null
        },
        DayCheer.Vegetables.takeIf { portions(FoodGroup.Vegetables) >= FoodGroup.Vegetables.goal },
        DayCheer.Water.takeIf { waterGlasses >= goals.waterGlasses },
        DayCheer.GoodFood(score ?: 0).takeIf { score != null && score >= GOOD_NUTRITION_SCORE },
    )
    return found.ifEmpty { listOf(DayCheer.Start) }
}
