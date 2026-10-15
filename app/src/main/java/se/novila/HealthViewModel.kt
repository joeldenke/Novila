package se.novila

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import se.novila.healthconnect.HealthConnectSource
import se.novila.healthconnect.MOVEMENT_PERMISSIONS
import se.novila.movement.InMemoryMovementCache
import se.novila.movement.MovementRepository
import se.novila.movement.MovementState
import java.time.LocalDate
import java.time.LocalTime

data class HealthUiState(
    val today: DayEntry,
    val history: List<DayEntry>,
    /** Hour of day when the state was read, for the greeting. */
    val hour: Int,
)

class HealthViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HealthStore(app)
    private val _state = MutableStateFlow(read())
    val state: StateFlow<HealthUiState> = _state.asStateFlow()

    /**
     * Movement for the dashboard and Training. The data source and cache are chosen here and nowhere
     * else: swap HealthConnectSource for another MovementSource, or the cache for a persistent one.
     */
    private val movementRepository = MovementRepository(
        source = HealthConnectSource(app),
        cache = InMemoryMovementCache(),
    )
    val movement: StateFlow<MovementState> = movementRepository.state

    private fun read(): HealthUiState {
        val days = store.lastDays(count = 7, today = LocalDate.now())
        return HealthUiState(today = days.first(), history = days.drop(1), hour = LocalTime.now().hour)
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        _state.value = read()
        refreshMovement()
    }

    /** Health Connect only allows reads while the app is in the foreground, so this runs on resume. */
    private fun refreshMovement() {
        viewModelScope.launch { movementRepository.refresh(LocalDate.now()) }
    }

    /** Result of Health Connect's permission dialog, with the permissions granted so far. */
    fun onMovementPermissionsResult(granted: Set<String>) {
        viewModelScope.launch {
            movementRepository.onPermissionResult(granted.any { it in MOVEMENT_PERMISSIONS }, LocalDate.now())
        }
    }

    private fun update(change: (DayEntry) -> DayEntry) {
        val today = store.load(LocalDate.now())
        store.save(change(today))
        _state.value = read()
    }

    fun addWater() = update { it.withWater(+1) }
    fun removeWater() = update { it.withWater(-1) }
    fun setSleep(hours: Float) = update { it.withSleep(hours) }
    fun setMood(mood: Int) = update { it.withMood(mood) }
}
