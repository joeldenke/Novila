package se.novila.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import se.novila.data.GoalsStore
import se.novila.data.ProfileStore
import se.novila.model.Goals
import se.novila.model.Profile
import java.time.LocalDate

/** The saved [goals], and the goals the profile suggests, or null when it has neither age nor weight. */
data class GoalsUiState(val goals: Goals, val suggestion: Goals?)

/** The goals screen. Scoped to its entry. Every change is saved at once, like the other settings. */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class GoalsViewModel(private val store: GoalsStore, private val profileStore: ProfileStore) : ViewModel() {
    private val currentYear = LocalDate.now().year

    /** The goals, or null before the stores' first reads. */
    val state: StateFlow<GoalsUiState?> =
        combine(store.goals, profileStore.profile) { goals, profile -> read(goals, profile) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, read(store.goals.value, profileStore.profile.value))

    private fun read(goals: Goals?, profile: Profile?): GoalsUiState? {
        if (goals == null || profile == null) return null
        return GoalsUiState(goals, Goals.suggested(profile, currentYear).takeIf { Goals.canSuggest(profile) })
    }

    /** The last goals saved from here. Saves are written in the background, so the store may not have them yet. */
    private var lastSaved: Goals? = null

    /** Changes the goals saved last, so quick taps on plus never undo each other. */
    fun change(update: (Goals) -> Goals) = save(update(lastSaved ?: store.goals.value ?: Goals()))

    fun save(goals: Goals) {
        lastSaved = goals
        store.save(goals)
    }
}
