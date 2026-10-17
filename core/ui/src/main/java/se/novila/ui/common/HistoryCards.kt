package se.novila.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import se.novila.designsystem.component.NovilaBarChart
import se.novila.designsystem.component.NovilaCard
import se.novila.designsystem.component.NovilaChoiceChip
import se.novila.designsystem.component.NovilaLineChart
import se.novila.designsystem.component.NovilaText
import se.novila.designsystem.theme.NovilaColorRole
import se.novila.designsystem.theme.NovilaTheme
import se.novila.model.DayEntry
import se.novila.model.GOOD_NUTRITION_SCORE
import se.novila.model.Goals
import se.novila.model.History
import se.novila.model.HistoryRange
import se.novila.model.averageNutrition
import se.novila.model.averageSleep
import se.novila.model.historyOfWeek
import se.novila.ui.R
import kotlin.math.abs

/** Sleep chart axis: short nights and long ones both stay readable. */
private val SleepChartRange = 4f..10f

/** The week's nights as a curve against the user's sleep goal, with the average per night. */
@Composable
fun SleepWeekCard(week: List<DayEntry>, goals: Goals) {
    SleepHistoryCard(remember(week) { historyOfWeek(week) }, goals)
}

/** The week's nutrition scores as bars, with water against the user's goal; days with a good score are solid. */
@Composable
fun NutritionWeekCard(week: List<DayEntry>, goals: Goals) {
    NutritionHistoryCard(remember(week) { historyOfWeek(week) }, goals)
}

/**
 * Sleep over the [history]'s range as a curve against the user's sleep goal: a point per night, week or month, each
 * the average of the nights logged in it. With [onRange] the user picks the range, and the average is compared with
 * the range before.
 */
@Composable
fun SleepHistoryCard(history: History, goals: Goals, onRange: ((HistoryRange) -> Unit)? = null) {
    val sleep = NovilaTheme.pillars.sleep
    val average = hoursText(history.days.averageSleep() ?: 0f)
    NovilaCard(
        title = stringResource(
            when (history.range) {
                HistoryRange.Week -> R.string.week_sleep_title
                HistoryRange.Month -> R.string.history_sleep_title_month
                HistoryRange.Year -> R.string.history_sleep_title_year
            },
        ),
        icon = painterResource(R.drawable.ic_sleep),
        role = sleep,
    ) {
        if (onRange != null) HistoryRangePicker(history.range, onRange, sleep)
        HistorySummary(
            value = average,
            caption = stringResource(R.string.week_sleep_caption, hoursText(goals.sleepHours)),
            trend = history.sleepTrend()?.let { sleepTrendText(it, history.range) },
        )
        NovilaLineChart(
            values = remember(history) { history.periods.map { it.days.averageSleep() } },
            labels = historyLabels(history),
            valueRange = SleepChartRange,
            role = sleep,
            contentDescription = stringResource(
                when (history.range) {
                    HistoryRange.Week -> R.string.week_sleep_description
                    HistoryRange.Month -> R.string.history_sleep_description_month
                    HistoryRange.Year -> R.string.history_sleep_description_year
                },
                average,
            ),
            goal = goals.sleepHours,
        )
    }
}

/**
 * Nutrition scores over the [history]'s range as bars, with water against the user's goal: a bar per day, week or
 * month, each the average of the days with food logged. Bars with a good score are solid. With [onRange] the user
 * picks the range, and the average is compared with the range before.
 */
@Composable
fun NutritionHistoryCard(history: History, goals: Goals, onRange: ((HistoryRange) -> Unit)? = null) {
    val nutrition = NovilaTheme.pillars.nutrition
    val average = history.days.averageNutrition(goals)?.let { stringResource(R.string.balance_value, it) }
        ?: stringResource(R.string.value_missing)
    NovilaCard(
        title = stringResource(
            when (history.range) {
                HistoryRange.Week -> R.string.week_nutrition_title
                HistoryRange.Month -> R.string.history_nutrition_title_month
                HistoryRange.Year -> R.string.history_nutrition_title_year
            },
        ),
        icon = painterResource(R.drawable.ic_nutrition),
        role = nutrition,
    ) {
        if (onRange != null) HistoryRangePicker(history.range, onRange, nutrition)
        HistorySummary(
            value = average,
            caption = stringResource(R.string.week_nutrition_caption, GOOD_NUTRITION_SCORE),
            trend = history.nutritionTrend(goals)?.let { nutritionTrendText(it, history.range) },
        )
        NovilaBarChart(
            values = remember(history, goals) {
                history.periods.map { period -> period.days.averageNutrition(goals)?.toFloat() }
            },
            labels = historyLabels(history),
            maxValue = 100f,
            role = nutrition,
            contentDescription = stringResource(
                when (history.range) {
                    HistoryRange.Week -> R.string.week_nutrition_description
                    HistoryRange.Month -> R.string.history_nutrition_description_month
                    HistoryRange.Year -> R.string.history_nutrition_description_year
                },
                average,
            ),
            goal = GOOD_NUTRITION_SCORE.toFloat(),
        )
    }
}

/** Week, four weeks or a year, as chips in the [role] color of the part of the app the chart is about. */
@Composable
private fun HistoryRangePicker(selected: HistoryRange, onSelect: (HistoryRange) -> Unit, role: NovilaColorRole) {
    Row(horizontalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.s)) {
        HistoryRange.entries.forEach { range ->
            NovilaChoiceChip(
                label = stringResource(
                    when (range) {
                        HistoryRange.Week -> R.string.history_week
                        HistoryRange.Month -> R.string.history_month
                        HistoryRange.Year -> R.string.history_year
                    },
                ),
                selected = range == selected,
                onClick = { onSelect(range) },
                role = role,
            )
        }
    }
}

@Composable
private fun HistorySummary(value: String, caption: String, trend: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(NovilaTheme.spacing.xxs)) {
        NovilaText(value, style = NovilaTheme.typography.headlineSmall)
        NovilaText(caption, style = NovilaTheme.typography.bodySmall, color = NovilaTheme.colors.onSurfaceVariant)
        if (trend != null) NovilaText(trend, style = NovilaTheme.typography.bodyMedium)
    }
}

/** What the range is compared with, e.g. "the week before". */
@Composable
private fun beforeText(range: HistoryRange): String = stringResource(
    when (range) {
        HistoryRange.Week -> R.string.history_before_week
        HistoryRange.Month -> R.string.history_before_month
        HistoryRange.Year -> R.string.history_before_year
    },
)

@Composable
private fun sleepTrendText(hours: Float, range: HistoryRange): String {
    val before = beforeText(range)
    return when {
        hours > 0f -> stringResource(R.string.history_sleep_more, hoursText(hours), before)
        hours < 0f -> stringResource(R.string.history_sleep_less, hoursText(abs(hours)), before)
        else -> stringResource(R.string.history_same, before)
    }
}

@Composable
private fun nutritionTrendText(points: Int, range: HistoryRange): String {
    val before = beforeText(range)
    return when {
        points > 0 -> pluralStringResource(R.plurals.history_nutrition_higher, points, points, before)
        points < 0 -> pluralStringResource(R.plurals.history_nutrition_lower, abs(points), abs(points), before)
        else -> stringResource(R.string.history_same, before)
    }
}

/** A label per point: the weekday for a week, the day each week starts for four weeks, the month for a year. */
@Composable
private fun historyLabels(history: History): List<String> = when (history.range) {
    HistoryRange.Week -> weekdayLabels(history.days)
    HistoryRange.Month -> shortDateLabels(history.periods.map { it.start })
    HistoryRange.Year -> monthLabels(history.periods.map { it.start })
}
