package se.novila.feature.settings

import androidx.lifecycle.ViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.StateFlow
import se.novila.data.CycleStore
import se.novila.data.SavedCycle

/** *Follow your cycle*, which turns it on or off. Scoped to its entry; the cycle itself is logged on Training. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class CycleViewModel(private val store: CycleStore) : ViewModel() {
    /** Whether the user follows their cycle and what they logged, or null before the store's first read. */
    val cycle: StateFlow<SavedCycle?> = store.cycle

    /** Turns the cycle on, or off, which deletes everything logged. */
    fun setEnabled(enabled: Boolean) = store.setEnabled(enabled)
}
