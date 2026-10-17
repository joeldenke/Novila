package se.novila.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import se.novila.model.BedtimeSpread
import se.novila.model.DriverChange
import se.novila.model.SleepChange
import se.novila.model.SleepDriver
import se.novila.model.SleepLink
import se.novila.model.SleepOutcome
import se.novila.ui.R
import kotlin.math.abs
import kotlin.math.roundToInt

/** A link in words, e.g. "After days you ate well, you slept 40 min longer." */
@Composable
fun sleepLinkText(link: SleepLink): String {
    val outcome = when (link.outcome) {
        SleepOutcome.Hours -> stringResource(
            if (link.difference > 0) R.string.sleep_link_longer else R.string.sleep_link_shorter,
            durationText((abs(link.difference) * 60).roundToInt()),
        )

        SleepOutcome.Quality -> stringResource(
            if (link.difference > 0) R.string.sleep_link_better else R.string.sleep_link_worse,
            decimalText(link.withAverage),
            decimalText(link.withoutAverage),
        )
    }
    return stringResource(R.string.sleep_link_sentence, stringResource(link.driver.after), outcome)
}

/** How many nights a link stands on, e.g. "12 nights with, 18 without". */
@Composable
fun sleepLinkNightsText(link: SleepLink): String =
    stringResource(R.string.sleep_link_nights, link.withNights, link.withoutNights)

/**
 * This week against the last in words, with what changed alongside and whether the user's earlier nights back it up,
 * e.g. "This week you slept 35 min more per night than the week before. At the same time, you ate well on 5 of 7
 * days (2 the week before). Your earlier nights show the same link."
 */
@Composable
fun sleepChangeText(change: SleepChange): String {
    val headline = when (change.outcome) {
        SleepOutcome.Hours -> stringResource(
            if (change.change > 0) R.string.sleep_change_longer else R.string.sleep_change_shorter,
            durationText((abs(change.change) * 60).roundToInt()),
        )

        SleepOutcome.Quality -> stringResource(
            if (change.change > 0) R.string.sleep_change_better else R.string.sleep_change_worse,
            decimalText(change.now),
            decimalText(change.before),
        )
    }
    val drivers = change.drivers.map { driverChangeText(it) }
    val with = drivers.reduceOrNull { first, second -> stringResource(R.string.sleep_change_and, first, second) }
    val support = stringResource(
        if (change.confirmed) R.string.sleep_change_confirmed else R.string.sleep_change_unconfirmed,
    )
    return listOfNotNull(headline, with?.let { stringResource(R.string.sleep_change_with, it) }, support)
        .joinToString(" ")
}

@Composable
private fun driverChangeText(change: DriverChange): String =
    stringResource(change.driver.changed, change.now, change.before)

/** How much the bedtime varies, and why a steady one helps. */
@Composable
fun bedtimeSpreadText(spread: BedtimeSpread): String =
    stringResource(R.string.sleep_bedtime_spread, durationText(spread.spreadMinutes))

private val SleepDriver.after: Int
    get() = when (this) {
        SleepDriver.AteWell -> R.string.sleep_driver_ate_well
        SleepDriver.DrankEnough -> R.string.sleep_driver_drank_enough
        SleepDriver.Trained -> R.string.sleep_driver_trained
        SleepDriver.Treats -> R.string.sleep_driver_treats
        SleepDriver.LateCaffeine -> R.string.sleep_driver_late_caffeine
        SleepDriver.Screens -> R.string.sleep_driver_screens
        SleepDriver.Alcohol -> R.string.sleep_driver_alcohol
        SleepDriver.Stress -> R.string.sleep_driver_stress
        SleepDriver.LateMeal -> R.string.sleep_driver_late_meal
        SleepDriver.LateExercise -> R.string.sleep_driver_late_exercise
    }

private val SleepDriver.changed: Int
    get() = when (this) {
        SleepDriver.AteWell -> R.string.sleep_changed_ate_well
        SleepDriver.DrankEnough -> R.string.sleep_changed_drank_enough
        SleepDriver.Trained -> R.string.sleep_changed_trained
        SleepDriver.Treats -> R.string.sleep_changed_treats
        SleepDriver.LateCaffeine -> R.string.sleep_changed_late_caffeine
        SleepDriver.Screens -> R.string.sleep_changed_screens
        SleepDriver.Alcohol -> R.string.sleep_changed_alcohol
        SleepDriver.Stress -> R.string.sleep_changed_stress
        SleepDriver.LateMeal -> R.string.sleep_changed_late_meal
        SleepDriver.LateExercise -> R.string.sleep_changed_late_exercise
    }
