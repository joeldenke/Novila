package se.novila.feature.training

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
import se.novila.coach.CoachPreferences
import se.novila.data.CoachStore
import java.time.LocalDate

/**
 * The coach screen: the answers and the plan they give. Scoped to the coach entry, so answers tried out before the
 * coach is set up are forgotten when the user leaves it.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class CoachViewModel(
    training: TrainingRepository,
    private val coachStore: CoachStore,
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())

    /** Answers the user is trying out before the coach is set up. Not saved until they create the program. */
    private val draft = MutableStateFlow<CoachPreferences?>(null)

    /** The answers and their plan, or null before the stores' first read. */
    val state: StateFlow<TrainingUiState?> = training.states(date, draft)
        .stateIn(viewModelScope, SharingStarted.Eagerly, training.current(date.value))

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        date.value = LocalDate.now()
    }

    /** A changed answer. Once the coach is set up it is saved and changes the week's program right away. */
    fun change(preferences: CoachPreferences) {
        if (state.value?.coach?.active == true) coachStore.save(preferences) else draft.value = preferences
    }

    /** Saves the answers, which makes the coach's plan the week's program. */
    fun createProgram() {
        state.value?.let { coachStore.save(it.coach.preferences) }
    }

    /** A new suggestion from the same answers, used right away. */
    fun nextSuggestion() {
        state.value?.let { coachStore.save(it.coach.preferences.nextVariation()) }
    }

    /** Back to the standard program generated from recent workouts. */
    fun reset() {
        coachStore.clear()
        draft.value = null
    }
}
