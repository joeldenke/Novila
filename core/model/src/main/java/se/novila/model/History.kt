package se.novila.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** How far back a history chart looks, and what each point on it is. */
enum class HistoryRange {
    /** The last seven days, a point per day. */
    Week,

    /** The last four weeks, a point per week of seven days. */
    Month,

    /** The last twelve calendar months, a point per month; the newest one runs up to the shown day. */
    Year,
}

/** One point on a history chart: the days from [start] on, oldest first. */
data class HistoryPeriod(val start: LocalDate, val days: List<DayEntry>)

/**
 * The days up to a date over a [range], split into [periods] (oldest first, the last one ends on that date), and the
 * same number of days just before them ([previous]), so the range can be compared with the one before it.
 */
data class History(val range: HistoryRange, val periods: List<HistoryPeriod>, val previous: List<DayEntry>) {
    /** Every day in the range, oldest first. */
    val days: List<DayEntry> get() = periods.flatMap { it.days }

    /** Average hours per logged night against the range before, rounded to the half hour; null without both. */
    fun sleepTrend(): Float? {
        val now = days.averageSleep() ?: return null
        val before = previous.averageSleep() ?: return null
        return (now - before).roundToHalfHour()
    }

    /** Average nutrition score against the range before, in points; null without both. */
    fun nutritionTrend(goals: Goals = Goals()): Int? {
        val now = days.averageNutrition(goals) ?: return null
        val before = previous.averageNutrition(goals) ?: return null
        return now - before
    }
}

/** A [HistoryRange.Week] history of [week] (oldest first, see [weekOf]), with nothing to compare with. */
fun historyOfWeek(week: List<DayEntry>): History =
    History(HistoryRange.Week, week.map { HistoryPeriod(it.date, listOf(it)) }, previous = emptyList())

/** The history over [range] up to and including [until], each day read with [day]. */
fun historyOf(range: HistoryRange, until: LocalDate, day: (LocalDate) -> DayEntry): History {
    val starts = when (range) {
        HistoryRange.Week -> (6L downTo 0L).map { until.minusDays(it) }
        HistoryRange.Month -> (3L downTo 0L).map { until.minusDays(it * 7 + 6) }
        HistoryRange.Year -> (11L downTo 0L).map { until.withDayOfMonth(1).minusMonths(it) }
    }
    val periods = starts.mapIndexed { i, start ->
        val end = starts.getOrNull(i + 1)?.minusDays(1) ?: until
        HistoryPeriod(start, datesBetween(start, end).map(day))
    }
    val first = starts.first()
    val length = ChronoUnit.DAYS.between(first, until) + 1
    val previous = datesBetween(first.minusDays(length), first.minusDays(1)).map(day)
    return History(range, periods, previous)
}

private fun datesBetween(start: LocalDate, end: LocalDate): List<LocalDate> =
    (0..ChronoUnit.DAYS.between(start, end)).map { start.plusDays(it) }
