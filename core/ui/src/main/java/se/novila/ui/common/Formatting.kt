package se.novila.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import se.novila.model.DayEntry
import se.novila.model.FoodGroup
import se.novila.model.NextPortion
import se.novila.model.formatHours
import se.novila.ui.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
@ReadOnlyComposable
fun hoursText(hours: Float): String {
    val value = formatHours(hours, currentLocale()) ?: return stringResource(R.string.value_missing)
    return stringResource(R.string.hours_value, value)
}

/** Step count with the locale's digit grouping, e.g. "8,412" / "8 412". */
@Composable
fun stepsText(steps: Long?): String =
    steps?.let { rememberIntegerFormat().format(it) } ?: stringResource(R.string.value_missing)

@Composable
fun kcalText(kilocalories: Int?): String = kilocalories?.let {
    stringResource(R.string.kcal_value, rememberIntegerFormat().format(it))
} ?: stringResource(R.string.value_missing)

@Composable
private fun rememberIntegerFormat(): NumberFormat {
    val locale = currentLocale()
    return remember(locale) { NumberFormat.getIntegerInstance(locale) }
}

@Composable
@ReadOnlyComposable
private fun currentLocale(): Locale =
    // Read straight from LocalConfiguration (observable) so text updates when the app language changes.
    LocalConfiguration.current.locales[0]

/** Short weekday per day for chart axes, e.g. "Mon" / "Mån". */
@Composable
fun weekdayLabels(days: List<DayEntry>): List<String> {
    val locale = currentLocale()
    return remember(days, locale) {
        days.map { day ->
            day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
                .trimEnd('.')
                .replaceFirstChar { it.titlecase(locale) }
        }
    }
}

/** Day and short month per date for chart axes, e.g. "Sep 20" / "20 sep". */
@Composable
fun shortDateLabels(dates: List<LocalDate>): List<String> {
    val formatter = rememberDateFormatter("dMMM")
    return remember(dates, formatter) { dates.map { it.format(formatter).trimEnd('.') } }
}

/** Short month per date for chart axes, e.g. "Oct" / "Okt". */
@Composable
fun monthLabels(dates: List<LocalDate>): List<String> {
    val locale = currentLocale()
    return remember(dates, locale) {
        dates.map { date ->
            date.month.getDisplayName(TextStyle.SHORT, locale)
                .trimEnd('.')
                .replaceFirstChar { it.titlecase(locale) }
        }
    }
}

/** Full weekday and date with a capital first letter, e.g. "Wednesday, October 14" / "Onsdag 14 oktober". */
@Composable
fun longDateText(date: LocalDate): String {
    val locale = currentLocale()
    return date.format(rememberDateFormatter("EEEEdMMMM")).replaceFirstChar { it.titlecase(locale) }
}

@Composable
private fun rememberDateFormatter(skeleton: String): DateTimeFormatter {
    val locale = currentLocale()
    return remember(locale, skeleton) {
        val pattern = android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
        DateTimeFormatter.ofPattern(pattern, locale)
    }
}

/** The next step for the nutrition score in words, with the points it is worth. */
@Composable
@ReadOnlyComposable
fun nextPortionText(next: NextPortion): String = stringResource(
    when (next.group) {
        FoodGroup.Vegetables -> R.string.tip_next_vegetables
        FoodGroup.Meals -> R.string.tip_next_meal
        FoodGroup.Protein -> R.string.tip_next_protein
        FoodGroup.WholeGrain -> R.string.tip_next_whole_grain
        FoodGroup.Water -> R.string.tip_next_water
    },
    next.points,
)
