package se.novila.model

import java.time.LocalDate

/**
 * Which days the user can go back to and change: today and the [DAYS] before it, enough to fill in what was
 * forgotten. Never a day in the future.
 */
object EditableDays {
    const val DAYS = 30L

    fun earliest(today: LocalDate): LocalDate = today.minusDays(DAYS)

    /** [date] moved into the editable range. */
    fun clamp(date: LocalDate, today: LocalDate): LocalDate = date.coerceIn(earliest(today), today)

    fun canGoBack(date: LocalDate, today: LocalDate): Boolean = date > earliest(today)

    fun canGoForward(date: LocalDate, today: LocalDate): Boolean = date < today
}
