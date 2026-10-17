package se.novila.model

import kotlin.math.roundToInt

/**
 * The user's own daily goals, chosen under Settings > Goals and saved by `GoalsStore` in `:core:data`. The defaults
 * are the general advice the app has always used. Food portions are not here: they follow the Swedish Food Agency's
 * dietary advice ([FoodGroup.goal]), which the nutrition score is built on.
 */
data class Goals(
    val steps: Int = STEPS_GOAL.toInt(),
    val sleepHours: Float = SLEEP_GOAL_HOURS,
    val waterGlasses: Int = WATER_GOAL,
) {
    /** [delta] steps of [STEPS_STEP] more or fewer, within [STEPS]. */
    fun withSteps(delta: Int) = copy(steps = (steps + delta * STEPS_STEP).coerceIn(STEPS))

    /** [delta] half hours more or fewer, within [SLEEP_HOURS]. */
    fun withSleep(delta: Int) = copy(sleepHours = halfHours(sleepHours + delta * SLEEP_STEP))

    fun withWater(delta: Int) = copy(waterGlasses = (waterGlasses + delta).coerceIn(WATER_GLASSES))

    /** Each value moved into its range, so an old or edited file never gives a goal of zero. */
    fun clamped() = Goals(steps.coerceIn(STEPS), halfHours(sleepHours), waterGlasses.coerceIn(WATER_GLASSES))

    companion object {
        val STEPS = 2000..20000
        const val STEPS_STEP = 500
        val SLEEP_HOURS = 6f..10f
        const val SLEEP_STEP = 0.5f
        val WATER_GLASSES = 4..16

        /** Steps a day from age 60, where the health benefit levels off (Paluch et al., Lancet Public Health 2022). */
        const val STEPS_FROM_60 = 7000

        /** Drink besides food per kilo of body weight, about 1.5 l at 70 kg (Livsmedelsverket: 1–1.5 l a day). */
        const val DRINK_ML_PER_KG = 22
        const val GLASS_ML = 200

        private fun halfHours(hours: Float) = (Math.round(hours * 2) / 2f).coerceIn(SLEEP_HOURS)

        /**
         * Goals that fit [profile] in [year], for the fields it has; the rest stay at the general advice.
         * Sleep follows age (teens 8–10 h, adults 7–9 h, from 65 7–8 h), steps follow age, and water follows weight.
         */
        fun suggested(profile: Profile, year: Int): Goals {
            val age = profile.ageIn(year)
            return Goals(
                steps = if (age != null && age >= 60) STEPS_FROM_60 else STEPS_GOAL.toInt(),
                sleepHours = when {
                    age == null -> SLEEP_GOAL_HOURS
                    age < 18 -> 9f
                    age >= 65 -> 7.5f
                    else -> SLEEP_GOAL_HOURS
                },
                waterGlasses = profile.weightKg?.let { (it * DRINK_ML_PER_KG / GLASS_ML.toFloat()).roundToInt() }
                    ?.coerceIn(6, 10)
                    ?: WATER_GOAL,
            )
        }

        /** Whether [profile] has what [suggested] goes on: age or weight. */
        fun canSuggest(profile: Profile) = profile.birthYear != null || profile.weightKg != null
    }
}
