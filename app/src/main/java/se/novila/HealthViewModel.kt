package se.novila

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

data class HealthUiState(
    val today: DayEntry,
    val history: List<DayEntry>,
)

class HealthViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HealthStore(app)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<HealthUiState> = _state.asStateFlow()

    private fun read(): HealthUiState {
        val days = store.lastDays(count = 7, today = LocalDate.now())
        return HealthUiState(today = days.first(), history = days.drop(1))
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        _state.value = read()
    }

    private fun update(change: (DayEntry) -> DayEntry) {
        val today = store.load(LocalDate.now())
        store.save(change(today))
        refresh()
    }

    fun addWater() = update { it.withWater(+1) }
    fun removeWater() = update { it.withWater(-1) }
    fun setSleep(hours: Float) = update { it.withSleep(hours) }
    fun setMood(mood: Int) = update { it.withMood(mood) }
}
