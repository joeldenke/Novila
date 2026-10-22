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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import se.novila.data.HealthStore
import se.novila.data.HealthStore.SavedDays
import se.novila.data.trainingMinutesByDate
import se.novila.exercise.ExerciseRepository
import se.novila.exercise.ExerciseState
import se.novila.model.History
import se.novila.model.HistoryRange
import java.time.LocalDate

/**
 * Training over the chosen range up to today ([history]), with the [minutes] trained on each date: Novila's own
 * sessions and the watch's workouts, the same walk counted once. [includesWatch] when workouts were read from
 * Health Connect.
 */
data class TrainingTrendsUiState(
    val history: History,
    val minutes: Map<LocalDate, Int>,
    val includesWatch: Boolean,
)

/**
 * The training over time, opened from the Training tab, scoped to its entry. The watch's workouts are the ones the
 * tab already read (the last five weeks); before that, what was logged in Novila counts.
 */
@ViewModelKey
@ContributesIntoMap(AppScope::class)
@Inject
class TrainingTrendsViewModel(
    private val store: HealthStore,
    private val exerciseRepository: ExerciseRepository,
) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())
    private val range = MutableStateFlow(HistoryRange.Week)

    /** The history and the minutes per date, or null before the store's first read. */
    val state: StateFlow<TrainingTrendsUiState?> =
        combine(store.days.filterNotNull(), exerciseRepository.state, today, range, ::read)
            .stateIn(viewModelScope, SharingStarted.Eagerly, initial())

    private fun initial(): TrainingTrendsUiState? {
        val saved = store.days.value ?: return null
        return read(saved, exerciseRepository.state.value, today.value, range.value)
    }

    private fun read(
        saved: SavedDays,
        recorded: ExerciseState,
        today: LocalDate,
        range: HistoryRange,
    ): TrainingTrendsUiState {
        val history = saved.history(range, today)
        val workouts = (recorded as? ExerciseState.Connected)?.exercises
        return TrainingTrendsUiState(
            history = history,
            minutes = trainingMinutesByDate(history.previous + history.days, workouts.orEmpty()),
            includesWatch = workouts != null,
        )
    }

    /** Called on resume so "today" stays correct after midnight. */
    fun refresh() {
        today.value = LocalDate.now()
    }

    /** Shows the training over a week, four weeks or a year. */
    fun selectRange(range: HistoryRange) {
        this.range.value = range
    }
}
