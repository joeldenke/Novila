package se.novila.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import se.novila.model.Connection
import se.novila.model.TrainingAfterSleep
import se.novila.model.WeekdayMetric
import se.novila.model.WeekdayPattern
import se.novila.model.Wellbeing
import se.novila.model.WellbeingConnection
import se.novila.model.WellbeingDriver
import se.novila.ui.R
import java.time.DayOfWeek
import java.time.format.TextStyle
import kotlin.math.abs
import kotlin.math.roundToInt

/** A connection in words, e.g. "On days after a full night you trained 20 min more (14 days against 9)." */
@Composable
fun connectionText(connection: Connection): String = when (connection) {
    is TrainingAfterSleep -> stringResource(
        if (connection.difference > 0) {
            R.string.connection_training_after_sleep_more
        } else {
            R.string.connection_training_after_sleep_less
        },
        durationText(abs(connection.difference).roundToInt()),
        connection.withDays,
        connection.withoutDays,
    )

    is WellbeingConnection -> wellbeingText(connection)
    is WeekdayPattern -> weekdayPatternText(connection)
}

/** How the user felt on days with a driver against days without, e.g. food and energy. */
@Composable
private fun wellbeingText(connection: WellbeingConnection): String {
    val driver = stringResource(
        when (connection.driver) {
            WellbeingDriver.Nutrition -> R.string.connection_driver_ate_well
            WellbeingDriver.Training -> R.string.connection_driver_trained
        },
    )
    val better = connection.difference > 0
    val key = when (connection.wellbeing) {
        Wellbeing.Energy -> if (better) R.string.connection_energy_better else R.string.connection_energy_worse
        Wellbeing.Mood -> if (better) R.string.connection_mood_better else R.string.connection_mood_worse
    }
    return stringResource(key, driver, decimalText(connection.withAverage), decimalText(connection.withoutAverage))
}

/** The weekday that stands out for one part, in that part's own unit. */
@Composable
private fun weekdayPatternText(pattern: WeekdayPattern): String {
    val best = weekdayText(pattern.best)
    val worst = weekdayText(pattern.worst)
    return when (pattern.metric) {
        WeekdayMetric.TrainingMinutes -> stringResource(
            R.string.connection_weekday_training,
            best,
            durationText(pattern.bestAverage.roundToInt()),
            worst,
            durationText(pattern.worstAverage.roundToInt()),
        )

        WeekdayMetric.SleepHours -> stringResource(
            R.string.connection_weekday_sleep,
            best,
            hoursText(pattern.bestAverage.toFloat()),
            worst,
            hoursText(pattern.worstAverage.toFloat()),
        )

        WeekdayMetric.NutritionScore -> stringResource(
            R.string.connection_weekday_nutrition,
            best,
            pattern.bestAverage.roundToInt(),
            worst,
            pattern.worstAverage.roundToInt(),
        )
    }
}

/** A weekday name with a capital first letter, e.g. "Monday" / "Måndag". */
@Composable
@ReadOnlyComposable
private fun weekdayText(day: DayOfWeek): String {
    val locale = LocalConfiguration.current.locales[0]
    return day.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.titlecase(locale) }
}
