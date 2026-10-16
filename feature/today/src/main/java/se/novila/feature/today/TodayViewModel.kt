package se.novila.feature.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import se.novila.data.HealthStore
import se.novila.data.HealthStore.SavedDays
import se.novila.data.Week
import se.novila.movement.MovementRepository
import se.novila.movement.MovementState
import java.time.LocalDate
import java.time.LocalDateTime

/** The dashboard: the week so far and [hour] of day when it was read, for the greeting. */
data class TodayUiState(val week: Week, val hour: Int)

/** The Today tab. Scoped to its entry; reads the same stores as the other tabs, so their changes show up here. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class TodayViewModel(
    private val store: HealthStore,
    private val movementRepository: MovementRepository,
) : ViewModel() {
    private val now = MutableStateFlow(LocalDateTime.now())

    /** The dashboard, or null before the store's first read. */
    val state: StateFlow<TodayUiState?> = combine(now, store.days.filterNotNull()) { now, days -> read(days, now) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, store.days.value?.let { read(it, now.value) })

    val movement: StateFlow<MovementState> = movementRepository.state

    private fun read(days: SavedDays, now: LocalDateTime) = TodayUiState(days.week(now.toLocalDate()), now.hour)

    /**
     * Called on resume, so "today" and the greeting stay correct. Health Connect only allows reads while the app is
     * in the foreground, so movement is read here too.
     */
    fun refresh() {
        now.value = LocalDateTime.now()
        viewModelScope.launch { movementRepository.refresh(LocalDate.now()) }
    }

    fun setMood(mood: Int) = store.update(LocalDate.now()) { it.withMood(mood) }
}
