package se.novila.feature.sleep

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import se.novila.data.HealthStore
import se.novila.data.Week
import java.time.LocalDate

/** The Sleep tab, scoped to its entry. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class SleepViewModel(private val store: HealthStore) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    /** The week so far, or null before the store's first read. */
    val week: StateFlow<Week?> = store.weeks(date)
        .stateIn(viewModelScope, SharingStarted.Eagerly, store.week(date.value))

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        date.value = LocalDate.now()
    }

    fun setSleep(hours: Float) = store.update(LocalDate.now()) { it.withSleep(hours) }
}
