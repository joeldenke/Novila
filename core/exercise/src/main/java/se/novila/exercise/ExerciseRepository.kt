package se.novila.exercise

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * The single place the UI gets recorded workouts from. Kept in memory only: nothing from the source is written to
 * disk, so revoking access (or the app's process ending) forgets it.
 * - a temporary failure keeps showing the last reading
 * - when access is gone or the source is missing, the last reading is dropped so old data never shows
 *
 * The source is chosen where the repository is created (AppGraph in :app).
 */
class ExerciseRepository(private val source: ExerciseSource) {
    private val _state = MutableStateFlow<ExerciseState>(ExerciseState.Unavailable)
    val state: StateFlow<ExerciseState> = _state.asStateFlow()

    /** Set when the user declined the permission request, until they grant something. */
    private var declined = false
    private val reading = Mutex()

    /** Reads the [days] up to [today]. Overlapping calls run one at a time, so the latest read wins. */
    suspend fun refresh(today: LocalDate, days: Long = HISTORY_DAYS): Unit = reading.withLock {
        val result = source.read(today.minusDays(days - 1)).getOrElse { return@withLock } // Keep what is shown.
        _state.value = when (result) {
            is ExerciseState.Connected -> {
                declined = false
                result
            }

            is ExerciseState.NeedsPermission -> ExerciseState.NeedsPermission(denied = declined)

            ExerciseState.Unavailable -> result
        }
    }

    /** Result of the permission request: whether the user gave access to anything. */
    suspend fun onPermissionResult(granted: Boolean, today: LocalDate) {
        declined = !granted
        refresh(today)
    }

    companion object {
        /** The program looks at four whole weeks before this one; five weeks covers them on any weekday. */
        const val HISTORY_DAYS = 35L
    }
}
