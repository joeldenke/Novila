package se.novila.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import se.novila.model.EditableDays
import java.time.LocalDate

/**
 * The day a screen shows and changes: today, unless the user went back to an earlier one ([EditableDays]). Each
 * ViewModel has its own, so going back on Sleep doesn't move Nutrition. Following today is the default, so the day
 * moves on by itself at midnight ([refresh]); an earlier day stays put.
 */
class SelectedDay(private val now: () -> LocalDate = LocalDate::now) {
    private val _today = MutableStateFlow(now())

    /** Today, as of the last [refresh]. */
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    /** The day the user chose, or null to follow today. */
    private val chosen = MutableStateFlow<LocalDate?>(null)

    /** The shown day, each time it changes. */
    val dates: Flow<LocalDate> = combine(_today, chosen, ::resolve)

    /** The shown day right now. */
    val value: LocalDate get() = resolve(_today.value, chosen.value)

    /** Called on resume, so "today" stays correct after midnight. */
    fun refresh() {
        _today.value = now()
    }

    /** Shows [date], kept within the editable days. Choosing today follows today again. */
    fun select(date: LocalDate) {
        val today = _today.value
        chosen.value = EditableDays.clamp(date, today).takeIf { it < today }
    }

    private fun resolve(today: LocalDate, chosen: LocalDate?) = chosen?.let { EditableDays.clamp(it, today) } ?: today
}
